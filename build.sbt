lazy val `play-file-watch` = project
  .in(file("."))
  .enablePlugins(PlayLibrary, PlayReleaseBase)
  .settings(scalariformSettings: _*)
  .settings(interplayOverrideSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.1"),
    libraryDependencies ++= Seq(
      "com.lightbend.play" % "jnotify" % "0.94-play-2",
      // Using this rather than sbt-io because it also needs to run on maven, and due to various circumstantial reasons,
      // sbt-io is not quite ready to be used as a stand alone library in something that needs to support both sbt 0.13
      // and maven. These reasons include sbt-io 0.13.x isn't published to maven central, and sbt-io 1.0.x may not
      // support Scala 2.10.
      betterFiles(scalaVersion.value),
      "org.specs2" %% "specs2-core" % "3.8.6" % Test
    )
  )

def betterFiles(scalaVersion: String): ModuleID = {
  val version = scalaVersion.split('.')(1) match {
    case "10" => "2.17.0"
    case "11" | "12" => "2.17.1"
  }
  "com.github.pathikrit" %% "better-files" % version
}

playBuildRepoName in ThisBuild := "play-file-watch"

def interplayOverrideSettings: Seq[Setting[_]] = Seq(
  organization := "com.lightbend.play",
  sonatypeProfileName := "com.lightbend"
)
