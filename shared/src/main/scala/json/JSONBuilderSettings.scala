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

//TODO: make these implicit
object JSONBuilderSettings {
  val defaultTabString = "  "

  private val maxNumTabs = 40

  private lazy val tabs: Map[Int, String] = {
    val seq = for(i <- 0 until maxNumTabs) yield i -> nTabsRaw(i, defaultTabString)
    seq.toMap.withDefault(nTabsRaw(_, defaultTabString))
  }

  val pretty = JSONBuilderSettings(
    newLineString = "\n", tabString = defaultTabString, spaceString = " "
  )
  val dense = JSONBuilderSettings(
    newLineString = "", tabString = "", spaceString = "")

  val default = pretty

  private def nTabsRaw(n: Int, tabString: String) =
    (for (i <- 0 until n) yield tabString).mkString
}

case class JSONBuilderSettings(
    spaceString: String,
    newLineString: String,
    tabString: String) {
  import JSONBuilderSettings._

  def nTabs(n: Int): String =
    if(n == 0 || tabString == "") ""
    else if(tabString != defaultTabString) nTabsRaw(n, tabString)
    else tabs(n)
}