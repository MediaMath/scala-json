/*
 * Copyright 2017 MediaMath, Inc
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

import scalanative.native

import janssonConstants._
import jansson._
import native._
import json._

object JanssonDeserializer {
  def throwError(error: Ptr[json_error_t]): Unit = {
    val line = !error._1
    val column = !error._2
    val position = !error._3
    val source = native fromCString error._4.cast[CString]
    val text = native fromCString error._5.cast[CString]

    val errString = s"$text. line: $line col: $column pos: $position: $source"

    sys.error(errString)
  }

  def parseString(jsonString: String): JValue = Zone { implicit z =>
    val error = stackalloc[json_error_t]

    val decoded = json_loads(native toCString jsonString, JSON_DECODE_ANY, error)

    if(decoded == null)
      throwError(error)

    val output = nativeToJSON(decoded)

    json_delete(decoded)

    output
  }

  def serializeString(str: String): String = Zone { implicit z =>
    val encoded = json_string(native toCString str)

    val outNative = json_dumps(encoded, JSON_ENCODE_ANY)

    json_delete(encoded)

    require(outNative != null)

    val out = native fromCString outNative

    native.stdlib.free(outNative)

    out
  }

  def nativeToJSON(x: Ptr[json_t]): JValue = {
    val typ: json_type = !x._1
    
    typ match {
      case JSON_OBJECT =>
        //TODO: is this messing with ordering?
        JObject.empty ++ ObjectIterator(x)
      case JSON_ARRAY =>
        JArray.empty ++ ArrayIterator(x)
      case JSON_STRING =>
        JString(native fromCString json_string_value(x))
      case JSON_INTEGER =>
        JNumber(json_number_value(x))
      case JSON_REAL =>
        JNumber(json_number_value(x))
      case JSON_TRUE =>
        JTrue
      case JSON_FALSE =>
        JFalse
      case JSON_NULL =>
        JNull
    }
  }

  case class ObjectIterator(obj: Ptr[json_t]) extends Iterator[(String, JValue)] {
    private var itr = json_object_iter(obj)

    def hasNext: Boolean = itr != null

    def next(): (String, JValue) = {
      val key: CString = json_object_iter_key(itr)
      val value: Ptr[json_t] = json_object_iter_value(itr)

      itr = json_object_iter_next(obj, itr)

      (native fromCString key, nativeToJSON(value))
    }
  }

  case class ArrayIterator(arr: Ptr[json_t]) extends Iterator[JValue] {
    private var idx = 0

    override val size = json_array_size(arr).toInt

    def hasNext: Boolean = idx < size

    def next(): JValue = {
      val res = nativeToJSON(json_array_get(arr, idx))

      idx += 1

      res
    }
  }
}
