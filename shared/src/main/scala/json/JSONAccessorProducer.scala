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

import scala.annotation.implicitNotFound
import scala.reflect.{ClassTag, classTag}

//TODO: add describe() to accessors, and default toString = describe.toString

object JSONAccessorProducer {
  def of[T](obj: T)(implicit acc: JSONAccessor[T]): JSONAccessor[T] = acc

  def of[T, U <: JValue](implicit acc: json.JSONAccessorProducer[T, U]) = acc

  def create[T: ClassTag, U <: JValue](toJ: T => U,
      fromJ: JValue => T): JSONAccessorProducer[T, U] = new JSONAccessorProducer[T, U] {
    def createJSON(from: T): U = toJ(from)
    def fromJSON(from: JValue): T = fromJ(from)
    def clazz = classTag[T].runtimeClass

    def describe = baseDescription ++ Map(
      "accessorType" -> "JSONAccessor.create"
    ).js
  }

  /** helper JSON producer trait used for contravariant type T */
  trait CreateJSON[-T, +JV <: JValue] {
    def createJSON(obj: T): JV
  }
}

@implicitNotFound(msg = "No implicit JSONAccessorProducer for [${T}, ${JV}] in scope. Did you define/import one? https://github.com/MediaMath/scala-json/blob/master/ACCESSORS.md")
trait JSONAccessorProducer[T, +JV <: JValue] extends JSONAccessorProducer.CreateJSON[T, JV] {
  def clazz: Class[_]
  def fromJSON(js: JValue): T

  //emits a JSON description of the accessor and all the others it encounters.
  def describe: JValue

  final protected def baseDescription: JObject = Map(
    "accessorClass" -> getClass.getName,
    "valueClass" -> clazz.getName,
    "accessorType" -> getClass.getSimpleName
  ).js

  final override def toString = describe.toString

  /** This is a special override for optimized versions that work with raw String keys */
  def fromString(value: String): T = fromJSON(JString(value))
  /** This is a special override for optimized versions that work with raw String keys */
  def toString(value: T): String = createJSON(value).jString.str

  def createSwaggerProperty: JObject = {
    val StrClassTag = ClassTag(classOf[String])

    val dat = (ClassTag(clazz): ClassTag[_]) match {
      case ClassTag.Int =>
        Map("type" -> "integer", "format" -> "int32")
      case ClassTag.Long =>
        Map("type" -> "long", "format" -> "int64")
      case ClassTag.Float =>
        Map("type" -> "number", "format" -> "float")
      case ClassTag.Double =>
        Map("type" -> "number", "format" -> "double")
      case StrClassTag =>
        Map("type" -> "string")
      case ClassTag.Byte =>
        Map("type" -> "string", "format" -> "byte")
      case ClassTag.Boolean =>
        Map("type" -> "boolean")
      case x => Map("type" -> x.runtimeClass.getSimpleName)
    }

    JValue(dat).toJObject + ("required" -> JTrue)
  }

  def extraSwaggerModels: Seq[JObject] = Nil
}