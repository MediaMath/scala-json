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

package json

/**
 * Base accessor class for field accessors in objects (seen via case class macro generally).
 *
 * The contains extended run-time info such as field name, field defaults, type access etc.
 *
 * @tparam T type of class that owns the fields
 * @tparam U type of the field
 */
abstract class FieldAccessor[T, U] extends Product2[Class[T], String] {
  type FieldType = U

  /** Name after translating via annotations if any */
  def name: String
  /** Get the raw field data from an object of type T */
  def getFrom(obj: T): U
  /** Get raw field data for default if any */
  def defOpt: Option[U]

  /** Contains any field annotations found (extending FieldAccessorAnnotation) */
  def annos: Set[FieldAccessorAnnotation]

  /** Accessor for field type */
  def fieldAccessor: JSONAccessor[U]

  /** Class of actual object that owns the field */
  def objClass: Class[T]

  /** Get raw default or exception if none */
  def default: Any = defOpt.get
  def hasDefault: Boolean = defOpt.isDefined

  /** Class of field type */
  def fieldClass = fieldAccessor.clazz

  /** Gets actual JValue for a field from an object of type T */
  def getJValue(obj: T): JValue = fieldAccessor createJSON getFrom(obj)

  def describe: JObject = fieldAccessor.describe + ("default" -> defaultJs)

  /** Gets actual JValue defualt value if any otherwise [[JUndefined]] */
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