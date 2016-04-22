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

import json.internal.ObjectAccessorFactory

import scala.language.experimental.macros


/**
 * ObjectAccessor is a [[JSONAccessor]] for objects that have named fields. Generally these
 * are mostly seen as auto-generated [[json.internal.CaseClassObjectAccessor]] case class accessors.
 * Extend this if you need to make a custom accessor that has named fields.
 * @tparam T The base type this accessor is for.
 */
trait ObjectAccessor[T] extends JSONAccessorProducer[T, JObject] {
  /** Accessor for each named field. Includes type accessor for field, defaults, annotations, etc. */
  def fields: IndexedSeq[FieldAccessor[T, _]]

  def canEqual(that: Any) = that.isInstanceOf[ObjectAccessor[_]]

  final override def referencedTypes: Seq[JSONAccessorProducer[_, _]] = fields.map(_.fieldAccessor)

  override def equals(that: Any) = that match {
    case x: ObjectAccessor[_] => x.clazz == clazz
    case _                    => false
  }

  override def describe: JObject = super.describe ++ JObject(
    "types" -> JUndefined,
    "fields" -> (JObject.empty ++ fields.map { field =>
      field.name -> field.fieldAccessor.describe
    })
  )

  override def hashCode = clazz.hashCode
}

object ObjectAccessor {
  /** This is the base method used to create an accessor for a case class via macros */
  def create[T]: ObjectAccessor[T] = macro ObjectAccessorFactory.impl[T]
}
