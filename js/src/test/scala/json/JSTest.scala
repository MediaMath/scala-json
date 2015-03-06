package json

import utest._
import scalajs.js
import scalajs.js.{JSON => NativeJSON}

object JSTest extends TestSuite {
  val tests = TestSuite("test JS" - {
    "test json" - {
      import Sample._

      val foo = Foo("a", 1)
      val fw = FooWrapper("b", Set(foo))
      val fooJs = fw.js

      val nativeSer = NativeJSON.stringify(fooJs.toJSON)
      val reserd = JValue fromString nativeSer

      require(reserd == fooJs)
    }
  })
}
