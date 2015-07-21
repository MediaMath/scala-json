scala-json
==========
Compile time JSON marshalling for [scala](https://github.com/scala/scala) and [scala-js](https://github.com/scala-js/scala-js)

Goals
-----
Compile time JSON marshalling of primitive values, case-classes, basic collections, and whatever you can imagine.
* Extensible Accessor API. Serialize any type you want.
* Provide a usable JS-like DSL for intermediate JSON data
* Create implicit accessors that chain to resolve Higher-Kind types (```scala Option[T]```)
* Produce normal looking Scala structures from any existing JSON API.
* Produce pretty and human readable JSON.
* Enable you to create readable APIs that match existing/specific structure.
* Uses defaults correctly.
* Extensible annotation API to make your accessors more dynamic.
* Use existing scala collection CanBuildFrom factories to support buildable collections.
* Provide support for unknown types (Any) via 'pickling' with a class Registry

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
* Compile-time case class marshalling
```tut
case class TestClass(a: Int, b: Option[Int], c: String = "", d: Option[Int] = None)
implicit val acc = ObjectAccessor.of[TestClass]
val testClassJs = TestClass(1, None).js
val testClassJsString = testClassJs.toDenseString
JValue.fromString(testClassJsString).toObject[TestClass]
JObject("a".js -> 23.js).toObject[TestClass]
TestClass(1, None).js + ("blah".js -> 1.js) - "a"
Seq(TestClass(1, None), TestClass(1, Some(10), c = "hihi")).js
```
* Typed exceptions with field data
```tut
try JObject("a".js -> "badint".js).toObject[TestClass] catch {
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

[Registry](./REGISTRY.md)
---

The Accessor Registry allows you to pickle registered types from untyped (Any) data.

SBT
---

```scala

resolvers += "mmreleases" at
    "https://artifactory.mediamath.com/artifactory/libs-release-global"

libraryDependencies += "com.mediamath" %% "scala-json" % "__VER__"

```

and for Scala.js

```scala

libraryDependencies += "com.mediamath" %%% "scala-json" % "__VER__"

```
