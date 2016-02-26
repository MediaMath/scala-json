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
trait CaseClassObjectAccessor[T] extends ObjectAccessor[T] {
  //function used to create field name from member name
  def nameMap: String => String

  lazy val fieldMap = fields.map(field => field.name -> field).toMap

  def getValue(obj: T, key: String) = fieldMap(key).getFrom(obj)

  def createJSON(obj: T): JObject = JObject((fields map { field =>
    field.name -> field.getJValue(obj)
  }): _*)

  override def toString = "CaseClassObjectAccessor"

  def describe: JObject = baseDescription ++ Map(
    "fields" -> (JObject.empty ++ fieldMap.map {
      case (name, fieldAcc) => name -> JObject(
        "type" -> fieldAcc.fieldAccessor.describe,
        "default" -> fieldAcc.defOpt.map(_.js(fieldAcc.fieldAccessor)).getOrElse(JUndefined)
      ).toJValue
    }),
    "accessorClass" -> JString("json.internal.CaseClassObjectAccessor")
  )

  def createSwaggerModels: Seq[JObject] = {
    val properties = fields.map(_.createSwagger.toJObject).foldLeft(JObject())(_ ++ _)

    //TODO: need annos for root class
    val description = ""

    val id = clazz.getSimpleName
    val extras = fields.flatMap(_.fieldAccessor.extraSwaggerModels)

    val jv = JObject(
      "id" -> id,
      "description" -> description,
      //"required".js -> required,
      "properties" -> properties
    )

    extras :+ JObject(id -> jv)
  }
}