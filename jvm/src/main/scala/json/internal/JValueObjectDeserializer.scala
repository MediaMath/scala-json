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

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{DeserializationContext, ObjectMapper}
import com.fasterxml.jackson.core.JsonToken._

import json._
import json.accessors._

import scala.annotation.switch
import scala.collection.immutable.VectorBuilder
import scala.collection.mutable

import shadow.{VMContext => JVMContext}

/** To be used THREAD LOCAL ONLY */
private[json] class JValueObjectDeserializer extends StdDeserializer[JValue](classOf[JValue]) {
  private val valCache = mutable.Map[String, JString]()
  private var cacheSizeChars = 0

  def enableStringCache = true
  def maxCacheStringLength = 128
  def maxCacheSizeMB = 5

  val maxCacheSizeChars = (maxCacheSizeMB * 1024 * 1024) / 2

  lazy val mapper = {
    val mpr = new ObjectMapper()
    val module = new SimpleModule("MyModule")

    module.addDeserializer(classOf[JValue], this)

    mpr.registerModule(module)

    mpr
  }

  def deserialize(jp: JsonParser, ctx: DeserializationContext): JValue = {
    (jp.getCurrentToken: @switch) match {
      case NOT_AVAILABLE => sys.error("JSON parser - unexpected end of token")
      case START_ARRAY   => parseArray(jp, ctx)
      case END_ARRAY     => sys.error("JSON parser - unexpected end of array")
      case START_OBJECT  => parseObject(jp, ctx)
      case END_OBJECT    => sys.error("JSON parser - unexpected end of object")
      case FIELD_NAME    => sys.error("JSON parser - unexpected field name")
      case VALUE_STRING  => jString(jp.getText)
      case VALUE_NUMBER_FLOAT | VALUE_NUMBER_INT => JNumber(_parseDouble(jp, ctx))
      case VALUE_TRUE  => JTrue
      case VALUE_FALSE => JFalse
      case VALUE_NULL  => JNull
      case VALUE_EMBEDDED_OBJECT =>
        sys.error("JSON parser - unexpected embedded object")
    }
  }

  override def deserializeWithType(jp: JsonParser,
      ctx: DeserializationContext, typDeser: TypeDeserializer): JValue =
    deserialize(jp, ctx)

  def reset() {
    while (cacheSizeChars > maxCacheSizeChars && valCache.nonEmpty) {
      valCache.headOption foreach {
        case (str, jstr) =>
          valCache -= str
          cacheSizeChars -= str.length
      }
    }

    if(valCache.isEmpty) cacheSizeChars = 0
  }

  def jString(str: String): JString =
    if (!enableStringCache) JString(str)
    else if (str.length > maxCacheStringLength) JString(str)
    else valCache.get(str) match {
      case Some(x) => x
      case None =>
        val x = JString(str)
        valCache += str -> x
        cacheSizeChars += str.length
        x
    }

  def parseObject(jp: JsonParser, ctx: DeserializationContext): JObject = {
    require(jp.getCurrentToken == START_OBJECT, "not start of object")

    val iterableBuilder = mutable.ArrayBuilder.make[(String, JValue)]

    var fields = Map.empty[String, JValue] //map builder just does this anyways....
    var isEmpty = true

    while (jp.nextToken != END_OBJECT) {
      require(jp.getCurrentToken == FIELD_NAME, "unexpected token " + jp.getCurrentToken)

      isEmpty = false

      val key = jp.getText

      jp.nextToken()

      val pair = key -> deserialize(jp, ctx)

      fields += pair
      iterableBuilder += pair
    }

    if (isEmpty) JObject.empty
    else new JObject(fields)(iterableBuilder.result())
  }

  def parseArray(jp: JsonParser, ctx: DeserializationContext): JArray = {
    require(jp.getCurrentToken == START_ARRAY, "not start of array")

    val anyBuilder = new VectorBuilder[JValue]

    var typeBroken = false

    def readAsJValue(): JArray = {
      do {
        anyBuilder += deserialize(jp, ctx)
      } while(jp.nextToken != END_ARRAY)

      JArray(anyBuilder.result())
    }

    jp.nextToken match {
      case END_ARRAY => JArray.empty
      case VALUE_NUMBER_INT =>
        val builder = mutable.ArrayBuilder.make[Int]
        builder.sizeHint(16)

        do {
          jp.getCurrentToken match {
            case VALUE_NUMBER_INT =>
              builder += jp.getIntValue()
            case _ =>
              typeBroken = true
          }
        } while(!typeBroken && jp.nextToken != END_ARRAY)

        if(typeBroken) {
          anyBuilder ++= builder.result().iterator.map(JNumber(_))
          readAsJValue()
        } else {
          new PrimitiveJArray(JVMContext wrapPrimitiveArray builder.result())
        }
      case VALUE_NUMBER_FLOAT =>
        val builder = mutable.ArrayBuilder.make[Double]
        builder.sizeHint(16)

        do {
          jp.getCurrentToken match {
            case VALUE_NUMBER_FLOAT =>
              builder += jp.getDoubleValue()
            case _ =>
              typeBroken = true
          }
        } while(!typeBroken && jp.nextToken != END_ARRAY)

        if(typeBroken) {
          anyBuilder ++= builder.result().iterator.map(JNumber(_))
          readAsJValue()
        } else {
          new PrimitiveJArray(JVMContext wrapPrimitiveArray builder.result())
        }
      case VALUE_TRUE | VALUE_FALSE =>
        val builder = mutable.ArrayBuilder.make[Boolean]
        builder.sizeHint(16)

        do {
          jp.getCurrentToken match {
            case VALUE_TRUE | VALUE_FALSE =>
              builder += jp.getBooleanValue
            case _ =>
              typeBroken = true
          }
        } while(!typeBroken && jp.nextToken != END_ARRAY)

        if(typeBroken) {
          anyBuilder ++= builder.result().iterator.map(JBoolean(_))
          readAsJValue()
        } else {
          new PrimitiveJArray(JVMContext wrapPrimitiveArray builder.result())
        }
      case _ => readAsJValue()
    }
  }
}