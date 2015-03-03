package json

import scalajs.js

object JSImplicits {
  implicit class JSAnyExt(val value: js.Any) extends AnyVal {
    def toJValue = JSJValue from value
  }

  implicit class JSValueExt(val value: JValue) extends AnyVal {
    def toJS = JSJValue toJS value
  }
}
