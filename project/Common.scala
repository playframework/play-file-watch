/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import Dependencies.*
import sbt.Keys.*
import sbt.Def
import sbt.*
import sbt.plugins.JvmPlugin
import sbtheader.HeaderPlugin.autoImport.HeaderPattern.commentBetween
import sbtheader.CommentStyle
import sbtheader.FileType
import sbtheader.HeaderPlugin
import sbtheader.LineCommentCreator

object Common extends AutoPlugin {

  import HeaderPlugin.autoImport._

  override def trigger = noTrigger

  override def requires = JvmPlugin && HeaderPlugin

  val repoName = "play-file-watch"

  val javacParameters = Seq(
    "--release",
    "11",
    "-Xlint:deprecation",
    "-Xlint:unchecked"
  )

  val scalacParameters = Seq(
    "-release",
    "11",
    "-encoding",
    "utf8",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Ywarn-unused:imports",
    "-Xlint:nullary-unit",
    "-Ywarn-dead-code"
  )

  override def globalSettings =
    Seq(
      organization         := "org.playframework",
      organizationName     := "The Play Framework Project",
      organizationHomepage := Some(url("https://playframework.com/")),
      homepage             := Some(url(s"https://github.com/playframework/${repoName}")),
      licenses             := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
      scalaVersion         := Scala212,
      crossScalaVersions   := ScalaVersions,
      scalacOptions ++= scalacParameters,
      javacOptions ++= javacParameters,
      scmInfo := Some(
        ScmInfo(
          url(s"https://github.com/playframework/${repoName}"),
          s"scm:git:git@github.com:playframework/${repoName}.git"
        )
      ),
      developers += Developer(
        "playframework",
        "The Play Framework Contributors",
        "contact@playframework.com",
        url("https://github.com/playframework")
      ),
      description := "Play File Watch Library. Watch files in a platform independent way."
    )

  override def projectSettings = Seq(
    headerLicense := Some(
      HeaderLicense.Custom(
        "Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>"
      )
    ),
    headerMappings ++= Map(
      FileType("sbt")        -> HeaderCommentStyle.cppStyleLineComment,
      FileType("properties") -> HeaderCommentStyle.hashLineComment,
      FileType("md") -> CommentStyle(new LineCommentCreator("<!---", "-->"), commentBetween("<!---", "*", "-->"))
    ),
    (Compile / headerSources) ++=
      ((baseDirectory.value ** ("*.properties" || "*.md" || "*.sbt"))
        --- (baseDirectory.value ** "target" ** "*")).get ++
        (baseDirectory.value / "project" ** "*.scala" --- (baseDirectory.value ** "target" ** "*")).get
  )

}
