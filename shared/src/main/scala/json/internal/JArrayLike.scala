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

import java.util.UUID

import json._

import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.VectorBuilder
import scala.collection.mutable.Builder
import scala.collection.{immutable, IndexedSeqLike}

trait JArrayCompanion/* extends GenericCompanion[scala.collection.immutable.Seq]*/ {
  def newCanBuildFrom = new CanBuildFrom[TraversableOnce[JValue], JValue, JArray] {
    def apply(from: TraversableOnce[JValue]) = newJArrayBuilder // ++= from
    def apply() = newJArrayBuilder
  }

  def newJArrayBuilder: Builder[JValue, JArray] = new JArrayBuilder

  def apply(values: immutable.IndexedSeq[JValue]): JArray = new JArraySeqImpl(values)
  def apply(seq: TraversableOnce[JValue]): JArray = apply(seq.toIndexedSeq)
  def apply(seq: JValue*): JArray = apply(seq.toIndexedSeq)
  def apply[T: JSONAccessor](seq: T*): JArray = apply(seq.iterator.map(_.js).toIndexedSeq)

  class JArrayBuilder extends Builder[JValue, JArray] {
    val builder = new VectorBuilder[JValue]

    def +=(item: JValue): this.type = {
      builder += item
      this
    }

    def result: JArray = JArray(builder.result)

    def clear() = builder.clear
  }

  def newBuilder[A]: Builder[A, immutable.IndexedSeq[A]] =
    newJArrayBuilder.asInstanceOf[Builder[A, immutable.IndexedSeq[A]]]
}

trait JArrayLike extends immutable.IndexedSeq[JValue] with IndexedSeqLike[JValue, JArray] { this: JArray =>
  override def apply(idx: Int): JValue

  lazy val uuid = UUID.randomUUID.toString

  def toJString: JString = JString("array " + uuid) //this... should be different
  def toJNumber: JNumber = JNaN
  def toJBoolean: JBoolean = JTrue

  def value: IndexedSeq[Any] = map(_.value)

  override def newBuilder = JArray.newJArrayBuilder

  override def seq = this
  override def values: Iterable[JValue] = this
  override def jValue = this
  override def toJArray: JArray = this

  override def keys = (0 until length).map(JNumber(_))

  override def jArray: JArray = this

  def apply(key: JNumber): JValue = apply(key.num.toInt)

  override def equals(that: Any): Boolean = that match {
    case x: JArrayLike =>
      val itr1 = iterator
      val itr2 = x.iterator

      while(itr1.hasNext && itr2.hasNext)
        if(itr1.next != itr2.next)
          return false

      !itr1.hasNext && !itr2.hasNext
    case _ => false
  }

  override def apply(key: JValue): JValue = key match {
    case JString("length") => JNumber(length)
    case _ =>
      val jNum = key.toJNumber
      val jInt = jNum.num.toInt

      if (jNum.num != jInt) JUndefined
      else if (jInt < 0) JUndefined
      else if (jInt >= length) JUndefined
      else apply(jInt)
  }

  def ++(that: JArray): JArray = this ++ that.iterator
}

private final class JArraySeqImpl(override val values: IndexedSeq[JValue]) extends JArray {
  def length = values.length

  def numStringFor(idx: Int): String = sys.error("unused")

  override def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: SimpleStringBuilder, lvl: Int): SimpleStringBuilder = {
    out.append("[")

    var isFirst = true
    for(v <- values) {
      if (!isFirst) out.append("," + settings.spaceString)

      v.appendJSONStringBuilder(settings, out, lvl)

      isFirst = false
    }

    out.append("]")
  }

  override def apply(idx: Int): JValue = values(idx)
}






