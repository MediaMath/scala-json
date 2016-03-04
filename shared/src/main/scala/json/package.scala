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

import json.exceptions.{GenericFieldException, InputFormatException}
import json.{JSONAccessorProducer, JValue}
import json.internal.JSONAnnotations

import scala.annotation.implicitNotFound
import scala.reflect.ClassTag
import scala.util.control.NonFatal

/**
 * ==Overview==
 * This is the package namespace for everything scala-json ( [[https://github.com/MediaMath/scala-json/]] ).
 * It is recommended to import json._ in your file when working
 * with scala json. The base type for all JSON values is the
 * [[json.JValue]].
 *
 * ==JValue Types==
 *  - [[json.JNumber]] - JSON Numeric Type
 *  - [[json.JString]] - JSON String Type
 *  - [[json.JObject]] - JSON Object Type
 *  - [[json.JArray]] - JSON Array Type
 *  - [[json.JBoolean]] - JSON Boolean Types: [[json.JTrue]] and [[json.JFalse]]
 *  - [[json.JUndefined]] - JS undefined value
 *  - [[json.JNull]] - JSON null value
 */
package object json extends JSONAnnotations with Implicits {
  /** Special NaN JNumber value */
  lazy val JNaN = JNumber(Double.NaN)

  /** JSONAccessor is shorthand for a [[JSONAccessorProducer]] of generic JValue type */
  @implicitNotFound(msg = "No implicit JSONAccessor for ${T} in scope. If using @accessor make sure macro-paradise plugin is enabled. https://github.com/MediaMath/scala-json/blob/master/ACCESSORS.md")
  type JSONAccessor[T] = JSONAccessorProducer[T, JValue]
  /** see [[JSONAccessorProducer]] */
  val JSONAccessor = JSONAccessorProducer

  /** Package space for all scala-json annotations */
  object annotations extends JSONAnnotations

  object accessors extends internal.Accessors

  /** Base type for all JSON exceptions. All exceptions are based off of case classes. */
  type JSONException = exceptions.JSONException

  def fromJSON[T](jval: JValue)(implicit acc: JSONAccessor[T]) = acc.fromJSON(jval)
  def toJSONString[T: JSONAccessor](obj: T) = obj.js.toString

  def accessorOf[T](implicit acc: JSONAccessor[T]) = acc
  def accessorFor[T](x: T)(implicit acc: JSONAccessor[T]) = acc

  /** Create a simple [[JValue]] [[JSONAccessor]] out of 'to' and 'from' functions. */
  def createAccessor[T: ClassTag](toJ: T => JValue, fromJ: JValue => T): JSONAccessor[T] =
    JSONAccessorProducer.create[T, JValue](toJ, fromJ)

  private[json] def fieldCatch[T](name: String)(f: => T): T = try f catch {
    case e: InputFormatException =>
      throw e.prependFieldName(name)
    case NonFatal(e) =>
      throw GenericFieldException(name, e)
  }
}

