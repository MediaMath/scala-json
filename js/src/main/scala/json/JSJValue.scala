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
  def fromNativeJS(default: => JValue)(v: Any): JValue = v match {
    case x: JValue => x
    case x if js.isUndefined(x) => JUndefined
    case x: String => JString(x)
    case seq0 if js.Array.isArray(seq0.asInstanceOf[js.Any]) =>
      val seq = seq0.asInstanceOf[js.Array[js.Any]]
      val jvals: IndexedSeq[JValue] = seq map JSJValue.safeReverseFromNativeJS

      JArray(jvals)
    case null      => JNull
    case true      => JTrue
    case false     => JFalse
    case x: Double => JNumber(x)
    case x0: js.Object =>
      val x = x0.asInstanceOf[js.Dynamic]
      val seq = (js.Object keys x0).map { key =>
        val value: JValue = JSJValue safeReverseFromNativeJS x.selectDynamic(key)
        JString(key) -> value
      }
      JObject(seq: _*)
    case _ => default
  }

  def fromNativeJS(x: Any): JValue =
    fromNativeJS(sys.error(s"cannot turn $x into a JValue from native JS value"))(x)

  def from(v: Any): JValue =
    JValue.fromAnyInternal(fromNativeJS(v))(v)

  def toJS(x: JValue): js.Any = x.toNativeJS

  //this method simply reverses the order of the pattern match for performance sake
  private def safeReverseFromNativeJS(x: Any): JValue = {
    def err = sys.error(s"cannot turn $x into a JValue from non-native JS value")
    fromNativeJS(JValue.fromAnyInternal(err)(x))(x)
  }
}
