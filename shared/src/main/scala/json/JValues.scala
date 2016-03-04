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

package json

import json.exceptions.JUndefinedException
import json.internal._

import scala.collection.generic.{CanBuildFrom, GenericCompanion}
import scala.collection.mutable.Builder
import scala.collection.{IterableLike, immutable}
import scala.collection.immutable.{VectorBuilder, StringOps}

object JValue extends JValueLikeCompanion {
  def fromString(str: String): JValue =  VM.Context.fromString(str)

  type JValueBase = VM.Context.JValueBase
}

/** This is the base type for all JSON values. JValue includes JS-like methods
  * for handling data in generic JS type. Like JS, some of these methods are
  * convenient but definitely not type safe, and some may result in odd
  * type conversion or exceptions. For type-safe handling of JValues, consider
  * using pattern matching the good ol' scala way.
  */
sealed abstract class JValue extends JValueLike with JValue.JValueBase {
  /** Boolean OR using [[toJBoolean]] */
  def ||[T >: this.type <: JValue](other: T): T = if (this.toJBoolean.bool) this else other
  override def toString: String = toJSONString
}

object JObject extends /*GenericCompanion[scala.collection.immutable.Iterable] with */JObjectCompanion {
  type Pair = (String, JValue)

  val empty = apply()

  implicit def canBuildFrom: CanBuildFrom[TraversableOnce[Pair], Pair, JObject] =
    newCanBuildFrom
}

/** JSON object as ordered pairs of String -> JValue */
final case class JObject private[json] (override val fields: Map[String, JValue])(
    val iterable: Iterable[JObject.Pair] = fields)
    extends JValue with Iterable[JObject.Pair] with IterableLike[JObject.Pair, JObject]
    with JObjectLike with VM.Context.JObjectBase {

  override def toString = toJSONString

  override def newBuilder = JObject.newJObjectBuilder

  override def companion = scala.collection.immutable.Iterable
}

object JArray extends JArrayCompanion {
  val empty = apply(IndexedSeq.empty)

  implicit def canBuildFrom: CanBuildFrom[TraversableOnce[JValue], JValue, JArray] =
    newCanBuildFrom

  def unapply(x: JArray): Option[IndexedSeq[JValue]] = Some(x)
}

/** JSON array as ordered sequence of JValues */
abstract class JArray private[json] extends JValue with JArrayLike with VM.Context.JArrayBase {
  override def toString = toJSONString

  def numStringFor(idx: Int): String

  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: SimpleStringBuilder, lvl: Int): SimpleStringBuilder = {
    out.append("[")

    var isFirst = true
    for(idx <- 0 until length) {
      if (!isFirst) out.append("," + settings.spaceString)

      //TODO: could be optimized more
      out append numStringFor(idx)

      isFirst = false
    }

    out.append("]")
  }
}

object JBoolean {
  def apply(b: Boolean) = if (b) JTrue else JFalse
  def unapply(x: JBoolean): Option[Boolean] = Some(x.value)
}

/** Base type for JSON primitives [[JTrue]] and [[JFalse]] */
sealed abstract class JBoolean extends JValue with VM.Context.JBooleanBase {
  def value: Boolean
  def not: JBoolean
  def toJNumber: JNumber
  def toJString: JString

  def isTrue = value

  override def jValue = this

  def toJBoolean: JBoolean = this
  override def isBoolean: Boolean = true
  override def jBoolean: JBoolean = this

  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: SimpleStringBuilder, lvl: Int): SimpleStringBuilder = out append toJString.str
}

final case object JTrue extends JBoolean {
  val toJString: JString = JString("true")

  def value = true
  def not = JFalse
  def toJNumber: JNumber = JNumber.one
}

final case object JFalse extends JBoolean {
  val toJString: JString = JString("false")

  def value = false
  def not = JTrue
  def toJNumber: JNumber = JNumber.zero
}

/** JSON String value */
final case class JString(value: String) extends JValue with Iterable[JString] with JStringLike with VM.Context.JStringBase

object JNumber {
  val zero: JNumber = JNumberImpl(0)
  val one: JNumber = JNumberImpl(1)

  def apply(value: Double): JNumber = value match {
    case 0 => zero
    case 1 => one
    case x => JNumberImpl(x)
  }

  def unapply(x: JNumber): Option[Double] = Some(x.value)
}

private[json] final case class JNumberImpl(value: Double) extends JNumber

/** JSON numeric value (stored as 64-bit double) */
sealed abstract class JNumber extends JValue with VM.Context.JNumberBase { _: JNumberImpl =>
  val value: Double

  def iterator: Iterator[JValue] = sys.error("Cannot iterate a number!")

  def numToString = if (isInt) toLong.toString else num.toString

  override def apply(key: JValue): JValue = JUndefined

  override def jValue = this

  def isInt = num == toInt
  override def isNaN: Boolean = num.isNaN
  def isInfinity: Boolean = num.isInfinity
  def isValid = !isNaN && !isInfinity

  def toLong = num.toLong
  def toInt = num.toInt
  def toDouble = num
  def toFloat = num.toFloat

  override def jNumber: JNumber = this

  def toJNumber: JNumber = this

  def toJBoolean: JBoolean =
    if (num == 0 || isNaN) JFalse
    else JTrue

  def toJString: JString = JString(numToString)

  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: SimpleStringBuilder, lvl: Int): SimpleStringBuilder = {
    require(!isNaN && !isInfinity, "invalid number for json")

    out append numToString
  }

  def -(other: JNumber) = JNumber(value - other.value)
}

/** JSON null primitive */
final case object JNull extends JValue with VM.Context.JNullBase {
  val toJString: JString = JString("null")

  def iterator: Iterator[JValue] = sys.error("Cannot iterate null!")

  def value = null
  override def jValue = this
  def toJBoolean: JBoolean = JFalse
  def toJNumber: JNumber = JNumber.zero
  override def apply(key: JValue): JValue = {
    sys.error(s"Cannot read property '$key' of null") //TypeError
  }
  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: SimpleStringBuilder, lvl: Int): SimpleStringBuilder = out append "null"
}

/** JS undefined primitive (not actually a JSON primitive) */
final case object JUndefined extends JValue with VM.Context.JUndefinedBase {
  def iterator: Iterator[JValue] = throw JUndefinedException("Cannot iterate undefined!")

  def value = throw JUndefinedException("Cannot access JUndefined")
  override def jValue = this
  val toJBoolean: JBoolean = JFalse
  val toJNumber: JNumber = JNaN
  def toJString: JString = throw JUndefinedException() //"undefined"
  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: SimpleStringBuilder, lvl: Int): SimpleStringBuilder = throw JUndefinedException("Cant serialize undefined!")
  override def apply(key: JValue): JValue = {
    val kstr = key.toJString.str
    throw JUndefinedException(s"Cannot read property '$kstr' of undefined")
  }
}
