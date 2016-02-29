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
* Uses default fields correctly.
* Provides a useful JS-like AST for intermediate JSON data.
* Implicit [accessors](./ACCESSORS.md) that chain to resolve Higher-Kind types (```Option[T]```).
* Enables use of normal looking scala structures with any existing JSON API.
* Produces pretty and human readable JSON from normal scala types.
* Supports [scala-js](https://github.com/scala-js/scala-js) so you can extend your models to the web.
* Enables you to create readable APIs that match existing/specific class structure.
* Exposes rich compile-time type info, more run-time type data than reflect could ever provide.
* Uses existing scala collection CanBuildFrom factories to support buildable collections.
* Provides support for unknown types (Any) via 'pickling' with a run-time class [registry](./REGISTRY.md).
* Support for scala 2.10.x, 2.11.x, 2.12.0-M3.
* Support for scala-js 0.6.x.


Docs
---

* [Usage and Examples](./USAGE.md) - Getting started with basic usage and examples.
* [Accessors](./ACCESSORS.md) - Accessors are the compile-time constructs that allow you to go from a JValue to a scala type and back.
* [Registry](./REGISTRY.md) - The Accessor Registry allows you to pickle registered types from untyped (Any) data.
* [Enumerator](./ENUMERATOR.md) - Allows enumerated case object values of a sealed trait.
* [Migration](./MIGRATION.md) - Simple JSON migration support.
* [Scaladocs](http://mediamath.github.io/scala-json/doc/json/package.html)

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
* [jackson](https://github.com/FasterXML/jackson) for JVM JSON string parsing
* [µTest](https://github.com/lihaoyi/utest) for testing
* [tut](https://github.com/tpolecat/tut) for doc building

