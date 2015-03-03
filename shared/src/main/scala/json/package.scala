import json.internal.{ JSONParser, JSONAnnotations }

/**
 * Created by crgodsey on 2/14/15.
 */
package object json extends JSONAnnotations.TypeAdder with JSONParser {
  val JNaN = JNumber(Double.NaN)

  type JSONAccessor[T] = JSONAccessorProducer[T, JValue]

  def fromJSON[T](jval: JValue)(implicit acc: JSONAccessor[T]) =
    acc.fromJSON(jval)

  def toJSONString[T](obj: T)(implicit acc: JSONAccessor[T]) =
    obj.js.toString

  implicit class AnyValJSEx[T](val x: T) extends AnyVal {
    def js[U <: JValue](implicit acc: JSONProducer[T, U]): U = acc.createJSON(x)

    /*def js(implicit acc: CaseClassObjectAccessor[T]): JObject =
			acc.createJSON(x)*/
  }

  implicit class JSONStringOps(val str: String) extends AnyVal {
    def ->>(v: JValue): (JString, JValue) = JString(str) -> v

    def jValue = JValue.fromString(str)
  }

  private[json] def fieldCatch[T](name: String)(f: => T): T = try f catch {
    case e: InputFormatException =>
      throw e.prependFieldName(name)
    case e: Throwable =>
      throw GenericFieldException(name, e)
  }
}
