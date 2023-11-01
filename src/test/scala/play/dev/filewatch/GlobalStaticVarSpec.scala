/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.dev.filewatch

package play.runsupport

import org.specs2.mutable.Specification

class GlobalStaticVarSpec extends Specification {

  "global static var" should {
    "support setting and getting" in {
      try {
        GlobalStaticVar.set("message", "hello world")
        GlobalStaticVar.get[String]("message") must beSome("hello world")
      } finally {
        GlobalStaticVar.remove("message")
      }
    }
    "return none when not set" in {
      GlobalStaticVar.get[String]("notset") must beNone
    }
    "support removing" in {
      try {
        GlobalStaticVar.set("msg", "hello world")
        GlobalStaticVar.get[String]("msg") must beSome("hello world")
        GlobalStaticVar.remove("msg")
        GlobalStaticVar.get[String]("msg") must beNone
      } finally {
        GlobalStaticVar.remove("msg")
      }
    }
    "support complex types like classloaders" in {
      try {
        val classLoader = this.getClass.getClassLoader
        GlobalStaticVar.set("classLoader", classLoader)
        GlobalStaticVar.get[ClassLoader]("classLoader") must beSome(classLoader)
      } finally {
        GlobalStaticVar.remove("classLoader")
      }
    }
  }
}
