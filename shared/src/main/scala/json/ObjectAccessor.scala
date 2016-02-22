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

import json.internal.{NameConversionGeneric, FieldAccessor, CaseClassObjectAccessor, ObjectAccessorFactory}

import scala.language.experimental.macros
import scala.annotation.{StaticAnnotation, implicitNotFound}
import scala.annotation.meta._


/**
 * ObjectAccessor is a [[JSONAccessor]] for objects that have named fields. Generally these
 * are mostly seen as auto-generated [[json.internal.CaseClassObjectAccessor]] case class accessors.
 * The ObjectAccessor base type exists for mostly for the purpose of future extensibility.
 * @tparam T The base type this accessor is for.
 */
trait ObjectAccessor[T] extends JSONAccessorProducer[T, JObject] {
  def fields: IndexedSeq[FieldAccessor[T]]

  def canEqual(that: Any) = that.isInstanceOf[ObjectAccessor[_]]

  override def equals(that: Any) = that match {
    case x: ObjectAccessor[_] => x.clazz == clazz
    case _                    => false
  }

  override def hashCode = clazz.hashCode
}

object ObjectAccessor {
  case object NoAccessor extends ObjectAccessor[Nothing] {
    def fields: IndexedSeq[FieldAccessor[Nothing]] = Nil.toIndexedSeq
    def clazz: Class[Nothing] = classOf[Nothing]
    def fromJSON(from: JValue): Nothing = sys.error("Cannot create Nothing object")
    def createJSON(obj: Nothing): JObject = sys.error("Cannot create Nothing json")

    override def canEqual(that: Any) = that.isInstanceOf[this.type]

    def apply[T] = NoAccessor.asInstanceOf[ObjectAccessor[T]]

    def describe = baseDescription
  }

  def create[T]: CaseClassObjectAccessor[T] = macro ObjectAccessorFactory.impl[T]

  @implicitNotFound(msg = "No implicit ObjectAccessor for ${T} in scope. Did you define/import one?")
  def of[T](implicit acc: ObjectAccessor[T]): ObjectAccessor[T] = acc
}
