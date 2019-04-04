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

import json._
import json.internal.DefaultVMContext.PrimitiveArray
import json.internal.{BaseVMContext, JSJValue, PrimitiveJArray, SimpleStringBuilder}
import JSJValue.TypedArrayExtractor
import json.Implicits._

import scala.collection.mutable
import scala.reflect.ClassTag
import scalajs.js.{JSON => NativeJSON}
import scalajs.js.annotation.JSExport
import scalajs.js

object VMContext extends BaseVMContext {
  //optimized to use js array.join- good stable performance on most js, plus smaller footprint
  def newVMStringBuilder: SimpleStringBuilder = new SimpleStringBuilder {
    val arr = new js.Array[String]

    def append(str: String): SimpleStringBuilder = {
      arr.push(str)
      this
    }

    def append(char: Char): SimpleStringBuilder = append(char.toString)
    def ensureCapacity(cap: Int): Unit = {}

    def result(): String = arr.join("")
  }

  def fromString(str: String): json.JValue = {
    def reviver = (key: js.Any, value: js.Any) =>
      (JSJValue fromNativeJS value).asInstanceOf[js.Any]
    val parsed = NativeJSON.parse(str, reviver)

    //run it again incase the reviver didnt work
    JSJValue from parsed
  }

  def fromAny(value: Any): JValue = JSJValue.from(value)

  def newJValueFromArray(arr: Array[_]): JArray = {
    import json.accessors._

    arr match {
      case x: Array[Byte] => new PrimitiveJArray[Byte](wrapPrimitiveArray(x))
      case x: Array[Short] => new PrimitiveJArray[Short](wrapPrimitiveArray(x))
      case x: Array[Int] => new PrimitiveJArray[Int](wrapPrimitiveArray(x))
      case x: Array[Long] => new PrimitiveJArray[Long](wrapPrimitiveArray(x))
      case x: Array[Double] => new PrimitiveJArray[Double](wrapPrimitiveArray(x))
      case x: Array[Float] => new PrimitiveJArray[Float](wrapPrimitiveArray(x))
      case x: Array[Boolean] => new PrimitiveJArray[Boolean](wrapPrimitiveArray(x))
    }
  }

  def extractPrimitiveJArray[T: ClassTag: PrimitiveJArray.Builder](x: Iterable[T]): Option[JArray] = {
    val builder = implicitly[PrimitiveJArray.Builder[T]]

    x match {
      case x: mutable.WrappedArray[T] => Some(newJValueFromArray(x.array))
      case x: js.WrappedArray[T] => Some(x.array match {
        case TypedArrayExtractor(typed) => JSJValue.typedArrayToJArray(typed)
        case jArray => new PrimitiveJArray[T](wrapPrimitiveArray(jArray))
      })
      case x: IndexedSeq[T] => Some(builder.createFrom(x))
      case _ => None
    }
  }

  trait JValueCompanionBase {
    implicit case object JsAnyAccessor extends JSONAccessorProducer[js.Any, JValue] {
      val clazz = classOf[JValue]

      def createJSON(obj: js.Any): JValue = JValue from obj
      def fromJSON(jValue: JValue): js.Any = jValue.toNativeJS
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
      case PrimitiveJArray(wrapped: js.WrappedArray[_]) => wrapped.array
      case PrimitiveJArray(seq) => seq.to[js.Array]
      case _ => new js.Array[js.Any] ++ values.iterator.map(_.toNativeJS)
    }

    @JSExport final override def toJSON(): js.Any = this match {
      case PrimitiveJArray(wrapped: js.WrappedArray[_]) => wrapped.array match {
        case TypedArrayExtractor(_) => wrapped.to[js.Array]
        case x => x
      }
      case PrimitiveJArray(seq) => seq.to[js.Array]
      case _ => values.iterator.map(_.toJSON).to[js.Array]
    }
  }

  private[json] trait JObjectBase extends JValueBase {
    def iterator: Iterator[JObject.Pair]

    def toNativeJS: js.Any = {
      val result = js.Dictionary.empty[js.Any]
      iterator.foreach { pair =>
        result(pair._1) = pair._2.toNativeJS
      }
      result
    }

    @JSExport final override def toJSON(): js.Any = {
      val result = js.Dictionary.empty[js.Any]
      iterator.foreach { pair =>
        result(pair._1) = pair._2.toJSON
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

  final def quoteJSONString(string: String, sb: SimpleStringBuilder): SimpleStringBuilder =
    sb append NativeJSON.stringify(string)

  /*def createPrimitiveArray[@specialized T: ClassTag](length: Int): DefaultVMContext.PrimitiveArray[T] = {
    val tag = implicitly[ClassTag[T]]
    val inst = tag.runtimeClass match {
      case java.lang.Byte.TYPE      => ByteImpl(buffer.asInstanceOf[Seq[Byte]])
      case java.lang.Short.TYPE     => ShortImpl(buffer.asInstanceOf[Seq[Short]])
      //case java.lang.Character.TYPE =>
      case java.lang.Integer.TYPE   => IntImpl(buffer.asInstanceOf[Seq[Int]])
      case java.lang.Long.TYPE      => LongImpl(buffer.asInstanceOf[Seq[Long]])
      case java.lang.Float.TYPE     => FloatImpl(buffer.asInstanceOf[Seq[Float]])
      case java.lang.Double.TYPE    => DoubleImpl(buffer.asInstanceOf[Seq[Double]])
      case java.lang.Boolean.TYPE   => BooleanImpl(buffer.asInstanceOf[Seq[Boolean]])
      //case java.lang.Void.TYPE      =>
      case x                        => throw new Error("Unknown JArrayPrimitive for class " + x)
    }
  }*/

  //TODO: optimize with typed arrays when possible
  def createPrimitiveArray[T: ClassTag](length: Int): PrimitiveArray[T] =
    wrapPrimitiveArray(new Array[T](length))

  def wrapPrimitiveArray[T: ClassTag](from: js.Array[T]): PrimitiveArray[T] = from

  def wrapPrimitiveArray[T: ClassTag](from: Array[T]): PrimitiveArray[T] = from
}
