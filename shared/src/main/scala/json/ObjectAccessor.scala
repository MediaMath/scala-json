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

package json

import json.internal.JSONAnnotations.{ FieldDescriptionGeneric, FieldAccessorAnnotation }
import json.internal.ObjectAccessorFactory

import scala.language.experimental.macros
import scala.annotation.{ implicitNotFound, ClassfileAnnotation }
import scala.reflect.{ClassTag, ManifestFactory, classTag}


//case class FieldAccessor[T](name: String, getFrom: T => Any)
trait FieldAccessor[T] extends Product2[Class[T], String] {
  def name: String
  def getFrom(obj: T): Any
  def defOpt: Option[Any]
  def getJValue(obj: T): JValue
  //def valueFromJValue(jval: JValue): Any

  def annos: Set[FieldAccessorAnnotation]
  //def pTypeClasss: IndexedSeq[Class[_]]
  def pTypeAccessors: IndexedSeq[Option[TypedValueAccessor[_]]]

  //def fieldClass: Class[_]
  def fieldAccessor: JSONAccessor[T]
  def objClass: Class[T]

  def default: Any = defOpt.get
  def hasDefault: Boolean = defOpt.isDefined
  def fieldClass = fieldAccessor.clazz

  def _1 = objClass
  def _2 = name

  def canEqual(that: Any) = that.isInstanceOf[FieldAccessor[_]]

  override def equals(that: Any) = that match {
    case x: FieldAccessor[T] =>
      x.objClass == objClass && x.name == name
    case _ => false
  }

  override def productPrefix = "FieldAccessor("
  override def toString = productPrefix + productIterator.mkString(", ") + ")"
  override def hashCode = scala.runtime.ScalaRunTime._hashCode(this)

  def createSwaggerProperty: JObject = {
    val defaultJs = defOpt.map(JValue(_)) match {
      case Some(JObject(_)) => JObject.empty
      case Some(JArray(_))  => JObject.empty
      case Some(x)          => JObject("defaultValue".js -> x)
      case _                => JObject.empty
    }

    val desc = annos.map {
      case FieldDescriptionGeneric(desc) => desc
      case _                             => ""
    }.mkString("\n")

    val requiredJs = if (hasDefault) JObject("required".js -> JFalse)
    else JObject.empty

    fieldAccessor.createSwaggerProperty ++ defaultJs ++ requiredJs ++ JObject(
      "description".js -> desc.js
    )
  }

  def createSwagger: JValue = try {
    val accProp = createSwaggerProperty

    JObject(name.js -> accProp)
  } catch {
    case e: NullPointerException => {
      JObject(name.js -> "null???".js)
    }
  }
}

trait CaseClassObjectAccessor[T] extends ObjectAccessor[T] with JSONProducer[T, JObject] {

  def createJSON(obj: T): JObject = JObject((fields map { field =>
    field.name ->> field.getJValue(obj)
  }): _*)

  def fields: IndexedSeq[FieldAccessor[T]]
  //function used to create field name from member name
  def nameMap: String => String

  override def requiresObject = true

  lazy val fieldMap = fields.map(field => field.name -> field).toMap

  def getValue(obj: T, key: String) = fieldMap(key).getFrom(obj)
  //def createObject(from: JObject): T

  def createJSON(from: JValue): T = fromJSON(from)

  def createSwaggerModels: Seq[JObject] = {
    val properties = fields.map(_.createSwagger.toJObject).foldLeft(JObject())(_ ++ _)

    //TODO: need annos for root class
    val description = ""

    val id = clazz.getSimpleName
    val extras = fields.flatMap(_.fieldAccessor.extraSwaggerModels)

    val jv = JObject(
      "id".js -> id.js,
      "description".js -> description.js,
      //"required".js -> required,
      "properties".js -> properties
    )

    extras :+ JObject(id.js -> jv)
  }
}

object CaseClassObjectAccessor {
  def of[T <: Product](implicit acc: CaseClassObjectAccessor[T]) = acc
}

trait JSONReader[T] extends TypedValueAccessor[T] {
  def fromJSON(js: JValue): T
}

trait JSONProducer[-T, +JV <: JValue] extends TypedValueAccessor[T] {
  def createJSON(obj: T): JV
}

object JSONAccessor {
  def of[T](implicit acc: json.JSONAccessor[T]) = acc

  def create[T: ClassTag, U <: JValue](toJ: T => U,
    fromJ: JValue => T) = new JSONAccessorProducer[T, U] {
    def createJSON(from: T): U = toJ(from)
    def fromJSON(from: JValue): T = fromJ(from)
    def clazz = classTag[T].runtimeClass
    //def fields: IndexedSeq[FieldAccessor[T]] = Nil.toIndexedSeq
  }
}

//trait JSONAccessor[T] extends JSONProducer[T, JValue] with JSONReader[T] {
trait JSONAccessorProducer[T, +JV <: JValue] extends JSONProducer[T, JV] with JSONReader[T] {
  //type SourceType = T

  def createSwaggerProperty: JObject = {
    val StrClassTag = ClassTag(classOf[String])

    val dat = (ClassTag(clazz): ClassTag[_]) match {
      case ClassTag.Int =>
        Map("type" -> "integer", "format" -> "int32")
      case ClassTag.Long =>
        Map("type" -> "long", "format" -> "int64")
      case ClassTag.Float =>
        Map("type" -> "number", "format" -> "float")
      case ClassTag.Double =>
        Map("type" -> "number", "format" -> "double")
      case StrClassTag =>
        Map("type" -> "string")
      case ClassTag.Byte =>
        Map("type" -> "string", "format" -> "byte")
      case ClassTag.Boolean =>
        Map("type" -> "boolean")
      //case x if x == classOf[Date] => Map("type" -> "string", "format" -> "date")
      //case x if x == classOf[DateTime] => Map("type" -> "string", "format" -> "date-time")
      case x => Map("type" -> x.runtimeClass.getSimpleName)
    }

    JValue(dat).toJObject + (JString("required") -> JTrue)
  }

  override def toString = s"JSONAccessorProducer[${clazz.getName}, _]"

  def extraSwaggerModels: Seq[JObject] = Nil
}

trait TypedValueAccessor[-T] { //extends Class[T] {
  def clazz: Class[_]

  override def toString = s"TypedValueAccessor[${clazz.getName}]"
}

trait ObjectAccessor[T] extends JSONAccessorProducer[T, JObject] {
  def clazz: Class[_]
  def createJSON(obj: T): JObject

  def requiresObject = false

  def canEqual(that: Any) = that.isInstanceOf[ObjectAccessor[_]]

  override def equals(that: Any) = that match {
    case x: ObjectAccessor[_] => x.clazz == clazz
    case _                    => false
  }

  override def toString = s"ObjectAccessor[${clazz.getName}]"
  override def hashCode = clazz.hashCode
}

object ObjectAccessor {
  case object NoAccessor extends ObjectAccessor[Nothing] {
    def fields: IndexedSeq[FieldAccessor[Nothing]] = Nil.toIndexedSeq
    def clazz: Class[Nothing] = classOf[Nothing]
    def fromJSON(from: JValue): Nothing = sys.error("Cannot create Nothing object")
    def createJSON(obj: Nothing): JObject = sys.error("Cannot create Nothing json")

    override def canEqual(that: Any) = that.isInstanceOf[this.type]

    def apply[T] = NoAccessor.asInstanceOf[ObjectAccessor[T]]
  }

  def createFor[T <: Product](origObjExpr: T): CaseClassObjectAccessor[T] = macro ObjectAccessorFactory.impl[T]

  def of[T <: Product]: CaseClassObjectAccessor[T] = macro ObjectAccessorFactory.newImpl[T]

  def create[T: ClassTag](toJ: T => JObject,
    fromJ: JValue => T) = new ObjectAccessor[T] {
    def createJSON(from: T): JObject = toJ(from)
    def fromJSON(from: JValue): T = fromJ(from)
    def clazz = classTag[T].runtimeClass
    def fields: IndexedSeq[FieldAccessor[T]] = Nil.toIndexedSeq
  }

  @implicitNotFound(msg = "No implicit ObjectAccessor for ${T} in scope. Did you define/import one?")
  def accessorFor[T](obj: T)(implicit acc: ObjectAccessor[T]): ObjectAccessor[T] = acc

  @implicitNotFound(msg = "No implicit ObjectAccessor for ${T} in scope. Did you define/import one?")
  def accessorOf[T](implicit acc: ObjectAccessor[T]): ObjectAccessor[T] = acc

  @implicitNotFound(msg = "No implicit CaseClassObjectAccessor for ${T} in scope. Did you define/import one?")
  def caseClassAccessorOf[T](implicit acc: CaseClassObjectAccessor[T]): CaseClassObjectAccessor[T] = acc

  /*def accessorOfOpt[T](implicit acc: ObjectAccessor[T] = NoAccessor[T]
			): Option[ObjectAccessor[T]] =
        if(acc == NoAccessor) None else Some(acc)*/
}

