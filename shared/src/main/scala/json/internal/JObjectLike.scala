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

import json.JObject._
import json._

import scala.collection.IterableLike
import scala.collection.generic.{CanBuildFrom, GenericCompanion}
import scala.collection.immutable.VectorBuilder
import scala.collection.mutable

trait JObjectCompanion {
  import JObject.Pair

  def newJObjectBuilder: mutable.Builder[Pair, JObject] = new Builder

  def newBuilder[A]: mutable.Builder[A, scala.collection.immutable.Iterable[A]] =
    scala.collection.immutable.Iterable.newBuilder

  //TODO: allows duplicates...
  def apply(values: Pair*): JObject = {
    val map = values.toMap

    if(map.size != values.length) throw DuplicateKeyException()

    new JObject(map)(values)
  }

  def apply[T](obj: T)(implicit accessor: ObjectAccessor[T]): JObject =
    accessor.createJSON(obj).toJObject

  def apply(fields: Map[JString, JValue]): JObject =
    new JObject(fields)(fields)

  def newCanBuildFrom = new CanBuildFrom[TraversableOnce[Pair], Pair, JObject] {
    def apply(from: TraversableOnce[Pair]) = newJObjectBuilder // ++= from
    def apply() = newJObjectBuilder
  }

  class Builder extends mutable.Builder[Pair, JObject] {
    val builder = new VectorBuilder[Pair]

    def +=(item: Pair): this.type = {
      builder += item
      this
    }

    def result: JObject = {
      JObject(builder.result: _*)
    }

    def clear() {
      builder.clear
    }
  }
}

trait JObjectLike { _: JObject =>
  import JObject.Pair

  lazy val uuid = UUID.randomUUID.toString

  def keyIterator = iterable.iterator.map(_._1)

  def empty = JObject.empty

  def default(key: JString): JValue = JUndefined

  override def values: Iterable[JValue] = fields.values
  def value = fields.map(pair => pair._1.str -> pair._2.value)

  override def apply(keyV: JValue) = {
    val key = keyV.toJString

    get(key).getOrElse(default(key))
  }

  def get(key: JString): Option[JValue] = fields.get(key)

  def iterator: Iterator[Pair] = keyIterator map { k =>
    k -> apply(k: JValue)
  }

  def +[B1 >: JValue](kv: (JString, B1)): JObject = {
    val thisMap = fields

    val (key, v: JValue) = kv

    val newMap = (thisMap + kv).asInstanceOf[Map[JString, JValue]]

    //append new keys to end
    if (thisMap.get(key).isDefined)
      new JObject(newMap)(iterable)
    else {
      val builder = new VectorBuilder[Pair]
      builder ++= iterable
      builder += key -> v
      new JObject(newMap)(builder.result())
    }
  }

  def -(key: JString): JObject = {
    val newIterator = iterator.filter(pair => pair._1 != key)

    new JObject(fields - key)(newIterator.toVector)
  }

  def toJString: JString =
    sys.error("cannot convert JObject to JString")

  def toJNumber: JNumber = JNaN
  def toJBoolean: JBoolean = JTrue

  override def jValue = this
  override def keys: Iterable[JString] = iterable.map(_._1)

  override def isObject = true

  override def toJObject: JObject = this

  override def jObject: JObject = this

  override def canEqual(that: Any) = true

  override def equals(that: Any) = that match {
    case JObject(f) => f == fields
    case _ => false
  }

  def ++(that: JObject): JObject = {
    val thisMap = toMap
    val thatMap = that.toMap
    val thisSeq = iterator
    val thisKeySet = thisMap.keySet
    val appendSeq = that.iterator.filter(pair => !thisKeySet(pair._1))

    val builder = new VectorBuilder[Pair]
    builder ++= iterable
    builder ++= appendSeq

    new JObject(thisMap ++ thatMap)(builder.result)
  }

  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: StringBuilder, lvl: Int): StringBuilder = {
    val nl = settings.newLineString
    val tab = settings.tabString

    def tabs = out append settings.nTabs(lvl)

    out append ("{" + nl)

    var isFirst = true
    keyIterator foreach { key =>
      val v = apply(key: JValue)
      if ((v !== JUndefined) && (!settings.ignoreNulls || (v !== JNull))) {
        if (!isFirst) out.append("," + settings.spaceString + nl)

        tabs append tab
        key.appendJSONStringBuilder(settings, out, lvl + 1)
        out append (":" + settings.spaceString)
        v.appendJSONStringBuilder(settings, out, lvl + 1)

        isFirst = false
      }
    }

    out append nl
    tabs append "}"
  }
}
