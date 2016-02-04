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

import language.dynamics

object JDynamic {
  //implicit def toJValue(dynamic: JDynamic): JValue = dynamic.value
}

case class JDynamic(value: JValue) extends Dynamic {
  def applyDynamic(method: String)(idx: Int): JDynamic = value(method).jArray(idx).dynamic

  def apply(method: String): JDynamic = value(method).dynamic

  def selectDynamic(field: String): JDynamic = value(field).dynamic

  def apply(idx: Int): JDynamic = value.jArray(idx).dynamic

  def length: JDynamic = value match {
    case x: JArray => JNumber(x.length).dynamic
    case _ => selectDynamic("length")
  }

  override def toString = value.toString

  override def hashCode = value.hashCode()
}

