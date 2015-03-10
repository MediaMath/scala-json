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

package json.shadow

import json._
import json.internal.{BaseVMContext, JValueObjectDeserializer}

import scala.collection.immutable.StringOps

object VMContext extends BaseVMContext {
  trait JValueCompanionBase

  trait JValueBase

  val localMapper = new ThreadLocal[JValueObjectDeserializer] {
    override protected def initialValue: JValueObjectDeserializer =
      new JValueObjectDeserializer
  }

  def fromString(str: String): JValue = {
    val deser = localMapper.get

    val res = deser.mapper.readValue[JValue](str, classOf[JValue])

    deser.reset()

    res
  }

  def fromAny(value: Any): JValue = JValue.fromAnyInternal(value)

  //modified some escaping for '/'
  final def quoteJSONString(string: String): StringBuilder = {
    require(string != null)

    val len = string.length
    val sb = new StringBuilder(len + 4)

    sb.append('"')
    for (i <- 0 until len) {
      string.charAt(i) match {
        case c if c == '"' || c == '\\' => //Set('"', '\\') contains c =>
          sb.append('\\')
          sb.append(c)
        //not needed?
        /*case c if c == '/' =>
					//                if (b == '<') {
					sb.append('\\')
					//                }
					sb.append(c)*/
        case '\b' => sb.append("\\b")
        case '\t' => sb.append("\\t")
        case '\n' => sb.append("\\n")
        case '\f' => sb.append("\\f")
        case '\r' => sb.append("\\r")
        case c =>
          if (c < ' ') {
            val t = "000" + Integer.toHexString(c)
            sb.append("\\u" + t.substring(t.length() - 4))
          } else {
            sb.append(c)
          }
      }
    }
    sb.append('"')

    sb
  }
}

