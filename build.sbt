ScalaJSON.settings

lazy val jsonJs = Project("scala-json", file("scalajs"))
    .enablePlugins(ScalaJSPlugin)
    .settings(ScalaJSON.jsSettings: _*)

lazy val json = project.aggregate(jsonJs).settings(ScalaJSON.settings: _*).aggregate(jsonJs)
