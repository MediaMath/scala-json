package src.test.scala.json

import json.{JValue,JString}
import json.tools.{MigratingObjectAccessor, Migration}
import json._
import utest.framework.TestSuite
import utest._

object MigratingObjectAccessorTest extends TestSuite {

  case class TestModel(version: Int, data: String)

  case class Thingie(name: String, model: TestModel, version: Int)

  val testModelMigrations: Seq[Migration[Int]] = Seq(

    Migration(1) { jObject =>

      jObject.get("oldData") match {

        case Some(data) => jObject + ("data" -> data) - "oldData"
        case _          => jObject
      }
    },
    Migration(2) { jObject =>

      jObject.get("data") match {

        case Some(data) => jObject + ("data" -> JString(data.jString.value + "-Version 2"))
        case _          => jObject
      }
    }
  )

  val thingieMigrations: Seq[Migration[Int]] = Seq(Migration(1).removeFieldFromChild("model", "bar"))

  implicit val testModelAcc: ObjectAccessor[TestModel] = new MigratingObjectAccessor(testModelMigrations, "version", ObjectAccessor.create[TestModel])
  implicit val thingieAcc : ObjectAccessor[Thingie] = new MigratingObjectAccessor(thingieMigrations,"version",ObjectAccessor.create[Thingie])

  val needsNoMigrations = """{"data": "Data","version": 2}"""
  val needsOneMigration = """{"data": "Awesome Data","version": 1}"""
  val needsTwoMigrations = """{"oldData": "Data","version": 0}"""

  val tests = TestSuite {
    "MigratingObjectAccessor" - {

      "Needs 1 Migration" - {
        val json = needsOneMigration
        val result = JValue.fromString(json).toObject[TestModel]
        println(result)
        assert(result.data  == "Awesome Data-Version 2")
        assert(result.version == 2)
      }
      "Needs 2 Migrations" - {
        val json = needsTwoMigrations
        val result = JValue.fromString(json).toObject[TestModel]
        assert(result.data  == "Data-Version 2")
        assert(result.version == 2)
      }
      "Needs No Migrations" - {
        val json = needsNoMigrations
        val result = JValue.fromString(json).toObject[TestModel]
        assert(result.data  == "Data")
        assert(result.version == 2)
      }
      "Serialize to JSON" - {

        val obj = TestModel(2, "Some Data")
        val jsonString = toJSONString(obj)
        assert(JValue.fromString(jsonString).toObject[TestModel] == obj)
      }
      "removeChildField" - {

        "handle when field doesn't exist" - {

          val obj = JValue.fromString("""{"name": "bar", "model": {"version": 1, "data": "foo"}, "version": 0}""")
          val result = obj.toObject[Thingie]
          assert(result.name == "bar")
          assert(result.model == TestModel(2, "foo-Version 2"))
        }
        "handle when field exists" - {

          val obj = JValue.fromString("""{"name": "bar", "model": {"version": 1, "data": "ah", "bar": "yo"}, "version": 0}""")
          val result = obj.toObject[Thingie]
          assert(result.name == "bar")
          assert(result.model == TestModel(2, "ah-Version 2"))
        }
      }
    }
  }
}
