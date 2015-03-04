scala-json
==========
Compile time JSON marshalling of primitive values and basic collections. Runtime
marshalling is available, but not required.

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

scala> testMap.js.toDenseString
res4: String = {"hey":"there"}

scala> Seq.fill(3)(Set(false, true)).js
res5: json.JArray = [[false, true], [false, true], [false, true]]

scala> testMap.keySet.headOption.js
res6: json.JValue = "hey"

scala> testMap.get("nokey").js
res7: json.JValue = null
```
* JS-like dynamic select
```scala
scala> require(testMapJs("nokey") == JUndefined)
```
* JS-like boolean conditions
```scala
scala> if(testMapJs("nokey")) sys.error("unexpected")
```
* Compile-time case class marshalling
```scala
scala> case class TestClass(a: Int, b: Option[Int], c: String = "", d: Option[Int] = None)
defined class TestClass

scala> implicit val acc = ObjectAccessor.of[TestClass]
acc: json.CaseClassObjectAccessor[TestClass]{val nameMap: String => String; val fields: IndexedSeq[json.FieldAccessor[TestClass]]; val manifest: Manifest[TestClass]} = ObjectAccessor(TestClass)

scala> TestClass(1, None).js
res10: json.JObject =
{
  "a": 1,
  "c": ""
}

scala> TestClass(1, None).js + ("blah".js -> 1.js)
res11: json.JObject =
{
  "a": 1,
  "c": "",
  "blah": 1
}

scala> Seq(TestClass(1, None), TestClass(1, Some(10), c = "hihi")).js
res12: json.JArray =
[{
  "a": 1,
  "c": ""
}, {
  "a": 1,
  "b": 10,
  "c": "hihi"
}]
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
