The AccessorRegistry gives you a way of 'pickling' objects based on their underlying type. Unlike when
using the accessors implicitly, this requires looking up the class type of a serialized/deserialized object to
locate the accessor. This can be especially handy in message-passing systems that may need to deserialize
a blob without knowing its destination type. The AccessorRegistry also provides an accessor for the Scala
type 'Any'. When used carefully, this can allow you to serialize too and from the type 'Any' assuming
the base types have been registered in the AccessorRegistry.

```scala
scala> import json._
import json._

scala> import json.tools.AccessorRegistry
import json.tools.AccessorRegistry

scala> case class TestClass(a: Int, b: Option[Int], c: String = "", d: Option[Int] = None)
defined class TestClass

scala> implicit val acc = ObjectAccessor.of[TestClass]
acc: json.CaseClassObjectAccessor[TestClass] = ObjectAccessor[TestClass]

scala> AccessorRegistry.addAccessor[TestClass]
```

```scala
scala> val inst = TestClass(1, None)
inst: TestClass = TestClass(1,None,,None)

scala> val jv = inst.js(AccessorRegistry.anyAccessor)
jv: json.JObject =
{
  "data": {
    "a": 1,
    "c": ""
  },
  "clazz": "TestClass"
}

scala> require(jv.toObject[Any](AccessorRegistry.anyAccessor) == inst)
```

