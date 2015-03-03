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

scala> def map = Map("hey" -> "there")
map: scala.collection.immutable.Map[String,String]

scala> map.js
res2: json.JObject =
{
  "hey": "there"
}

scala> Seq.fill(3)(map).js.toDenseString
res3: String = [{"hey":"there"},{"hey":"there"},{"hey":"there"}]

scala> map.keySet.headOption.js
res4: json.JValue = "hey"
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
