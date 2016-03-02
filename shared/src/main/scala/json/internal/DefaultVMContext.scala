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

import json._

import scala.reflect.ClassTag

trait BaseVMContext {
  def fromString(str: String): JValue
  def fromAny(value: Any): JValue
  def quoteJSONString(string: String, builder: SimpleStringBuilder): SimpleStringBuilder
  def newVMStringBuilder: SimpleStringBuilder
  def createPrimitiveArray[@specialized T: ClassTag](length: Int): DefaultVMContext.PrimitiveArray[T] /*{
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

    inst.asInstanceOf[JArrayPrimitive[T]]
  }*/

  private[json] trait JValueCompanionBase

  private[json] trait JValueBase
  private[json] trait JBooleanBase
  private[json] trait JNumberBase
  private[json] trait JArrayBase
  private[json] trait JObjectBase
  private[json] trait JUndefinedBase
  private[json] trait JNullBase
  private[json] trait JStringBase
}

object DefaultVMContext {
  //to be replaced via shadowing by build for proper VM
  object VMContext extends BaseVMContext {
    private[json] trait JValueCompanionBase

    private[json] trait JValueBase
    private[json] trait JBooleanBase
    private[json] trait JNumberBase
    private[json] trait JArrayBase
    private[json] trait JObjectBase
    private[json] trait JUndefinedBase
    private[json] trait JNullBase
    private[json] trait JStringBase

    def fromString(str: String): JValue = ???
    def fromAny(value: Any): JValue = ???
    def quoteJSONString(string: String, builder: SimpleStringBuilder): SimpleStringBuilder = ???
    def newVMStringBuilder: SimpleStringBuilder = ???
    def createPrimitiveArray[@specialized T: ClassTag](length: Int): PrimitiveArray[T] = ???
  }

  trait PrimitiveArray[@specialized T] {
    def length: Int
    def apply(idx: Int): T
    def update(idx: Int, value: T): Unit

    //for direct wrapping if/when available
    def toIndexedSeq: IndexedSeq[T]

    //def iterator: Iterator[T] = Iterator.from(0, length) map apply
  }
}
