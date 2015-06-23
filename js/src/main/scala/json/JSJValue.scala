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

import scala.scalajs.js
import scalajs.js.{JSON => NativeJSON}
import js.JSConverters._

object JSJValue {
  def from(v: Any): JValue = v match {
    case x if js.isUndefined(x) => JUndefined
    case x: JValue => x
    case seq0 if js.Array.isArray(seq0.asInstanceOf[js.Any]) =>
      val seq: Seq[Any] = seq0.asInstanceOf[js.Array[_]]
      val jvals: Seq[JValue] = seq.map(JSJValue.from)

      JArray(jvals)
    //TODO: look into re-implementing PF inline in this match
    case x: String => JString(x)
    case x if JValue.fromAnyInternalPF.isDefinedAt(x) => JValue.fromAnyInternal(x)
    case x0: js.Object =>
      val x = x0.asInstanceOf[js.Dynamic]
      val seq = (js.Object keys x0).toSeq.map { key =>
        val value = JSJValue from x.selectDynamic(key)
        JString(key) -> value
      }
      JObject(seq: _*)
    case x         => sys.error(s"cannot turn $x into a JValue")
  }

  def toJS(from: JValue): js.Any = from match {
    case x: JObject =>
      val vals = for ((JString(key), value) <- x.iterator)
        yield key -> toJS(value)

      vals.toMap.toJSDictionary
    case JArray(values) =>
      values.map(toJS).toJSArray
    case JUndefined => js.undefined
    case JNull      => null
    case x          => x.value.asInstanceOf[js.Any] //assumed to be a primitive here
  }
}
