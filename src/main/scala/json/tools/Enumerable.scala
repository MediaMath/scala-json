package json.tools

import json._

trait Enumerable[K, T <: Enumerable[K, T]] extends Product {
  def enumerator: TypedEnumerator[K, T]
  def key: K
  def acc: JSONProducer[K, JValue]

  def toJSON: JValue = key js acc
}

//TODO: figure out how to make this work with ints and stuff too
abstract class Enumerator[T <: Enumerable[String, T]](implicit m: Manifest[T])
  extends TypedEnumerator[String, T]

abstract class TypedEnumerator[K, T <: Enumerable[K, T]](implicit m: Manifest[T], acc0: JSONAccessor[K]) {
  def values: Set[_ <: T]

  def valueMap[U](f: T => U): Map[U, T] = {
    val valSeq = values.toSeq

    val out = valSeq map { x =>
      (f(x), x)
    }

    require(out.length == valSeq.length, "Non unique value in enum!")

    out.toMap
  }

  trait Value extends Enumerable[K, T] { typed: T =>
    def enumerator = TypedEnumerator.this
    def acc = acc
  }

  lazy val keyMap = valueMap(_.key)

  def default(jv: JValue): T = sys.error(s"Unknown Enumerable type $jv for ${m.runtimeClass}")

  implicit lazy val accessor = new JSONAccessorProducer[T, JString] {
    def createJSON(from: T): JString = from.toJSON.toJString
    def fromJSON(from: JValue): T = keyMap.getOrElse(from.to[K], default(from))
    def manifest = m

    val jsValues = values.toSeq.map(_.toJSON)

    override def createSwaggerProperty: JObject =
      super.createSwaggerProperty ++ JObject("enum".js -> JArray(jsValues))
  }
}