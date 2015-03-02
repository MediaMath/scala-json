package json

import scala.scalajs.js
import json.internal.DefaultJVMShadowContext.{ VMContext => JVMContext }

import js.JSConverters._

object JSJValue {
  def from(v: Any): JValue = v match {
    case x: JValue => x
    case seq0: js.Array[Any] =>
      val seq: Seq[Any] = seq0
      val jvals: Seq[JValue] = seq.map(JSJValue.from)

      JArray(jvals)
    case x0: js.Object =>
      val x = x0.asInstanceOf[js.Dynamic]
      val seq = (js.Object keys x0).toSeq.map { key =>
        val value = JSJValue from x.selectDynamic(key)
        JString(key) -> value
      }
      JObject(seq: _*)
    case x: String => JString(x)
    case x         => JVMContext.fromAny(x)
  }

  def toJS(from: JValue): js.Any = from match {
    case x: JObject =>
      val vals = for ((JString(key), value) <- x.iterator)
        yield key -> toJS(value)

      vals.toMap.toJSDictionary
    case JArray(values) =>
      values.map(toJS).toJSArray
    case JUndefined => js.undefined
    case JNull      => null
    case x          => x.value.asInstanceOf[js.Any] //assumed to be a primitive here
  }
}
