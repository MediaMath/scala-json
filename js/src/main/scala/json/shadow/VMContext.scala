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

import json.internal.BaseVMContext
import json.{JSONAccessorProducer, JSJValue, JValue}
import scalajs.js.{JSON => NativeJSON}
import scala.scalajs.js.annotation.JSExport
import scalajs.js

object VMContext extends BaseVMContext {
  def fromString(str: String): JValue = {
    def reviver = (key: js.Any, value: js.Any) =>
      (JSJValue from value).asInstanceOf[js.Any]
    val parsed = NativeJSON.parse(str, reviver)

    //run it again incase the reviver didnt work
    JSJValue from parsed
  }

  def fromAny(value: Any): JValue = JSJValue.from(value)

  trait JValueCompanionBase {
    implicit case object JsAnyAccessor extends JSONAccessorProducer[js.Any, JValue] {
      val clazz = classOf[JValue]

      def createJSON(obj: js.Any): JValue = JValue from obj
      def fromJSON(jValue: JValue): js.Any = JSJValue toJS jValue
    }
  }

  trait JValueBase { _: JValue =>
    //this adds JSON.stringify support
    @JSExport def toJSON: js.Any = JSJValue toJS this
  }

  def quoteJSONString(string: String): StringBuilder =
    new StringBuilder(NativeJSON.stringify(string))

}
