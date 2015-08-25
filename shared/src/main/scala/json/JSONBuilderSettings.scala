/*
 * Copyright 2015 MediaMath, Inc
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
  val ignoreNulls = true

  val pretty = JSONBuilderSettings(
    newLineString = "\n", tabString = "  ", spaceString = " "
  )
  val dense = JSONBuilderSettings(
    newLineString = "", tabString = "", spaceString = "")

  val default = pretty
}

case class JSONBuilderSettings private(
    spaceString: String = " ",
    ignoreNulls: Boolean = JSONBuilderSettings.ignoreNulls,
    newLineString: String = "\n",
    tabString: String = "  ") {
  private val tabs = {
    val seq = for(i <- 0 until 30) yield i -> nTabsRaw(i)
    seq.toMap withDefault nTabsRaw
  }

  private def nTabsRaw(n: Int) =
    (for (i <- 0 until n) yield tabString).mkString

  def nTabs(n: Int) = tabs(n)
}