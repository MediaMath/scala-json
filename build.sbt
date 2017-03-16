//shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.{crossProject, CrossType}

version in ThisBuild := "1.0"

lazy val json = crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Full)
    .settings(ScalaJSON.commonSettings: _*)
    .jvmSettings(ScalaJSON.jvmSettings: _*)
    .jsSettings(ScalaJSON.jsSettings: _*)
    .nativeSettings(Nil)
    .in(file("."))

lazy val jsonJVM = json.jvm
lazy val jsonJS = json.js
lazy val jsonNative = json.native

lazy val tutProject = project.in(file("docs"))
    .settings(ScalaJSON.tutSettings(jsonJVM): _*)
    .dependsOn(jsonJVM)

ScalaJSON.baseSettings

publish := {}
publishLocal := {}

publish <<= publish.dependsOn(doc in Compile)
publishLocal <<= publishLocal.dependsOn(doc in Compile)

