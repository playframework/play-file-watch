lazy val `play-file-watch` = project
  .in(file("."))
  .enablePlugins(PlayLibrary)
  .settings(scalariformSettings: _*)
  .settings(interplayOverrideSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.10.5", "2.11.7"),
    libraryDependencies ++= Seq(
      "org.scala-sbt" %% "io" % "1.0.0-M6",
      "org.specs2" %% "specs2-core" % "3.6.6" % Test
    )
  )

playBuildRepoName in ThisBuild := "play-file-watch"

def interplayOverrideSettings: Seq[Setting[_]] = Seq(
  organization := "com.lightbend.play",
  sonatypeProfileName := "com.lightbend"
)