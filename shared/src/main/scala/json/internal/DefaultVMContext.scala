package json.internal

import json._

object DefaultVMContext {
  //to be replaced via shadowing by build for proper VM
  object VMContext {
    def fromString(str: String): JValue = ???

    def fromAny(value: Any): JValue = ???

    trait JValueCompanionBase

    trait JValueBase
  }
}
