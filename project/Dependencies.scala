/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._

object Dependencies {
  // Sync versions in .travis.yml
  val Scala212 = "2.12.10"
  val Scala213 = "2.13.1"

  val ScalaVersions = Seq(Scala212, Scala213)
}
