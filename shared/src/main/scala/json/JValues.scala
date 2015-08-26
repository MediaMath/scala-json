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

import scala.collection.immutable.StringOps

object JBoolean {
  def apply(b: Boolean) = if (b) JTrue else JFalse
  def unapply(x: JBoolean): Option[Boolean] = Some(x.value)
}

sealed trait JBoolean extends JValue with VM.Context.JBooleanBase {
  def value: Boolean
  def not: JBoolean
  def toJNumber: JNumber
  def toJString: JString

  def isTrue = value

  override def jValue = this

  //def apply(x: JValue): JValue = JUndefined

  def toJBoolean: JBoolean = this
  override def isBoolean: Boolean = true

  override def jObject: JObject = throw GenericJSONException("Expected JObject")
  override def jArray: JArray = throw GenericJSONException("Expected JArray")
  override def jNumber: JNumber = throw GenericJSONException("Expected JNumber")
  override def jString: JString = throw GenericJSONException("Expected JString")
  override def jBoolean: JBoolean = this

  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: StringBuilder, lvl: Int): StringBuilder = out append toJString.str
}

final case object JTrue extends JBoolean {
  def value = true
  def not = JFalse
  def toJNumber: JNumber = JNumber.one
  def toJString: JString = Constants.trueString
}
final case object JFalse extends JBoolean {
  def value = false
  def not = JTrue
  def toJNumber: JNumber = JNumber.zero
  def toJString: JString = Constants.falseString
}

object JString {
  implicit def stringToJValue(v: String): JString = JString(v)
}

final case class JString(value: String) extends JValue with Iterable[JString] with VM.Context.JStringBase { //with IterableLike[JString, JString] {
  def iterator: Iterator[JString] =
    (new StringOps(str)).toIterator.map(c => JString(c.toString))

  //override def newBuilder: Builder[JValue, JString] = ??? //JValue.newBuilder

  def toJBoolean: JBoolean = if (str.isEmpty) JFalse else JTrue
  def toJNumber: JNumber =
    if (str.trim.isEmpty) JNumber(0)
    else try JNumber(str.trim.toDouble) catch {
      case x: Throwable => JNaN
    }
  def toJString: JString = this
  override def toString = toJSONString

  override def apply(x: JValue): JString =
    str.charAt(x.toJNumber.value.toInt).toString.js

  override def jObject: JObject = throw GenericJSONException("Expected JObject")
  override def jArray: JArray = throw GenericJSONException("Expected JArray")
  override def jNumber: JNumber = throw GenericJSONException("Expected JNumber")
  override def jString: JString = this
  override def jBoolean: JBoolean = throw GenericJSONException("Expected JBoolean")

  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: StringBuilder, lvl: Int): StringBuilder = JValue.Context.quoteJSONString(str, out)

  override def jValue = this

  override def hashCode = str.hashCode

  override def canEqual(that: Any) = that match {
    case _: String  => true
    case _: JString => true
    case _          => false
  }

  override def equals(that: Any) = that match {
    case x: String  => x == str
    case JString(x) => x == str
    case _          => false
  }

  def ->>[T <: JValue](other: T): (JString, T) = this -> other
}

object JNumber {
  implicit def ItoJValue(x: Int): JNumber = apply(x)
  implicit def LtoJValue(x: Long): JNumber = apply(x)
  implicit def StoJValue(x: Short): JNumber = apply(x)
  implicit def DtoJValue(x: Double): JNumber = apply(x)
  implicit def FtoJValue(x: Float): JNumber = apply(x)

  val zero: JNumber = JNumberImpl(0)
  val one: JNumber = JNumberImpl(1)

  def apply(value: Double): JNumber = value match {
    case 0 => zero
    case 1 => one
    case x => JNumberImpl(x)
  }

  def unapply(x: JNumber): Option[Double] = x match {
    case x: JNumberImpl => Some(x.value)
    case _ => None
  }
}

private[json] final case class JNumberImpl(value: Double) extends JNumber

sealed trait JNumber extends JValue with VM.Context.JNumberBase {
  val value: Double

  def iterator: Iterator[JValue] = sys.error("Cannot iterate a number!")

  def numToString = if (isLong) toLong.toString else num.toString

  def isLong = num == toInt

  override def apply(key: JValue): JValue = JUndefined

  override def jValue = this

  override def isNaN: Boolean = num.isNaN
  def isInfinity: Boolean = num.isInfinity
  def isValid = !isNaN && !isInfinity

  def toLong = num.toLong
  def toInt = num.toInt
  def toDouble = num
  def toFloat = num.toFloat

  override def jObject: JObject = throw GenericJSONException("Expected JObject")
  override def jArray: JArray = throw GenericJSONException("Expected JArray")
  override def jNumber: JNumber = this
  override def jString: JString = throw GenericJSONException("Expected JString")
  override def jBoolean: JBoolean = throw GenericJSONException("Expected JBoolean")

  def toJNumber: JNumber = this

  def toJBoolean: JBoolean =
    if (num == 0 || isNaN) JFalse
    else JTrue

  def toJString: JString = JString(numToString)

  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: StringBuilder, lvl: Int): StringBuilder = {
    require(!isNaN && !isInfinity, "invalid number for json")

    out append numToString
  }
}

final case object JNull extends JValue with VM.Context.JNullBase {
  def iterator: Iterator[JValue] = sys.error("Cannot iterate null!")

  override def jObject: JObject = throw GenericJSONException("Expected JObject")
  override def jArray: JArray = throw GenericJSONException("Expected JArray")
  override def jNumber: JNumber = throw GenericJSONException("Expected JNumber")
  override def jString: JString = throw GenericJSONException("Expected JString")
  override def jBoolean: JBoolean = throw GenericJSONException("Expected JBoolean")

  def value = null
  override def jValue = this
  def toJBoolean: JBoolean = JFalse
  def toJString: JString = Constants.nullString
  def toJNumber: JNumber = JNumber.zero
  override def apply(key: JValue): JValue = {
    sys.error(s"Cannot read property '$key' of null") //TypeError
  }
  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: StringBuilder, lvl: Int): StringBuilder = out append "null"
}

final case object JUndefined extends JValue with VM.Context.JUndefinedBase {
  def iterator: Iterator[JValue] = throw JUndefinedException("Cannot iterate undefined!")

  override def jObject: JObject = throw GenericJSONException("Expected JObject")
  override def jArray: JArray = throw GenericJSONException("Expected JArray")
  override def jNumber: JNumber = throw GenericJSONException("Expected JNumber")
  override def jString: JString = throw GenericJSONException("Expected JString")
  override def jBoolean: JBoolean = throw GenericJSONException("Expected JBoolean")

  def value = throw JUndefinedException("Cannot access JUndefined")
  override def jValue = this
  val toJBoolean: JBoolean = JFalse
  val toJNumber: JNumber = JNaN
  def toJString: JString = throw JUndefinedException() //"undefined"
  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: StringBuilder, lvl: Int): StringBuilder = throw JUndefinedException("Cant serialize undefined!")
  override def apply(key: JValue): JValue = {
    val kstr = key.toJString.str
    throw JUndefinedException(s"Cannot read property '$kstr' of undefined")
  }
}
