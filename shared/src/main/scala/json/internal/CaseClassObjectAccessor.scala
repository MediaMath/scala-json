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

import json._

import scala.annotation.implicitNotFound

/** Base trait for macro generated ObjectAccessors. Not recommended to extend. */
abstract class CaseClassObjectAccessor[T] extends ObjectAccessor[T] {
  //function used to create field name from member name
  //def nameMap: String => String

  //lazy val fieldMap = fields.map(field => field.name -> field).toMap[String, FieldAccessor[T, _]]

  //final def getValue(obj: T, key: String) = fieldMap(key).getFrom(obj)

  final def createJSON(obj: T): JObject = JObject((fields map { field =>
    field.name -> field.getJValue(obj)
  }): _*)

  override def toString = "CaseClassObjectAccessor"
}