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

package json.shadow

import json.JSJValue.TypedArrayExtractor
import json.internal.{JArrayPrimitive, BaseVMContext}
import json._
import scalajs.js.{JSON => NativeJSON}
import scala.scalajs.js.annotation.JSExport
import scalajs.js
import scalajs.js.typedarray



object VMContext extends BaseVMContext {
  def fromString(str: String): JValue = {
    def reviver = (key: js.Any, value: js.Any) =>
      (JSJValue fromNativeJS value).asInstanceOf[js.Any]
    val parsed = NativeJSON.parse(str, reviver)

    //run it again incase the reviver didnt work
    JSJValue from parsed
  }

  def fromAny(value: Any): JValue = JSJValue.from(value)

  trait JValueCompanionBase {
    implicit case object JsAnyAccessor extends JSONAccessorProducer[js.Any, JValue] {
      val clazz = classOf[JValue]

      def createJSON(obj: js.Any): JValue = JValue from obj
      def fromJSON(jValue: JValue): js.Any = jValue.toNativeJS

      def describe = baseDescription
    }
  }

  private[json] trait JValueBase {
    //this adds JSON.stringify support
    @JSExport def toJSON(): js.Any = toNativeJS

    def toNativeJS: js.Any
  }

  private[json] trait JBooleanBase {
    def value: Boolean
    
    def toNativeJS: js.Any = value 
  }

  private[json] trait JNumberBase {
    def value: Double
    
    def toNativeJS: js.Any = value
  }

  private[json] trait JArrayBase extends JValueBase {
    def values: Iterable[JValue]

    def toNativeJS: js.Any = this match {
      case JArrayPrimitive(wrapped: js.WrappedArray[Any]) => wrapped.array
      case JArrayPrimitive(seq) => seq.to[js.Array]
      case _ => new js.Array[js.Any] ++ values.iterator.map(_.toNativeJS)
    }

    @JSExport final override def toJSON(): js.Any = this match {
      case JArrayPrimitive(wrapped: js.WrappedArray[Any]) => wrapped.array match {
        case TypedArrayExtractor(_) => wrapped.to[js.Array]
        case x => x
      }
      case JArrayPrimitive(seq) => seq.to[js.Array]
      case _ => values.iterator.map(_.toJSON).to[js.Array]
    }
  }

  private[json] trait JObjectBase extends JValueBase {
    def iterator: Iterator[JObject.Pair]

    def toNativeJS: js.Any = {
      val result = js.Dictionary.empty[js.Any]
      iterator.foreach { pair =>
        result(pair._1.str) = pair._2.toNativeJS
      }
      result
    }

    @JSExport final override def toJSON(): js.Any = {
      val result = js.Dictionary.empty[js.Any]
      iterator.foreach { pair =>
        result(pair._1.str) = pair._2.toJSON
      }
      result
    }
  }

  private[json] trait JUndefinedBase {
    def toNativeJS: js.Any = js.undefined
  }

  private[json] trait JNullBase {
    def toNativeJS: js.Any = null
  }

  private[json] trait JStringBase {
    def value: String

    def toNativeJS: js.Any = value
  }

  final def quoteJSONString(string: String, sb: StringBuilder): StringBuilder =
    sb append NativeJSON.stringify(string)
}
