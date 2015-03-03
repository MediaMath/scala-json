package json.shadow

import json.internal.DefaultJVMShadowContext
import json.{ JSJValue, JValue }

import scalajs.js

object VMContext {
  def fromString(str: String): JValue = {
    //TODO: use parse 2nd arg for inline wrapper!

    def reviver(key: js.Dynamic, value: js.Dynamic): JValue =
      JSJValue from value

    val parsed = js.Dynamic.global.JSON.parse(str,
      (key: js.Dynamic, value: js.Dynamic) => JSJValue from value)

    //run it again incase the reviver didnt work
    JSJValue.from(parsed)
  }

  def fromAny(value: Any): JValue = JSJValue.from(value)

  trait JValueCompanionBase extends DefaultJVMShadowContext.VMContext.JValueCompanionBase

  trait JValueBase extends DefaultJVMShadowContext.VMContext.JValueBase
}
