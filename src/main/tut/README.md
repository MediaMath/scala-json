scala-json
==========
Compile time JSON marshalling of primitive values and basic collections.

Goals
-----
* Compile-time marshalling via implicits
* Provide a usable JS-like API for intermediate JSON data
* Create implicit accessors that chain to resolve Higher-Kind types (```Option[T]```)
* Enable you to create structures for existing JSON APIs
* Enable you to create readable APIs that match existing/specific structure.
* Use existing scala collection CanBuildFrom factories to support buildable collections.
* Provide support for unkown-types via 'pickling' with a class Registry


Getting Started
---------------

* Import the json packag=
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
  case e: json.InputFormatException =>
    e.getExceptions.map {
      case fieldEx: InputFieldException if fieldEx.fieldName == "a" =>
        fieldEx.getMessage
      case _ => ""
    }.mkString
}
```

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