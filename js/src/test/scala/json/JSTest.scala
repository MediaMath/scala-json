/*
 * Copyright 2015 MediaMath, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
