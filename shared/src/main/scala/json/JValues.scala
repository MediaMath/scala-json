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

import json.internal._

import scala.collection.generic.{CanBuildFrom, GenericCompanion}
import scala.collection.mutable.Builder
import scala.collection.{IterableLike, immutable}
import scala.collection.immutable.{VectorBuilder, StringOps}

object JValue extends JValueLikeCompanion {
  def fromString(str: String): JValue =  VM.Context.fromString(str)

  type JValueBase = VM.Context.JValueBase
}

sealed trait JValue extends AnyRef with JValueLike with JValue.JValueBase {
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

final case class JObject(override val fields: Map[String, JValue])(
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

  def unapply(x: JArray): Option[IndexedSeq[JValue]] = Some(x.values.toIndexedSeq)
}

abstract class JArray private[json] extends JValue with JArrayLike with VM.Context.JArrayBase {
  override def toString = toJSONString

  final override def canEqual(that: Any) = that.isInstanceOf[JArray]
  final override def equals(that: Any) = that match {
    case x: JArray => x.values == values
    case _ => false
  }
  final override def hashCode() = values.hashCode()
}

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

  def toJBoolean: JBoolean = this
  override def isBoolean: Boolean = true
  override def jBoolean: JBoolean = this

  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: StringBuilder, lvl: Int): StringBuilder = out append toJString.str
}

final case object JTrue extends JBoolean {
  def value = true
  def not = JFalse
  def toJNumber: JNumber = JNumber.one
  def toJString: JString = constants.trueString
}

final case object JFalse extends JBoolean {
  def value = false
  def not = JTrue
  def toJNumber: JNumber = JNumber.zero
  def toJString: JString = constants.falseString
}

final case class JString(value: String) extends JValue with Iterable[JString] with JStringLike with VM.Context.JStringBase

object JNumber {
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

  override def jNumber: JNumber = this

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

  def -(other: JNumber) = JNumber(value - other.value)
}

final case object JNull extends JValue with VM.Context.JNullBase {
  def iterator: Iterator[JValue] = sys.error("Cannot iterate null!")

  def value = null
  override def jValue = this
  def toJBoolean: JBoolean = JFalse
  def toJString: JString = constants.nullString
  def toJNumber: JNumber = JNumber.zero
  override def apply(key: JValue): JValue = {
    sys.error(s"Cannot read property '$key' of null") //TypeError
  }
  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: StringBuilder, lvl: Int): StringBuilder = out append "null"
}

final case object JUndefined extends JValue with VM.Context.JUndefinedBase {
  def iterator: Iterator[JValue] = throw JUndefinedException("Cannot iterate undefined!")

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
