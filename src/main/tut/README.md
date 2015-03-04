scala-json
==========
Compile time JSON marshalling of primitive values and basic collections. Runtime
marshalling is available, but not required.

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
TestClass(1, None).js
TestClass(1, None).js + ("blah".js -> 1.js) - "a"
Seq(TestClass(1, None), TestClass(1, Some(10), c = "hihi")).js
```

SBT
---

```scala

resolvers += "mmreleases" at
    "https://artifactory.mediamath.com/artifactory/libs-release-global"

libraryDependencies += "com.mediamath" %% "scala-json" % "0.1"

```

and for scala.js

```scala

libraryDependencies += "com.mediamath" %%% "scala-json" % "0.1"

```