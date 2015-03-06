package json.internal

import json._

trait BaseVMContext {
  def fromString(str: String): JValue
  def fromAny(value: Any): JValue
  def quoteJSONString(string: String): StringBuilder

  type JValueCompanionBase

  type JValueBase
}

object DefaultVMContext {
  //to be replaced via shadowing by build for proper VM
  object VMContext extends BaseVMContext {
    trait JValueCompanionBase

    trait JValueBase

    def fromString(str: String): JValue = ???
    def fromAny(value: Any): JValue = ???
    def quoteJSONString(string: String): StringBuilder = ???
  }
}
