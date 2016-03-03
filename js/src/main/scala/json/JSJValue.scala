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

import json.internal.DefaultVMContext.PrimitiveArray
import json.internal.PrimitiveJArray

import scala.scalajs.js
import scala.scalajs.js.typedarray._
import scalajs.js.{JSON => NativeJSON}

import js.JSConverters._

import json.accessors._

object JSJValue {
  private implicit def typedArrToArr[T, U](x: TypedArray[T, U]): js.Array[T] = x.asInstanceOf[js.Array[T]]

  def newPrimArr[T, U](from: TypedArray[T, U]) = new PrimitiveArray[T] {
    def length: Int = from.length

    def update(idx: Int, value: T): Unit = from(idx) = value

    def apply(idx: Int): T = from(idx)

    //for direct wrapping if/when available
    def toIndexedSeq: IndexedSeq[T] = from.asInstanceOf[js.Array[T]]

    def underlying = js.WrappedArray(from)
  }

  private[json] object TypedArrayExtractor {
    def unapply(x: js.Object): Option[TypedArray[_, _]] = {
      val dynamic = x.asInstanceOf[js.Dynamic]
      if(!scalajs.runtime.Bits.areTypedArraysSupported) None
      else if(js.isUndefined(dynamic.length) || js.isUndefined(dynamic.byteLength)) None
      else Some(x.asInstanceOf[TypedArray[_, _]])
    }
  }

  def typedArrayToJArray(arr: TypedArray[_, _]) = arr match {
    case x: Int8Array => new PrimitiveJArray(newPrimArr(x))
    case x: Int16Array => new PrimitiveJArray(newPrimArr(x))
    case x: Int32Array => new PrimitiveJArray(newPrimArr(x))
    case x: Uint8Array => new PrimitiveJArray(newPrimArr(x))
    case x: Uint16Array => new PrimitiveJArray(newPrimArr(x))
    case x: Uint32Array => new PrimitiveJArray(newPrimArr(x))
    case _ => sys.error("Unsupported native array of type " + js.typeOf(arr))
  }

  def fromNativeJS(default: => JValue)(v: Any): JValue = v match {
    case x: JValue => x
    case x if js.isUndefined(x) => JUndefined
    case x: String => JString(x)
    case seq0 if js.Array.isArray(seq0.asInstanceOf[js.Any]) =>
      val seq = seq0.asInstanceOf[js.Array[js.Any]]
      //TODO: this needs a more optimized implementation.... perhaps lazy-eval ?
      val jvals: IndexedSeq[JValue] = seq map JSJValue.safeReverseFromNativeJS

      JArray(jvals)
    case null      => JNull
    case true      => JTrue
    case false     => JFalse
    case x: Double => JNumber(x)
    case TypedArrayExtractor(arr) => typedArrayToJArray(arr)
    case x0: js.Object =>
      val x = x0.asInstanceOf[js.Dynamic]
      val seq = (js.Object keys x0).map { key =>
        val value: JValue = JSJValue safeReverseFromNativeJS x.selectDynamic(key)
        key -> value
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
