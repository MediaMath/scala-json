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

import json.annotations._
import json.internal.JSONAnnotations.FieldAccessorAnnotation

import utest._

import scala.annotation.meta

object Sample {
  case class NumAnnoGeneric(n: Int) extends FieldAccessorAnnotation
  type NumAnno = (NumAnnoGeneric @meta.field @meta.getter)

  sealed trait FooBase {
    def foo: String
  }

  object Foo {
    implicit val acc = ObjectAccessor.of[Foo]
  }
  case class Foo(foo: String,
    @JSONFieldName(field = "aa11") bar: Int,
    @NumAnno(11) optField: Option[String] = None,
    anArray: Seq[Int] = Nil) extends FooBase

  object TestObjectCase {
    implicit val acc = ObjectAccessor.of[TestObjectCase]
  }
  @NameConversion(s => s.toUpperCase)
  case class TestObjectCase(camelCase1: String = "", anotherFieldName: Int = 1)

  object FooWrapper {
    implicit val acc = ObjectAccessor.of[FooWrapper]
  }
  case class FooWrapper(foo: String, extra: Set[Foo]) extends FooBase
}

//this is more of a sample than a test....
object SampleTest extends TestSuite {
  import Sample._

  //custom type marshalling
  implicit val testCustomAcc = ObjectAccessor.create[FooBase](
    {
      case x: FooWrapper => x.js + ("type".js -> "foowrapper".js)
      case x: Foo        => x.js + ("type".js -> "foo".js)
    },
    jval => jval("type") match {
      case JString("foo")        => jval.to[Foo]
      case JString("foowrapper") => jval.to[FooWrapper]
      case JString(x)            => sys.error("Unknown foo type " + x)
      case _                     => sys.error("Malformed foo " + jval)
    }
  )

  val tests = TestSuite {
    "a sample test" - {
      "access as json" - {
        runTest()
      }
    }
  }

  def runTest() {
    val foo = Foo("a", 1)
    val fw = FooWrapper("b", Set(foo))

    println(foo.js.toString)

    //append untype JValue with JObject
    val foo2 = foo.js ++ JObject("newfield".js -> 4.js)

    //append an object or new field
    require(foo2 == foo.js + ("newfield".js -> 4.js))

    //respects ordering
    val reverseAddFoo2 = JObject("newfield".js -> 4.js) ++ foo.js
    require(foo2.headOption != reverseAddFoo2.headOption)

    //map over objects
    val mappedJs = foo.js.jObject map {
      case (JString(key), JString(str)) =>
        ("prefix_" + key).js -> str.js
      case (key, value) => key -> "0".js
    }

    //direct key reference
    require(mappedJs("prefix_foo").str == "a")

    //null for none
    val fooJs = foo.js
    require(fooJs("optField") == JNull)

    println(mappedJs.toString)

    //undefined for all other keys
    require(mappedJs("no_field") == JUndefined)

    val fb: FooBase = fw

    //using custom accessor to handle super-type
    require(fb.js == (fw.js + ("type".js -> "foowrapper".js)))
    require(fb.js.toObject[FooBase] == fw)

    val annoSet = (for {
      field <- ObjectAccessor.of[Foo].fields
      anno <- field.annos
    } yield anno).toSet

    //creates actual instances of case class annotation objects
    require(annoSet(NumAnnoGeneric(11)))

    val testCase = TestObjectCase(camelCase1 = "hi")

    println(testCase.js.toString)
    require(testCase.js.jObject("CAMELCASE1") == "hi".js)
    require(testCase.js.toObject[TestObjectCase] == testCase)
  }

  /*it should "allow new accessor types" in {
	}*/

}
