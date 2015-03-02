package json

import json.annotations._
import json.internal.JSONAnnotations.FieldAccessorAnnotation

import org.qirx.littlespec.Specification

import scala.annotation.meta

object Sample {
  case class NumAnnoGeneric(n: Int) extends FieldAccessorAnnotation
  type NumAnno = (NumAnnoGeneric @meta.field @meta.getter)

  sealed trait FooBase {
    def foo: String
  }

  case class Foo(foo: String,
    @JSONFieldName(field = "aa11") bar: Int,
    @NumAnno(11) optField: Option[String] = None,
    anArray: Seq[Int] = Nil) extends FooBase

  @NameConversion(s => s.toUpperCase)
  case class TestObjectCase(camelCase1: String = "", aNothingFieldNamee: Int = 1)

  //val oacc = CaseClassObjectAccessor.of[Foo]
}

//this is more of a sample than a test....
class Sample extends Specification {
  import Sample._

  case class FooWrapper(foo: String, extra: Set[Foo]) extends FooBase

  implicit val accessorForFoo = ObjectAccessor.of[Foo]
  implicit val accessorForFooWrapper = ObjectAccessor.of[FooWrapper]
  implicit val accessorForFooTestObjectCase = ObjectAccessor.of[TestObjectCase]

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

  "a sample test" - {
    "access as json" - {
      runTest()
      success
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
    require(fb.js == (fw.js + ("type".js, "foowrapper".js)))
    require(fb.js.toObject[FooBase] == fw)

    val annoSet = (for {
      field <- ObjectAccessor.caseClassAccessorOf[Foo].fields
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
		trait XMLAccessorWrapsObjectAccessor[T, ObjectAccessor[T]] {
			def marshal(x: T): String
		}

		implicit case object StringSerialLongConverter extends
				ObjectAccessorStringConverter[Long, LongAccessor.type] {
			def marshal(x: Long): String = x.asInstanceOf[Long].toString
		}

		class StringAccessorType[T, U <: ObjectAccessor[T]](
				implicit acc: U, converter: ObjectAccessorStringConverter[T, U]) {
			/*val marshalFunc = acc match {
				case LongAccessor => { x: T =>
					x.asInstanceOf[Long].toString
				}
				case cacc: CaseClassObjectAccessor[T] =>
					cacc.fields.map(_.fieldAccessor)

			}*/

			def from(x: T) = converter.marshal(x)
		}
		def newStringAccessor[T](implicit )

		implicit val customFooAcc = StringAccessorType[Foo]
	}*/
}
