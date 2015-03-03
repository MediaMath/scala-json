Getting Started
===============

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

scala> def testMap = Map("hey" -> "there")
testMap: scala.collection.immutable.Map[String,String]

scala> val testMapJs = testMap.js
testMapJs: json.JObject =
{
  "hey": "there"
}

scala> Seq.fill(3)(testMap).js.toDenseString
res2: String = [{"hey":"there"},{"hey":"there"},{"hey":"there"}]

scala> testMap.keySet.headOption.js
res3: json.JValue = "hey"

scala> testMap.get("nokey").js
res4: json.JValue = null
```
* Use JValues dynamically
```scala
scala> require(testMapJs("nokey") == JUndefined)
```

SBT
---

```scala

resolvers += "mmreleases" at "https://artifactory.mediamath.com/artifactory/libs-release-global"

libraryDependencies += "com.mediamath" %% "scala-json" % "0.1"

```

and for scala.js

```scala

libraryDependencies += "com.mediamath" %%% "scala-json" % "0.1"

```
