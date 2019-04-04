/*
 * Copyright 2016 MediaMath, Inc
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
  import json.exceptions.{MissingFieldException, InputFormatsException, InputFormatException}

  def annotation_impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    @deprecated("", "")
    object Shadow210 { //any way to avoid having this copy-pasted twice?
      def TermName(s: String) = c.universe.newTermName(s)

      def TypeName(s: String) = c.universe.newTypeName(s)

      val termNames = c.universe.nme

      def noSelfType = c.universe.emptyValDef

      def pendingSuperCall = {
        import c.universe._

        Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), Nil)
      }

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

      val inputs = annottees.map(_.tree).toList

      val classDef = inputs.collect {
        case x: ClassDef => x
      }.headOption.getOrElse(sys.error("@accessor must be run on class!"))

      val moduleOpt = inputs.collect {
        case x: ModuleDef => x
      }.headOption

      val simpleName = classDef.name.decodedName.toString

      val defaultModuleCons = DefDef(Modifiers(), termNames.CONSTRUCTOR, List(), List(List()),
        TypeTree(), Block(List(pendingSuperCall), Literal(Constant(()))))
      val defaultModule = ModuleDef(Modifiers(), TermName(simpleName),
        Template(List(Select(Ident(TermName("scala")), TypeName("AnyRef"))), noSelfType, List(defaultModuleCons)))

      val moduleDef = moduleOpt.getOrElse(defaultModule)

      //TODO: change accessor name to something else maybe?
      //TODO: allow somebody to place a stub accessor with ??? and we can swap it out. will still be named and seen by ide etc
      val accessorImplTree: c.Tree = ValDef(
        Modifiers(Flag.IMPLICIT),
        TermName("acc"),
        TypeTree(),
        TypeApply(
          Select(Select(Ident(TermName("json")), TermName("ObjectAccessor")), TermName("create")),
          List(Ident(classDef.name))
        )
      )

      val newModule = moduleDef match {
        case ModuleDef(mods, name, Template(parents, selfType, impl)) =>
          ModuleDef(mods, name, Template(parents, selfType, accessorImplTree :: impl))
      }

      c.Expr[Any](Block(List(newModule, classDef), Literal(Constant(()))))
    }
  }

  def impl[T: c.WeakTypeTag](c: Context): c.Expr[CaseClassObjectAccessor[T]] = {
    @deprecated("", "")
    object Shadow210 {
      def TermName(s: String) = c.universe.newTermName(s)

      object TypeName {
        def apply(s: String) = c.universe.newTypeName(s)
        def unapply(x: c.universe.TypeName): Option[String] = Some(x.decodedName.toString)
      }

      val termNames = c.universe.nme

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

      case class MemberInfo(name: c.Expr[String],
          preConvName: c.Expr[String],
          getter: c.Expr[Any], // of obj: T
          default: Option[c.Expr[Any]], typ: Type, origName: Name, ephemeral: Boolean)

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

      val seqApply = Select(Ident(TermName("IndexedSeq")), TermName("apply"))

      val classAnnos = typ0.typeSymbol.asClass.annotations filter
        (_.tree.tpe <:< typeOf[ObjectAccessorAnnotation])

      val nameConversionAnno = typ0.typeSymbol.annotations filter
        (_.tree.tpe <:< typeOf[NameConversionGeneric])

      val nameConversion = nameConversionAnno.headOption match {
        case None => reify {s: String => s}.tree
        case Some(anno) =>
          println(showRaw(anno))
          val outTree = anno.tree.children.tail.head
          println(showRaw(outTree))
          println(show(outTree))
          outTree
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

      def getAccessorFor(typ: Type) = c.Expr[JSONAccessor[Any]](TypeApply(
        Select(Ident(TermName("json")), TermName("accessorOf")),
        List(TypeTree(typ))
      ))

      val methodName = "apply"
      val applyMethod = methodWithMostArgs(methodName)

      val objMfExpr = classExpr(typ0)

      val memberAnnos = (for {
        mem <- typ0.members
        anno <- mem.annotations
        if anno.tree.tpe <:< typeOf[JSONAnnotation]
      } yield mem.name -> anno) ++ (for {
        paramSet <- applyMethod.paramLists
        param <- paramSet
        anno <- param.annotations
        if anno.tree.tpe <:< typeOf[JSONAnnotation]
      } yield param.name -> anno)

      //scan for annotations that show we should use a
      //different field name
      val methodFieldName: Map[Name, Tree] = (for {
        (name, anno) <- memberAnnos.iterator
        if anno.tree.tpe =:= typeOf[FieldNameGeneric]
        field = anno.tree.children.tail(0)
      } yield name -> field).toMap

      val ephemeralFieldsEnclosed: List[Symbol] = for {
        unit <- c.enclosingRun.units.toList
        classDef <- unit.body.collect {
          case x: ClassDef if x.name == typ0.typeSymbol.name => x //println(showRaw(x)); x
        }
        theDef <- classDef.impl.body.collect {
          case x: ValOrDefDef => x
        }
        //method annotations arent always available yet, so we have to search the tree
        if theDef.mods.annotations exists {
          case Apply(Select(New(Ident(TypeName("ephemeral"))), termNames.CONSTRUCTOR), Nil) => true
          case Apply(Select(New(Ident(TypeName("EphemeralGeneric"))), termNames.CONSTRUCTOR), Nil) => true
          case Apply(Select(New(Select(_, TypeName("ephemeral"))), termNames.CONSTRUCTOR), Nil) => true
          case Apply(Select(New(Select(_, TypeName("EphemeralGeneric"))), termNames.CONSTRUCTOR), Nil) => true
          case _ => false
        }
      } yield typ0.member(theDef.name)

      val ephemeralFieldNames: List[Symbol] = for {
        mem <- typ0.members.toList
        anno <- mem.annotations
        if anno.tree.tpe =:= typeOf[EphemeralGeneric]
      } yield mem

      //de-dup
      val ephemeralFields = (ephemeralFieldNames ++ ephemeralFieldsEnclosed).toSet.toList

      val memberInfo: Seq[MemberInfo] = {
        def defaultFor(idx: Int): Option[Tree] = {
          val argTerm = s"$methodName$$default$$${idx}"
          val defarg = moduleTypeSig member TermName(argTerm)

          if (defarg != NoSymbol) Some(Select(Ident(module.asTerm), defarg))
          else None
        }

        for {
          methodSymbolList <- applyMethod.paramLists
          (symbol, idx) <- (methodSymbolList ++ ephemeralFields).zipWithIndex
          default = defaultFor(idx + 1).map(c.Expr[Any](_))
          originalName = symbol.name
          fieldNameTree = methodFieldName.getOrElse(originalName,
            Literal(Constant(originalName.decodedName.toString.trim)))
          nameExpr = c.Expr[String](fieldNameTree)
          convdNameExpr = reify(nameConversionExpr.splice(nameExpr.splice))
          typeSig = symbol.typeSignature match {
            case NullaryMethodType(tpe) => tpe //reduce out nullary methods
            case x => x
          }
          ephemeral = ephemeralFields contains symbol
          getExpr = c.Expr[Any](Select(objXpr.tree, originalName))
        } yield MemberInfo(convdNameExpr, nameExpr, getExpr, default, typeSig, originalName, ephemeral = ephemeral)
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
          Select(Ident(TermName("obj")), info.origName)
        )

        val typeIdent: Tree = TypeTree(info.typ)
        val typeExpr = c.Expr[AnyRef](typeIdent)

        //jsTree function of obj: T
        val accExpr = getAccessorFor(info.typ)

        val mfExpr = classExpr(info.typ)

        val pTypeManifests = (info.typ match {
          case TypeRef(_, _, args) => args
        }).map(classExpr(_).tree)

        val defOptExpr = info.default match {
          case Some(xpr) => reify(Some(xpr.splice))
          case None      => reify(None)
        }

        //arguments for annotations
        //TODO: needs full trees for anno args, including hacky classtag ones above
        val annoArgs = for {
          (name, anno) <- memberAnnos
          if name == info.origName
        } yield Apply(
          Select(
            New(TypeTree(anno.tree.tpe)),
            termNames.CONSTRUCTOR
          ),
          anno.tree.children.tail
        )

        val pTypeManifestExpr = c.Expr[IndexedSeq[Manifest[_]]](
          Apply(seqApply, pTypeManifests.toList))

        //create seq of field accessor annotations
        val annosSeqExpr = c.Expr[Seq[FieldAccessorAnnotation]](Apply(seqApply, annoArgs.toList))

        (reify {
          new FieldAccessor[T, Any] {
            val name: String = info.name.splice

            val annos: Set[FieldAccessorAnnotation] = annosSeqExpr.splice.toSet

            def defOpt: Option[Any] = defOptExpr.splice

            c.Expr(goodGetter).splice

            val objClass: Class[T] = objMfExpr.splice.asInstanceOf[Class[T]]

            val fieldAccessor = accExpr.splice.asInstanceOf[JSONAccessor[Any]]
          }
        }).tree
      }

      def deSerFrom(jval: c.Expr[JValue]): c.Expr[T] = {
        require(!memberInfo.isEmpty, "memberinfo empty for " + typ0)

        val lastInfo = memberInfo.filter(_.ephemeral == false).last
        //only deser non-ephemeral fields
        val trees = memberInfo.toList.filter(_.ephemeral == false) map { info =>
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

          val accExpr = getAccessorFor(info.typ)

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
            case t if t <:< typeOf[Any]     => reify(null)
            case t                          => sys.error("Unkown default type for " + t)
          }

          //special 'Option' handling
          if (info.typ <:< typeOf[Option[Any]]) reify {
            val defOpt = defOptExpr.splice

            val b = (jval.splice.apply(JString(info.name.splice)): JValue) match {
              case JUndefined if defOpt.isDefined => defOpt.get
              case JNull      => None
              case JUndefined => None
              case jv => try accExpr.splice.fromJSON(jv) catch {
                case e: InputFormatException =>
                  inputExceptionsXpr.splice += e.prependFieldName(info.name.splice)
                  typedNull.splice
              }
            }

            checkLastExpr.splice

            b
          }.tree else reify {
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
          }.tree
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

        (new json.internal.CaseClassObjectAccessor[T] {
          val nameMap = nameConversionExpr.splice

          val fields: IndexedSeq[FieldAccessor[T, _]] =
            fieldsExpr.splice.asInstanceOf[IndexedSeq[FieldAccessor[T, _]]]

          val clazz: Class[T] = objMfExpr.splice.asInstanceOf[Class[T]]

          def fromJSON(_x: JValue): T = {
            val x = _x.jObject
            val inputExceptions = mutable.Buffer[InputFormatException]()

            deSerExpr.splice
          }
        }: json.internal.CaseClassObjectAccessor[T])
      }
    }
  }
}