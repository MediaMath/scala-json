ScalaJSON.settings

lazy val jsonJs = Project("scala-json-js", file("scalajs"))
    .enablePlugins(ScalaJSPlugin)
    .settings(ScalaJSON.jsSettings: _*)

lazy val root = Project("scala-json", file(".")).settings(ScalaJSON.settings: _*).aggregate(jsonJs)

version in ThisBuild := "0.1-RC1"

crossScalaVersions in ThisBuild := Seq("2.11.4")//, "2.10.4")
