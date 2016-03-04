The [Enumerator](http://mediamath.github.io/scala-json/doc/index.html#json.tools.Enumerator)
is another handy tool provided in this library. It allows you to use an enumerated value
for instances of a scala type. The default enumerator works with String types, but the
[TypedEnumerator](http://mediamath.github.io/scala-json/doc/index.html#json.tools.TypedEnumerator)
can work with any result type.

```scala
scala> import json._
import json._

scala> import json.tools.Enumerator
import json.tools.Enumerator

scala> object Example {
     |     object TestEnumerator extends Enumerator[TestEnumerator] {
     |         case object ThisValue extends TestEnumerator
     |         case object ThatValue extends TestEnumerator
     |         val values = Set(ThisValue, ThatValue) //all values needed here for the reverse mapping
     |     }
     |     sealed trait TestEnumerator extends TestEnumerator.Value {
     |         //define the key used for each value.
     |         //this takes the lower case of the case object name.
     |         def key = toString.toLowerCase
     |     }
     | }
defined object Example

scala> Example.TestEnumerator.ThisValue.js
res0: json.JString = "thisvalue"

scala> "thatvalue".js.toObject[Example.TestEnumerator]
res1: Example.TestEnumerator = ThatValue
```

