import ScalaJSON._

version in ThisBuild := "0.1-SNAPSHOT"

crossScalaVersions in ThisBuild := Seq("2.11.4")//, "2.10.4")

lazy val json = crossProject.in(file("."))
    .settings(commonSettings: _*)
    .jvmSettings(jvmSettings: _*)
    .jsSettings()

lazy val jsonJVM = json.jvm
lazy val jsonJS = json.js


publish := {}

publishLocal := {}

