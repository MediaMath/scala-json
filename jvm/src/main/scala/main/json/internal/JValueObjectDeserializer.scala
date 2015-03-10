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

package json.internal

import com.fasterxml.jackson.core.{ JsonParser, JsonToken }
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{ DeserializationContext, ObjectMapper }
import json._

import scala.collection.immutable.{StringOps, VectorBuilder}

class JValueObjectDeserializer extends StdDeserializer[JValue](classOf[JValue]) {
  import com.fasterxml.jackson.core.JsonToken._

  @volatile private var valCache = Map[String, JString]()

  def enableStringCache = true
  def maxCacheLength = 128
  def maxCacheSizePerThread = 1 //MB
  val maxCacheSizeBeforeFlush = (maxCacheSizePerThread * 1024 * 1024) / (maxCacheLength * 2)

  lazy val mapper = {
    val mpr = new ObjectMapper()
    val module = new SimpleModule("MyModule")

    module.addDeserializer(classOf[JValue], this)

    mpr.registerModule(module)

    mpr
  }

  def deserialize(jp: JsonParser, ctx: DeserializationContext): JValue = {

    jp.getCurrentToken match {
      case NOT_AVAILABLE => sys.error("JSON parser - unexpected end of token")
      case START_ARRAY   => parseArray(jp, ctx)
      case END_ARRAY     => sys.error("JSON parser - unexpected end of array")
      case START_OBJECT  => parseObject(jp, ctx)
      case END_OBJECT    => sys.error("JSON parser - unexpected end of object")
      case FIELD_NAME    => sys.error("JSON parser - unexpected field name")
      case VALUE_STRING  => jString(jp.getText)
      case VALUE_NUMBER_INT =>
        // For [JACKSON-100], see above =>
        // (ctxt.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)) {
        //	return jp.getBigIntegerValue();
        //}

        //return jp.getNumberValue();
        JNumber(_parseDouble(jp, ctx))
      case VALUE_NUMBER_FLOAT =>
        // For [JACKSON-72], see above
        //if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
        //return jp.getDecimalValue();
        //}
        JNumber(_parseDouble(jp, ctx))
      case VALUE_TRUE  => JTrue
      case VALUE_FALSE => JFalse
      case VALUE_NULL  => JNull
      case VALUE_EMBEDDED_OBJECT =>
        sys.error("JSON parser - unexpected embedded object")
    }
  }

  override def deserializeWithType(jp: JsonParser,
    ctx: DeserializationContext, typDeser: TypeDeserializer): JValue = {
    deserialize(jp, ctx)
  }

  def reset() {
    if (valCache.size > maxCacheSizeBeforeFlush)
      valCache = valCache.empty
  }

  def jString(str: String): JString =
    if (!enableStringCache) JString(str)
    else if (str.length > maxCacheLength) JString(str)
    else valCache.get(str) match {
      case Some(x) => x
      case _ =>
        val x = JString(str)
        valCache += str -> x
        x
    }

  def parseObject(jp: JsonParser, ctx: DeserializationContext): JObject = {
    require(jp.getCurrentToken == START_OBJECT, "not start of object")

    val seqBuilder = new VectorBuilder[(JString, JValue)]

    while (jp.nextToken != END_OBJECT) {
      require(jp.getCurrentToken == FIELD_NAME, "unexpected token " + jp.getCurrentToken)
      val key = jString(jp.getText)

      jp.nextToken
      val v = deserialize(jp, ctx)

      seqBuilder += key -> v
    }

    val res = seqBuilder.result

    if (res.isEmpty) JObject.empty else JObject(res: _*)
  }

  def parseArray(jp: JsonParser, ctx: DeserializationContext): JArray = {
    require(jp.getCurrentToken == START_ARRAY, "not start of array")

    val builder = new VectorBuilder[JValue]
    while (jp.nextToken != END_ARRAY) {
      builder += deserialize(jp, ctx)
    }

    val res = builder.result

    if (res.isEmpty) JArray.empty else JArray(res)
  }
}