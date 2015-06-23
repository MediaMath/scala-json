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

object annotations extends JSONAnnotations.TypeAdder

object Constants {
  val number1 = JNumber(1)
  val number0 = JNumber(0)
  val trueString = JString("true")
  val falseString = JString("false")
  val nullString = JString("null")
}

sealed trait JSONException extends Exception

case class GenericJSONException(msg: String = "JSON Exception") extends Exception(msg) with JSONException
case class JUndefinedException(msg: String = "Cannot access JUndefined") extends Exception(msg) with JSONException

trait InputFormatException extends Exception with Product {
  //def fieldName: String
  def prependFieldName(newField: String): InputFormatException
  def messageWithField: String = getMessage
  def getExceptions: Set[InputFormatException]
}

trait InputFieldException extends InputFormatException {
  def fieldName: String
  override def messageWithField: String = fieldName + ": " + getMessage
  def getExceptions: Set[InputFormatException] = Set(this)
}

case class InputFormatsException(set: Set[InputFormatException])
    extends Exception(set.map(_.messageWithField).mkString(", "),
      set.headOption.getOrElse(null))
    with InputFormatException {
  def prependFieldName(newField: String): InputFormatException =
    InputFormatsException(set.map(_.prependFieldName(newField)))
  def getExceptions: Set[InputFormatException] = set
}

case class GenericFieldException(fieldName: String, cause: Throwable)
    extends Exception(fieldName + " failed to parse", cause)
    with InputFieldException {
  def prependFieldName(newField: String): InputFormatException =
    if (fieldName == "") copy(fieldName = newField)
    else copy(fieldName = newField + "." + fieldName)
}

case class MissingFieldException(fieldName: String)
    extends Exception(fieldName + " not provided and is not optional")
    with InputFieldException {
  def prependFieldName(newField: String): InputFormatException =
    if (fieldName == "") copy(fieldName = newField)
    else copy(fieldName = newField + "." + fieldName)
}

case class NumericTypeException(fieldName: String, v: Any, typString: String)
    extends Exception(v + " is not valid (wrong size) for " + typString)
    with InputFieldException {
  def prependFieldName(newField: String): InputFormatException =
    if (fieldName == "") copy(fieldName = newField)
    else copy(fieldName = newField + "." + fieldName)
}

case class InputTypeException(fieldName: String, needsType: String, isType: String, v: Any)
    extends Exception(s"$needsType expected but found $isType (of value $v)")
    with InputFieldException {
  def prependFieldName(newField: String): InputFormatException =
    if (fieldName == "") copy(fieldName = newField)
    else copy(fieldName = newField + "." + fieldName)
}