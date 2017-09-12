lazy val `play-file-watch` = project
  .in(file("."))
  .enablePlugins(PlayLibrary, PlayReleaseBase)
  .settings(scalariformSettings: _*)
  .settings(interplayOverrideSettings: _*)
  .settings(
    // workaround for https://github.com/scala/scala-dev/issues/249
    scalacOptions in (Compile, doc) ++= (if (scalaBinaryVersion.value == "2.12") Seq("-no-java-comments") else Nil),

    crossScalaVersions := Seq("2.10.6", "2.11.11", "2.12.3"),
    libraryDependencies ++= Seq(
      "io.methvin" % "directory-watcher" % "0.1.1",
      betterFiles(scalaBinaryVersion.value),
      "org.specs2" %% "specs2-core" % "3.8.6" % Test,

      // jnotify dependency needs to be added explicitly in user's apps
      "com.lightbend.play" % "jnotify" % "0.94-play-2" % Test
    ),
    parallelExecution in Test := false
  )

def betterFiles(scalaBinaryVersion: String): ModuleID = {
  val version = scalaBinaryVersion match {
    case "2.10" => "2.17.0"
    case "2.11" | "2.12" => "2.17.1"
  }
  "com.github.pathikrit" %% "better-files" % version
}

playBuildRepoName in ThisBuild := "play-file-watch"

def interplayOverrideSettings: Seq[Setting[_]] = Seq(
  organization := "com.lightbend.play",
  sonatypeProfileName := "com.lightbend"
)
