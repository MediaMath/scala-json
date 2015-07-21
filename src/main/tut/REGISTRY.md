AccessorRegistry
================

The AccessorRegistry gives you a way of 'pickling' objects based on their underlying type. Unlike when
using the accessors implicitly, this requires a runtime lookup of the class type of a serialized/deserialized object to
locate the accessor. This can be especially handy in message-passing systems that may need to marshal
a blob without knowing its source/destination type.

```tut
import json._
import json.tools.AccessorRegistry

case class TestClass(a: Int, b: Option[Int], c: String = "", d: Option[Int] = None)
implicit val accessor = ObjectAccessor.of[TestClass]

AccessorRegistry.addAccessor[TestClass]
```

The AccessorRegistry also provides an accessor for the Scala
type 'Any'. When used carefully, this can allow you to serialize to and from the type 'Any' assuming
the base types have been registered in the AccessorRegistry.

```tut
case class Envelope(msg: Any)
implicit val envAccessor = {
  import AccessorRegistry.anyAccessor

  ObjectAccessor.of[Envelope]
}
AccessorRegistry.addAccessor[Envelope]

val inst = Envelope(TestClass(1, None))
val regularJS = inst.js
val pickeledJS = inst.js(AccessorRegistry.anyAccessor)
require(pickeledJS.toObject[Any](AccessorRegistry.anyAccessor) == inst)
require(regularJS.toObject[Envelope] == inst)
```