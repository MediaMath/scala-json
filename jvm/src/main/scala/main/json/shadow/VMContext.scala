package json.shadow

import json._
import json.internal.JValueObjectDeserializer

object VMContext {
  val localMapper = new ThreadLocal[JValueObjectDeserializer] {
    override protected def initialValue: JValueObjectDeserializer =
      new JValueObjectDeserializer
  }

  def fromString(str: String): JValue = {
    val deser = localMapper.get

    val res = deser.mapper.readValue[JValue](str, classOf[JValue])

    deser.reset()

    res
  }

  def fromAny(value: Any): JValue = JValue.fromAnyInternal(value)

  trait JValueCompanionBase

  trait JValueBase
}

