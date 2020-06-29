import Dependencies._
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object Common extends AutoPlugin {

  override def trigger = noTrigger

  override def requires = JvmPlugin

  val repoName = "play-file-watch"

  val javacParameters = Seq(
    "-source",
    "1.8",
    "-target",
    "1.8",
    "-Xlint:deprecation",
    "-Xlint:unchecked"
  )

  val scalacParameters = Seq(
    "-target:jvm-1.8",
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
      organization := "com.lightbend.play",
      organizationName := "Lightbend Inc.",
      organizationHomepage := Some(url("https://www.lightbend.com/")),
      homepage := Some(url(s"https://github.com/playframework/${repoName}")),
      licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
      scalaVersion := Scala212,
      crossScalaVersions := ScalaVersions,
      scalacOptions ++= scalacParameters,
      javacOptions ++= javacParameters,
      scmInfo := Some(
        ScmInfo(
          url(s"https://github.com/playframework/${repoName}"),
          s"scm:git:git@github.com:playframework/${repoName}.git"
        )
      ),
      developers += Developer(
        "contributors",
        "Contributors",
        "https://gitter.im/playframework/contributors",
        url("https://github.com/playframework")
      ),
      description := "Play File Watch Library. Watch files in a platform independent way."
    )

}
