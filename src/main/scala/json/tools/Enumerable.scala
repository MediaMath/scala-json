package json.tools

import json._

trait Enumerable[K, T <: Enumerable[K, T, J], +J <: JValue] extends Product {
  def enumerator: TypedEnumerator[K, T, J]
  def key: K
  def acc: JSONProducer[K, J]

  def toJSON: J = key js acc
}

abstract class Enumerator[T <: Enumerable[String, T, JString]](implicit m: Manifest[T])
  extends TypedEnumerator[String, T, JString]

abstract class TypedEnumerator[K, T <: Enumerable[K, T, J], +J <: JValue](
    implicit m: Manifest[T], acc0: JSONAccessorProducer[K, J]) {
  def values: Set[_ <: T]

  def valueMap[U](f: T => U): Map[U, T] = {
    val valSeq = values.toSeq

    val out = valSeq map { x =>
      (f(x), x)
    }

    require(out.length == valSeq.length, "Non unique value in enum!")

    out.toMap
  }

  trait Value extends Enumerable[K, T, J] { typed: T =>
    def enumerator = TypedEnumerator.this
    def acc = acc0
  }

  lazy val keyMap = valueMap(_.key)

  def default(jv: JValue): T = sys.error(s"Unknown Enumerable type $jv for ${m.runtimeClass}")

  implicit lazy val accessor = new JSONAccessorProducer[T, J] {
    def createJSON(from: T): J = from.toJSON// from.toJSON.toJString    dislikee
    def fromJSON(from: JValue): T = keyMap.getOrElse(from.to[K], default(from))
    def manifest = m

    val jsValues = values.toSeq.map(_.toJSON)

    override def createSwaggerProperty: JObject =
      super.createSwaggerProperty ++ JObject(JString("enum") -> JArray(jsValues))
  }
}