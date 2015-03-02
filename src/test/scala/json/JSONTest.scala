
package json

import json.internal.JSONAnnotations.FieldAccessorAnnotation

import scala.annotation.meta

/*case class TestObject3(label: Option[String],
		url: Option[String], id: Long)*/

trait TestObject3 extends Product4[String, Double, Long, Option[String]] {
  def `status`: String
  def `percent`: Double
  def `id`: Long
  def `errorMessage`: Option[String]

  def _1 = `status`
  def _2 = `percent`
  def _3 = `id`
  def _4 = `errorMessage`

  def canEqual(other: Any) = other.isInstanceOf[TestObject3]

  override def equals(other: Any): Boolean = runtime.ScalaRunTime._equals(this, other)

  override def hashCode: Int = runtime.ScalaRunTime._hashCode(this)

  override def toString: String = runtime.ScalaRunTime._toString(this)
}

object TestObject3 {
  def apply(
    `status`: String,
    `percent`: Double,
    `id`: Long,
    `errorMessage`: Option[String] = None) = new Immutable(status, percent, id, errorMessage)

  class Immutable(val `status`: String,
    val `percent`: Double,
    val `id`: Long,
    val `errorMessage`: Option[String]) extends TestObject3
}

case class TestObject2(a: String, b: String, qqq: Double = 2.456,
  c: String = "ddddd", d: Option[String] = Some("XXXX"),
  @JSONFieldName(field = "aa11")@Tester.NumAnno(11) ffff: Option[String] = None,
  @JSONFieldName(field = "zzz2") zzz: Map[String, TestObject3] = Map("asfa" ->
    TestObject3("", 0.5, 4, None)),
  seqa: Seq[Int] = Nil,
  blah: Int = 123)

case class TestObject(a: String,
  @JSONFieldName(field = "BLAH") b: String,
  c: Int, d: TestObject2)

trait TestTrait {
  def num: Int
}

object Tester {
  case class NumAnnoGeneric(n: Int) extends FieldAccessorAnnotation
  type NumAnno = (NumAnnoGeneric @meta.field @meta.getter)

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

  implicit val accessorForTestObject3: CaseClassObjectAccessor[TestObject3] =
    (ObjectAccessor.createFor(null: TestObject3))

  implicit val accessorForTestObject2: CaseClassObjectAccessor[TestObject2] =
    (ObjectAccessor.createFor(null: TestObject2))

  implicit val accessorForTestObject: CaseClassObjectAccessor[TestObject] =
    (ObjectAccessor.createFor(null: TestObject))

  def main(args: Array[String]) {
    val test2 = TestObject2("sdfsdg", "dfdfd")
    val test1 = TestObject("aaa", "bbbb", 5, test2)

    println(accessorForTestObject2.fields.flatMap(_.annos))

    val acc = ObjectAccessor.accessorFor(test1)
    println(acc)
    val jval = JObject(test1)
    val t2jval = JObject(test2)

    //import JValue.canBuildFrom

    println(JValue.fromString(jval.toString))

    val testiter: JValue = jval.map(x => x) //(JValue.canBuildFrom[JValue])
    println("mapped", testiter)

    require(jval("sdgsdgsdgsdg") === JUndefined)

    println((jval, t2jval))

    val badt2 = t2jval + ("a" ->> JUndefined) + ("b" ->> JUndefined)
    val badt1 = jval + ("d" ->> badt2) + ("BLAH" ->> JUndefined)
    println(badt1)

    val parsedt2 = fromJSON[TestObject2](t2jval)

    println(parsedt2)

    require(test2 == parsedt2)

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