version in ThisBuild := "0.2-SNAPSHOT"

lazy val json = crossProject.in(file("."))
    .settings(ScalaJSON.commonSettings: _*)
    .jvmSettings(ScalaJSON.jvmSettings: _*)
    .jsSettings(ScalaJSON.jsSettings: _*)

lazy val jsonJVM = json.jvm
lazy val jsonJS = json.js

ScalaJSON.settings(jsonJS = jsonJS, jsonJVM = jsonJVM)
