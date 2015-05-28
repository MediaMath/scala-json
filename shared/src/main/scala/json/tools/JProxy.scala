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

package json.tools

import json._

//TODO: this kinda sucks. Look into a way to pick out notes from sub-classes
//like actually building up the alternate looking models and usings notes to show where it differs
//pull common fields for a 'common' description, then descs for each model.

//TODO: pull annotations from underlying traits (if not already there)
trait JProxy {
  type ProxyType <: Proxy
  type ItemType <: To

  type JV = JValue

  implicit def acc: JSONAccessorProducer[ProxyType, JV]

  trait Proxy {
    def self: ItemType
  }

  trait To {
    def proxyTo: ProxyType
  }

  val proxyAccessor: JSONAccessorProducer[ItemType, JV] =
    new JSONAccessorProducer[ItemType, JV] {
      def fromJSON(jv: JValue): ItemType = jv.toObject[ProxyType].self

      def createJSON(x: ItemType): JValue = x.proxyTo.js

      def clazz: Class[_] = acc.clazz

      override def extraSwaggerModels: Seq[JObject] = acc.extraSwaggerModels

      override def createSwaggerProperty: JObject = acc.createSwaggerProperty
    }
}