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

abstract class FieldAccessor[T, U] extends Product2[Class[T], String] {
  type FieldType = U

  def name: String

  def getFrom(obj: T): U
  def defOpt: Option[U]

  def annos: Set[FieldAccessorAnnotation]
  def fieldAccessor: JSONAccessor[U]
  def objClass: Class[T]

  def default: Any = defOpt.get
  def hasDefault: Boolean = defOpt.isDefined
  def fieldClass = fieldAccessor.clazz

  def getJValue(obj: T): JValue = fieldAccessor createJSON getFrom(obj)

  def describe = JObject(
    "default" -> defaultJs,
    "type" -> fieldAccessor.describe
  )

  def defaultJs = defOpt map fieldAccessor.createJSON getOrElse JUndefined

  def _1 = objClass
  def _2 = name

  def canEqual(that: Any) = that.isInstanceOf[FieldAccessor[T, U]]

  override def equals(that: Any) = that match {
    case x: FieldAccessor[T, U] =>
      x.objClass == objClass && x.name == name
    case _ => false
  }

  override def productPrefix = "FieldAccessor("
  override def toString = productPrefix + productIterator.mkString(", ") + ")"
  override def hashCode = scala.runtime.ScalaRunTime._hashCode(this)
}
