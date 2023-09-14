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
      "io.methvin"            % "directory-watcher" % "0.18.0",
      "com.github.pathikrit" %% "better-files"      % "3.9.2",
      "org.specs2"           %% "specs2-core"       % "4.20.2" % Test
    ),
    Test / parallelExecution := false,
    mimaPreviousArtifacts := Set(
      organization.value %% name.value % previousStableVersion.value
        .getOrElse(throw new Error("Unable to determine previous version"))
    ),
    mimaBinaryIssueFilters ++= Seq(
    )
  )

addCommandAlias(
  "validateCode",
  List(
    "scalafmtSbtCheck",
    "scalafmtCheckAll",
  ).mkString(";")
)
