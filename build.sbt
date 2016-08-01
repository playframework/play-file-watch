lazy val `play-file-watch` = project
  .in(file("."))
  .enablePlugins(PlayLibrary)
  .settings(scalariformSettings: _*)
  .settings(interplayOverrideSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.10.6", "2.11.8"),
    libraryDependencies ++= Seq(
      "com.lightbend.play" % "jnotify" % "0.94-play-2",
      // Using this rather than sbt-io because it also needs to run on maven, and due to various circumstantial reasons,
      // sbt-io is not quite ready to be used as a stand alone library in something that needs to support both sbt 0.13
      // and maven. These reasons include sbt-io 0.13.x isn't published to maven central, and sbt-io 1.0.x may not
      // support Scala 2.10.
      "com.github.pathikrit" %% "better-files" % "2.14.0",
      "org.specs2" %% "specs2-core" % "3.6.6" % Test
    )
  )

playBuildRepoName in ThisBuild := "play-file-watch"

def interplayOverrideSettings: Seq[Setting[_]] = Seq(
  organization := "com.lightbend.play",
  sonatypeProfileName := "com.lightbend"
)
