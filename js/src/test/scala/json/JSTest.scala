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
  def emptyArray = js.Dynamic.newInstance(js.Dynamic.global.Array)()

  val tests = TestSuite("test JS" - {
    "test produce" - {
      import Sample._

      val foo = Foo("a", 1)
      val fw = FooWrapper("b", Set(foo))
      val fooJs = fw.js

      val nativeSer = NativeJSON.stringify(fooJs.toJSON)
      val reserd = JValue fromString nativeSer

      assert(reserd == fooJs)
    }

    "test parse" - {
      val real = JValue from Seq(
        1, 2, Map.empty, Nil, Map("a" -> 1), "", null, 5.123, Nil
      )
      val parsed = (NativeJSON parse """[1,2,{},[],{"a":1},"",null,5.123]""").asInstanceOf[js.Array[js.Dynamic]]

      parsed.push(emptyArray)

      val reserd = JValue from parsed

      assert(reserd == real)
    }

    "undefined" - {
      assert(JValue.from(js.undefined) == JUndefined)
    }
  })
}
