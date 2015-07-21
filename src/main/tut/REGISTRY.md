The AccessorRegistry gives you a way of 'pickling' objects based on their underlying type. Unlike when
using the accessors implicitly, this requires looking up the class type of a serialized/deserialized object to
locate the accessor. This can be especially handy in message-passing systems that may need to deserialize
a blob without knowing its destination type. The AccessorRegistry also provides an accessor for the Scala
type 'Any'. When used carefully, this can allow you to serialize too and from the type 'Any' assuming
the base types have been registered in the AccessorRegistry.

```tut
import json._
import json.tools.AccessorRegistry

case class TestClass(a: Int, b: Option[Int], c: String = "", d: Option[Int] = None)
implicit val acc = ObjectAccessor.of[TestClass]
AccessorRegistry.addAccessor[TestClass]
```

```tut
val inst = TestClass(1, None)
val jv = inst.js(AccessorRegistry.anyAccessor)
require(jv.toObject[Any](AccessorRegistry.anyAccessor) == inst)
```