import com.typesafe.tools.mima.core.DirectMissingMethodProblem

import com.typesafe.tools.mima.core.MissingClassProblem
import com.typesafe.tools.mima.core.ProblemFilters
import com.typesafe.tools.mima.plugin.MimaKeys.mimaBinaryIssueFilters

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
ThisBuild / dynverVTagPrefix := false

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}

lazy val `play-file-watch` = project
  .in(file("."))
  .enablePlugins(Common)
  .settings(
    // workaround for https://github.com/scala/scala-dev/issues/249
    Compile / doc / scalacOptions ++= (
      if (scalaBinaryVersion.value == "2.12")
        Seq("-no-java-comments", "-Ywarn-unused:imports", "-Xlint:nullary-unit")
      else
        Nil
    ),
    libraryDependencies ++= Seq(
      "io.methvin"             % "directory-watcher" % "0.15.1",
      ("com.github.pathikrit" %% "better-files" % pickVersion(
        scalaBinaryVersion.value,
        default = "3.9.1",
        forScala210 = "2.17.0"
      )).cross(CrossVersion.for3Use2_13),
      scalaBinaryVersion.value match {
        case "2.10" =>
          "org.specs2" %% "specs2-core" % "3.10.0" % Test
        case "2.11" =>
          "org.specs2" %% "specs2-core" % "4.10.6" % Test
        case _ =>
          "org.specs2" %% "specs2-core" % "4.16.1" % Test
      }
    ),
    Test / parallelExecution := false,
    mimaPreviousArtifacts := {
      if (scalaBinaryVersion.value == "2.10" || scalaBinaryVersion.value == "3")
        Set.empty
      else
        Set(
          // TODO: uncomment after 1.2.0 final release:
          // organization.value %% name.value % previousStableVersion.value.getOrElse(throw new Error("Unable to determine previous version"))
        )
    },
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

addCommandAlias(
  "validateCode",
  List(
    "scalafmtSbtCheck",
    "scalafmtCheckAll",
  ).mkString(";")
)
