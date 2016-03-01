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
import json.internal.JArrayPrimitive.{SpecialBuilders, PrimitiveAccessor}

import scala.collection.generic.CanBuildFrom
import scala.reflect.ClassTag

/** These accessors get their own lower-priority space in implicit resolution */
trait LowPriorityAccessors {
  class Tuple2Accessor[A: JSONAccessor, B: JSONAccessor] extends JSONAccessorProducer[(A, B), JArray] {
    def clazz: Class[_] = classOf[(A, B)]

    override def referencedTypes: Seq[JSONAccessorProducer[_, _]] = Seq(accessorOf[A], accessorOf[B])

    val aAcc = accessorOf[A]
    val bAcc = accessorOf[B]

    override def toString = "Tuple2Accessor"

    def fromJSON(js: JValue): (A, B) = js match {
      case JArray(x) if x.length == 2 =>
        (x(0).to[A], x(1).to[B])
      case x => throw InputTypeException("",
        "numeric", x.getClass.getName, x)
    }

    def createJSON(x: (A, B)): JArray = JArray(x._1.js, x._2.js)
  }

  implicit def tuple2Accessor[A: JSONAccessor, B: JSONAccessor] = new Tuple2Accessor[A, B]

  class Tuple3Accessor[A: JSONAccessor, B: JSONAccessor, C: JSONAccessor] extends JSONAccessorProducer[(A, B, C), JArray] {
    def clazz: Class[_] = classOf[(A, B, C)]

    val aAcc = accessorOf[A]
    val bAcc = accessorOf[B]
    val cAcc = accessorOf[C]

    override def referencedTypes: Seq[JSONAccessorProducer[_, _]] = Seq(accessorOf[A], accessorOf[B], accessorOf[C])

    override def toString = "Tuple3Accessor"

    def fromJSON(js: JValue): (A, B, C) = js match {
      case JArray(x) if x.length == 3 =>
        (x(0).to[A], x(1).to[B], x(2).to[C])
      case x => throw InputTypeException("",
        "numeric", x.getClass.getName, x)
    }

    def createJSON(x: (A, B, C)): JArray = JArray(x._1.js, x._2.js, x._3.js)
  }

  implicit def tuple3Accessor[A: JSONAccessor, B: JSONAccessor, C: JSONAccessor] = new Tuple3Accessor[A, B, C]

  implicit def iterableAccessor[T, U[T] <: Iterable[T]](
      implicit acc: JSONAccessor[T], cbf: CanBuildFrom[Nothing, T, U[T]],
      ctag: ClassTag[U[T]],
      primitive: PrimitiveAccessor[T] = PrimitiveAccessor.NonPrimitive[T],
      specialBuilder: SpecialBuilders[U] = SpecialBuilders.ForAny[U]) = new IterableAccessor[T, U]

  final class IterableAccessor[T, U[T] <: Iterable[T]](implicit val acc: JSONAccessor[T],
      val cbf: CanBuildFrom[Nothing, T, U[T]], val ctag: ClassTag[U[T]],
      val primitive: PrimitiveAccessor[T],
      val specialBuilder: SpecialBuilders[U]) extends JSONAccessorProducer[U[T], JArray] {
    def clazz = ctag.runtimeClass

    override def toString = "IterableAccessor"

    override def referencedTypes: Seq[JSONAccessorProducer[_, _]] = Seq(accessorOf[T])

    def createJSON(obj: U[T]): JArray = {
      if(primitive.isPrimitive) primitive.createJSON(obj)
      else JArray(obj.map(_.js))
    }

    def fromJSON(js: JValue): U[T] = js match {
      case x: JArrayPrimitive[_] if primitive.isPrimitive =>
        val iterable = primitive.iterableFromJValue(x)

        specialBuilder match {
          case x if x.isGeneric =>
            iterable.to[U](cbf)
          case builder =>
            builder.buildFrom(iterable)
        }

      case JArray(vals) =>
        var exceptions = List[InputFormatException]()

        val res = vals.iterator.zipWithIndex.flatMap {
          case (x, idx) =>
            try Seq(x.to[T]) catch {
              case e: InputFormatException =>
                exceptions ::= e.prependFieldName(idx.toString)
                Nil
            }
        }.to[U](cbf)

        if (!exceptions.isEmpty)
          throw InputFormatsException(exceptions.flatMap(_.getExceptions).toSet)

        res
      case x => throw InputTypeException("",
        "array", x.getClass.getName, x)
    }

  }
}
