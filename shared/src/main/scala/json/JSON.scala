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

package json

import json.internal.JSONAnnotations

/** Package space for all scala-json annotations */
object annotations extends JSONAnnotations.TypeAdder

/** Constants that can be used to avoid allocation */
object Constants {
  val number1 = new JNumber(1)
  val number0 = new JNumber(0)
  val trueString = JString("true")
  val falseString = JString("false")
  val nullString = JString("null")
}

/** Base type for all JSON exceptions. All exceptions are based off of case classes. */
sealed trait JSONException extends Exception with Product

/** Base type for 'input format' exceptions. */
sealed trait InputFormatException extends JSONException {
  //def fieldName: String
  def prependFieldName(newField: String): InputFormatException
  def messageWithField: String = getMessage
  def getExceptions: Set[InputFormatException]
}

/** Base type for 'input format' exceptions that can contain exceptions for multiple fields. */
sealed trait InputFieldException extends JSONException with InputFormatException {
  def fieldName: String
  override def messageWithField: String = fieldName + ": " + getMessage
  def getExceptions: Set[InputFormatException] = Set(this)
}

case class GenericJSONException(msg: String = "JSON Exception") extends Exception(msg) with JSONException
/** Exception that happens when accessing fields of JUndefined */
case class JUndefinedException(msg: String = "Cannot access JUndefined") extends Exception(msg) with JSONException
/** Exception that happens when creating a JObject with ordered pairs that contain duplicate keys */
case class DuplicateKeyException(msg: String = "Duplicate keys in object!") extends Exception(msg) with JSONException

/** Exception that contains format exceptions for multiple fields */
case class InputFormatsException(set: Set[InputFormatException])
    extends Exception(set.map(_.messageWithField).mkString(", "),
      set.headOption.getOrElse(null))
    with InputFormatException {
  def prependFieldName(newField: String): InputFormatException =
    InputFormatsException(set.map(_.prependFieldName(newField)))
  def getExceptions: Set[InputFormatException] = set
}

/** Generic field exception for parsing errors on a field. */
case class GenericFieldException(fieldName: String, cause: Throwable)
    extends Exception(fieldName + " failed to parse", cause)
    with InputFieldException {
  def prependFieldName(newField: String): InputFormatException =
    if (fieldName == "") copy(fieldName = newField)
    else copy(fieldName = newField + "." + fieldName)
}

/** Field exception for a field that was expecting a value but had no default. */
case class MissingFieldException(fieldName: String)
    extends Exception(fieldName + " not provided and is not optional")
    with InputFieldException {
  def prependFieldName(newField: String): InputFormatException =
    if (fieldName == "") copy(fieldName = newField)
    else copy(fieldName = newField + "." + fieldName)
}

/** Field exception for a field that was expecting a numeric type. */
case class NumericTypeException(fieldName: String, v: Any, typString: String)
    extends Exception(v + " is not valid (wrong size) for " + typString)
    with InputFieldException {
  def prependFieldName(newField: String): InputFormatException =
    if (fieldName == "") copy(fieldName = newField)
    else copy(fieldName = newField + "." + fieldName)
}

/** Field exception that occurs when receiving an unexpected type for a field. */
case class InputTypeException(fieldName: String, needsType: String, isType: String, v: Any)
    extends Exception(s"$needsType expected but found $isType (of value $v)")
    with InputFieldException {
  def prependFieldName(newField: String): InputFormatException =
    if (fieldName == "") copy(fieldName = newField)
    else copy(fieldName = newField + "." + fieldName)
}