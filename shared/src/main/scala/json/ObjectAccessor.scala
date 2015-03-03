package json

import json.internal.JSONAnnotations.{ FieldDescriptionGeneric, FieldAccessorAnnotation }
import json.internal.ObjectAccessorFactory

import scala.reflect.macros.Context
import scala.language.experimental.macros
import scala.annotation.{ implicitNotFound, ClassfileAnnotation }

//case class FieldAccessor[T](name: String, getFrom: T => Any)
trait FieldAccessor[T] extends Product2[Manifest[T], String] {
  def name: String
  def getFrom(obj: T): Any
  def defOpt: Option[Any]
  def getJValue(obj: T): JValue
  //def valueFromJValue(jval: JValue): Any

  def annos: Set[FieldAccessorAnnotation]
  //def pTypeManifests: IndexedSeq[Manifest[_]]
  def pTypeAccessors: IndexedSeq[Option[TypedValueAccessor[_]]]

  //def fieldManifest: Manifest[_]
  def fieldAccessor: JSONAccessor[T]
  def objManifest: Manifest[T]

  def default: Any = defOpt.get
  def hasDefault: Boolean = defOpt.isDefined
  def fieldManifest = fieldAccessor.manifest

  def _1 = objManifest
  def _2 = name

  def canEqual(that: Any) = that.isInstanceOf[FieldAccessor[_]]

  override def equals(that: Any) = that match {
    case x: FieldAccessor[T] =>
      x.objManifest == objManifest && x.name == name
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

    val id = manifest.runtimeClass.getSimpleName
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

trait JSONProducer[T, +JV <: JValue] extends TypedValueAccessor[T] {
  def createJSON(obj: T): JV
}

object JSONAccessor {
  def of[T](implicit acc: json.JSONAccessor[T]) = acc

  def create[T, U <: JValue](toJ: T => U,
    fromJ: JValue => T)(implicit m: Manifest[T]) = new JSONAccessorProducer[T, U] {
    def createJSON(from: T): U = toJ(from)
    def fromJSON(from: JValue): T = fromJ(from)
    def manifest = m
    //def fields: IndexedSeq[FieldAccessor[T]] = Nil.toIndexedSeq
  }
}

//trait JSONAccessor[T] extends JSONProducer[T, JValue] with JSONReader[T] {
trait JSONAccessorProducer[T, +JV <: JValue] extends JSONProducer[T, JV] with JSONReader[T] {
  //type SourceType = T

  def createSwaggerProperty: JObject = {
    val dat = manifest.runtimeClass match {
      case x if x == classOf[Int] =>
        Map("type" -> "integer", "format" -> "int32")
      case x if x == classOf[Long] =>
        Map("type" -> "long", "format" -> "int64")
      case x if x == classOf[Float] =>
        Map("type" -> "number", "format" -> "float")
      case x if x == classOf[Double] =>
        Map("type" -> "number", "format" -> "double")
      case x if x == classOf[String] =>
        Map("type" -> "string")
      case x if x == classOf[Byte] =>
        Map("type" -> "string", "format" -> "byte")
      case x if x == classOf[Boolean] =>
        Map("type" -> "boolean")
      //case x if x == classOf[Date] => Map("type" -> "string", "format" -> "date")
      //case x if x == classOf[DateTime] => Map("type" -> "string", "format" -> "date-time")
      case x => Map("type" -> x.getSimpleName)
    }

    JValue(dat).toJObject + (JString("required") -> JTrue)
  }

  def extraSwaggerModels: Seq[JObject] = Nil
}

trait TypedValueAccessor[T] { //extends Manifest[T] {
  def manifest: Manifest[T]

  def runtimeClass: Class[_] = manifest.runtimeClass
}

trait ObjectAccessor[T] extends JSONAccessorProducer[T, JObject] {
  def manifest: Manifest[T]
  def createJSON(obj: T): JObject

  def requiresObject = false

  def canEqual(that: Any) = that.isInstanceOf[ObjectAccessor[_]]

  override def equals(that: Any) = that match {
    case x: ObjectAccessor[_] => x.manifest == manifest
    case _                    => false
  }

  override def toString = "ObjectAccessor(" + manifest + ")"
  override def hashCode = manifest.hashCode
}

object ObjectAccessor {
  case object NoAccessor extends ObjectAccessor[Nothing] {
    def fields: IndexedSeq[FieldAccessor[Nothing]] = Nil.toIndexedSeq
    def manifest: Manifest[Nothing] = manifestOf[Nothing]
    def fromJSON(from: JValue): Nothing = sys.error("Cannot create Nothing object")
    def createJSON(obj: Nothing): JObject = sys.error("Cannot create Nothing json")

    override def canEqual(that: Any) = that.isInstanceOf[this.type]

    def apply[T] = NoAccessor.asInstanceOf[ObjectAccessor[T]]
  }

  def createFor[T <: Product](origObjExpr: T): CaseClassObjectAccessor[T] = macro ObjectAccessorFactory.impl[T]

  def of[T <: Product]: CaseClassObjectAccessor[T] = macro ObjectAccessorFactory.newImpl[T]

  def create[T: Manifest](toJ: T => JObject,
    fromJ: JValue => T) = new ObjectAccessor[T] {
    def createJSON(from: T): JObject = toJ(from)
    def fromJSON(from: JValue): T = fromJ(from)
    def manifest = manifestOf[T]
    def fields: IndexedSeq[FieldAccessor[T]] = Nil.toIndexedSeq
  }

  def manifestOf[T](implicit m: Manifest[T]): Manifest[T] = m

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

