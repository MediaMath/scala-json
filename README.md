scala-json
[![Known Vulnerabilities](https://snyk.io/test/github/MediaMath/scala-json/badge.svg?targetFile=build.sbt)](https://snyk.io/test/github/MediaMath/scala-json?targetFile=build.sbt)
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
for [scala](https://github.com/scala/scala), [scala-native](https://github.com/scala-native/scala-native)
and [scala-js](https://github.com/scala-js/scala-js).
* Extensible accessor API. Serialize any type you want.
* Provides a useful JS-like AST for intermediate JSON data.
* Implicit [accessors](./docs/ACCESSORS.md) that chain to resolve Higher-Kind types (e.g. ```Option[T]```).
* Uses default fields correctly.
* Preserves object field order.
* Rich field exceptions with field names (all field errors, not just the first), perfect for form validation.
* Ability to use non-string key types in a Map (for key types that serialize to JString).
* Enables use of normal looking scala structures with any previously existing JSON API.
* Produces pretty and human readable JSON from normal scala types.
* Supports [scala-js](https://github.com/scala-js/scala-js) so you can extend your models to the web.
* Supports [scala-native](https://github.com/scala-native/scala-native) so you can take your models everywhere else
  (requires [jansson](https://github.com/akheron/jansson), available through apt, brew, etc).
* Enables you to create readable APIs that match existing/specific class structure.
* Exposes rich compile-time type info, more run-time type data than reflect could ever provide.
* Uses existing scala collection CanBuildFrom factories to support buildable collections.
* Provides support for unknown types (Any) via 'pickling' with a run-time class [registry](./docs/REGISTRY.md).
* Support for scala 2.10.x, 2.11.x, 2.12.0-M3.
* Support for scala-js 0.6.x.
* Support for scala-native 0.1.x.


Docs
---

* [Usage and Examples](./docs/USAGE.md) - Getting started with basic usage and examples.
* [Accessors](./docs/ACCESSORS.md) - Accessors are the compile-time constructs that allow you to go from a JValue to a scala type and back.
* [Registry](./docs/REGISTRY.md) - The Accessor Registry allows you to pickle registered types from untyped (Any) data.
* [Enumerator](./docs/ENUMERATOR.md) - Allows enumerated case object values of a sealed trait (useful for map keys).
* [EpochDeadline](http://mediamath.github.io/scala-json/doc/index.html#json.tools.EpochDeadline$) - Clone of scala's Deadline that serializes to numeric Unix epoch time.
* [Migration](./docs/MIGRATION.md) - Uses a version field in JSON to transform old schemas to new ones. 
* [Scaladocs](http://mediamath.github.io/scala-json/doc/json/package.html)

SBT
---

```scala
resolvers += "mmreleases" at "https://artifactory.mediamath.com/artifactory/libs-release-global"

//scala
libraryDependencies += "com.mediamath" %% "scala-json" % "1.1"

//or scala + scala-js/scala-native
libraryDependencies += "com.mediamath" %%% "scala-json" % "1.1"

//for @accessor annotation support
resolvers += Resolver.sonatypeRepo("releases")
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
```

Dependencies
---

* [macro-paradise](http://docs.scala-lang.org/overviews/macros/paradise.html) 2.1.0+ required for @accessor annotation
* [jackson](https://github.com/FasterXML/jackson) for JVM JSON string parsing
* [jansson](https://github.com/akheron/jansson) for Scala Native JSON string parsing
* [re2](https://github.com/google/re2) for fast and safe RegEx
* [µTest](https://github.com/lihaoyi/utest) for testing
* [tut](https://github.com/tpolecat/tut) for doc building

[Contributing](./docs/CONTRIBUTING.md)
---


