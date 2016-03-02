/*
 * Copyright 2016 MediaMath, Inc
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

import json.internal.PrimitiveJArray
import utest._
import scalajs.js
import scalajs.js.{JSON => NativeJSON}

@accessor case class TestTypedArray(seq: Seq[Byte])

object JSTest extends TestSuite {
  def emptyArray = js.Dynamic.newInstance(js.Dynamic.global.Array)()

  def baseJSValueTests(x: js.Any, result: JValue): Unit = {
    assert(JSJValue.fromNativeJS(x) == result)
    assert(JSJValue.from(x) == result)
  }

  val tests = TestSuite("test JS" - {
    "test native JSON" - {
      val str = Tester.testJSON2
      val native = NativeJSON.parse(str)

      assert(JSJValue.from(native) == JValue.from(native))
    }

    "test object" - {
      val x = js.Dictionary.empty[js.Any]
      val result = JObject()

      baseJSValueTests(x, result)
    }

    "test array" - {
      val x = new js.Array[js.Any]
      val result = JArray.empty

      baseJSValueTests(x, result)
    }

    "test string" - {
      val x = "blah"
      val result = JString(x)

      baseJSValueTests(x, result)
    }

    "test int" - {
      val x = 1
      val result = JNumber(x)

      baseJSValueTests(x, result)
    }

    "test double" - {
      val x = 1.124484
      val result = JNumber(x)

      baseJSValueTests(x, result)
    }

    "test produce" - {
      import Sample._

      val foo = Foo("a", 1)
      val fw = FooWrapper("b", Set(foo))
      val fooJs = fw.js

      val nativeSer = NativeJSON.stringify(fooJs.toJSON)
      
      val reserd = JValue fromString nativeSer

      assert(reserd == fooJs)
    }

    "use primitive arrays byte" - {
      val arr = new js.typedarray.Int8Array(3)

      val jv = JSJValue.fromNativeJS(arr)

      assert(jv.isInstanceOf[PrimitiveJArray[_]])
    }

    "use primitive arrays int" - {
      val arr = new js.typedarray.Int32Array(3)

      val jv = JSJValue.fromNativeJS(arr)

      assert(jv.isInstanceOf[PrimitiveJArray[_]])
    }

    "use primitive arrays" - {
      val arr = (new js.typedarray.Int8Array(3)).asInstanceOf[js.Array[Byte]]

      val obj = TestTypedArray(arr)
      val jval = obj.js
      val native = jval.toNativeJS
      val asString = js.JSON.stringify(jval.asInstanceOf[js.Any])
      val reone: TestTypedArray = JSJValue.fromNativeJS(native).toObject[TestTypedArray]
      val fromJSON = JValue fromString asString

      require(fromJSON == jval)

      require(arr.js match {
        case PrimitiveJArray(wrapped: js.WrappedArray[_])
          if (wrapped.array: js.Any).isInstanceOf[js.typedarray.Int8Array] => true
        case PrimitiveJArray(x) =>
          sys.error("got a primitive, but not wrapper")
        case _ => false
      })

      require(jval("seq") match {
        case PrimitiveJArray(wrapped: js.WrappedArray[_])
          if (wrapped.array: js.Any).isInstanceOf[js.typedarray.Int8Array] => true
        case PrimitiveJArray(x) =>
          sys.error("got a primitive, but not wrapper")
        case _ => false
      })

      require(arr.js.jArray.toObject[Seq[Byte]] match {
        case wrapped: js.WrappedArray[_]
          if (wrapped.array: js.Any).isInstanceOf[js.typedarray.Int8Array] => true
        case _ => false
      })

      require(reone.seq match {
        case wrapped: js.WrappedArray[_]
          if (wrapped.array: js.Any).isInstanceOf[js.typedarray.Int8Array] => true
        case _ => false
      })
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
