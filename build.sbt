import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

version in ThisBuild := "1.1"

lazy val json = crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Full)
    .settings(ScalaJSON.commonSettings: _*)
    .settings(libraryDependencies += "com.lihaoyi" %%% "utest" % "0.6.6" % "test")
    .jvmSettings(ScalaJSON.jvmSettings: _*)
    .jsSettings(ScalaJSON.jsSettings: _*)
    .nativeSettings(nativeLinkStubs := true)
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

