import sbt.Keys._
import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.dsl._

object Repository {
  val host = "artifactory.mediamath.com"
  val artifactory = s"https://$host/artifactory/"
  val snapshots = s"${artifactory}libs-snapshot"
  val releases = s"${artifactory}libs-release"
  val remote = s"${artifactory}remote-repos"
  val pluginSnapshots = s"${artifactory}sbt-plugins-snapshot-local"
  val pluginRelease = s"${artifactory}sbt-plugins-release-local"

  val snapshotRepo = "snapshots" at Repository.snapshots
  val releaseRepo = "releases" at Repository.releases
  val remoteRepo = "remote" at Repository.remote

  def repo(isSnapshot: Boolean) = if (isSnapshot) Repository.snapshots else Repository.releases
  def globalPublishTo(isSnapshot: Boolean) = repo(isSnapshot) + "-global"
  def userCredentials = (Path.userHome / ".ivy2" / "credentials" ** "*").filter(_.isFile).get.map(Credentials(_))
}

object ScalaJSON {
  val genDocsTask = TaskKey[Unit]("gen-docs")

  val commonSettings = Seq(
    name := "scala-json",
    scalaVersion := "2.11.5",
    organization := "com.mediamath",
    organizationName := "MediaMath, Inc",
    organizationHomepage := Some(url("http://www.mediamath.com")),
    credentials ++= Repository.userCredentials,
    crossPaths := true,
    publishTo := Some("publish" at Repository.globalPublishTo(isSnapshot.value)),
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

  val jvmSettings = tut.Plugin.tutSettings ++ Seq(
    libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.1",

    genDocsTask <<= (tut.Plugin.tut, baseDirectory in ThisBuild) map { (outFiles, baseDir) =>
      for((outFile, _) <- outFiles) outFile.renameTo(baseDir / outFile.getName)
    },

    tut.Plugin.tutSourceDirectory := (baseDirectory in ThisBuild).value / "src" / "main" / "tut",

    (sbt.Keys.`package` in Compile) <<= (sbt.Keys.`package` in Compile).dependsOn(genDocsTask)
  )

  val settings = tut.Plugin.tutSettings ++ Seq(
    publish := {},
    publishLocal := {},
    crossScalaVersions in ThisBuild := Seq("2.11.4")//, "2.10.4")
  )
}
