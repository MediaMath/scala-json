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

import json.JObject._
import json._

import scala.collection.IterableLike
import scala.collection.generic.{Growable, CanBuildFrom, GenericCompanion}
import scala.collection.immutable.VectorBuilder
import scala.collection.mutable

trait JObjectCompanion {
  import JObject.Pair

  def newJObjectBuilder: mutable.Builder[Pair, JObject] = new Builder

  def newBuilder[A]: mutable.Builder[A, scala.collection.immutable.Iterable[A]] =
    scala.collection.immutable.Iterable.newBuilder

  //TODO: size is slow, but allows order, checks for dupes.....
  def apply(values: Pair*): JObject = {
    val map = values.toMap

    require(map.size == values.length, "JObject created with duplicate keys")

    new JObject(map)(values)
  }

  def apply(fields: Map[String, JValue]): JObject = new JObject(fields)(fields)

  def newCanBuildFrom = new CanBuildFrom[TraversableOnce[Pair], Pair, JObject] {
    def apply(from: TraversableOnce[Pair]) = newJObjectBuilder // ++= from
    def apply() = newJObjectBuilder
  }

  /** Builder for JObject. Removes dupes. */
  final class Builder extends mutable.Builder[Pair, JObject] {
    val builder = new VectorBuilder[Pair]
    var fields = Map.empty[String, JValue]

    def isEmpty = fields.isEmpty

    def alreadyHas(key: String) = fields contains key

    override def ++=(other: TraversableOnce[Pair]): this.type = other match {
      //only trust map as we know there's no dupes
      case x: scala.collection.Map[String, JValue] if isEmpty =>
        builder ++= x
        fields = x.toMap
        this
      case _ => super.++=(other)
    }

    def +=(item: Pair): this.type = {
      //only add new keys
      if(!alreadyHas(item._1)) builder += item

      fields += item
      this
    }

    def result: JObject = {
      new JObject(fields)(builder.result())
    }

    def clear() {
      builder.clear()
      fields = Map.empty
    }
  }
}

trait JObjectLike { _: JObject =>
  import JObject.Pair

  lazy val uuid = UUID.randomUUID.toString

  def keyIterator = iterator.map(_._1)

  def empty = JObject.empty

  def default(key: String): JValue = JUndefined

  def toMap: Map[String, JValue] = fields

  override def values: Iterable[JValue] = fields.values
  def value = fields.map(pair => pair._1 -> pair._2.value)

  override def apply(x: JValue): JValue = x match {
    case JString(str) => apply(str)
    case _ => apply(x.toString)
  }

  override def apply(key: String): JValue = get(key).getOrElse(default(key))

  def get(key: String): Option[JValue] = fields.get(key)

  def iterator: Iterator[Pair] = iterable.iterator

  def +[B1 >: JValue](kv: (String, B1)): JObject = {
    val (key, v: JValue) = kv
    val newMap = fields + (key -> v)

    //append new keys to end
    if (fields.get(key).isDefined)
      new JObject(newMap)(iterable)
    else {
      new JObject(newMap)(iterable ++ Seq(key -> v))
    }
  }

  def -(key: String): JObject = {
    val newIterator = iterator.filter(pair => pair._1 != key)

    new JObject(fields - key)(newIterator.toVector)
  }

  def toJString: JString =
    sys.error("cannot convert JObject to String")

  def toJNumber: JNumber = JNaN
  def toJBoolean: JBoolean = JTrue

  override def jValue = this
  override def keys: Iterable[JString] = iterable.map(pair => JString(pair._1))

  override def isObject = true

  override def toJObject: JObject = this

  override def jObject: JObject = this

  override def canEqual(that: Any) = true

  def contains(key: String) = fields contains key

  override def equals(that: Any) = that match {
    case x: JObject => x.fields == fields
    case _ => false
  }

  def ++(that: JObject): JObject = {
    val appendSeq = that.iterator.filter(pair => !fields.contains(pair._1))

    val builder = new VectorBuilder[Pair]
    builder ++= iterable
    builder ++= appendSeq

    new JObject(this.fields ++ that.fields)(builder.result)
  }

  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: SimpleStringBuilder, lvl: Int): SimpleStringBuilder = {
    val nl = settings.newLineString
    val tab = settings.tabString

    def tabs = out append settings.nTabs(lvl)

    if(isEmpty) out append "{}"
    else {
      out append ("{" + nl)

      var isFirst = true
      keyIterator foreach { key =>
        val v = apply(key)

        if (v !== JUndefined) {
          if (!isFirst) out.append("," + settings.spaceString + nl)

          tabs append tab
          JString(key).appendJSONStringBuilder(settings, out, lvl + 1)
          out append (":" + settings.spaceString)
          v.appendJSONStringBuilder(settings, out, lvl + 1)

          isFirst = false
        }
      }

      out append nl
      tabs append "}"
    }
  }
}
