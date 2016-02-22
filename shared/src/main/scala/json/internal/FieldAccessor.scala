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

trait FieldAccessor[T] extends Product2[Class[T], String] {
  def name: String
  def getFrom(obj: T): Any
  def defOpt: Option[Any]
  def getJValue(obj: T): JValue

  def annos: Set[FieldAccessorAnnotation]
  def pTypeAccessors: IndexedSeq[Option[JSONAccessor[_]]]
  def fieldAccessor: JSONAccessor[T]
  def objClass: Class[T]

  def default: Any = defOpt.get
  def hasDefault: Boolean = defOpt.isDefined
  def fieldClass = fieldAccessor.clazz

  def _1 = objClass
  def _2 = name

  def canEqual(that: Any) = that.isInstanceOf[FieldAccessor[_]]

  override def equals(that: Any) = that match {
    case x: FieldAccessor[T] =>
      x.objClass == objClass && x.name == name
    case _ => false
  }

  override def productPrefix = "FieldAccessor("
  override def toString = productPrefix + productIterator.mkString(", ") + ")"
  override def hashCode = scala.runtime.ScalaRunTime._hashCode(this)

  def createSwaggerProperty: JObject = {
    val defaultJs = defOpt.map(JValue.from) match {
      case Some(_ :JObject) => JObject.empty
      case Some(_ :JArray)  => JObject.empty
      case Some(x)          => JObject("defaultValue" -> x)
      case _                => JObject.empty
    }

    val desc = annos.map {
      case FieldDescriptionGeneric(desc) => desc
      case _                             => ""
    }.mkString("\n")

    val requiredJs = if (hasDefault) JObject("required" -> JFalse)
    else JObject.empty

    fieldAccessor.createSwaggerProperty ++ defaultJs ++ requiredJs ++ JObject(
      "description" -> desc.js
    )
  }

  def createSwagger: JValue = try {
    val accProp = createSwaggerProperty

    JObject(name -> accProp)
  } catch {
    case e: NullPointerException => {
      JObject(name -> "null???".js)
    }
  }
}
