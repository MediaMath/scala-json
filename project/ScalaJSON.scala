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

import sbt.Keys._
import sbt._
import sbt.dsl._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Repository {
  val host = "artifactory.mediamath.com"
  val artifactory = s"https://$host/artifactory/"
  val snapshots = s"${artifactory}libs-snapshot"
  val releases = s"${artifactory}libs-release"
  val remote = s"${artifactory}remote-repos"
  val pluginRelease = s"${artifactory}sbt-plugins-release-local"

  val snapshotRepo = "snapshots" at Repository.snapshots
  val releaseRepo = "releases" at Repository.releases

  val publishSuffix = "-local" //"-global"

  def repo(isSnapshot: Boolean) = if (isSnapshot) Repository.snapshots else Repository.releases
  def publishTo(isSnapshot: Boolean) = repo(isSnapshot) + publishSuffix

  def userCredentials = (Path.userHome / ".ivy2" / "credentials" ** "*").filter(_.isFile).get.map(Credentials(_))
}

object ScalaJSON {
  val genDocsTask = TaskKey[Unit]("gen-docs")

  val baseSettings = Seq(
    scalaVersion := "2.11.6",
    organization := "com.mediamath",
    organizationName := "MediaMath, Inc",
    organizationHomepage := Some(url("http://www.mediamath.com")),
    crossScalaVersions := Seq(scalaVersion.value, "2.10.5")
  )

  val commonSettings = baseSettings ++ Seq(
    name := "scala-json",
    credentials ++= Repository.userCredentials,
    crossPaths := true,
    publishTo := Some("publish" at Repository.publishTo(isSnapshot.value)),
    publishArtifact in Test := false,

    scalacOptions ++= Seq("-deprecation", "-language:_", "-unchecked", "-Xlint",
      "-Xlog-free-terms", "-target:jvm-1.7", "-encoding", "UTF-8"),

    libraryDependencies <++= scalaVersion { x =>
      Seq(
        "org.scala-lang" % "scala-reflect" % x,
        "org.scala-lang" % "scala-compiler" % x
      )
    },

    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "utest" % "0.3.0" % "test"
    ),

    testFrameworks += new TestFramework("utest.runner.Framework")
  )

  val jvmSettings = Seq(
    libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.1"
  )

  def settings(jsonJS: Project, jsonJVM: Project) = baseSettings ++ tut.Plugin.tutSettings ++ Seq(
    publish := {},
    publishLocal := {},

    genDocsTask <<= (tut.Plugin.tut, baseDirectory, version) map { (outFiles, baseDir, ver) =>
      for((outFile, _) <- outFiles) {
        val out = readFile(outFile).replaceAllLiterally("__VER__", ver)

        writeFile(baseDir / outFile.getName, out)
      }
    },

    (sbt.Keys.`package` in Compile) <<= (sbt.Keys.`package` in Compile).dependsOn(genDocsTask),
    publish <<= publish.dependsOn(genDocsTask),

    (test in Test) <<= (test in Test).dependsOn(tut.Plugin.tut, fastOptJS in jsonJS in Test),

    (unmanagedClasspath in Compile) <<= (fullClasspath in Compile in jsonJVM)
  )

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def writeFile(f: File, content: String) = printToFile(f)(_.println(content))
  def readFile(f: File): String = scala.io.Source.fromFile(f).mkString
}
