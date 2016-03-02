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

import scala.collection.generic.CanBuildFrom
import scala.reflect.{classTag, ClassTag}

trait Accessors extends LowPriorityAccessors {
  implicit def optionAccessor[T: JSONAccessor, U <: Option[T]] = new OptionAccessor[T, U]

  final class OptionAccessor[T: JSONAccessor, U <: Option[T]]
      extends JSONAccessorProducer[Option[T], JValue] {
    def clazz = classOf[Option[Any]]

    override def toString = "OptionAccessor"

    def createJSON(obj: Option[T]): JValue = obj match {
      case Some(x) => x.js
      case _       => JNull
    }

    def fromJSON(js: JValue): Option[T] = js match {
      case JUndefined => None //case class accessor will use defaults here for
      case JNull      => None
      case x          => Some(x.to[T])
    }

    override def referencedTypes: Seq[JSONAccessorProducer[_, _]] = Seq(accessorOf[T])
  }

  implicit def mapAccessor[K, T](implicit valueAcc: JSONAccessor[T], keyAcc: JSONAccessorProducer[K, JString]) =
    new MapAccessor[K, T]

  final class MapAccessor[K, T](implicit val valueAcc: JSONAccessor[T], val keyAcc: JSONAccessorProducer[K, JString])
      extends JSONAccessorProducer[Map[K, T], JObject] {
    def clazz = classOf[Map[Any, Any]]

    override def toString = "MapAccessor"

    override def referencedTypes: Seq[JSONAccessorProducer[_, _]] = Seq(accessorOf[K], accessorOf[T])

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
  }

  //TODO: needs to be rethought, moved or removed
  implicit case object BigDecimalAccessor extends JSONAccessorProducerA[BigDecimal, JNumber] {
    def createJSON(obj: BigDecimal): JNumber = JNumber(obj.toDouble)
    def fromJSON(js: JValue): BigDecimal = js.toJNumber match {
      case jn @ JNumber(d) if jn.isValid => BigDecimal(d)
      case x => throw InputTypeException("",
        "numeric", x.getClass.getName, x)
    }
  }

  //TODO: needs to be rethought, moved or removed
  implicit case object DateAccessor extends JSONAccessorProducerA[java.util.Date, JString] {
    def createJSON(obj: java.util.Date): JString = JString(obj.toString)
    def fromJSON(js: JValue): java.util.Date = js match {
      case str: JString =>
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(str.toString)
      case x => throw InputTypeException("",
        "date", x.getClass.getName, x)
    }
  }

  implicit case object StringAccessor extends JSONAccessorProducerA[String, JString] {
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

  implicit case object BooleanAccessor extends JSONAccessorProducerA[Boolean, JBoolean] with PrimitiveJArray.Builder[Boolean] {
    def createJSON(obj: Boolean): JBoolean = JBoolean(obj)
    def fromJSON(js: JValue): Boolean = js match {
      case JFalse       => false
      case JTrue        => true
      case JNumber(1.0) => true
      case JNumber(0.0) => false
      case JString("false") => false
      case JString("true") => true
      case JString("0") => false
      case JString("1") => true
      case x => throw InputTypeException("",
        "boolean", x.getClass.getName, x)
    }
    def toDouble(x: Boolean): Double = if(x) 1.0 else 0.0
    def fromDouble(x: Double): Boolean = x != 0.0

    override def toJValue(x: Boolean): JValue = JBoolean(x)
  }

  implicit case object IntAccessor extends JSONAccessorProducerA[Int, JNumber] with PrimitiveJArray.Builder[Int] {
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
    def toDouble(x: Int): Double = x.toDouble
    def fromDouble(x: Double): Int = x.toInt
  }

  implicit case object LongAccessor extends JSONAccessorProducerA[Long, JNumber] with PrimitiveJArray.Builder[Long] {
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
    def toDouble(x: Long): Double = x.toDouble
    def fromDouble(x: Double): Long = x.toLong
  }

  implicit case object DoubleAccessor extends JSONAccessorProducerA[Double, JNumber] with PrimitiveJArray.Builder[Double] {
    def createJSON(obj: Double): JNumber = JNumber(obj)
    def fromJSON(js: JValue): Double = js match {
      case x: JString if x.toJNumber.isValid =>
        fromJSON(x.toJNumber)
      case jn @ JNumber(d) if jn.isValid => d
      case x => throw InputTypeException("",
        "numeric", x.getClass.getName, x)
    }
    def toDouble(x: Double): Double = x
    def fromDouble(x: Double): Double = x
  }

  implicit case object FloatAccessor extends JSONAccessorProducerA[Float, JNumber] with PrimitiveJArray.Builder[Float] {
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
    def toDouble(x: Float): Double = x.toDouble
    def fromDouble(x: Double): Float = x.toFloat
  }

  implicit case object ShortAccessor extends JSONAccessorProducerA[Short, JNumber] with PrimitiveJArray.Builder[Short] {
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
    def toDouble(x: Short): Double = x.toDouble
    def fromDouble(x: Double): Short = x.toShort
  }

  implicit case object ByteAccessor extends JSONAccessorProducerA[Byte, JNumber] with PrimitiveJArray.Builder[Byte] {
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
    def toDouble(x: Byte): Double = x.toDouble
    def fromDouble(x: Double): Byte = x.toByte
  }

  implicit case object JValueAccessor extends JSONAccessorProducerA[JValue, JValue] {
    def createJSON(obj: JValue): JValue = obj
    def fromJSON(js: JValue): JValue = js
  }

  /** convenience abstract class to reduce class size from trait */
  abstract class JSONAccessorProducerA[T: ClassTag, U <: JValue] extends JSONAccessorProducer[T, U] {
    val classTag = implicitly[ClassTag[T]]
    val clazz = classTag.runtimeClass
  }
}

