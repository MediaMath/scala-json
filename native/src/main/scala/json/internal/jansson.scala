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

import scala.scalanative.native
import native._
import Nat._

@native.link("jansson")
@native.extern
object jansson {
  type JSON_ERROR_TEXT_LENGTH = Digit[_1, Digit[_6, _0]]
  type JSON_ERROR_SOURCE_LENGTH = Digit[_8, _0]

  type json_type = CInt //enum

  type json_error_t = CStruct5[
      CInt, //line
      CInt, //column
      CInt, //position,
      CArray[CChar, JSON_ERROR_SOURCE_LENGTH], //source
      CArray[CChar, JSON_ERROR_TEXT_LENGTH] //text
  ]

  type json_t = CStruct2[
      json_type, //type
      CSize //refcount
  ]

  type json_iter = Ptr[Byte]

  def json_loads(input: CString, flags: CSize, error: Ptr[json_error_t]): Ptr[json_t] = extern

  def json_dumps(json: Ptr[json_t], flags: CSize): CString = extern

  def json_delete(json: Ptr[json_t]): Unit = extern

  def json_array_size(json: Ptr[json_t]): CSize = extern

  def json_array_get(json: Ptr[json_t], index: CSize): Ptr[json_t] = extern

  def json_object_iter(obj: Ptr[json_t]): json_iter = extern

  def json_object_iter_at(obj: Ptr[json_t], key: CString): json_iter = extern

  def json_object_iter_next(obj: Ptr[json_t], itr: json_iter): json_iter = extern

  def json_object_iter_key(itr: json_iter): CString = extern

  def json_object_iter_value(itr: json_iter): Ptr[json_t] = extern

  def json_number_value(num: Ptr[json_t]): Double = extern

  def json_string(value: CString): Ptr[json_t] = extern

  def json_string_value(str: Ptr[json_t]): CString = extern
}

object janssonConstants {
  //json_type
  val JSON_OBJECT  = 0
  val JSON_ARRAY   = 1
  val JSON_STRING  = 2
  val JSON_INTEGER = 3
  val JSON_REAL    = 4
  val JSON_TRUE    = 5
  val JSON_FALSE   = 6
  val JSON_NULL    = 7

  //flags
  val JSON_REJECT_DUPLICATES      = 0x1
  val JSON_DISABLE_EOF_CHECK      = 0x2
  val JSON_DECODE_ANY             = 0x4
  val JSON_DECODE_INT_AS_REAL     = 0x8
  val JSON_ALLOW_NUL              = 0x10
  
  val JSON_MAX_INDENT             = 0x1F
  def JSON_INDENT(n: Int)         = ((n) & JSON_MAX_INDENT)
  val JSON_COMPACT                = 0x20
  val JSON_ENSURE_ASCII           = 0x40
  val JSON_SORT_KEYS              = 0x80
  val JSON_PRESERVE_ORDER         = 0x100
  val JSON_ENCODE_ANY             = 0x200
  val JSON_ESCAPE_SLASH           = 0x400
  def JSON_REAL_PRECISION(n: Int) = (((n) & 0x1F) << 11)
  val JSON_EMBED                  = 0x10000
}