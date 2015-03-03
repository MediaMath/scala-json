package json.internal

import json._

object DefaultVMContext {
  object VMContext {
    def fromString(str: String): JValue = ???

    def fromAny(value: Any): JValue = ???

    trait JValueCompanionBase

    trait JValueBase
  }
}
