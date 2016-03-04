Migration
=========

The MigratingObjectAccessor can wrap a standard ObjectAccessor and provide migration functionality for JSON documents.
By specifying an Ordered version field and a sequence of Migrations, JSON documents will automatically be migrated through
the sequence of Migrations (sorted by version) before being marshaled into the final Scala object.


```scala
scala> import json._
import json._

scala> import json.tools.Migration
import json.tools.Migration

scala> case class TestModel(version: Int, data: String)
defined class TestModel

scala> implicit val testModelAcc: ObjectAccessor[TestModel] = {
     |     val testModelMigrations: Seq[Migration[Int]] = Seq(
     |         Migration(1) { jObject =>
     |           jObject.get("oldData") match {
     |             case Some(data) => jObject + ("data" -> data) - "oldData"
     |             case _          => jObject
     |           }
     |         }
     |     )
     |     new Migration.Accessor(testModelMigrations, "version", ObjectAccessor.create[TestModel])
     | }
testModelAcc: json.ObjectAccessor[TestModel] = Migration.Accessor

scala> val needsMigration = """ {"oldData": "Data","version": 0} """
needsMigration: String = " {"oldData": "Data","version": 0} "

scala> JValue.fromString(needsMigration).toObject[TestModel]
res0: TestModel = TestModel(1,Data)
```


