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
import json.internal.DefaultVMContext.PrimitiveArray

import scala.reflect.ClassTag

object PrimitiveJArray {
  private[json] def unapply[T](x: PrimitiveJArray[T]): Option[IndexedSeq[T]] = Some(x.primArr)

  sealed trait SpecialBuilders[+U[_] <: Iterable[_]] {
    def canIndexedSeq: Boolean
  }

  object SpecialBuilders {
    implicit case object ForIndexedSeq extends SpecialBuilders[IndexedSeq] {
      def canIndexedSeq: Boolean = true
    }

    case object ForAny extends SpecialBuilders[Nothing] {
      def canIndexedSeq: Boolean = false
    }
  }

  trait Builder[T] { _: JSONAccessorProducer[T, _] =>
    def classTag: ClassTag[T]

    def toDouble(x: T): Double
    def fromDouble(x: Double): T

    def toJValue(x: T): JValue = JNumber(toDouble(x))

    def createFrom(prim: PrimitiveArray[T]) = new PrimitiveJArray[T](prim)(this)

    def toPrimitiveString(x: T): String = x.toString

    def create(length: Int) = {
      val prim = VM.Context.createPrimitiveArray[T](length)(classTag)
      createFrom(prim)
    }
  }
}

final class PrimitiveJArray[T: PrimitiveJArray.Builder] private[json] (private[json] val primArr: PrimitiveArray[T]) extends JArray {
  type Elem = T

  val builder = implicitly[PrimitiveJArray.Builder[T]]

  def numStringFor(idx: Int): String =  builder toPrimitiveString primArr(idx)

  def length = primArr.length

  override def apply(idx: Int): JValue = builder toJValue primArr(idx)

  def getDouble(idx: Int): Double = builder toDouble primArr(idx)

  def copyFrom[F](from: PrimitiveJArray[F]): Unit = {
    require(length == from.length)

    for(idx <- 0 until length)
      primArr(idx) = builder.fromDouble(from getDouble idx)
  }

}

//TODO: one day we should specialize this class with the following form, all VM specific:
/*
abstract class PrimitiveJArray[@specialized T: PrimitiveJArray.Builder] private[json] extends JArray {
  type Elem = T

  def toWrapped: IndexedSeq[T]
  def length: Int
  private[json] def applyRaw(idx: Int): T
  private[json] def update(idx: Int, value: T): Unit

  val builder = implicitly[PrimitiveJArray.Builder[T]]

  def numStringFor(idx: Int): String = builder.toPrimitiveString(applyRaw(idx))

  override def apply(idx: Int): JValue = builder.toJValue(applyRaw(idx))

  def getDouble(idx: Int): Double = builder toDouble applyRaw(idx)

  def copyFrom[F](from: PrimitiveJArray[F]): Unit = {
    require(length == from.length)

    for(idx <- 0 until length)
      this(idx) = builder.fromDouble(from getDouble idx)
  }
}
 */