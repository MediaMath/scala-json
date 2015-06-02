scala-json
==========
Compile time JSON marshalling of primitive values, case-classes, basic collections, and whatever you can imagine.
* Extensible Accessor API. Serialize any type you want.
* Produce normal looking Scala structures from any existing JSON API.
* Produce pretty and human readable JSON.
* Uses defaults correctly.
* Extensible annotation API to make your accessors more dynamic.

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
