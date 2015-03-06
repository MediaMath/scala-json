package json.shadow

import json.internal.BaseVMContext
import json.{ JSJValue, JValue }
import scalajs.js.{JSON => NativeJSON}
import scala.scalajs.js.annotation.JSExport
import scalajs.js

object VMContext extends BaseVMContext {
  def fromString(str: String): JValue = {
    def reviver = (key: js.Any, value: js.Any) =>
      (JSJValue from value).asInstanceOf[js.Any]
    val parsed = NativeJSON.parse(str, reviver)

    //run it again incase the reviver didnt work
    JSJValue from parsed
  }

  def fromAny(value: Any): JValue = JSJValue.from(value)

  trait JValueCompanionBase

  trait JValueBase { _: JValue =>
    //this adds JSON.stringify support
    @JSExport def toJSON: js.Any = JSJValue toJS this
  }

  def quoteJSONString(string: String): StringBuilder =
    new StringBuilder(NativeJSON.stringify(string))

}
