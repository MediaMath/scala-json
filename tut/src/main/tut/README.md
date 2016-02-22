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
* Extensible accessor API. Serialize any type you want.
* Provides a usable JS-like DSL for intermediate JSON data.
* Create implicit [accessors](./ACCESSORS.md) that chain to resolve Higher-Kind types (```Option[T]```).
* Produce normal looking Scala structures from any existing JSON API.
* Support [scala-js](https://github.com/scala-js/scala-js) so you can extend your models to the web.
* Produce pretty and human readable JSON.
* Enable you to create readable APIs that match existing/specific structure.
* Uses defaults correctly.
* Use existing scala collection CanBuildFrom factories to support buildable collections.
* Provide support for unknown types (Any) via 'pickling' with a class [registry](./REGISTRY.md).
* Support for scala 2.10.x, 2.11.x, 2.12.0-M3.
* Support for scala-js 0.6.x.

Getting Started
---------------

* Import the json package
```tut
import json._
JValue fromString "[1,2,3,4,5]"
```
* Implicit conversion to JValue types
```tut
"hello".js
true.js
1.7.js
def testMap = Map("hey" -> "there")
val testMapJs = testMap.js
Map("key" -> Seq.fill(3)(Set(Some(false), None))).js
testMap.keySet.headOption.js
testMap.get("nokey").js
testMap.js.toDenseString
```
* JS-like dynamic select
```tut
require(testMapJs("nokey") == JUndefined)
```
* JS-like boolean conditions
```tut
if(testMapJs("nokey")) sys.error("unexpected")
```
* JArrays as scala collections
```tut
JArray(1, 2, 3, 4).map(_.toJString)
JArray(1, 2, 3, 4).map(_.num)
JArray(1, 2, 3, 4) ++ JArray(5)
JArray(1, 2, 3, 4) ++ Seq(JNumber(5))
JArray(JObject.empty, JArray.empty) ++ Seq("nonjval")
```
* JObjects as scala collections
```tut
JObject("foo" -> 1.js, "a" -> false.js) ++ Map("bar" -> true).js - "a"
```
* Compile-time case class marshalling
```tut
case class TestClass(@name("FIELD_A") a: Int, b: Option[Int], c: String = "", d: Option[Int] = None) {
    @ephemeral def concat = a.toString + b + c + d
}
implicit val acc = ObjectAccessor.create[TestClass]
val testClassJs = TestClass(1, None).js
val testClassJsString = testClassJs.toDenseString
JValue.fromString(testClassJsString).toObject[TestClass]
JObject("FIELD_A" -> 23.js).toObject[TestClass]
TestClass(1, None).js + ("blah" -> 1.js) - "FIELD_A"
val seqJson = Seq(TestClass(1, None), TestClass(1, Some(10), c = "hihi")).js
```
* Streamlined compile-time case class marshalling (requires [macro-paradise](#dependencies))
```tut
@json.accessor case class SomeModel(a: String, other: Int)
SomeModel("foo", 22).js
implicitly[JSONAccessor[SomeModel]]
json.accessorOf[SomeModel]
```
* Dynamic field access
```tut
seqJson.dynamic(1).c.value
seqJson.dynamic.length
require(seqJson.d == seqJson.dynamic)
```
* Typed exceptions with field data
```tut
try JObject("a" -> "badint".js).toObject[TestClass] catch {
  case e: InputFormatException =>
    e.getExceptions.map {
      case fieldEx: InputFieldException if fieldEx.fieldName == "a" =>
        fieldEx.getMessage
      case _ => ""
    }.mkString
}
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
libraryDependencies += "com.mediamath" %% "scala-json" % "__VER__"

//or scala + scala-js
libraryDependencies += "com.mediamath" %%% "scala-json" % "__VER__"

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

