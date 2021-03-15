import com.typesafe.tools.mima.core.DirectMissingMethodProblem
import com.typesafe.tools.mima.core.MissingClassProblem
import com.typesafe.tools.mima.core.ProblemFilters
import com.typesafe.tools.mima.plugin.MimaKeys.mimaBinaryIssueFilters

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
dynverVTagPrefix in ThisBuild := false

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  val v = version.value
  if (dynverGitDescribeOutput.value.hasNoTags)
    throw new MessageOnlyException(
      s"Failed to derive version from git tags. Maybe run `git fetch --unshallow`? Version: $v"
    )
  s
}

lazy val `play-file-watch` = project
  .in(file("."))
  .enablePlugins(Common, Publish)
  .settings(
    // workaround for https://github.com/scala/scala-dev/issues/249
    scalacOptions in (Compile, doc) ++= (
      if (scalaBinaryVersion.value == "2.12")
        Seq("-no-java-comments", "-Ywarn-unused:imports", "-Xlint:nullary-unit")
      else
        Nil
    ),
    libraryDependencies ++= Seq(
      "io.methvin"            % "directory-watcher" % "0.15.0",
      "com.github.pathikrit" %% "better-files" % pickVersion(
        scalaBinaryVersion.value,
        default = "3.8.0",
        forScala210 = "2.17.0"
      ),
      "org.specs2" %% "specs2-core" % pickVersion(
        scalaBinaryVersion.value,
        default = "4.8.3",
        forScala210 = "3.10.0"
      ) % Test
    ),
    parallelExecution in Test := false,
    mimaPreviousArtifacts := Set(
      organization.value %% name.value % "1.1.13"
      // this didn't pick the 1.1.12 tag:
      // previousStableVersion.value.getOrElse(throw new Error("Unable to determine previous version"))
    ),
    mimaBinaryIssueFilters ++= Seq(
      // Remove JNotify
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.dev.filewatch.FileWatchService.jnotify"),
      ProblemFilters.exclude[MissingClassProblem]("play.dev.filewatch.JNotifyFileWatchService"),
      ProblemFilters.exclude[MissingClassProblem]("play.dev.filewatch.JNotifyFileWatchService$"),
      ProblemFilters.exclude[MissingClassProblem]("play.dev.filewatch.JNotifyFileWatchService$JNotifyDelegate"),
    )
  )

def pickVersion(scalaBinaryVersion: String, default: String, forScala210: String): String = scalaBinaryVersion match {
  case "2.10" => forScala210
  case _      => default
}
