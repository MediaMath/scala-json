/*
 * Copyright 2017 MediaMath, Inc
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
import json.internal.PrimitiveJArray.Builder
import json.internal.{JanssonDeserializer, PrimitiveJArray, SimpleStringBuilder, BaseVMContext}

import scala.collection.immutable.StringOps
import scala.collection.mutable
import scala.reflect.ClassTag

object VMContext extends BaseVMContext {
  def newVMStringBuilder: SimpleStringBuilder = new SimpleStringBuilder {
    val builder = new StringBuilder(128)

    def append(str: String): internal.SimpleStringBuilder = {
      builder append str
      this
    }

    def append(char: Char): SimpleStringBuilder = {
      builder.append(char)
      this
    }

    def ensureCapacity(cap: Int): Unit = builder.ensureCapacity(cap)

    def result(): String = builder.result()
  }

  //TODO: do these need to be specialized?
  def createPrimitiveArray[/*@specialized */T: ClassTag](length: Int): PrimitiveArray[T] =
    wrapPrimitiveArray(new Array[T](length))

  def wrapPrimitiveArray[/*@specialized */T: ClassTag](from: Array[T]): PrimitiveArray[T] = from

  def fromString(str: String): JValue = {
    JanssonDeserializer.parseString(str)
  }

  def fromAny(value: Any): JValue = JValue.fromAnyInternal(value)

  final def quoteJSONString(string: String, sb: SimpleStringBuilder): SimpleStringBuilder = {
    require(string != null)

    sb.ensureCapacity(string.length)

    sb.append(JanssonDeserializer.serializeString(string))

    sb
  }

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
      case x: IndexedSeq[T] => Some(builder.createFrom(x))
      case _ => None
    }
  }
}


