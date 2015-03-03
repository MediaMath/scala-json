package json

import json.internal.{ DefaultJVMShadowContext, JValueObjectDeserializer, Accessors }

import DefaultJVMShadowContext._

object JValue extends Accessors with VMContext.JValueCompanionBase /* extends GenericCompanion[JValueColl]*/ {
  //import shadow context! Will shadow default terms from on high
  import shadow._

  //TODO: add applys for creating values from different types
  def apply(n: Int) = JNumber(n)
  def apply(str: String) = JString(str)

  //def from[T](x: T)(implicit acc: JSONAccessor[T]) = acc.createJSON(x)

  //TODO: make explicit any accessor

  def from(v: Any): JValue = apply(v)

  def apply[T](v: T) /*(implicit acc: JSONProducer[T, JValue] = null)*/ : JValue =
    VMContext.fromAny(v)

  //implicit def anyToJVal[T, U <: JValue](x: T)(implicit acc: JSONProducer[T, U]): U = x.js
  //implicit def anyToJVal[T](x: T)(implicit acc: JSONProducer[T, JValue]): JValue = x.js

  implicit def jValuetoJBoolean(v: JValue): Boolean = v.toJBoolean.isTrue

  implicit def stringToJValue(v: String): JString = JString(v)
  implicit def intToJValue(v: Int): JNumber = JNumber(v)

  def fromString(str: String): JValue = {
    VMContext.fromString(str)
  }

  type JValueBase = VMContext.JValueBase
}

trait JValue extends JValue.JValueBase with Equals { //} with PartialFunction[JValue, JValue] {
  def toJSONStringBuilder(settings: JSONBuilderSettings = prettyJSONBuilder, lvl: Int = 0): StringBuilder

  def toJString: JString
  def toJNumber: JNumber //can return JNaN
  def toJBoolean: JBoolean
  def value: Any

  def toJValue: JValue = jValue

  def apply(x: JValue): JValue = JUndefined

  def jValue = this

  //most of these will need overrides by other types
  def isNaN = false
  def isBoolean = false
  def isObject = false
  def isUndefined = this === JUndefined
  def isNull = this === JNull
  def isNullOrUndefined = isNull || isUndefined
  def isDefined = !isUndefined

  def isDefinedAt(x: JValue): Boolean = apply(x).isDefined

  def keys: Iterable[JValue] = Nil

  def toJObject: JObject = throw GenericJSONException("Cannot create JObject from " + getClass.getName)
  def toJArray: JArray = throw GenericJSONException("Cannot create JArray from " + getClass.getName)

  def jObject: JObject = throw GenericJSONException("Expected JObject")
  def jArray: JArray = throw GenericJSONException("Expected JArray")
  def jNumber: JNumber = throw GenericJSONException("Expected JNumber")
  def jString: JString = throw GenericJSONException("Expected JString")
  def jBoolean: JBoolean = throw GenericJSONException("Expected JBoolean")

  def str: String = jString.value
  def num: Double = jNumber.value
  def bool: Boolean = jBoolean.value
  def fields: Map[JString, JValue] = jObject.toMap
  def values: Iterable[JValue] = jArray.values

  def to[T](implicit acc: JSONReader[T]): T = toObject[T]
  def toObject[T](implicit acc: JSONReader[T]): T = acc.fromJSON(toJValue)

  def apply(key: String): JValue = apply(JString(key))

  def select(seqStr: String, safe: Boolean): JValue = {
    val seq = seqStr.split(".").toSeq.map(_.trim).filter(_ != "")

    select(seq, safe)
  }
  def select(seq: Seq[String], safe: Boolean): JValue =
    if (seq.isEmpty) toJValue
    else if (isUndefined && safe) JUndefined
    else apply(JString(seq.head)).select(seq.tail, safe)

  def select(seqStr: String): JValue = select(seqStr, false)

  def /(key: String): JValue = select(List(key), true)

  def ||[T >: this.type <: JValue](other: T): T = if (this.toJBoolean.bool) this else other

  def -(key: Any): JValue = this match {
    case JNumber(d) =>
      (d - JValue(key).toJNumber.value).js
    case x: JObject =>
      x - JValue(key).toJString
  }

  def ++(that: JValue): JValue =
    (this, that) match {
      case _ if isObject && that.isObject =>
        toJObject ++ that.toJObject
      case (a: JArray, b: JArray) => a ++ b
      case _                      => throw GenericJSONException("Can only append 2 objs or 2 arrs")
    }

  /*def +(pair: (String, JValue)): JValue =
		this + (JString(pair._1) -> pair._2)*/

  //http://bclary.com/2004/11/07/#a-11.9.3
  def ~~(to: JValue): Boolean = (this: Any, to: Any) match {
    case _ if isNaN || to.isNaN          => false
    case _ if this === to                => true
    //same but not nan
    case (num: JNumber, _) if equals(to) => true
    //check if either is object
    //case (JUndefined, _) => true
    //case (JNull, _) => true
    //case (JNumber(n1), JNumber(n2)) => n1 == n2
    //case x => ???//11
    case (JNull, JUndefined)             => true
    case (JUndefined, JNull)             => true
    case (num: JNumber, str: JString) =>
      num == str.toJNumber
    case (str: JString, num: JNumber) =>
      num == str.toJNumber
    case (jval: JValue, num: JNumber) if jval.isBoolean =>
      num == jval.toJNumber
    case (num: JNumber, jval: JValue) if jval.isBoolean =>
      num == jval.toJNumber
    //tostring stuff primitives? 20-21
    case _ => false
  }

  def ===(x: JValue) = equals(x)
  def !==(to: JValue) = !(this === to)

  def unary_!(): JBoolean = toJBoolean.not

  def toString(settings: JSONBuilderSettings,
    lvl: Int = 0): String = toJSONStringBuilder(settings).toString

  def toDenseString = toString(denseJSONBuilder)
  def toPrettyString = toString(prettyJSONBuilder)

  def toJSONString = toPrettyString

  /*override def toString: String = productPrefix +
			"(" + productIterator.mkString(", ") + ")"*/
  override def toString = toJSONString
}
