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

import java.text.SimpleDateFormat

import json._
import json.internal.JArrayPrimitive.{SpecialBuilders, PrimitiveAccessor}

import scala.collection.generic.CanBuildFrom
import scala.reflect.{classTag, ClassTag}

trait Accessors {
  def accessorFor[T](implicit acc: JSONAccessor[T]) = acc

  implicit def optionAccessor[T, U <: Option[T]](implicit acc: JSONAccessor[T]) =
    new OptionAccessor[T, U]

  final class OptionAccessor[T, U <: Option[T]](implicit val acc: JSONAccessor[T])
      extends JSONAccessorProducer[Option[T], JValue] {
    def clazz = classOf[Option[Any]]

    override def toString = "OptionAccessor"

    def createJSON(obj: Option[T]): JValue = obj match {
      case Some(x) => x.js
      case _       => JNull
    }

    def fromJSON(js: JValue): Option[T] = js match {
      case JUndefined => None
      case JNull      => None
      case x          => Some(x.to(acc))
    }

    override def createSwaggerProperty: JObject = {
      acc.createSwaggerProperty + ("required" -> JFalse)
    }

    override def extraSwaggerModels: Seq[JObject] = acc match {
      case x: CaseClassObjectAccessor[_] => x.createSwaggerModels
      case _                             => Nil
    }

    def describe = baseDescription ++ Map(
      "types" -> Seq("T").js,
      "T" -> acc.describe
    ).js
  }

  implicit def mapAccessor[K, T](implicit valueAcc: JSONAccessor[T], keyAcc: JSONAccessorProducer[K, JString]) =
    new MapAccessor[K, T]

  final class MapAccessor[K, T](implicit val valueAcc: JSONAccessor[T], val keyAcc: JSONAccessorProducer[K, JString])
      extends JSONAccessorProducer[Map[K, T], JObject] {
    def clazz = classOf[Map[Any, Any]]

    val swaggerModelName = s"Map[String,${valueAcc.clazz.getSimpleName}]"

    override def toString = "MapAccessor"

    def describe = baseDescription ++ Map(
      "types" -> Seq("K", "T").js,
      "K" -> keyAcc.describe,
      "T" -> valueAcc.describe
    ).js

    def createJSON(obj: Map[K, T]): JObject = JObject(obj map {
      case (k, v) => keyAcc.toString(k) -> v.js
    })

    def fromJSON(js: JValue): Map[K, T] = js match {
      case JObject(fields) =>
        var exceptions = List[InputFormatException]()

        val res = fields flatMap {
          case (k, v) =>
            try Seq(keyAcc.fromString(k) -> v.to[T]) catch {
              case e: InputFormatException =>
                exceptions ::= e.prependFieldName(k.str)
                Nil
            }
        }

        if (!exceptions.isEmpty)
          throw InputFormatsException(exceptions.flatMap(_.getExceptions).toSet[InputFormatException])

        res
      case x => throw InputTypeException("",
        "object", x.getClass.getName, x)
    }

    override def extraSwaggerModels: Seq[JObject] = {
      val typ = valueAcc.clazz.getSimpleName

      val jstr = s"""{
        "id": "$swaggerModelName",
        "description": "Key value pair of String -> $typ",
        "properties": {
          "{key}": {
            "type": "string",
            "required": true,
            "defaultValue": false
          },
          "{value}": {
            "type": "$typ",
            "required": true,
            "defaultValue": false
          }
        }
      }"""

      JObject(swaggerModelName -> jstr.parseJSON.toJObject) +: (valueAcc match {
        case x: CaseClassObjectAccessor[_] => x.createSwaggerModels
        case _                             => Nil
      })
    }

    override def createSwaggerProperty: JObject = JObject("type" -> JString(swaggerModelName))
  }

  implicit def iterableAccessor[T, U[T] <: Iterable[T]](
      implicit acc: JSONAccessor[T], cbf: CanBuildFrom[Nothing, T, U[T]],
      ctag: ClassTag[U[T]],
      primitive: PrimitiveAccessor[T] = PrimitiveAccessor.NonPrimitive[T],
      specialBuilder: SpecialBuilders[U] = SpecialBuilders.ForAny[U]) =
    new IterableAccessor[T, U]

  final class IterableAccessor[T, U[T] <: Iterable[T]](implicit val acc: JSONAccessor[T],
      val cbf: CanBuildFrom[Nothing, T, U[T]], val ctag: ClassTag[U[T]],
      val primitive: PrimitiveAccessor[T],
      val specialBuilder: SpecialBuilders[U]) extends JSONAccessorProducer[U[T], JArray] {
    def clazz = ctag.runtimeClass

    override def toString = "IterableAccessor"

    def describe = baseDescription ++ Map(
      "types" -> Seq("T").js,
      "repr" -> ctag.runtimeClass.getName.js,
      "T" -> acc.describe
    ).js

    def createJSON(obj: U[T]): JArray = {
      if(primitive.isPrimitive) primitive.createJSON(obj)
      else JArray(obj.map(_.js))
    }

    def fromJSON(js: JValue): U[T] = js match {
      case x: JArrayPrimitive[_] if primitive.isPrimitive =>
        val iterable = primitive.iterableFromJValue(x)

        specialBuilder match {
          case x if x.isGeneric =>
            iterable.to[U](cbf)
          case builder =>
            builder.buildFrom(iterable)
        }

      case JArray(vals) =>
        var exceptions = List[InputFormatException]()

        val res = vals.iterator.zipWithIndex.flatMap {
          case (x, idx) =>
            try Seq(x.to[T]) catch {
              case e: InputFormatException =>
                exceptions ::= e.prependFieldName(idx.toString)
                Nil
            }
        }.to[U](cbf)

        if (!exceptions.isEmpty)
          throw InputFormatsException(exceptions.flatMap(_.getExceptions).toSet)

        res
      case x => throw InputTypeException("",
        "array", x.getClass.getName, x)
    }

    override def createSwaggerProperty: JObject = {
      val unique = if (clazz == classOf[Set[_]])
        JObject("uniqueItems" -> JTrue)
      else JObject.empty

      JObject("type" -> JString("array"), "items" -> JObject(
        ("$" + "ref") -> JString(acc.clazz.getSimpleName)
      )) ++ unique
    }

    override def extraSwaggerModels: Seq[JObject] = acc match {
      case x: CaseClassObjectAccessor[_] => x.createSwaggerModels
      case _                             => Nil
    }
  }

  //TODO: needs to be rethought, moved or removed
  implicit case object BigDecimalAccessor extends JSONAccessorProducer[BigDecimal, JNumber] {
    val clazz = classOf[BigDecimal]

    def describe = baseDescription

    def createJSON(obj: BigDecimal): JNumber = JNumber(obj.toDouble)
    def fromJSON(js: JValue): BigDecimal = js.toJNumber match {
      case jn @ JNumber(d) if jn.isValid => BigDecimal(d)
      case x => throw InputTypeException("",
        "numeric", x.getClass.getName, x)
    }
  }

  //TODO: needs to be rethought, moved or removed
  implicit case object DateAccessor extends JSONAccessorProducer[java.util.Date, JString] {
    val clazz = classOf[java.util.Date]

    def describe = baseDescription

    def createJSON(obj: java.util.Date): JString = JString(obj.toString)
    def fromJSON(js: JValue): java.util.Date = js match {
      case str: JString =>
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(str.toString)
      case x => throw InputTypeException("",
        "date", x.getClass.getName, x)
    }
  }

  implicit case object StringAccessor extends JSONAccessorProducer[String, JString] {
    val clazz = classOf[String]

    def describe = baseDescription

    //optimized str to str passthrough for object keys
    override def fromString(str: String): String = str

    override def toString(value: String): String = value

    def createJSON(obj: String): JString = JString(obj)
    def fromJSON(js: JValue): String = js match {
      case JString(str) => str
      case x => throw InputTypeException("",
        "string", x.getClass.getName, x)
    }
  }

  implicit case object BooleanAccessor extends JSONAccessorProducer[Boolean, JBoolean] {
    val clazz = classOf[Boolean]

    def describe = baseDescription

    def createJSON(obj: Boolean): JBoolean = JBoolean(obj)
    def fromJSON(js: JValue): Boolean = js match {
      case JFalse       => false
      case JTrue        => true
      case JNumber(1.0) => true
      case JNumber(0.0) => false
      case JString("0") => false
      case JString("1") => true
      case x => throw InputTypeException("",
        "boolean", x.getClass.getName, x)
    }
  }

  implicit case object IntAccessor extends JSONAccessorProducer[Int, JNumber] {
    val clazz = classOf[Int]

    def describe = baseDescription

    def createJSON(obj: Int): JNumber = JNumber(obj)
    def fromJSON(js: JValue): Int = js match {
      case x: JString if x.toJNumber.isValid =>
        fromJSON(x.toJNumber)
      case jn @ JNumber(d) if jn.isValid =>
        val nval = d.toInt
        if (nval.toDouble == d) nval
        else throw NumericTypeException("", d, "int")
      case x => throw InputTypeException("",
        "numeric", x.getClass.getName, x)
    }
  }

  implicit case object LongAccessor extends JSONAccessorProducer[Long, JNumber] {
    val clazz = classOf[Long]

    def describe = baseDescription

    def createJSON(obj: Long): JNumber = JNumber(obj)
    def fromJSON(js: JValue): Long = js match {
      case x: JString if x.toJNumber.isValid =>
        fromJSON(x.toJNumber)
      case jn @ JNumber(d) if jn.isValid =>
        val nval = d.toLong
        if (nval.toDouble == d) nval
        else throw NumericTypeException("", d, "long")
      case x => throw InputTypeException("",
        "numeric", x.getClass.getName, x)
    }
  }

  implicit case object DoubleAccessor extends JSONAccessorProducer[Double, JNumber] {
    val clazz = classOf[Double]

    def describe = baseDescription

    def createJSON(obj: Double): JNumber = JNumber(obj)
    def fromJSON(js: JValue): Double = js match {
      case x: JString if x.toJNumber.isValid =>
        fromJSON(x.toJNumber)
      case jn @ JNumber(d) if jn.isValid => d
      case x => throw InputTypeException("",
        "numeric", x.getClass.getName, x)
    }
  }

  implicit case object FloatAccessor extends JSONAccessorProducer[Float, JNumber] {
    val clazz = classOf[Float]

    def describe = baseDescription

    def createJSON(obj: Float): JNumber = JNumber(obj)
    def fromJSON(js: JValue): Float = js match {
      case x: JString if x.toJNumber.isValid =>
        fromJSON(x.toJNumber)
      case jn @ JNumber(d) if jn.isValid =>
        val nval = d.toFloat
        if (nval.toDouble == d) nval
        else throw NumericTypeException("", d, "float")
      case x => throw InputTypeException("",
        "numeric", x.getClass.getName, x)
    }
  }

  implicit case object ShortAccessor extends JSONAccessorProducer[Short, JNumber] {
    val clazz = classOf[Short]

    def describe = baseDescription

    def createJSON(obj: Short): JNumber = JNumber(obj)
    def fromJSON(js: JValue): Short = js match {
      case x: JString if x.toJNumber.isValid =>
        fromJSON(x.toJNumber)
      case jn @ JNumber(d) if jn.isValid =>
        val nval = d.toShort
        if (nval.toDouble == d) nval
        else throw NumericTypeException("", d, "short")
      case x => throw InputTypeException("",
        "numeric", x.getClass.getName, x)
    }
  }

  implicit case object ByteAccessor extends JSONAccessorProducer[Byte, JNumber] {
    val clazz = classOf[Byte]

    def describe = baseDescription

    def createJSON(obj: Byte): JNumber = JNumber(obj)
    def fromJSON(js: JValue): Byte = js match {
      case x: JString if x.toJNumber.isValid =>
        fromJSON(x.toJNumber)
      case jn @ JNumber(d) if jn.isValid =>
        val nval = d.toByte
        if (nval.toDouble == d) nval
        else throw NumericTypeException("", d, "byte")
      case x => throw InputTypeException("",
        "numeric", x.getClass.getName, x)
    }
  }

  implicit case object JValueAccessor extends JSONAccessorProducer[JValue, JValue] {
    val clazz = classOf[JValue]

    def describe = baseDescription

    def createJSON(obj: JValue): JValue = obj
    def fromJSON(js: JValue): JValue = js
  }
}

