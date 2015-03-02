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
