AccessorRegistry
================

The AccessorRegistry gives you a way of 'pickling' objects based on their underlying type. Unlike when
using the accessors implicitly, this requires a runtime lookup of the class type of a serialized/deserialized object to
locate the accessor. This can be especially handy in message-passing systems that may need to marshal
a blob without knowing its source/destination type.

```scala
scala> import json._
import json._

scala> import json.tools.AccessorRegistry
import json.tools.AccessorRegistry

scala> case class TestClass(a: Int, b: Option[Int], c: String = "", d: Option[Int] = None)
defined class TestClass

scala> implicit val accessor = ObjectAccessor.of[TestClass]
accessor: json.CaseClassObjectAccessor[TestClass] = ObjectAccessor[TestClass]

scala> AccessorRegistry.addAccessor[TestClass]
```

The AccessorRegistry also provides an accessor for the Scala
type 'Any'. When used carefully, this can allow you to serialize to and from the type 'Any' assuming
the base types have been registered in the AccessorRegistry.

```scala
scala> case class Envelope(msg: Any)
defined class Envelope

scala> implicit val envAccessor = {
     |   import AccessorRegistry.anyAccessor
     |   ObjectAccessor.of[Envelope]
     | }
envAccessor: json.CaseClassObjectAccessor[Envelope] = ObjectAccessor[Envelope]

scala> AccessorRegistry.addAccessor[Envelope]

scala> val inst = Envelope(TestClass(1, None))
inst: Envelope = Envelope(TestClass(1,None,,None))

scala> val regularJS = inst.js
regularJS: json.JObject =
{
  "msg": {
    "data": {
      "a": 1,
      "c": ""
    },
    "clazz": "TestClass"
  }
}

scala> val pickeledJS = inst.js(AccessorRegistry.anyAccessor)
pickeledJS: json.JObject =
{
  "data": {
    "msg": {
      "data": {
        "a": 1,
        "c": ""
      },
      "clazz": "TestClass"
    }
  },
  "clazz": "Envelope"
}

scala> require(pickeledJS.toObject[Any](AccessorRegistry.anyAccessor) == inst)

scala> require(regularJS.toObject[Envelope] == inst)
```

