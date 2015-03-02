package json.internal

import json._

/**
 * Created by crgodsey on 2/14/15.
 */
object DefaultJVMShadowContext {
  //default context for JVM. Can be shadowed by other impls
  object VMContext {
    def fromString(str: String): JValue = JValue.fromStringJackson(str)

    def fromAny(value: Any): JValue = value match {
      //case x if acc == null => v.js
      case x: JValue => x
      case x: String => JString(x)
      case None      => JNull
      case null      => JNull
      case true      => JTrue
      case false     => JFalse
      case x: Double => JNumber(x)
      case x: Iterable[Any] =>
        val seq = x.toSeq
        if (seq.isEmpty) (x: @unchecked) match {
          case _: Map[_, _] => JObject(Map.empty[JString, JValue])
          case _            => JArray(Nil.toIndexedSeq)
        }
        else seq.head match {
          case (k: String, v: Any) =>
            val vals = x.toSeq map {
              case (k: String, v) => k.js -> JValue(v)
            }
            JObject(vals: _*)
          case (v: Any) =>
            JArray(x.map(JValue.from).toIndexedSeq)
        }
      case x: Int   => JNumber(x)
      case x: Short => JNumber(x)
      case x: Long  => JNumber(x)
      case x: Float => JNumber(x)
    }

    trait JValueCompanionBase

    trait JValueBase
  }
}
