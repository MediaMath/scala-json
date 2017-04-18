package json

import json.tools.Migration
import utest._

object MigratingObjectAccessorTest extends TestSuite {

  case class TestModel(version: Int, data: String)

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


  implicit val testModelAcc: ObjectAccessor[TestModel] = new Migration.Accessor(testModelMigrations, "version", ObjectAccessor.create[TestModel])

  val needsNoMigrations = """{"data": "Data","version": 2}"""
  val needsOneMigration = """{"data": "Awesome Data","version": 1}"""
  val needsTwoMigrations = """{"oldData": "Data","version": 0}"""

  val tests = this {
    "MigratingObjectAccessor" - {

      "Needs 1 Migration" - {
        val json = needsOneMigration
        val result = JValue.fromString(json).toObject[TestModel]
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
    }
  }
}
