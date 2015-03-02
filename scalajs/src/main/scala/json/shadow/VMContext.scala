package json.shadow

import json.internal.DefaultJVMShadowContext
import json.{ JSJValue, JValue }

import scalajs.js

object VMContext {
  def fromString(str: String): JValue = {
    //TODO: use parse 2nd arg for inline wrapper!
    val parsed = js.Dynamic.global.JSON.parse(str)

    JSJValue.from(parsed)
  }

  def fromAny(value: Any): JValue = JSJValue.from(value)

  trait JValueCompanionBase extends DefaultJVMShadowContext.VMContext.JValueCompanionBase

  trait JValueBase extends DefaultJVMShadowContext.VMContext.JValueBase
}
