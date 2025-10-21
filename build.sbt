// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

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

val previousVersion: Option[String] = Some("2.0.0")

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
      "io.methvin"            % "directory-watcher" % "0.19.1",
      "com.github.pathikrit" %% "better-files"      % "3.9.2",
      "org.specs2"           %% "specs2-core"       % "4.23.0" % Test
    ),
    Test / parallelExecution := false,
    mimaPreviousArtifacts    := previousVersion.map(organization.value %% moduleName.value % _).toSet,
    mimaBinaryIssueFilters ++= Seq(
    ),
  )

addCommandAlias(
  "validateCode",
  List(
    "headerCheckAll",
    "scalafmtSbtCheck",
    "scalafmtCheckAll",
  ).mkString(";")
)
