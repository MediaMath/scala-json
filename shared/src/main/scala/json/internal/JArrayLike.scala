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

package json.internal

import java.util.UUID

import json._

import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.VectorBuilder
import scala.collection.mutable.Builder
import scala.collection.{IndexedSeqLike, immutable}

trait JArrayCompanion/* extends GenericCompanion[scala.collection.immutable.Seq]*/ {
  def newCanBuildFrom = new CanBuildFrom[TraversableOnce[JValue], JValue, JArray] {
    def apply(from: TraversableOnce[JValue]) = newJArrayBuilder // ++= from
    def apply() = newJArrayBuilder
  }

  def newJArrayBuilder: Builder[JValue, JArray] = new JArrayBuilder

  def apply(seq: TraversableOnce[JValue]): JArray = new JArray(seq.toIndexedSeq)
  def apply(seq: JValue*) = new JArray(seq.toIndexedSeq)

  class JArrayBuilder extends Builder[JValue, JArray] {
    val builder = new VectorBuilder[JValue]

    def +=(item: JValue): this.type = {
      builder += item
      this
    }

    def result: JArray = {
      JArray(builder.result)
    }

    def clear() {
      builder.clear
    }
  }

  def newBuilder[A]: Builder[A, immutable.IndexedSeq[A]] =
    newJArrayBuilder.asInstanceOf[Builder[A, immutable.IndexedSeq[A]]]
}

trait JArrayLike extends immutable.IndexedSeq[JValue] with IndexedSeqLike[JValue, JArray] { _: JArray =>
  lazy val uuid = UUID.randomUUID.toString

  def toJString: JString = JString("array " + uuid) //this... should be different
  def toJNumber: JNumber = JNaN
  def toJBoolean: JBoolean = JTrue

  def length = values.length

  def value = values.map(_.value)

  //override def companion: GenericCompanion[scala.collection.immutable.Iterable] = JArray
  override def newBuilder = JArray.newJArrayBuilder

  override def toSeq = values
  override def toIndexedSeq = values
  override def seq = this

  override def jValue = this
  override def toJArray: JArray = this

  override def keys = (0 until values.length).map(JNumber(_))

  override def jArray: JArray = this

  def apply(key: JNumber): JValue = apply(key.num)
  def apply(key: Int): JValue = values(key)

  override def apply(key: JValue): JValue = key match {
    case JString("length") => JNumber(length)
    case _ =>
      val jNum = key.toJNumber
      val jInt = jNum.num.toInt

      if (jNum.num != jInt) JUndefined
      else if (jInt < 0) JUndefined
      else if (jInt >= values.length) JUndefined
      else values(jInt)
  }

  def ++(that: JArray): JArray =
    JArray(values ++ that.values)

  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: StringBuilder, lvl: Int): StringBuilder = {
    out.append("[")

    var isFirst = true
    values foreach { v =>
      if (!isFirst) out.append("," + settings.spaceString)

      v.appendJSONStringBuilder(settings, out, lvl)

      isFirst = false
    }

    out.append("]")
  }
}
