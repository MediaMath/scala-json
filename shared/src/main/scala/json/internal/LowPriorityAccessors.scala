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
import json.exceptions.{InputTypeException, InputFormatsException, InputFormatException}

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
      implicit acc: JSONAccessor[T],
      cbf: CanBuildFrom[Nothing, T, U[T]],
      ctag: ClassTag[U[T]],
      ctagForT: ClassTag[T],
      specialBuilder: PrimitiveJArray.SpecialBuilders[U] = PrimitiveJArray.SpecialBuilders.ForAny) = new IterableAccessor[T, U]

  final class IterableAccessor[T, U[T] <: Iterable[T]](implicit val acc: JSONAccessor[T],
      val cbf: CanBuildFrom[Nothing, T, U[T]],
      val ctag: ClassTag[U[T]],
      val specialBuilder: PrimitiveJArray.SpecialBuilders[U]) extends JSONAccessorProducer[U[T], JArray] {
    def clazz = ctag.runtimeClass

    implicit def classTagForT: ClassTag[T] = ClassTag(acc.clazz.asInstanceOf[Class[T]])
    
    val primitiveAccessor = acc match {
      case x: PrimitiveJArray.Builder[T] => Some(x)
      case _ => None
    }

    override def toString = "IterableAccessor"

    override def referencedTypes: Seq[JSONAccessorProducer[_, _]] = Seq(accessorOf[T])

    def createJSON(obj: U[T]): JArray = primitiveAccessor match {
      case None => JArray(obj.map(_.js))
      case Some(jvPrim) =>
        implicit def primBuild = jvPrim

        obj match {
          case VM.Context.PrimitiveJArrayExtractor(jarr) => jarr //extract primitive jarray using base array
          case _ =>
            val primArr = VM.Context.createPrimitiveArray[T](obj.size)

            var idx = 0
            for(x <- obj) {
              primArr(idx) = x
              idx += 1
            }

            jvPrim.createFrom(primArr)
        }
    }

    def fromJSON(js: JValue): U[T] = js match {
      //if primitive array of matching internal type
      case x: PrimitiveJArray[_] if specialBuilder.canIndexedSeq && primitiveAccessor.isDefined =>
        implicit val builder = primitiveAccessor.get

        val indexed = if(x.builder.classTag == builder.classTag)
          x.primArr.asInstanceOf[IndexedSeq[T]]
        else
          builder.createFrom(x).primArr

        indexed.asInstanceOf[U[T]]
      //TODO: maybe some translation for boxed arrays
      /*case x: BoxedJArray[_] if x.builder.classTag == ctagForT && specialBuilder.canIndexedSeq =>
        val indexed = x.primValues.asInstanceOf[IndexedSeq[T]]
        specialBuilder.buildFrom(indexed)*/

      case JArray(vals) =>
        var exceptions = List[InputFormatException]()
        val builder = cbf()

        for((x, idx) <- vals.iterator.zipWithIndex) {
          try {
            val value = x.to[T]
            builder += value
          } catch {
            case e: InputFormatException =>
              exceptions ::= e.prependFieldName(idx.toString)
          }
        }

        if (!exceptions.isEmpty)
          throw InputFormatsException(exceptions.flatMap(_.getExceptions).toSet)

        builder.result()
      case x => throw InputTypeException("", "array", x.getClass.getName, x)
    }

  }
}
