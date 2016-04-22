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

import scala.annotation.{StaticAnnotation, meta}

trait JSONAnnotation extends StaticAnnotation with Product

@meta.field
trait FieldAccessorAnnotation extends JSONAnnotation

@meta.companionClass @meta.companionObject
trait ObjectAccessorAnnotation extends JSONAnnotation

trait NameConversionGeneric extends ObjectAccessorAnnotation {
  def convert: String => String
}

case class FieldNameGeneric(field: String) extends FieldAccessorAnnotation
case class FieldDescriptionGeneric(desc: String) extends FieldAccessorAnnotation

case class EphemeralGeneric() extends FieldAccessorAnnotation

/** holder for annotation types */
trait JSONAnnotations {
  /** Base type of all annotations, useful when reflecting `annos` in [[json.FieldAccessor]] */
  type FieldAccessorAnnotation = json.internal.FieldAccessorAnnotation

  //TODO: need additional useful name function annos- upper case, lower case, camel, snake-case

  /** Provide field name that will be used in the JSON. Useful when making case classes for
    * existing JSON models that aren't camel cased.
    */
  type name = FieldNameGeneric

  /** Provide field description, useful when reflect on accessors for auto-gen docs, etc */
  type description = FieldDescriptionGeneric

  /** Marks an internal method/val to be emitted when serialized only,
    * but not needed for construction (basically read only).
    *
    * {{{
    *  @json.accessor case class Foo(bar: String) {
    *    @json.ephemeral def twice = bar + bar
    *  }
    *  }}}
    *
    * */
  type ephemeral = EphemeralGeneric

  //TODO: this needs to be updated to not use anonymous functions. not macro friendly for 2.10
  //type nameConversion = NameConversion
}
