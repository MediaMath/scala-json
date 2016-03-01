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

import java.util.UUID

import json._

import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.VectorBuilder
import scala.collection.mutable.Builder
import scala.collection.{IndexedSeqLike, immutable}
import scala.reflect.ClassTag

trait JArrayCompanion/* extends GenericCompanion[scala.collection.immutable.Seq]*/ {
  def newCanBuildFrom = new CanBuildFrom[TraversableOnce[JValue], JValue, JArray] {
    def apply(from: TraversableOnce[JValue]) = newJArrayBuilder // ++= from
    def apply() = newJArrayBuilder
  }

  def newJArrayBuilder: Builder[JValue, JArray] = new JArrayBuilder

  def apply(values: immutable.IndexedSeq[JValue]): JArray = new JArraySeqImpl(values)
  def apply(seq: TraversableOnce[JValue]): JArray = apply(seq.toIndexedSeq)
  def apply(seq: JValue*): JArray = apply(seq.toIndexedSeq)
  def apply[T: JSONAccessor](seq: T*): JArray = apply(seq.iterator.map(_.js).toIndexedSeq)

  class JArrayBuilder extends Builder[JValue, JArray] {
    val builder = new VectorBuilder[JValue]

    def +=(item: JValue): this.type = {
      builder += item
      this
    }

    def result: JArray = JArray(builder.result)

    def clear() = builder.clear
  }

  def newBuilder[A]: Builder[A, immutable.IndexedSeq[A]] =
    newJArrayBuilder.asInstanceOf[Builder[A, immutable.IndexedSeq[A]]]
}

trait JArrayLike extends immutable.IndexedSeq[JValue] with IndexedSeqLike[JValue, JArray] { this: JArray =>
  lazy val uuid = UUID.randomUUID.toString

  protected def _values: immutable.IndexedSeq[JValue]

  def toJString: JString = JString("array " + uuid) //this... should be different
  def toJNumber: JNumber = JNaN
  def toJBoolean: JBoolean = JTrue

  def length = _values.length

  def value = values.map(_.value)

  //override def companion: GenericCompanion[scala.collection.immutable.Iterable] = JArray
  override def newBuilder = JArray.newJArrayBuilder

  override def seq = this

  override def jValue = this
  override def toJArray: JArray = this

  override def keys = (0 until length).map(JNumber(_))

  override def jArray: JArray = this

  def apply(key: JNumber): JValue = apply(key.num.toInt)
  def apply(key: Int): JValue = _values(key)

  override def apply(key: JValue): JValue = key match {
    case JString("length") => JNumber(length)
    case _ =>
      val jNum = key.toJNumber
      val jInt = jNum.num.toInt

      if (jNum.num != jInt) JUndefined
      else if (jInt < 0) JUndefined
      else if (jInt >= length) JUndefined
      else apply(jInt)
  }

  def ++(that: JArray): JArray =
    JArray(values ++ that.values)
}

private final case class JArraySeqImpl(override val values: immutable.IndexedSeq[JValue]) extends JArray {
  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: StringBuilder, lvl: Int): StringBuilder = {
    out.append("[")

    var isFirst = true
    values foreach { v =>
      if (!isFirst) out.append("," + settings.spaceString)

      v.appendJSONStringBuilder(settings, out, lvl)

      isFirst = false
    }

    out.append("]")
  }

  protected def _values: immutable.IndexedSeq[JValue] = values
}

object JArrayPrimitive {
  def apply[T: ClassTag](buffer: Iterable[T]): JArrayPrimitive[T] = {
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
  }

  def unapply(x: JArrayPrimitive[_]): Option[Iterable[_]] = Some(x.buffer)

  trait SpecialBuilders[U[_] <: TraversableOnce[_]] {
    def buildFrom[T](iterable: Iterable[T]): U[T]

    def isGeneric = false
  }
  object SpecialBuilders {
    implicit case object ForIndexedSeq extends SpecialBuilders[IndexedSeq] {
      def buildFrom[T](iterable: Iterable[T]): IndexedSeq[T] = iterable match {
        case x: IndexedSeq[T] => x
        case _ => iterable.toIndexedSeq
      }
    }

    implicit case object ForSeq extends SpecialBuilders[Seq] {
      def buildFrom[T](iterable: Iterable[T]): Seq[T] = iterable match {
        case x: Seq[T] => x
        case _ => iterable.toSeq
      }
    }

    implicit case object ForIterable extends SpecialBuilders[Iterable] {
      def buildFrom[T](iterable: Iterable[T]): Iterable[T] = iterable
    }

    case object ForAny extends SpecialBuilders[Nothing] {
      def apply[T[_] <: TraversableOnce[_]]: SpecialBuilders[T] = this.asInstanceOf[SpecialBuilders[T]]
      def buildFrom[T](iterable: Iterable[T]) = sys.error("no real implementation")

      override def isGeneric = true
    }
  }

  sealed trait PrimitiveAccessor[T] {
    def isPrimitive = true

    def createJSON(obj: Iterable[T]): JArray

    def iterableFromJValue(js: JValue): Iterable[T] = js match {
      case x: IntImpl => iterableFrom(x)
      case x: ByteImpl => iterableFrom(x)
      case x: ShortImpl => iterableFrom(x)
      case x: LongImpl => iterableFrom(x)
      case x: BooleanImpl => iterableFrom(x)
      case x: FloatImpl => iterableFrom(x)
      case x: DoubleImpl => iterableFrom(x)
      case x: JArray => iterableFrom(x)
      case x => throw InputTypeException("",
        "array", x.getClass.getName, x)
    }

    def fromDoubleIterator(itr: Iterator[Double]): Iterable[T]

    def iterableFrom(x: IntImpl): Iterable[T] = fromDoubleIterator(x.buffer.iterator.map(x => x: Double))
    def iterableFrom(x: ByteImpl): Iterable[T] = fromDoubleIterator(x.buffer.iterator.map(x => x: Double))
    def iterableFrom(x: ShortImpl): Iterable[T] = fromDoubleIterator(x.buffer.iterator.map(x => x: Double))
    def iterableFrom(x: LongImpl): Iterable[T] = fromDoubleIterator(x.buffer.iterator.map(x => x: Double))
    def iterableFrom(x: BooleanImpl): Iterable[T] = fromDoubleIterator(x.buffer.iterator.map(x => if(x) 1 else 0))
    def iterableFrom(x: FloatImpl): Iterable[T] = fromDoubleIterator(x.buffer.iterator.map(x => x: Double))
    def iterableFrom(x: DoubleImpl): Iterable[T] = fromDoubleIterator(x.buffer.iterator.map(x => x: Double))
    def iterableFrom(x: JArray): Iterable[T] = fromDoubleIterator(x.iterator.map(_.toJNumber.num))
  }

  object PrimitiveAccessor {
    case object NonPrimitive extends PrimitiveAccessor[Nothing] {
      override def isPrimitive = false

      def apply[T]: PrimitiveAccessor[T] = this.asInstanceOf[PrimitiveAccessor[T]]

      def createJSON(obj: Iterable[Nothing]): JArray = sys.error("not to be used directly")

      def fromDoubleIterator(itr: Iterator[Double]): Iterable[Nothing] = sys.error("not to be used directly")
    }

    implicit case object JArrayPrimitiveIntAccessor extends PrimitiveAccessor[Int] {
      def createJSON(obj: Iterable[Int]): JArray = IntImpl(obj)

      override def iterableFrom(x: IntImpl): Iterable[Int] = x.buffer

      def fromDoubleIterator(itr: Iterator[Double]): Iterable[Int] = itr.map(_.toInt).toArray[Int]
    }

    implicit case object JArrayPrimitiveLongAccessor extends PrimitiveAccessor[Long] {
      def createJSON(obj: Iterable[Long]): JArray = LongImpl(obj)

      override def iterableFrom(x: LongImpl): Iterable[Long] = x.buffer

      def fromDoubleIterator(itr: Iterator[Double]): Iterable[Long] = itr.map(_.toLong).toArray[Long]
    }

    implicit case object JArrayPrimitiveShortAccessor extends PrimitiveAccessor[Short] {
      def createJSON(obj: Iterable[Short]): JArray = ShortImpl(obj)

      override def iterableFrom(x: ShortImpl): Iterable[Short] = x.buffer

      def fromDoubleIterator(itr: Iterator[Double]): Iterable[Short] = itr.map(_.toShort).toArray[Short]
    }

    implicit case object JArrayPrimitiveByteAccessor extends PrimitiveAccessor[Byte] {
      def createJSON(obj: Iterable[Byte]): JArray = ByteImpl(obj)

      override def iterableFrom(x: ByteImpl): Iterable[Byte] = x.buffer

      def fromDoubleIterator(itr: Iterator[Double]): Iterable[Byte] = itr.map(_.toByte).toArray[Byte]
    }

    implicit case object JArrayPrimitiveBooleanAccessor extends PrimitiveAccessor[Boolean] {
      def createJSON(obj: Iterable[Boolean]): JArray = BooleanImpl(obj)

      override def iterableFrom(x: BooleanImpl): Iterable[Boolean] = x.buffer

      def fromDoubleIterator(itr: Iterator[Double]): Iterable[Boolean] = 
        itr.map(x => if(x == 0) false else true).toArray[Boolean]
    }

    implicit case object JArrayPrimitiveDoubleAccessor extends PrimitiveAccessor[Double] {
      def createJSON(obj: Iterable[Double]): JArray = DoubleImpl(obj)

      override def iterableFrom(x: DoubleImpl): Iterable[Double] = x.buffer

      def fromDoubleIterator(itr: Iterator[Double]): Iterable[Double] = itr.toArray[Double]
    }

    implicit case object JArrayPrimitiveFloatAccessor extends PrimitiveAccessor[Float] {
      def createJSON(obj: Iterable[Float]): JArray = FloatImpl(obj)

      override def iterableFrom(x: FloatImpl): Iterable[Float] = x.buffer

      def fromDoubleIterator(itr: Iterator[Double]): Iterable[Float] = itr.map(_.toFloat).toArray[Float]
    }
  }

  case class IntImpl(buffer: Iterable[Int]) extends JArrayPrimitive[Int] {
    protected def getJValueIterator: Iterator[JNumber] = buffer.iterator.map(JNumber(_))

    protected def getStringValueIterator: Iterator[String] = buffer.iterator.map(_.toString)
  }

  case class ByteImpl(buffer: Iterable[Byte]) extends JArrayPrimitive[Byte] {
    protected def getJValueIterator: Iterator[JNumber] = buffer.iterator.map(JNumber(_))

    protected def getStringValueIterator: Iterator[String] = buffer.iterator.map(_.toString)
  }

  case class ShortImpl(buffer: Iterable[Short]) extends JArrayPrimitive[Short] {
    protected def getJValueIterator: Iterator[JNumber] = buffer.iterator.map(JNumber(_))

    protected def getStringValueIterator: Iterator[String] = buffer.iterator.map(_.toString)
  }

  case class LongImpl(buffer: Iterable[Long]) extends JArrayPrimitive[Long] {
    protected def getJValueIterator: Iterator[JNumber] = buffer.iterator.map(JNumber(_))

    protected def getStringValueIterator: Iterator[String] = buffer.iterator.map(_.toString)
  }

  case class BooleanImpl(buffer: Iterable[Boolean]) extends JArrayPrimitive[Boolean] {
    protected def getJValueIterator: Iterator[JValue] = buffer.iterator.map(JBoolean(_))

    protected def getStringValueIterator: Iterator[String] = buffer.iterator map {
      case true => "true"
      case false => "false"
    }
  }

  case class FloatImpl(buffer: Iterable[Float]) extends JArrayPrimitive[Float] {
    protected def getJValueIterator: Iterator[JNumber] = buffer.iterator.map(JNumber(_))

    protected def getStringValueIterator: Iterator[String] = buffer.iterator map { v =>
      val x = v.toString

      if(x.endsWith(".0")) x.take(x.length - 2)
      else x
    }
  }

  case class DoubleImpl(buffer: Iterable[Double]) extends JArrayPrimitive[Double] {
    protected def getJValueIterator: Iterator[JNumber] = buffer.iterator.map(JNumber(_))

    protected def getStringValueIterator: Iterator[String] = buffer.iterator map { v =>
      val x = v.toString

      if(x.endsWith(".0")) x.take(x.length - 2)
      else x
    }
  }
}

sealed abstract class JArrayPrimitive[@specialized(Long, Int, Short, Byte, Double, Float, Boolean) T] extends JArray {
  def buffer: Iterable[T]
  protected def getJValueIterator: Iterator[JValue]
  protected def getStringValueIterator: Iterator[String]

  def appendJSONStringBuilder(settings: JSONBuilderSettings = JSONBuilderSettings.pretty,
      out: StringBuilder, lvl: Int): StringBuilder = {
    out.append("[")

    var isFirst = true
    getStringValueIterator foreach { v =>
      if (!isFirst) out.append("," + settings.spaceString)

      out append v

      isFirst = false
    }

    out.append("]")
  }

  override lazy val values: immutable.IndexedSeq[JValue] = {
    val builder = new VectorBuilder[JValue]

    builder.sizeHint(buffer)
    builder ++= getJValueIterator

    builder.result()
  }

  protected def _values: immutable.IndexedSeq[JValue] = values
}

//case class JArrayPrimitive[@specialized(Long, Int, Short, Byte, Double, Float) T](buffer: IndexedSeq[T]) extends JArray() {


