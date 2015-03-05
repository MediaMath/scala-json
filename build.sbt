version in ThisBuild := "0.1-SNPASHOT"

lazy val json = crossProject.in(file("."))
    .settings(ScalaJSON.commonSettings: _*)
    .jvmSettings(ScalaJSON.jvmSettings: _*)
    .jsSettings()

lazy val jsonJVM = json.jvm
lazy val jsonJS = json.js

ScalaJSON.settings(jsonJS = jsonJS, jsonJVM = jsonJVM)
