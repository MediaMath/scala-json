package json

import scala.scalajs.js.annotation.JSExport
import scalajs.js

import utest.ExecutionContext.RunNow

class ExportTest extends js.JSApp {
  @JSExport
  def main(): Unit = {
    SampleTest.runTest()
  }
}
