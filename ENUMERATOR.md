Enumerators are another handy tool provided in this library. They allow you to use an enumerated type
with case objects. The default enumerator works with String types, but the typed enumerator can work with
any result type.

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

