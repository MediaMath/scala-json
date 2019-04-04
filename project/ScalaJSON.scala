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

import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

object Repository {
  val host = "artifactory.mediamath.com"
  val artifactory = s"https://$host/artifactory/"
  val snapshots = s"${artifactory}libs-snapshot"
  val releases = s"${artifactory}libs-release"
  val remote = s"${artifactory}remote-repos"
  val pluginRelease = s"${artifactory}sbt-plugins-release-local"

  val snapshotRepo = "snapshots" at Repository.snapshots
  val releaseRepo = "releases" at Repository.releases

  val publishSuffix = "-global"

  def repo(isSnapshot: Boolean) = if (isSnapshot) Repository.snapshots else Repository.releases
  def publishTo(isSnapshot: Boolean) = repo(isSnapshot) + publishSuffix

  def userCredentials = (Path.userHome / ".ivy2" / "credentials" ** "*").filter(_.isFile).get.map(Credentials(_))
}

object ScalaJSON {
  val genDocsTask = TaskKey[Seq[File]]("gen-docs")
  val genDocsTaskReal = TaskKey[Seq[File]]("gen-docs-real")
  val genDocsTaskNil = TaskKey[Seq[File]]("gen-docs-nil")

  val targetScalaVer = "2.11.8"

  val baseSettings = Seq(
    scalaVersion := targetScalaVer,
    organization := "com.mediamath",
    organizationName := "MediaMath, Inc",
    organizationHomepage := Some(url("http://www.mediamath.com")),
    crossPaths := true,
    crossScalaVersions := Seq("2.10.6", targetScalaVer, "2.12.0-M3"),
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )

  val commonSettings = baseSettings ++ Seq(
    name := "scala-json",
    credentials ++= Repository.userCredentials,
    publishTo := Some("publish" at Repository.publishTo(isSnapshot.value)),
    publishArtifact in Test := false,

    scalacOptions in (Compile, doc) ++= Seq("-skip-packages", "json.internal:json.shadow"),

    scalacOptions ++= Seq("-deprecation", "-language:_", "-unchecked", "-Xlint",
      "-Xlog-free-terms", "-encoding", "UTF-8"),

    scalacOptions ++= Seq(scalaVersion.value match {
      case x if x.startsWith("2.12.") => "-target:jvm-1.8"
      case x => "-target:jvm-1.6"
    }),

    javacOptions <++= scalaVersion map {
      case x if x.startsWith("2.12.") => Nil
      case x => Seq("-source", "1.6", "-target", "1.6")
    },

    libraryDependencies <++= scalaVersion { x =>
      Seq(
        "org.scala-lang" % "scala-reflect" % x,
        "org.scala-lang" % "scala-compiler" % x
      )
    },

    pomExtra := {
      <url>https://github.com/MediaMath/scala-json</url>
      <licenses>
        <license>
          <name>Apache 2.0</name>
          <url>https://github.com/MediaMath/scala-json/blob/master/LICENSE</url>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:MediaMath/scala-json.git</url>
        <connection>scm:git@github.com:MediaMath/scala-json.git</connection>
      </scm>
      <developers>
        <developer>
          <id>MediaMath</id>
          <name>MediaMath, Inc</name>
          <url>https://github.com/MediaMath</url>
        </developer>
      </developers>
    },

    //temporary resolver for colinrgodsey fork of utest@0.3.1 for 2.12.0-M3 support
    resolvers += "mvn repo" at "https://raw.githubusercontent.com/colinrgodsey/maven/master",

    testFrameworks += new TestFramework("utest.runner.Framework")
  )

  val jvmSettings = Seq(
    libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.5",
    libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.5",
    fork in Test := true
  )

  val jsSettings = Seq(
    target in Compile in doc := baseDirectory.value / ".." / "doc",

    postLinkJSEnv := NodeJSEnv().value,

    scalaJSStage in Global := FastOptStage
  )

  def tutSettings(jsonJVM: Project): Seq[Setting[_]] = baseSettings ++ tut.Plugin.tutSettings ++ Seq(
    publish := {},
    publishLocal := {},
    test in Test := {},
    genDocsTaskNil := Nil,

    name := "tut",

    //lots of filtering here to only make tut run on 2.11
    libraryDependencies ~= (_.filter(_.name != "tut-core")),

    libraryDependencies <++= scalaVersion {
      case x if x.startsWith("2.11.") => Seq("org.tpolecat" %% "tut-core" % "0.3.2")
      case x => Nil
    },

    genDocsTaskReal <<= (tut.Plugin.tut, baseDirectory, version) map { (outFiles, baseDir, ver) =>
      for((outFile, _) <- outFiles.toSeq) yield {
        val out = readFile(outFile).replaceAllLiterally("__VER__", ver)

        val outPath = outFile.getName.toLowerCase match {
          case "readme.md" => baseDir / ".." / outFile.getName
          case _ => baseDir / ".." / "docs" / outFile.getName
        }

        writeFile(outPath, out)

        outPath
      }
    },

    //only do gendocs (tut) for 2.11
    genDocsTask <<= (scalaVersion, genDocsTaskReal, genDocsTaskNil) {
      case (scalaV, genTask, _) if scalaV.startsWith("2.11.") => genTask
      case (_, _, genTaskNil) => genTaskNil
    },

    publish <<= publish.dependsOn(genDocsTask, doc in Compile),
    publishLocal <<= publishLocal.dependsOn(genDocsTask, doc in Compile),

    sbt.Keys.`package` in Compile <<= (sbt.Keys.`package` in Compile).dependsOn(genDocsTask),

    test in Test <<= (test in Test).dependsOn(genDocsTask),

    doc in Compile <<= (doc in Compile).dependsOn(genDocsTask)
  )

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def writeFile(f: File, content: String) = printToFile(f)(_.println(content))
  def readFile(f: File): String = scala.io.Source.fromFile(f).mkString
}
