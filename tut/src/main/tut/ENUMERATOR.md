Enumerators are another handy tool provided in this library. They allow you to use an enumerated type
with case objects. The default enumerator works with String types, but the typed enumerator can work with
any result type.

```tut
import json._
import json.tools.Enumerator

object Example {
    object TestEnumerator extends Enumerator[TestEnumerator] {
        case object ThisValue extends TestEnumerator
        case object ThatValue extends TestEnumerator

        val values = Set(ThisValue, ThatValue) //all values needed here for the reverse mapping
    }

    sealed trait TestEnumerator extends TestEnumerator.Value {
        //define the key used for each value.
        //this takes the lower case of the case object name.
        def key = toString.toLowerCase
    }
}

Example.TestEnumerator.ThisValue.js
"thatvalue".js.toObject[Example.TestEnumerator]
```