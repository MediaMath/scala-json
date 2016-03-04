version in ThisBuild := "1.0-SNAPSHOT"

lazy val json = crossProject.in(file("."))
    .settings(ScalaJSON.commonSettings: _*)
    .jvmSettings(ScalaJSON.jvmSettings: _*)
    .jsSettings(ScalaJSON.jsSettings: _*)

lazy val jsonJVM = json.jvm
lazy val jsonJS = json.js

lazy val tutProject = project.in(file("docs"))
    .settings(ScalaJSON.tutSettings(jsonJVM): _*)
    .dependsOn(jsonJVM)

ScalaJSON.baseSettings

publish := {}
publishLocal := {}

publish <<= publish.dependsOn(doc in Compile)
publishLocal <<= publishLocal.dependsOn(doc in Compile)

