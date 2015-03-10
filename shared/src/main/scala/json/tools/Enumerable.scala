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

package json.tools

import json._

import scala.reflect.ClassTag
import scala.reflect.classTag

abstract class Enumerator[T <: Enumerator[T]#Value: ClassTag]
  extends TypedEnumerator[String, T, JString]

abstract class TypedEnumerator[K, T <: TypedEnumerator[K, T, J]#Value: ClassTag, +J <: JValue](
    implicit acc0: JSONAccessorProducer[K, J]) {
  def values: Set[_ <: T]

  def valueMap[U](f: T => U): Map[U, T] = {
    val valSeq = values.toSeq

    val out = valSeq map { x =>
      (f(x), x)
    }

    require(out.length == valSeq.length, "Non unique value in enum!")

    out.toMap
  }

  trait Value { typed: T =>
    def key: K

    def enumerator = TypedEnumerator.this
    def acc = acc0
    def toJSON: J = key js acc
  }

  lazy val keyMap: Map[K, T] = valueMap(_.key)

  def default(jv: JValue): T = sys.error(s"Unknown Enumerable type $jv for ${classTag[T]}")

  implicit lazy val accessor = new JSONAccessorProducer[T, J] {
    def createJSON(from: T): J = from.toJSON// from.toJSON.toJString    dislikee
    def fromJSON(from: JValue): T = keyMap.getOrElse(from.to[K], default(from))
    def clazz = classTag[T].runtimeClass

    val jsValues = values.toSeq.map(_.toJSON)

    override def createSwaggerProperty: JObject =
      super.createSwaggerProperty ++ JObject(JString("enum") -> JArray(jsValues))
  }
}