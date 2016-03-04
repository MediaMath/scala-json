AccessorRegistry
================

The [AccessorRegistry](http://mediamath.github.io/scala-json/doc/index.html#json.tools.AccessorRegistry)
gives you a way of 'pickling' objects based on their underlying type. Unlike when
using the accessors implicitly, this requires a runtime lookup of the class type of a serialized/deserialized object to
locate the accessor. This can be especially handy in message-passing systems that may need to marshal
a blob without knowing its source/destination type. When combined with pattern matching, this can provide you
with an actor-like message passing system.

```scala
scala> import json._
import json._

scala> import json.tools.AccessorRegistry
import json.tools.AccessorRegistry

scala> @accessor case class TestClass(a: Int, b: Option[Int], c: String = "", d: Option[Int] = None)
defined object TestClass
defined class TestClass

scala> AccessorRegistry.add[TestClass]
```

The AccessorRegistry also exposes itself as a selfless trait if you would like to encapsulate your own registry.

```scala
scala> object MyRegistry extends AccessorRegistry
defined object MyRegistry

scala> MyRegistry.add[TestClass]
```

You can also add singleton objects (these are bound by their name classname, case objects work best)

```scala
scala> case object StartCommand
defined object StartCommand

scala> MyRegistry.add(StartCommand)
```

The AccessorRegistry also provides an accessor for the Scala
type ```Any```. When used carefully, this can allow you to serialize to and from the type ```Any``` assuming
the base types have been registered in the AccessorRegistry.

```scala
scala> case class Envelope(msg: Any)
defined class Envelope

scala> implicit val envAccessor = {
     |   import AccessorRegistry.anyAccessor //or MyRegistry.anyAccessor if using your own registry
     |   ObjectAccessor.create[Envelope]
     | }
envAccessor: json.internal.CaseClassObjectAccessor[Envelope] = CaseClassObjectAccessor

scala> AccessorRegistry.add[Envelope]

scala> val inst = Envelope(TestClass(1, None))
inst: Envelope = Envelope(TestClass(1,None,,None))

scala> val regularJS = inst.js
regularJS: json.JObject =
{
  "msg": {
    "data": {
      "a": 1,
      "b": null,
      "c": "",
      "d": null
    },
    "class": "TestClass"
  }
}

scala> val pickeledJS = inst.js(AccessorRegistry.anyAccessor)
pickeledJS: json.JValue =
{
  "data": {
    "msg": {
      "data": {
        "a": 1,
        "b": null,
        "c": "",
        "d": null
      },
      "class": "TestClass"
    }
  },
  "class": "Envelope"
}

scala> require(pickeledJS.toObject[Any](AccessorRegistry.anyAccessor) == inst)

scala> require(regularJS.toObject[Envelope] == inst)
```

