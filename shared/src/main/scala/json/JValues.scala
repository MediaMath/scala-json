package json

import scala.collection.immutable.StringOps

object JBoolean {
  def apply(b: Boolean) = if (b) JTrue else JFalse
}

sealed trait JBoolean extends JValue {
  def value: Boolean

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

  lazy val not: JBoolean = JBoolean(!value)
  lazy val toJString: JString =
    if (value) Constants.trueString else Constants.falseString
  lazy val toJNumber: JNumber =
    if (value) Constants.number1 else Constants.number0

  def toJSONStringBuilder(settings: JSONBuilderSettings,
    lvl: Int): StringBuilder = new StringBuilder(value.toString)
}

//TODO: also cant serialize case objects extending abstract classes here... gahhhhh
final case object JTrue extends JBoolean {
  def value = true
}
final case object JFalse extends JBoolean {
  def value = false
}

object JString {
  implicit def stringToJValue(v: String): JString = JString(v)
}

final case class JString(value: String) extends JValue with Iterable[JString] { //with IterableLike[JString, JString] {
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

  def toJSONStringBuilder(settings: JSONBuilderSettings,
    lvl: Int): StringBuilder = quoteJSONString(str)

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
  implicit def ItoJValue(x: Int): JNumber = JNumber(x)
  implicit def DtoJValue(x: Double): JNumber = JNumber(x)
  implicit def LtoJValue(x: Long): JNumber = JNumber(x)
  implicit def FtoJValue(x: Float): JNumber = JNumber(x)
}

final case class JNumber(value: Double) extends JValue {
  //require(num != null) //hmmm

  def iterator: Iterator[JValue] = sys.error("Cannot iterate a number!")

  def numToString = if (isLong) toLong.toString else num.toString

  def isLong = num == toLong.toDouble

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

  def toJSONStringBuilder(settings: JSONBuilderSettings, lvl: Int): StringBuilder = {
    require(!isNaN && !isInfinity, "invalid number for json")

    new StringBuilder(numToString)
  }
}

final case object JNull extends JValue {
  def iterator: Iterator[JValue] = sys.error("Cannot iterate null!")

  override def jObject: JObject = throw GenericJSONException("Expected JObject")
  override def jArray: JArray = throw GenericJSONException("Expected JArray")
  override def jNumber: JNumber = throw GenericJSONException("Expected JNumber")
  override def jString: JString = throw GenericJSONException("Expected JString")
  override def jBoolean: JBoolean = throw GenericJSONException("Expected JBoolean")

  def value = null
  override def jValue = this
  val toJBoolean: JBoolean = JFalse
  val toJString: JString = Constants.nullString
  val toJNumber: JNumber = JNumber(0)
  override def apply(key: JValue): JValue = {
    val kstr = key.toJString.str
    sys.error(s"Cannot read property '$kstr' of null") //TypeError
  }
  def toJSONStringBuilder(settings: JSONBuilderSettings,
    lvl: Int): StringBuilder = new StringBuilder("null")
}

final case object JUndefined extends JValue {
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
  def toJSONStringBuilder(settings: JSONBuilderSettings,
    lvl: Int): StringBuilder = throw JUndefinedException("Cant serialize undefined!")
  override def apply(key: JValue): JValue = {
    val kstr = key.toJString.str
    throw JUndefinedException(s"Cannot read property '$kstr' of undefined")
  }
}
