scala-json
==========
```scala
import json._

@accessor case class Book(name: String, pages: Int, chapters: Seq[String])

Book("Making JSON Easy in Scala", 2, List("Getting Started, Fast", "Getting Back to Work")).js

res0: json.JObject =
{
  "name": "Making JSON Easy in Scala",
  "pages": 2,
  "chapters": ["Getting Started, Fast", "Getting Back to Work"]
}
```

Features
-----
Compile time JSON marshalling of primitive values, case-classes, basic collections, and whatever you can imagine
for [scala](https://github.com/scala/scala) and [scala-js](https://github.com/scala-js/scala-js).
* Extensible Accessor API. Serialize any type you want.
* Provide a usable JS-like DSL for intermediate JSON data.
* Create implicit [accessors](./ACCESSORS.md) that chain to resolve Higher-Kind types (```Option[T]```).
* Produce normal looking Scala structures from any existing JSON API.
* Support [scala-js](https://github.com/scala-js/scala-js) so you can extend your models to the web.
* Produce pretty and human readable JSON.
* Enable you to create readable APIs that match existing/specific structure.
* Uses defaults correctly.
* Extensible annotation API to make your accessors more dynamic.
* Use existing scala collection CanBuildFrom factories to support buildable collections.
* Provide support for unknown types (Any) via 'pickling' with a class [registry](./REGISTRY.md).
* Support for Scala 2.12.0-M3

Getting Started
---------------

* Import the json package
```scala
scala> import json._
import json._

scala> JValue fromString "[1,2,3,4,5]"
res0: json.JValue = [1, 2, 3, 4, 5]
```
* Implicit conversion to JValue types
```scala
scala> "hello".js
res1: json.JString = "hello"

scala> true.js
res2: json.JBoolean = true

scala> 1.7.js
res3: json.JNumber = 1.7

scala> def testMap = Map("hey" -> "there")
testMap: scala.collection.immutable.Map[String,String]

scala> val testMapJs = testMap.js
testMapJs: json.JObject =
{
  "hey": "there"
}

scala> Map("key" -> Seq.fill(3)(Set(Some(false), None))).js
res4: json.JObject =
{
  "key": [[false, null], [false, null], [false, null]]
}

scala> testMap.keySet.headOption.js
res5: json.JValue = "hey"

scala> testMap.get("nokey").js
res6: json.JValue = null

scala> testMap.js.toDenseString
res7: String = {"hey":"there"}
```
* JS-like dynamic select
```scala
scala> require(testMapJs("nokey") == JUndefined)
```
* JS-like boolean conditions
```scala
scala> if(testMapJs("nokey")) sys.error("unexpected")
```
* JArrays as scala collections
```scala
scala> JArray(1, 2, 3, 4).map(x => x.toJString)
res10: json.JArray = ["1", "2", "3", "4"]

scala> JArray(1, 2, 3, 4).map(_.num)
res11: scala.collection.immutable.IndexedSeq[Double] = Vector(1.0, 2.0, 3.0, 4.0)

scala> JArray(1, 2, 3, 4) ++ JArray(5)
res12: json.JArray = [1, 2, 3, 4, 5]
```
* JObjects as scala collections
```scala
scala> JObject("foo" -> 1.js, "a" -> false.js) ++ Map("bar" -> true).js - "a"
res13: json.JObject =
{
  "foo": 1,
  "bar": true
}
```
* Compile-time case class marshalling
```scala
scala> case class TestClass(a: Int, b: Option[Int], c: String = "", d: Option[Int] = None)
defined class TestClass

scala> implicit val acc = ObjectAccessor.create[TestClass]
acc: json.internal.CaseClassObjectAccessor[TestClass] =
{
  "accessorClass": "json.internal.CaseClassObjectAccessor",
  "valueClass": "TestClass",
  "accessorType": "CaseClassObjectAccessor",
  "fields": {
    "a": {
      "type": {
        "accessorClass": "json.internal.Accessors$IntAccessor$",
        "valueClass": "int",
        "accessorType": "IntAccessor$"
      }
    },
    "b": {
      "type": {
        "accessorClass": "json.internal.Accessors$OptionAccessor",
        "valueClass": "scala.Option",
        "accessorType": "OptionAccessor",
        "types": ["T"],
        "T": {
          "accessorClass": "json.internal.Accessors$IntAccessor$",
          "valueClass": "int",
          "accessorType": "IntAccessor$"
        }
      }
    },
    "c": {
      "type": {
        "acc...
scala> val testClassJs = TestClass(1, None).js
testClassJs: json.JObject =
{
  "a": 1,
  "b": null,
  "c": "",
  "d": null
}

scala> val testClassJsString = testClassJs.toDenseString
testClassJsString: String = {"a":1,"b":null,"c":"","d":null}

scala> JValue.fromString(testClassJsString).toObject[TestClass]
res14: TestClass = TestClass(1,None,,None)

scala> JObject("a" -> 23.js).toObject[TestClass]
res15: TestClass = TestClass(23,None,,None)

scala> TestClass(1, None).js + ("blah" -> 1.js) - "a"
res16: json.JObject =
{
  "b": null,
  "c": "",
  "d": null,
  "blah": 1
}

scala> val seqJson = Seq(TestClass(1, None), TestClass(1, Some(10), c = "hihi")).js
seqJson: json.JArray =
[{
  "a": 1,
  "b": null,
  "c": "",
  "d": null
}, {
  "a": 1,
  "b": 10,
  "c": "hihi",
  "d": null
}]
```
* Streamlined compile-time case class marshalling (requires [macro-paradise](#dependencies))
```scala
scala> @json.accessor case class SomeModel(a: String, other: Int)
defined object SomeModel
defined class SomeModel

scala> SomeModel("foo", 22).js
res17: json.JObject =
{
  "a": "foo",
  "other": 22
}

scala> implicitly[JSONAccessor[SomeModel]]
res18: json.JSONAccessor[SomeModel] =
{
  "accessorClass": "json.internal.CaseClassObjectAccessor",
  "valueClass": "SomeModel",
  "accessorType": "CaseClassObjectAccessor",
  "fields": {
    "a": {
      "type": {
        "accessorClass": "json.internal.Accessors$StringAccessor$",
        "valueClass": "java.lang.String",
        "accessorType": "StringAccessor$"
      }
    },
    "other": {
      "type": {
        "accessorClass": "json.internal.Accessors$IntAccessor$",
        "valueClass": "int",
        "accessorType": "IntAccessor$"
      }
    }
  }
}

scala> json.accessorOf[SomeModel]
res19: json.JSONAccessor[SomeModel] =
{
  "accessorClass": "json.internal.CaseClassObjectAccessor",
  "valueClass": "SomeModel",
  "accessorType": "CaseClassObjectAccessor",
  "fields": {
    "a": {
      "type": {
        "accessorClass": "json.internal.Accessors$StringAccessor$",
        "valueClass": "java.lang.String",
        "accessorType": "StringAccessor$"
      }
    },
    "other": {
      "type": {
        "accessorClass": "json.internal.Accessors$IntAccessor$",
        "valueClass": "int",
        "accessorType": "IntAccessor$"
      }
    }
  }
}
```
* Dynamic field access
```scala
scala> seqJson.dynamic(1).c.value
res20: json.JValue = "hihi"

scala> seqJson.dynamic.length
res21: json.JDynamic = 2

scala> require(seqJson.d == seqJson.dynamic)
```
* Typed exceptions with field data
```scala
scala> try JObject("a" -> "badint".js).toObject[TestClass] catch {
     |   case e: InputFormatException =>
     |     e.getExceptions.map {
     |       case fieldEx: InputFieldException if fieldEx.fieldName == "a" =>
     |         fieldEx.getMessage
     |       case _ => ""
     |     }.mkString
     | }
res23: java.io.Serializable = numeric expected but found json.JString (of value "badint")
```

[Accessors](./ACCESSORS.md)
---

Accessors are the compile-time constructs that allow you to marshal scala types.

Tools
---

* [Registry](./REGISTRY.md) - The Accessor Registry allows you to pickle registered types from untyped (Any) data.
* [Enumerator](./ENUMERATOR.md) - Allows enumerated case object values of a sealed trait.


[Scaladocs](http://mediamath.github.io/scala-json/doc/json/package.html)
---

SBT
---

```scala
resolvers += "mmreleases" at "https://artifactory.mediamath.com/artifactory/libs-release-global"

//scala
libraryDependencies += "com.mediamath" %% "scala-json" % "1.0-SNAPSHOT"

//or scala + scala-js
libraryDependencies += "com.mediamath" %%% "scala-json" % "1.0-SNAPSHOT"

//for @accessor annotation support
resolvers += Resolver.sonatypeRepo("releases")
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
```

Dependencies
---

* [macro-paradise](http://docs.scala-lang.org/overviews/macros/paradise.html) 2.1.0+ required for @accessor annotation
* [jackson](https://github.com/FasterXML/jackson)
* [µTest](https://github.com/lihaoyi/utest) for testing
* [tut](https://github.com/tpolecat/tut) for doc building


