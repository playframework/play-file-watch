// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

import com.typesafe.tools.mima.core.DirectMissingMethodProblem
import com.typesafe.tools.mima.core.InaccessibleClassProblem
import com.typesafe.tools.mima.core.IncompatibleMethTypeProblem
import com.typesafe.tools.mima.core.MissingClassProblem
import com.typesafe.tools.mima.core.ProblemFilters
import com.typesafe.tools.mima.core.ReversedMissingMethodProblem
import com.typesafe.tools.mima.plugin.MimaKeys.mimaBinaryIssueFilters

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
ThisBuild / dynverVTagPrefix := false

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}

val previousVersion: Option[String] = Some("2.0.0")

lazy val `play-file-watch` = project
  .in(file("."))
  .enablePlugins(Common)
  .settings(
    libraryDependencies ++= Seq(
      "io.methvin"            % "directory-watcher" % "0.19.0",
      "com.github.pathikrit" %% "better-files"      % "3.9.2"  % Test,
      "org.specs2"           %% "specs2-core"       % "4.20.9" % Test
    ),
    Test / parallelExecution := false,
    mimaPreviousArtifacts    := previousVersion.map(organization.value %% moduleName.value % _).toSet,
    mimaBinaryIssueFilters ++= Seq(
      // Remove unused GlobalStaticVar
      ProblemFilters.exclude[MissingClassProblem]("play.dev.filewatch.GlobalStaticVar"),
      ProblemFilters.exclude[MissingClassProblem]("play.dev.filewatch.GlobalStaticVar$"),
      // Migrate to pure Java implementation
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.dev.filewatch.FileWatchService.*"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.dev.filewatch.FileWatchService.*"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.dev.filewatch.FileWatchService.watch"),
      ProblemFilters.exclude[MissingClassProblem]("play.dev.filewatch.FileWatchService*"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.dev.filewatch.LoggerProxy.*"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("play.dev.filewatch.LoggerProxy.*"),
      ProblemFilters.exclude[MissingClassProblem]("play.dev.filewatch.OptionalFileWatchServiceDelegate"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.dev.filewatch.PollingFileWatchService.pollDelayMillis"),
      ProblemFilters.exclude[MissingClassProblem]("play.dev.filewatch.SourceModificationWatch$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("play.dev.filewatch.WatchState.*"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.dev.filewatch.DefaultFileWatchService.watch"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.dev.filewatch.PollingFileWatchService.watch"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.dev.filewatch.SourceModificationWatch.watch"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("play.dev.filewatch.WatchState.this"),
      ProblemFilters.exclude[MissingClassProblem]("play.dev.filewatch.WatchState$"),
    ),
  )

addCommandAlias(
  "validateCode",
  List(
    "headerCheckAll",
    "scalafmtSbtCheck",
    "scalafmtCheckAll",
    "javafmtCheckAll"
  ).mkString(";")
)
