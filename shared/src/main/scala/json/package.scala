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

import json.internal.JSONAnnotations

/**
 * ==Overview==
 * This is the package namespace for everything scala-json.
 * It is recommended to import json._ in your file when working
 * with scala json. The base type for all JSON values is the
 * [[json.JValue]].
 *
 * ==JValue Types==
 *  - [[json.JNumber]] - JSON Numeric Type
 *  - [[json.JString]] - JSON String Type
 *  - [[json.JBoolean]] - JSON Boolean Types: [[json.JTrue]] and [[json.JFalse]]
 *  - [[json.JUndefined]] - JSON undefined value
 *  - [[json.JNull]] - JSON null value
 */
package object json extends JSONAnnotations.TypeAdder {
  /** Special NaN JNumber value */
  val JNaN = JNumber(Double.NaN)

  type JSONAccessor[T] = JSONAccessorProducer[T, JValue]
  val JSONAccessor = internal.JSONAccessor

  def fromJSON[T](jval: JValue)(implicit acc: JSONAccessor[T]) =
    acc.fromJSON(jval)

  def toJSONString[T](obj: T)(implicit acc: JSONAccessor[T]) =
    obj.js.toString

  /** This is the class extension that allows you to use the .js method on any value */
  implicit class AnyValJSEx[T](val x: T) extends AnyVal {
    def js[U <: JValue](implicit acc: JSONProducer[T, U]): U = acc.createJSON(x)

    /*def js(implicit acc: CaseClassObjectAccessor[T]): JObject =
			acc.createJSON(x)*/
  }

  implicit class JSONStringOps(val str: String) extends AnyVal {
    def ->>(v: JValue): (JString, JValue) = JString(str) -> v

    def jValue = JValue.fromString(str)
  }

  private[json] def fieldCatch[T](name: String)(f: => T): T = try f catch {
    case e: InputFormatException =>
      throw e.prependFieldName(name)
    case e: Throwable =>
      throw GenericFieldException(name, e)
  }
}
