
/*
 * Copyright 2016 MediaMath, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package json

import json.internal.PrimitiveJArray
import json.tools.{TypedEnumerator, Enumerator}
import utest._

import scala.annotation.meta


object Tester extends TestSuite {
  case class NumAnnoGeneric(n: Int) extends FieldAccessorAnnotation
  type NumAnno = (NumAnnoGeneric @meta.field @meta.getter)

  case class TestObject3(val `status`: String,
      val `percent`: Double,
      val `id`: Long,
      ᛋᚳᛖᚪ: String = "ᛋᚳᛖᚪᛚ᛫ᚦᛖᚪᚻ᛫ᛗᚪᚾᚾᚪ᛫ᚷᛖᚻᚹᛦᛚᚳ᛫ᛗᛁᚳᛚᚢᚾ᛫ᚻᛦᛏ᛫ᛞᚫᛚᚪᚾ")
  object TestObject3 {
    implicit val acc = ObjectAccessor.create[TestObject3]
  }

  object TestObject2 {
    implicit val acc = ObjectAccessor.create[TestObject2]
  }
  case class TestObject2(a: String, b: String, qqq: Double = 2.456,
      c: String = "ddddd", d: Option[String] = Some("XXXX"),
      @name(field = "aa11") @Tester.NumAnno(11) ffff: Option[String] = None,
      @name(field = "zzz2") zzz: Map[String, TestObject3] = Map("asfa" ->
          TestObject3("", 0.5, 4)),
      seqa: Seq[Int] = Nil,
      blah: Int = 123)

  object TestObject {
    implicit val acc = ObjectAccessor.create[TestObject]
  }
  case class TestObject(a: String,
      @name(field = "BLAH") b: String,
      c: Int, d: TestObject2)

  trait TestTrait {
    def num: Int
  }

  val testJSON = """
{
    "x": 1,
    "y": "str",
    "c": 2.2,
    "d": {
    	"A": 1
    }
}

                 """
  sealed trait CorrectionReason extends CorrectionReason.Value {
    def key = toString
  }

  object CorrectionReason extends Enumerator[CorrectionReason] {
    case object AA extends CorrectionReason
    case object BB extends CorrectionReason
    case object CC extends CorrectionReason
    case object DD extends CorrectionReason

    val values = Set[CorrectionReason](AA, BB, CC, DD)
  }

  sealed trait TypedEnumA extends TypedEnumA.Value {
    def key = toString.charAt(0).toInt
  }

  object TypedEnumA extends TypedEnumerator[Int, TypedEnumA, JNumber] {
    case object AA extends TypedEnumA
    case object BB extends TypedEnumA
    case object CC extends TypedEnumA
    case object DD extends TypedEnumA

    val values = Set(AA, BB, CC, DD)
  }

  implicit val testCustomAcc2 = JSONAccessor.create(
    (x: TestTrait) => JNumber(x.num),
    x => new TestTrait {
      def num = x.toJNumber.toInt
    }
  )

  def testJSONEqual(jv: JValue) =
    assert(JValue.fromString(jv.toString) == jv)

  val tests = TestSuite {
    "JSON accessor should" - {
      val test3 = TestObject3("", 0.5, 4)
      val test2 = TestObject2("sdfsdg", "dfdfd")
      val test1 = TestObject("aaa", "bbbb", 5, test2)
      val jval = test1.js
      val t2jval = test2.js
      val t3jval = test3.js
      def testiter: JValue = jval.map(x => x) //(JValue.canBuildFrom[JValue])
      def badt2 = t2jval + ("a" -> JUndefined) + ("b" -> JUndefined)
      def badt1 = jval + ("d" -> badt2) + ("BLAH" -> JUndefined)
      def parsedt2 = fromJSON[TestObject2](t2jval)

      "have annos" - require(TestObject2.acc.fields.flatMap(_.annos).nonEmpty)

      "parse json" - {
        val jv = JValue.fromString(testJSON2)
        val field = jv \ "web-app" \ "servlet" \ 0 \ "init-param" \ "templateProcessorClass"

        assert(field != JUndefined)
      }

      //TODO: this breaks on native for no apparent reason
      /*"parse json equality" - {
        val parsed = JValue.fromString(testJSON2)
        val reparsed = JValue fromString parsed.toString

        assert(parsed == reparsed)
      }*/

      "have equality3" - testJSONEqual(t3jval)
      "have equality string" - testJSONEqual(JString("test"))
      "have equality num" - testJSONEqual(JNumber(0.5))
      "have equality arr" - testJSONEqual(JArray(JNumber(0.5), "blah".js))
      "have equality arr2" - testJSONEqual(JArray(0.5.js, 0.25.js, 0.8.js))
      "have equality arr3" - testJSONEqual(JArray(5.js, 25.js, 8.js))
      "have equality arr4" - testJSONEqual(JArray(JTrue, JFalse, JTrue))
      "have equality bool" - testJSONEqual(JTrue)
      "have equality json" - testJSONEqual(JValue.fromString("""{"status": "", "percent": 0.5, "id": 4}"""))
      "have equality obj" - testJSONEqual(JObject("test" -> "test".js, "test2" -> 0.5.js))

      "native implicit accessor for int array" - {
        val jv = Seq[Int](1, 2, 3).js

        assert(jv.isInstanceOf[PrimitiveJArray[_]])
      }

      "native implicit accessor for bool array" - {
        val jv = Seq[Boolean](true, false, true).js

        assert(jv.isInstanceOf[PrimitiveJArray[_]])
      }

      "native implicit accessor for double array" - {
        val jv = Seq[Double](1, 2, 3).js

        assert(jv.isInstanceOf[PrimitiveJArray[_]])
      }

      "native implicit accessor for byte array" - {
        val jv = Seq[Byte](1, 2, 3).js

        assert(accessorOf[Byte].isInstanceOf[PrimitiveJArray.Builder[_]])

        assert(jv.isInstanceOf[PrimitiveJArray[_]])
      }

      "enumerator" - {
        val k = CorrectionReason.AA.js(CorrectionReason.accessor)
        assert(CorrectionReason.AA == k.toObject[CorrectionReason])
      }

      "typed enumerator" - {
        val k = TypedEnumA.AA.js
        assert(TypedEnumA.AA == k.toObject[TypedEnumA])
      }

      "have order equality" - assert(testiter == jval)

      "access properties as undefined" - assert(jval("sdgsdgsdgsdg") === JUndefined)

      "ignore unidentified fields" - assert(test2 == parsedt2)

      "jarray as collection" - {
        val jarr = JArray(1, 2, 3, 4)
        val arr: JArray = jarr.map(x => x)
        val arr2: Seq[Double] = jarr.map(_.jNumber.value)
      }

      //TODO: fix this later, formatting got weird
      /*"exact dense json duplicate (ordering)" - {
        val parsed = JValue.fromString(compactJSONString)

        assert(parsed.toDenseString == compactJSONString)
      }*/
    }
  }

  val compactJSONString = """[{"_id":"56d5iįe2d0758anñuübe1992a5b1d7","index":0,"guid":"2e0e537a-0376-43da-bc09-6b1c8e6a073a","isActive":false,"balance":"$2,105.11","aaa":1231.1,"bb":12,"picture":"http://placehold.it/32x32","age":26,"eyeColor":"green","name":"Kerri Webb","gender":"female","company":"MAXEMIA","email":"kerriwebb@maxemia.com","phone":"+1 (807) 422-3117","address":"262 Little Street, Shepardsville, North Dakota, 9759","about":"Eiusmod do aliqua sunt est ut et cillum incididunt culpa do in. Aute excepteur nulla excepteur esse cillum dolore fugiat pariatur labore cillum labore cupidatat. Nisi consectetur occaecat consequat reprehenderit pariatur nulla cillum enim irure ipsum fugiat reprehenderit. Aliqua labore sunt ut elit quis consectetur. Voluptate incididunt exercitation proident adipisicing nisi. Ipsum occaecat dolor consequat consectetur sunt labore eiusmod.\r\n","registered":"2014-01-04T03:55:21 +07:00","latitude":9.304404,"longitude":176.760414,"tags":["magna","sint","non","proident","sunt","commodo","ex"],"friends":[{"id":0,"name":"Hull Hahn"},{"id":1,"name":"Katharine Frazier"},{"id":2,"name":"Kay Fox"}],"greeting":"Hello, Kerri Webb! You have 9 unread messages.","favoriteFruit":"strawberry"},{"_id":"56d5e2d0ab98bd97dc80746d","index":1,"guid":"91337808-aa61-42e4-8e0d-6b591564df09","isActive":true,"balance":"$2,659.26","picture":"http://placehold.it/32x32","age":23,"eyeColor":"green","name":"Monroe Wright","gender":"male","company":"DANCITY","email":"monroewright@dancity.com","phone":"+1 (848) 406-2882","address":"320 Harden Street, Belvoir, Oregon, 1004","about":"Id nostrud officia dolor reprehenderit anim aliqua aute nisi excepteur nisi sit commodo fugiat consequat. Consectetur do aute dolor sint est laborum amet. Do consectetur aute ex nostrud elit mollit veniam eu ad pariatur est et.\r\n","registered":"2014-11-09T07:24:39 +07:00","latitude":1.774727,"longitude":95.401304,"tags":["in","amet","qui","ex","enim","voluptate","consequat"],"friends":[{"id":0,"name":"Charles Allen"},{"id":1,"name":"Dotson Freeman"},{"id":2,"name":"Logan Santos"}],"greeting":"Hello, Monroe Wright! You have 5 unread messages.","favoriteFruit":"banana"},{"_id":"56d5e2d0dbb9437f955f101c","index":2,"guid":"4e8c0e86-deaf-4c28-a5b6-5ab1d094ab27","isActive":false,"balance":"$2,983.78","picture":"http://placehold.it/32x32","age":35,"eyeColor":"green","name":"Stacey Turner","gender":"female","company":"ROUGHIES","email":"staceyturner@roughies.com","phone":"+1 (853) 573-3609","address":"442 Cropsey Avenue, Summerset, Georgia, 4613","about":"Consequat nisi deserunt eiusmod nostrud. Sunt dolore anim esse voluptate velit dolore magna magna labore proident consequat mollit. Sunt ut exercitation aliquip officia dolor nulla nisi.\r\n","registered":"2014-01-02T05:45:31 +07:00","latitude":36.242548,"longitude":60.604439,"tags":["incididunt","adipisicing","cillum","consectetur","amet","eiusmod","nulla"],"friends":[{"id":0,"name":"Geraldine Noble"},{"id":1,"name":"Strong Mcconnell"},{"id":2,"name":"Freida Smith"}],"greeting":"Hello, Stacey Turner! You have 7 unread messages.","favoriteFruit":"strawberry"},{"_id":"56d5e2d0ed650cdfa62f8c24","index":3,"guid":"65dde550-0b9a-4466-95e7-ca71b67af2ba","isActive":true,"balance":"$2,689.08","picture":"http://placehold.it/32x32","age":30,"eyeColor":"blue","name":"Francine Williamson","gender":"female","company":"CHILLIUM","email":"francinewilliamson@chillium.com","phone":"+1 (939) 402-2815","address":"169 Bevy Court, Muse, Louisiana, 7491","about":"Do nisi reprehenderit sit elit mollit tempor nostrud in non cillum ipsum exercitation. Reprehenderit veniam qui enim eiusmod tempor pariatur. Eiusmod tempor amet laboris deserunt dolor anim enim laboris sint. Cupidatat ad reprehenderit elit quis aliqua consequat sit nostrud cupidatat ea.\r\n","registered":"2014-05-04T05:51:19 +06:00","latitude":44.749722,"longitude":112.9393,"tags":["ad","est","incididunt","sint","nostrud","ad","laborum"],"friends":[{"id":0,"name":"Hughes Cole"},{"id":1,"name":"Buchanan Vang"},{"id":2,"name":"Melissa Baker"}],"greeting":"Hello, Francine Williamson! You have 3 unread messages.","favoriteFruit":"banana"},{"_id":"56d5e2d089cbd5b3582ad250","index":4,"guid":"d1d765cf-2624-4631-99b2-073314a653e8","isActive":false,"balance":"$1,249.26","picture":"http://placehold.it/32x32","age":24,"eyeColor":"green","name":"Mamie Sears","gender":"female","company":"MEMORA","email":"mamiesears@memora.com","phone":"+1 (991) 501-2985","address":"155 Manhattan Court, Somerset, Oklahoma, 6501","about":"Cupidatat minim ex laborum anim qui id consequat velit dolore labore consectetur occaecat. Ad magna do ipsum culpa ipsum ullamco aute nisi consectetur mollit. Ullamco culpa magna aute enim anim et cillum elit adipisicing eu velit nisi ea magna. Eiusmod ad non cillum aliqua aliqua tempor duis aliquip. Amet amet in adipisicing ut eu amet laboris aute sint incididunt. Voluptate veniam quis minim enim dolor cillum eu duis.\r\n","registered":"2015-11-14T02:38:49 +07:00","latitude":-78.584269,"longitude":38.50967,"tags":["qui","ipsum","excepteur","ullamco","laborum","sit","amet"],"friends":[{"id":0,"name":"Evangelina Burgess"},{"id":1,"name":"Wolf Calderon"},{"id":2,"name":"Marlene Kramer"}],"greeting":"Hello, Mamie Sears! You have 1 unread messages.","favoriteFruit":"apple"},{"_id":"56d5e2d0b93d369174536e30","index":5,"guid":"0011f749-91c3-494b-ac61-64cff41ee787","isActive":true,"balance":"$3,614.81","picture":"http://placehold.it/32x32","age":32,"eyeColor":"blue","name":"Sparks Dale","gender":"male","company":"INEAR","email":"sparksdale@inear.com","phone":"+1 (840) 429-3099","address":"343 Homecrest Avenue, Cucumber, Colorado, 4107","about":"Eu aliquip adipisicing quis amet ea duis. Non ipsum minim reprehenderit excepteur ipsum esse esse commodo culpa fugiat excepteur est dolor. Id proident magna et laboris commodo cupidatat Lorem culpa sunt irure occaecat sint. Laborum dolor elit eiusmod Lorem minim.\r\n","registered":"2014-02-05T05:47:21 +07:00","latitude":-51.82869,"longitude":136.932002,"tags":["officia","do","do","commodo","sit","esse","aute"],"friends":[{"id":0,"name":"Cardenas Keller"},{"id":1,"name":"Arlene Woodward"},{"id":2,"name":"Moreno Kirk"}],"greeting":"Hello, Sparks Dale! You have 4 unread messages.","favoriteFruit":"strawberry"}]"""

  val testJSON2 = """
    {"web-app": {
  "servlet": [
    {
      "servlet-name": "cofaxCDS",
      "servlet-class": "org.cofax.cds.CDSServlet",
      "init-param": {
        "configGlossary:installationAt": "Philadelphia, PA",
        "configGlossary:adminEmail": "ksm@pobox.com",
        "configGlossary:poweredBy": "Cofax",
        "configGlossary:poweredByIcon": "/images/cofax.gif",
        "configGlossary:staticPath": "/content/static",
        "templateProcessorClass": "org.cofax.WysiwygTemplate",
        "templateLoaderClass": "org.cofax.FilesTemplateLoader",
        "templatePath": "templates",
        "templateOverridePath": "",
        "defaultListTemplate": "listTe\"mplate.htm",
        "defaultFileTemplate": "articleTemplate.htm",
        "useJSP": false,
        "jspListTemplate": "listTemplate.jsp",
        "jspFileTemplate": "articleTem\"plate.jsp",
        "cachePackageTagsTrack": 200,
        "cachePackageTagsStore": 200.11241241,
        "cachePackageTagsRefresh": 60,
        "cacheTemplatesTrack": 100,
        "cacheTemplatesStore": 50,
        "cacheTemplatesRefresh": 15,
        "cachePagesTrack": 200,
        "cachePagesStore": 100,
        "cachePagesRefresh": 10,
        "cachePagesDirtyRead": 10,
        "searchEngineListTemplate": "forSearchEnginesList.htm",
        "searchEngineFileTemplate": "forSearc\"hEngines.htm",
        "searchEngineRobotsDb": "WEB-INF/robots.db",
        "useDataStore": true,
        "dataStoreClass": "org.cofax.SqlDataStore",
        "redirectionClass": "org.cofax.SqlRedirection",
        "dataStoreName": "cofax",
        "dataStoreDriver": "com.microsoft.jdbc.sqlserver.SQLServerDriver",
        "dataStoreUrl": "jdbc:microsoft:sqlserver://LOCALHOST:1433;DatabaseName=goon",
        "dataStoreUser": "sa",
        "dataStorePassword": "dataStoreTestQuery",
        "dataStoreTestQuery": "SET NOCOUNT ON;select test='test';",
        "dataStoreLogFile": "/usr/local/tomcat/logs/datastore.log",
        "dataStoreInitConns": 10,
        "dataStoreMaxConns": 100,
        "dataStoreConnUsageLimit": 100,
        "dataStoreLogLevel": "debug",
        "maxUrlLength": 500}},
    {
      "servlet-name": "cofaxEmail",
      "servlet-class": "org.cofax.cds.EmailServlet",
      "init-param": {
      "mailHost": "mail1",
      "mailHostOverride": "mail2"}},
    {
      "servlet-name": "cofaxAdmin",
      "servlet-class": "org.cofax.cds.AdminServlet"},

    {
      "servlet-name": "fileServlet",
      "servlet-class": "org.cofax.cds.FileServlet"},
    {
      "servlet-name": "cofaxTools",
      "servlet-class": "org.cofax.cms.CofaxToolsServlet",
      "init-param": {
        "templatePath": "toolstemplates/",
        "log": 1,
        "logLocation": "/usr/local/tomcat/logs/CofaxTools.log",
        "logMaxSize": "",
        "dataLog": 1,
        "dataLogLocation": "/usr/local/tomcat/logs/dataLog.log",
        "dataLogMaxSize": "",
        "removePageCache": "/content/admin/remove?cache=pages&id=",
        "removeTemplateCache": "/content/admin/remove?cache=templates&id=",
        "fileTransferFolder": "/usr/local/tomcat/webapps/content/fileTransferFolder",
        "lookInContext": 1,
        "adminGroupID": 4,
        "betaServer": true}}],
  "servlet-mapping": {
    "cofaxCDS": "/",
    "cofaxEmail": "/cofaxutil/aemail/*",
    "cofaxAdmin": "/admin/*",
    "fileServlet": "/static/*",
    "cofaxTools": "/tools/*"},

  "taglib": {
    "taglib-uri": "cofax.tld",
    "taglib-location": "/WEB-INF/tlds/cofax.tld"}}}
                  """

}