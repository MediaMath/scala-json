package json

import utest.TestSuite
import json._
import utest._

import scala.annotation.meta

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

object MaybeJNullTest extends TestSuite {

  import Foo._

  case class Foo(foo: MaybeJNull[String])
  case class FooWithOpt(foo: MaybeJNull[Option[String]])
  case class FooWithDefault(foo: MaybeJNull[String] = ValueWrapper("X"))

  object Foo {
    implicit val acc1 = ObjectAccessor.create[Foo]
    implicit val acc2 = ObjectAccessor.create[FooWithOpt]
    implicit val acc3 = ObjectAccessor.create[FooWithDefault]
  }

  val tests = TestSuite {
    "MaybeJNull should" - {
      "parse JNullWrapper while in class" - {
        val rawJson = """{"foo": null}"""
        val value = JValue.fromString(rawJson).to[Foo](acc1)
        assert(value == Foo(JNullWrapper))
      }

      "parse ValueWrapper while in class" - {
        val rawJson = """{"foo": "best string ever!"}"""
        val value = JValue.fromString(rawJson).to[Foo](acc1)
        assert(value == Foo(ValueWrapper("best string ever!")))
      }

      "parse JNullWrapper while in list" - {
        val rawJson = """[null, "abc", null, "def"]"""
        val value = JValue.fromString(rawJson).to[List[MaybeJNull[String]]]
        assert(value == List(JNullWrapper, ValueWrapper("abc"), JNullWrapper, ValueWrapper("def")))
      }

      "parse JNullWrapper while in map" - {
        val rawJson = """{"a": 1, "b": null, "c": 3}"""
        val value = JValue.fromString(rawJson).to[Map[String, MaybeJNull[Int]]]
        val expected = Map("c" -> ValueWrapper(3), "a" -> ValueWrapper(1), "b" -> JNullWrapper)
        assert(value == expected)
      }

      "serialize JNullWrapper while in map" - {
        val map = Map("c" -> ValueWrapper(3), "a" -> ValueWrapper(1), "b" -> JNullWrapper)
        val got = JValue.fromString(map.js.toDenseString).to[Map[String, MaybeJNull[Int]]]
        assert(got == map)
      }

      "serialize JNullWrapper while in class" - {
        val obj = Foo(JNullWrapper)
        val rawJson = """{"foo":null}"""
        assert(obj.js.toDenseString == rawJson)
      }

      "serialize ValueWrapper while in class" - {
        val obj = Foo(ValueWrapper("best string ever!"))
        val rawJson = """{"foo":"best string ever!"}"""
        assert(obj.js.toDenseString == rawJson)
      }

      "serialize JNullWrapper while in list" - {
        val list = List(JNullWrapper, ValueWrapper("abc"), JNullWrapper, ValueWrapper("def"))
        val rawJson = """[null,"abc",null,"def"]"""
        assert(rawJson == list.js.toDenseString)
      }

      "parse MaybeJNull[Optional] to JNullWrapper if json value was null" - {
        val rawJson = """{"foo": null}"""
        val value = JValue.fromString(rawJson).to[FooWithOpt](acc2)
        assert(value == FooWithOpt(JNullWrapper))
      }

      "parse MaybeJNull[Optional] to ValueWrapper if json value was null" - {
        val rawJson = """{"foo": "aaa"}"""
        val value = JValue.fromString(rawJson).to[FooWithOpt](acc2)
        assert(value == FooWithOpt(ValueWrapper(Some("aaa"))))
      }

      "parse MaybeJNull[Optional] to ValueWrapper of None if field was omitted" - {
        val rawJson = """{}"""
        val value = JValue.fromString(rawJson).to[FooWithOpt](acc2)
        assert(value == FooWithOpt(ValueWrapper(None)))
      }

      "parse MaybeJNull falls back to default if value was omitted" - {
        val rawJson = """{}"""
        val value = JValue.fromString(rawJson).to[FooWithDefault](acc3)
        assert(value == FooWithDefault(ValueWrapper("X")))
      }

      "parse MaybeJNull does not fall back to default if value was provided" - {
        val rawJson = """{"foo": "ZZ"}"""
        val value = JValue.fromString(rawJson).to[FooWithDefault](acc3)
        assert(value == FooWithDefault(ValueWrapper("ZZ")))
      }

      "parse MaybeJNull does not fall back to default if null provided" - {
        val rawJson = """{"foo": null}"""
        val value = JValue.fromString(rawJson).to[FooWithDefault](acc3)
        assert(value == FooWithDefault(JNullWrapper))
      }
    }
  }
}
