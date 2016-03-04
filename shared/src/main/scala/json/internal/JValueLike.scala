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
import json.exceptions.GenericJSONException
import json.internal.DefaultVMContext._

import scala.collection.generic.Growable

private[json] object VM {
  //import shadow context! Will shadow default terms from on high

  import json.shadow._

  val Context = VMContext
}

trait JValueLikeCompanion extends Accessors with VM.Context.JValueCompanionBase /* extends GenericCompanion[JValueColl]*/ {
  val Context = VM.Context

  def apply[T](v: T)(implicit acc: JSONAccessor[T]): JValue = v.js

  /** Uses run-time type detection to produce JValue from Any if possible */
  def from(v: Any): JValue = VM.Context.fromAny(v)

  private[json] def fromAnyInternal(default: => JValue)(x: Any): JValue = x match {
    case x: JValue => x
    case x: String => JString(x)
    case None      => JNull
    case null      => JNull
    case true      => JTrue
    case false     => JFalse
    case x: Iterable[Any] =>
      val seq = x.toSeq
      if (seq.isEmpty) (x: @unchecked) match {
        case _: Map[_, _] => JObject.empty
        case _            => JArray(IndexedSeq.empty)
      }
      else seq.head match {
        case (k: String, v: Any) =>
          val vals = x.toSeq map {
            case (k: String, v) => k -> JValue.from(v)
          }
          JObject(vals: _*)
        case (v: Any) =>
          JArray(x.map(JValue.from).toIndexedSeq)
      }
    case x: Double => JNumber(x)
    case x: Int   => JNumber(x)
    case x: Short => JNumber(x)
    case x: Long  => JNumber(x)
    case x: Float => JNumber(x)
    case _ => default
  }

  private[json] def fromAnyInternal(value: Any): JValue =
    fromAnyInternal(sys.error(s"Cannot convert $value to JValue"))(value)

  implicit def jValuetoJBoolean(v: JValue): Boolean = v.toJBoolean.isTrue

  implicit def stringToJValue(v: String): JString = JString(v)
  implicit def intToJValue(v: Int): JNumber = JNumber(v)
}

trait SimpleStringBuilder {
  def append(str: String): SimpleStringBuilder
  def result(): String

  def append(char: Char): SimpleStringBuilder
  def ensureCapacity(cap: Int): Unit

  final override def toString = result()
}

trait JValueLike extends Equals { _: JValue =>
  def appendJSONStringBuilder(settings: JSONBuilderSettings,
      out: SimpleStringBuilder, lvl: Int): SimpleStringBuilder

  /** JS-like string representation of this value. */
  def toJString: JString
  /** JS-like numeric representation of this value. */
  def toJNumber: JNumber //can return JNaN
  /** JS-like boolean representation of this value. */
  def toJBoolean: JBoolean
  /** underlying scala value for this JValue */
  def value: Any

  def toJValue: JValue = jValue

  def jValue = this

  //most of these will need overrides by other types
  def isNaN = false
  def isBoolean = false
  def isObject = false
  final def isUndefined = this === JUndefined
  final def isNull = this === JNull
  final def isNullOrUndefined = isNull || isUndefined
  final def isDefined = !isUndefined

  /** return Some(x) if value is not undefined or null */
  final def toOption: Option[JValue] = if(isNullOrUndefined) None else Some(this)

  def isDefinedAt(x: JValue): Boolean = apply(x).isDefined

  /** keys for this JValue. Either iterable array indexes from [[JArray]] or iterable keys from a [[JObject]] */
  def keys: Iterable[JValue] = Nil

  def dynamic = JDynamic(this)
  def d = dynamic

  /** convert this JValue into an object if possible */
  def toJObject: JObject = throw GenericJSONException("Cannot create JObject from " + getClass.getName)
  /** convert this JValue into an array if possible */
  def toJArray: JArray = throw GenericJSONException("Cannot create JArray from " + getClass.getName)

  /** quickly cast to and object or throw a [[GenericJSONException]] otherwise. */
  def jObject: JObject = throw GenericJSONException("Expected JObject")
  /** quickly cast to and array or throw a [[GenericJSONException]] otherwise. */
  def jArray: JArray = throw GenericJSONException("Expected JArray")
  /** quickly cast to a number or throw a [[GenericJSONException]] otherwise. */
  def jNumber: JNumber = throw GenericJSONException("Expected JNumber")
  /** quickly cast to a string or throw a [[GenericJSONException]] otherwise. */
  def jString: JString = throw GenericJSONException("Expected JString")
  /** quickly cast to a boolean or throw a [[GenericJSONException]] otherwise. */
  def jBoolean: JBoolean = throw GenericJSONException("Expected JBoolean")

  /** gets the string value using [[jString]] */
  def str: String = jString.value
  /** gets the number value using [[jNumber]] */
  def num: Double = jNumber.value
  /** gets the boolean value using [[jBoolean]] */
  def bool: Boolean = jBoolean.value
  /** gets the Map[String, JValue] value using [[jObject]] */
  def fields: Map[String, JValue] = jObject.toMap
  /** gets the iterable JValues using [[jArray]] or the object values if [[JObject]] */
  def values: Iterable[JValue] = jArray.values

  /** converts this JValue into the desired type using the implicit [[JSONAccessor]] */
  def to[T: JSONAccessor]: T = toObject[T]
  /** alternate and recommended form of [[to]] */
  def toObject[T](implicit acc: JSONAccessor[T]): T = acc.fromJSON(toJValue)

  /** select a key from this JValue using a JValue. Equivalent to bracket selects in JS. */
  def apply(x: JValue): JValue = JUndefined

  /** select a key from this JValue. Equivalent to bracket selects in JS. */
  def apply(key: String): JValue = apply(JString(key))

  /**
   * select from a value with a '.' delimited string
   * @param seqStr period delimited string
   * @param safe if true, you will get back [[JUndefined]] instead of an exception for ''undefined'' access
   * @return
   */
  def select(seqStr: String, safe: Boolean): JValue = {
    val seq = seqStr.split(".").toSeq.map(_.trim).filter(_ != "")

    select(seq, safe)
  }

  /** select from a value with a sequence of keys */
  def select(seq: Seq[String], safe: Boolean): JValue =
    if (seq.isEmpty) toJValue
    else if (isUndefined && safe) JUndefined
    else apply(JString(seq.head)).select(seq.tail, safe)

  /** select from a value with a '.' delimited string */
  def select(seqStr: String): JValue = select(seqStr, false)

  /** alternate operator form of [[json.JValue#apply(key:String):json\.JValue]] */
  def \(key: String): JValue = select(List(key), true)
  def \(idx: Int): JValue = apply(JNumber(idx))

  //TODO: deep path \\
  //def \\(key: String): JValue = ???

  /** equivalent to javascript ''delete object[field]'' */
  final def -(x: JValue): JValue = this match {
    case JNumber(d) => JNumber(d - x.toJNumber.value)
    case seq: JArray => seq.updated(x.toJNumber.toInt, JUndefined)
    case x: JObject => x - x.toJString.str
    case x => x
  }

  def -[T](key: T)(implicit acc: JSONAccessor[T]): JValue = this - key.js

  /** concats another JObject or JArray onto this value. Must be the same type! */
  def ++(that: JValue): JValue =
    (this, that) match {
      case _ if isObject && that.isObject =>
        toJObject ++ that.toJObject
      case (a: JArray, b: JArray) => a ++ b
      case _                      => throw GenericJSONException("Can only append 2 objs or 2 arrs")
    }

  /** This operator implements the JavaScript-like equality logic according to the [[http://bclary.com/2004/11/07/#a-11.9.3 JS Spec]] */
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

  /** Boolean ''not'' according to JS boolean logic */
  def unary_!(): JBoolean = toJBoolean.not

  def toJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty, lvl: Int = 0): SimpleStringBuilder =
    appendJSONStringBuilder(settings, VM.Context.newVMStringBuilder, lvl)

  /** toString method that uses a specific [[json.JSONBuilderSettings]] and specific indent level to generate JSON */
  def toString(settings: JSONBuilderSettings,
    lvl: Int = 0): String = toJSONStringBuilder(settings).toString

  /** toString method that uses the ''dense'' builder settings */
  def toDenseString = toString(JSONBuilderSettings.dense)
  /** toString method that uses the ''pretty'' builder settings */
  def toPrettyString = toString(JSONBuilderSettings.pretty)

  def toJSONString = toPrettyString
}
