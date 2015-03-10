/*
 * Copyright 2015 MediaMath, Inc
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

import scala.annotation.{ StaticAnnotation, meta }

object JSONAnnotations {
  trait FieldAccessorAnnotation extends StaticAnnotation with Product
  trait ObjectAccessorAnnotation extends StaticAnnotation with Product

  trait NameConversionAnnotation extends ObjectAccessorAnnotation with (String => String)

  case class NameConversionGeneric(convert: String => String) extends NameConversionAnnotation {
    def apply(s: String): String = convert(s)
  }

  case class IgnoreFieldDocGeneric(ignoreRead: Boolean = true,
    ignoreWrite: Boolean = true) extends FieldAccessorAnnotation

  //case object JSONFieldIgnoreGeneric extends AccessorNotation
  case class JSONFieldNameGeneric(field: String) extends FieldAccessorAnnotation
  case class FieldDescriptionGeneric(desc: String) extends FieldAccessorAnnotation

  private[json] trait TypeAdder {
    //type JSONFieldIgnore = (JSONFieldIgnoreGeneric.type @meta.field @meta.getter)
    type IgnoreFieldDoc = (IgnoreFieldDocGeneric @meta.field @meta.getter)

    type JSONFieldName = (JSONFieldNameGeneric @meta.field @meta.getter)
    type FieldName = (JSONFieldNameGeneric @meta.field @meta.getter)
    //type JSONGetterName = (JSONFieldNameGeneric @meta.getter)

    //type JSONReadOnly = (JSONReadOnlyField @meta.field @meta.getter)

    type FieldDescription = (FieldDescriptionGeneric @meta.field @meta.getter)

    type NameConversion = (NameConversionGeneric @meta.companionClass @meta.companionObject)
  }
}
