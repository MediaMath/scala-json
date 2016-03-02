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

import scala.collection.immutable.StringOps

trait JStringLike extends VM.Context.JStringBase { _: JString =>
  def iterator: Iterator[JString] =
    (new StringOps(str)).toIterator.map(c => JString(c.toString))

  def toJBoolean: JBoolean = if (str.isEmpty) JFalse else JTrue
  def toJNumber: JNumber =
    if (str.trim.isEmpty) JNumber(0)
    else try JNumber(str.trim.toDouble) catch {
      case x: Throwable => JNaN
    }
  def toJString: JString = this
  override def toString = toJSONString

  override def apply(x: JValue): JString =
    str.charAt(x.toJNumber.value.toInt).toString.js

  override def jString: JString = this

  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: SimpleStringBuilder, lvl: Int): SimpleStringBuilder = JValue.Context.quoteJSONString(str, out)

  override def jValue = this

  override def hashCode = str.hashCode

  override def canEqual(that: Any) = that match {
    case _: String  => true
    case _: JString => true
    case _          => false
  }

  override def equals(that: Any) = that match {
    case x: String  => x == str
    case JString(x) => x == str
    case _          => false
  }

  def ->>[T <: JValue](other: T): (JString, T) = this -> other
}
