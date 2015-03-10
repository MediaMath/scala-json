/*
 * Copyright 2015 MediaMath, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package json.internal

import json._
import json.internal.JSONAnnotations._

import scala.collection.mutable
import scala.language.experimental.macros

import language.experimental.macros
import scala.reflect.api.Names
import scala.reflect.macros.Universe

/** Dummy object to get the right shadowing for 2.10 / 2.11 cross compilation */
private object Compat210 {
  @deprecated("", "")
  object whitebox {
    type Context = scala.reflect.macros.Context
  }
}

import Compat210._

object ObjectAccessorFactory {
  import scala.reflect.macros._ // shadows blackbox from above
  import whitebox._

  def impl[T: c.WeakTypeTag](c: Context)(
    origObjExpr: c.Expr[T]): c.Expr[CaseClassObjectAccessor[T]] = {
    newImpl[T](c)
  }

  def newImpl[T: c.WeakTypeTag](c: Context): c.Expr[CaseClassObjectAccessor[T]] = {
    @deprecated("", "")
    object Shadow210 { //shadow universe methods for 210 compat
      def TermName(s: String) = c.universe.newTermName(s)

      def termNames = c.universe.nme

      implicit class SymbolExt(s: c.universe.ClassSymbol) {
        def companion = s.companionSymbol
      }

      implicit class MethodSymbolExt(s: c.universe.MethodSymbol) {
        def paramLists = s.paramss
      }

      implicit class AnnotationExt(a: c.universe.Annotation) {
        object tree {
          def tpe = a.tpe
          object children {
            def tail = a.scalaArgs
          }
        }
      }
    }

    import Shadow210._
    {
      import c.universe._ //shadow old Shadow210 compat methods

      //val typ = typ0.asInstanceOf[Type]
      val typ0 = implicitly[c.WeakTypeTag[T]].tpe

      val clsSymbol = typ0.typeSymbol.asClass
      val module = clsSymbol.companion.asModule
      val moduleTypeSig = module.typeSignature

      def valTermExpr[X](field: String) = c.Expr[X](Ident(TermName(field)))

      val objXpr = valTermExpr[T]("obj")
      val obj0Xpr = valTermExpr[Any]("obj")
      val xXpr = valTermExpr[Any]("x")
      val xJValue = valTermExpr[JValue]("x")
      val inputExceptionsXpr = valTermExpr[mutable.Buffer[InputFormatException]]("inputExceptions")

      val seqApply = Select(Ident(TermName("IndexedSeq")),
        TermName("apply"))

      val classAnnos = typ0.typeSymbol.asClass.annotations filter
        (_.tree.tpe <:< typeOf[ObjectAccessorAnnotation])

      val nameConversionAnno = typ0.typeSymbol.annotations filter
        (_.tree.tpe <:< typeOf[NameConversionGeneric])

      val nameConversion = nameConversionAnno.headOption match {
        case None => reify({ s: String => s }).tree
        case Some(anno) => Apply(
          Select(
            New(TypeTree(anno.tree.tpe)),
            termNames.CONSTRUCTOR
          ),
          anno.tree.children.tail
        )
      }
      val nameConversionExpr = c.Expr[String => String](nameConversion)

      def classExpr(t: Type) = c.Expr[Class[_]](
        TypeApply(Select(Ident(TermName("Predef")),
          TermName("classOf")), List(TypeTree(t))))

      def methodWithMostArgs(methodName: String): MethodSymbol = {
        val mm = moduleTypeSig.member(TermName(methodName))
        val sorted = mm.asTerm.alternatives.sortBy(
          -_.asMethod.paramLists.head.length)

        sorted.head.asMethod
      }

      case class MemberInfo(name: c.Expr[String],
        preConvName: c.Expr[String],
        getter: c.Expr[Any], // of obj: T
        default: Option[c.Expr[Any]], typ: Type, origName: Name)

      val methodName = "apply"
      val applyMethod = methodWithMostArgs(methodName)

      val objMfExpr = classExpr(typ0)

      val memberAnnos = (for {
        mem <- typ0.members
        anno <- mem.annotations
        if anno.tree.tpe <:< typeOf[FieldAccessorAnnotation]
      } yield mem.name -> anno) ++ (for {
        paramSet <- applyMethod.paramLists
        param <- paramSet
        anno <- param.annotations
        if anno.tree.tpe <:< typeOf[FieldAccessorAnnotation]
      } yield param.name -> anno)

      val memberInfo: Seq[MemberInfo] = {
        //scan for annotations that show we should use a
        //different field name
        val methodFieldName: Map[Name, Tree] = (for {
          (name, anno) <- memberAnnos.iterator
          if anno.tree.tpe =:= typeOf[JSONFieldNameGeneric]
          field = anno.tree.children.tail(0)
        } yield name -> field).toMap

        def defaultFor(idx: Int): Option[Tree] = {
          val argTerm = s"$methodName$$default$$${idx}"
          val defarg = moduleTypeSig member TermName(argTerm)

          if (defarg != NoSymbol) Some(Select(Ident(module.asTerm), defarg))
          else None
        }

        for {
          methodSymbolList <- applyMethod.paramLists
          (symbol, idx) <- methodSymbolList.zipWithIndex
          default = defaultFor(idx + 1).map(c.Expr[Any](_))
          originalName = symbol.name
          fieldNameTree = methodFieldName.getOrElse(originalName,
            Literal(Constant(originalName.decodedName.toString.trim)))
          nameExpr = c.Expr[String](fieldNameTree)
          convdNameExpr = reify(nameConversionExpr.splice(nameExpr.splice))
          typeSig = symbol.typeSignature
          getExpr = c.Expr[Any](Select(objXpr.tree, originalName))
        } yield MemberInfo(convdNameExpr, nameExpr, getExpr, default, typeSig, originalName)
      }

      val accessorTrees = memberInfo.toList map { info =>
        val goodGetter = DefDef( // def getFrom(obj: T): Any
          Modifiers(),
          TermName("getFrom"),
          Nil,
          List(List(
            ValDef(Modifiers(), TermName("obj"),
              TypeTree(typ0), EmptyTree)
          )),
          TypeTree(info.typ),
          //Ident(newTypeName("U")),
          Select(Ident(TermName("obj")), info.origName)
        )

        val typeIdent: Tree = TypeTree(info.typ)
        val typeExpr = c.Expr[AnyRef](typeIdent)

        val jvExpr = c.Expr[Any](Select(Ident(TermName("obj")), info.origName))

        //jsTree function of obj: T
        val accExpr = c.Expr[JSONAccessor[Any]](TypeApply(
          Select(Ident(TermName("JSONAccessor")),
            TermName("of")),
          List(TypeTree(info.typ))
        ))

        val jsTree = reify {
          accExpr.splice.createJSON(jvExpr.splice)
        }

        val getJValueExpr = DefDef( // def getJValue(obj: T): JValue
          Modifiers(),
          TermName("getJValue"),
          Nil,
          List(List(
            ValDef(Modifiers(), TermName("obj"),
              TypeTree(typ0), EmptyTree)
          )),
          TypeTree(typeOf[JValue]),
          jsTree.tree
        )

        val mfExpr = classExpr(info.typ)

        val pTypeManifests = (info.typ match {
          case TypeRef(_, _, args) => args
        }).map(classExpr(_).tree)

        val defOptExpr = info.default match {
          case Some(xpr) => reify(Some(xpr.splice))
          case None      => reify(None)
        }

        val annos = for {
          (name, anno) <- memberAnnos
          if name == info.origName
        } yield anno

        //arguments for annotations
        val annoArgs = annos map { anno =>
          Apply(
            Select(
              New(TypeTree(anno.tree.tpe)),
              termNames.CONSTRUCTOR
            ),
            anno.tree.children.tail
          )
        }

        val accSeqTrees = (info.typ match {
          case TypeRef(_, _, args) => args
        }) map { pt =>
          TypeApply(
            Select(Ident(TermName("JSONAccessor")),
              TermName("of")),
            List(TypeTree(pt))
          )
        }

        val pTypeManifestExpr = c.Expr[IndexedSeq[Manifest[_]]](
          Apply(seqApply, pTypeManifests.toList))
        //create seq of field accessor annotations
        val annosSeqExpr = c.Expr[Seq[FieldAccessorAnnotation]](Apply(seqApply, annoArgs.toList))
        val accSeqExpr = c.Expr[IndexedSeq[JSONAccessor[Any]]](
          Apply(seqApply, accSeqTrees.toList))

        (reify {
          new FieldAccessor[T] {
            //U is field type
            val name: String = info.name.splice

            val annos: Set[FieldAccessorAnnotation] = annosSeqExpr.splice.toSet

            //val pTypeManifests: IndexedSeq[Manifest[_]] = pTypeManifestExpr.splice
            val pTypeAccessors = //: IndexedSeq[Option[ValueAccessor[_]]]
              accSeqExpr.splice.map(Some(_))

            //def getFrom(obj: T): Any = info.getter.splice
            def defOpt: Option[Any] = defOptExpr.splice

            c.Expr(getJValueExpr).splice

            c.Expr(goodGetter).splice

            /*val fieldManifest: Manifest[Any] =
              mfExpr.splice.asInstanceOf[Manifest[Any]]*/
            val objClass: Class[T] =
              objMfExpr.splice.asInstanceOf[Class[T]]

            val fieldAccessor = accExpr.splice.asInstanceOf[JSONAccessor[T]]
          }
        }).tree
      }

      def deSerFrom(jval: c.Expr[JValue]): c.Expr[T] = {
        require(!memberInfo.isEmpty, "memberinfo empty for " + typ0)

        val lastInfo = memberInfo(memberInfo.length - 1)
        val trees = memberInfo.toList map { info =>
          val isLast = info == lastInfo

          val checkLastExpr = if (isLast) reify {
            if (!inputExceptionsXpr.splice.isEmpty) {
              val set = inputExceptionsXpr.splice.flatMap(_.getExceptions).toSet
              throw InputFormatsException(set)
            }
          }
          else reify(Unit)

          val defOptExpr = info.default match {
            case Some(xpr) => reify(Some(xpr.splice))
            case None      => reify(None)
          }

          val accExpr = c.Expr[JSONAccessor[Any]](TypeApply(
            Select(Ident(TermName("JSONAccessor")),
              TermName("of")),
            List(TypeTree(info.typ))
          ))

          //really hacky, but return a valid 'null' until the last field, then throw
          //the actual list of exceptions
          val typedNull = info.typ match {
            case t if t <:< typeOf[AnyRef]  => reify(null)
            case t if t =:= typeOf[Int]     => reify(0)
            case t if t =:= typeOf[Float]   => reify(0f)
            case t if t =:= typeOf[Long]    => reify(0L)
            case t if t =:= typeOf[Double]  => reify(0.0)
            case t if t =:= typeOf[Short]   => reify(0.toShort)
            case t if t =:= typeOf[Boolean] => reify(false)
            case t                          => sys.error("Unkown default type for " + t)
          }

          (if (info.typ <:< typeOf[Option[Any]]) reify {
            val defOpt = defOptExpr.splice

            val b = (jval.splice.apply(JString(info.name.splice)): JValue) match {
              case JUndefined if defOpt.isDefined =>
                defOpt.get
              case JNull      => None
              case JUndefined => None //TODO: this should throw an error
              case jv => try accExpr.splice.fromJSON(jv) catch {
                case e: InputFormatException =>
                  inputExceptionsXpr.splice += e.prependFieldName(info.name.splice)
                  typedNull.splice
              }
            }

            checkLastExpr.splice

            b
          }
          else reify {
            val defOpt = defOptExpr.splice

            val b = (jval.splice.apply(JString(info.name.splice)): JValue) match {
              case j if j.isNullOrUndefined && defOpt.isDefined =>
                defOpt.get
              case j if j.isNullOrUndefined =>
                val e = new MissingFieldException(info.name.splice)
                inputExceptionsXpr.splice += e
                typedNull.splice
              case jv => try accExpr.splice.fromJSON(jv) catch {
                case e: InputFormatException =>
                  inputExceptionsXpr.splice += e.prependFieldName(info.name.splice)
                  typedNull.splice
              }
            }

            checkLastExpr.splice

            b
          }).tree
        }

        c.Expr[T](Apply(Select(Ident(module),
          TermName("apply")), trees))
      }

      val keysArgs = memberInfo.map(_.name.tree).toList
      val keysExpr = c.Expr[Seq[String]](Apply(seqApply, keysArgs))

      val fieldsExpr = c.Expr[Any](
        Apply(seqApply, accessorTrees))

      val deSerExpr = deSerFrom(xJValue)

      reify {
        import json._

        val finalAcc = new CaseClassObjectAccessor[T] {
          val nameMap = nameConversionExpr.splice

          val fields: IndexedSeq[FieldAccessor[T]] =
            fieldsExpr.splice.asInstanceOf[IndexedSeq[FieldAccessor[T]]]

          val clazz: Class[T] = objMfExpr.splice.asInstanceOf[Class[T]]

          def fromJSON(_x: JValue): T = {
            val x = _x.jObject
            val inputExceptions = mutable.Buffer[InputFormatException]()

            deSerExpr.splice
          }
        }

        finalAcc: CaseClassObjectAccessor[T]
      }
    }
  }
}
