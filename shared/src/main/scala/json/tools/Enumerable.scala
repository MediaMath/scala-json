package json.tools

import json._

abstract class Enumerator[T <: Enumerator[T]#Value](implicit m: Manifest[T])
  extends TypedEnumerator[String, T, JString]

abstract class TypedEnumerator[K, T <: TypedEnumerator[K, T, J]#Value, +J <: JValue](
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

  trait Value { typed: T =>
    def key: K

    def enumerator = TypedEnumerator.this
    def acc = acc0
    def toJSON: J = key js acc
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