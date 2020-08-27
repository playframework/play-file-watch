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
    libraryDependencies ++= Seq(
      "io.methvin"            % "directory-watcher" % "0.10.0",
      "com.github.pathikrit" %% "better-files"      % "3.9.1",
      "org.specs2"           %% "specs2-core"       % "4.10.3" % Test,
      // jnotify dependency needs to be added explicitly in user's apps
      "com.lightbend.play" % "jnotify" % "0.94-play-2" % Test
    ),
    parallelExecution in Test := false,
    mimaPreviousArtifacts := Set(
      organization.value %% name.value % "1.1.12"
      // this didn't pick the 1.1.12 tag:
      // previousStableVersion.value.getOrElse(throw new Error("Unable to determine previous version"))
    )
  )
