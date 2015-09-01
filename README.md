scala-json
==========
Compile time JSON marshalling for [scala](https://github.com/scala/scala) and [scala-js](https://github.com/scala-js/scala-js)

Goals
-----
Compile time JSON marshalling of primitive values, case-classes, basic collections, and whatever you can imagine.
* Extensible Accessor API. Serialize any type you want.
* Provide a usable JS-like DSL for intermediate JSON data
* Create implicit accessors that chain to resolve Higher-Kind types (```Option[T]```)
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
* Compile-time case class marshalling
```scala
scala> case class TestClass(a: Int, b: Option[Int], c: String = "", d: Option[Int] = None)
defined class TestClass

scala> implicit val acc = ObjectAccessor.of[TestClass]
acc: json.CaseClassObjectAccessor[TestClass] = ObjectAccessor[TestClass]

scala> val testClassJs = TestClass(1, None).js
testClassJs: json.JObject =
{
  "a": 1,
  "c": ""
}

scala> val testClassJsString = testClassJs.toDenseString
testClassJsString: String = {"a":1,"c":""}

scala> JValue.fromString(testClassJsString).toObject[TestClass]
res10: TestClass = TestClass(1,None,,None)

scala> JObject("a".js -> 23.js).toObject[TestClass]
res11: TestClass = TestClass(23,None,,None)

scala> TestClass(1, None).js + ("blah".js -> 1.js) - "a"
res12: json.JValue =
{
  "c": "",
  "blah": 1
}

scala> val seqJson = Seq(TestClass(1, None), TestClass(1, Some(10), c = "hihi")).js
seqJson: json.JArray =
[{
  "a": 1,
  "c": ""
}, {
  "a": 1,
  "b": 10,
  "c": "hihi"
}]
```
* Dynamic field access
```scala
scala> seqJson.dynamic(1).c.value
res13: json.JValue = "hihi"

scala> seqJson.dynamic.length
res14: json.JDynamic = 2

scala> require(seqJson.d == seqJson.dynamic)
```
* Typed exceptions with field data
```scala
scala> try JObject("a".js -> "badint".js).toObject[TestClass] catch {
     |   case e: InputFormatException =>
     |     e.getExceptions.map {
     |       case fieldEx: InputFieldException if fieldEx.fieldName == "a" =>
     |         fieldEx.getMessage
     |       case _ => ""
     |     }.mkString
     | }
res16: java.io.Serializable = numeric expected but found json.JString (of value "badint")
```
* JArrays as scala collections
```scala
scala> JArray(1, 2, 3, 4).map(x => x.toJString)
res17: json.JArray = ["1", "2", "3", "4"]

scala> JArray(1, 2, 3, 4).map(_.num)
res18: scala.collection.immutable.IndexedSeq[Double] = Vector(1.0, 2.0, 3.0, 4.0)
```

[Accessors](./ACCESSORS.md)
---

Accessors are the compile-time constructs that allow you to marshal scala types.

[Registry](./REGISTRY.md)
---

The Accessor Registry allows you to pickle registered types from untyped (Any) data.

[Scaladocs](http://mediamath.github.io/scala-json/doc/json/package.html)
---

SBT
---

```scala

resolvers += "mmreleases" at
    "https://artifactory.mediamath.com/artifactory/libs-release-global"

libraryDependencies += "com.mediamath" %% "scala-json" % "0.2-SNAPSHOT"

```

and for Scala.js

```scala

libraryDependencies += "com.mediamath" %%% "scala-json" % "0.2-SNAPSHOT"

```

