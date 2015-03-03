
package json

import json.internal.JSONAnnotations.FieldAccessorAnnotation
import json.tools.{TypedEnumerator, Enumerator}
import utest.framework.TestSuite
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
    implicit val acc = ObjectAccessor.of[TestObject3]
  }

  object TestObject2 {
    implicit val acc = ObjectAccessor.of[TestObject2]
  }
  case class TestObject2(a: String, b: String, qqq: Double = 2.456,
      c: String = "ddddd", d: Option[String] = Some("XXXX"),
      @JSONFieldName(field = "aa11") @Tester.NumAnno(11) ffff: Option[String] = None,
      @JSONFieldName(field = "zzz2") zzz: Map[String, TestObject3] = Map("asfa" ->
          TestObject3("", 0.5, 4)),
      seqa: Seq[Int] = Nil,
      blah: Int = 123)

  object TestObject {
    implicit val acc = ObjectAccessor.of[TestObject]
  }
  case class TestObject(a: String,
      @JSONFieldName(field = "BLAH") b: String,
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

  implicit val testCustomAcc = JSONAccessor.create(
    (x: Byte) => JNumber(x),
    _.toJNumber.num.toByte
  )

  implicit val testCustomAcc2 = JSONAccessor.create(
    (x: TestTrait) => JNumber(x.num),
    x => new TestTrait {
      def num = x.toJNumber.toInt
    }
  )

  def testJSONEqual(jv: JValue) =
    require(JValue.fromString(jv.toString) == jv,
      jv.toString + " != " + JValue.fromString(jv.toString))

  val tests = TestSuite {
    "JSON accessor should" - {
      val test3 = TestObject3("", 0.5, 4)
      val test2 = TestObject2("sdfsdg", "dfdfd")
      val test1 = TestObject("aaa", "bbbb", 5, test2)
      val acc = ObjectAccessor.accessorFor(test1)
      val jval = test1.js
      val t2jval = test2.js
      val t3jval = test3.js
      def testiter: JValue = jval.map(x => x) //(JValue.canBuildFrom[JValue])
      def badt2 = t2jval + ("a" ->> JUndefined) + ("b" ->> JUndefined)
      def badt1 = jval + ("d" ->> badt2) + ("BLAH" ->> JUndefined)
      def parsedt2 = fromJSON[TestObject2](t2jval)

      "have annos" - require(TestObject2.acc.fields.flatMap(_.annos).nonEmpty)

      "parse json" - JValue.fromString(testJSON2)

      "have equality3" - testJSONEqual(t3jval)
      "have equality string" - testJSONEqual(JString("test"))
      "have equality num" - testJSONEqual(JNumber(0.5))
      "have equality arr" - testJSONEqual(JArray(JNumber(0.5), "blah".js))
      "have equality bool" - testJSONEqual(JTrue)
      "have equality json" - testJSONEqual(JValue.fromString("""{"status": "", "percent": 0.5, "id": 4}"""))
      "have equality obj" - testJSONEqual(JObject("test".js -> "test".js, "test2".js -> 0.5.js))

      "enumerator" - {
        val k = CorrectionReason.AA.js
        require(CorrectionReason.AA == k.toObject[CorrectionReason])
      }

      "typed enumerator" - {
        val k = TypedEnumA.AA.js
        require(TypedEnumA.AA == k.toObject[TypedEnumA])
      }

      "have order equality" - require(testiter == jval)

      "access properties as undefined" - require(jval("sdgsdgsdgsdg") === JUndefined)

      "ignore unidentified fields" - require(test2 == parsedt2)

    }
  }

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