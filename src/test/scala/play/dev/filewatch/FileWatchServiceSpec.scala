/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.dev.filewatch

import better.files._
import org.specs2.mutable.Specification

import java.util.function.Supplier
import scala.concurrent.duration._
import scala.jdk.FunctionConverters._
import scala.jdk.CollectionConverters._

class JavaFileWatchServiceSpec extends FileWatchServiceSpec {
  // the mac impl consistently fails because it takes more than 5s so skip this one on mac
  args(skipAll = FileWatchService.OS.getCurrent == FileWatchService.OS.Mac)

  override def watchService: FileWatchService =
    FileWatchService.jdk7(FileWatchServiceSpecLoggerProxy, false)
}

class JavaFileWatchServiceHashCheckDisabledSpec extends FileWatchServiceSpec {
  // the mac impl consistently fails because it takes more than 5s so skip this one on mac
  args(skipAll = FileWatchService.OS.getCurrent == FileWatchService.OS.Mac)

  override def watchService: FileWatchService =
    FileWatchService.jdk7(FileWatchServiceSpecLoggerProxy, true)
}

class MacFileWatchServiceSpec extends FileWatchServiceSpec {
  // this only works on mac
  args(skipAll = FileWatchService.OS.getCurrent != FileWatchService.OS.Mac)

  override def watchService: FileWatchService =
    FileWatchService.mac(FileWatchServiceSpecLoggerProxy, false)
}

class MacFileWatchServiceHashCheckDisabledSpec extends FileWatchServiceSpec {
  // this only works on mac
  args(skipAll = FileWatchService.OS.getCurrent != FileWatchService.OS.Mac)

  override def watchService: FileWatchService =
    FileWatchService.mac(FileWatchServiceSpecLoggerProxy, true)
}

class PollingFileWatchServiceSpec extends FileWatchServiceSpec {
  override def watchService: FileWatchService = FileWatchService.polling(200)
}

object FileWatchServiceSpecLoggerProxy extends LoggerProxy {
  override def verbose(message: Supplier[String]): Unit = ()
  override def debug(message: Supplier[String]): Unit   = ()
  override def info(message: Supplier[String]): Unit    = ()
  override def warn(message: Supplier[String]): Unit    = ()
  override def error(message: Supplier[String]): Unit   = ()
  override def trace(t: Supplier[Throwable]): Unit      = ()
  override def success(message: Supplier[String]): Unit = ()
}

abstract class FileWatchServiceSpec extends Specification {

  sequential

  def watchService: FileWatchService

  private def withTempDir[T](block: File => T): T = {
    val baseDir = File.newTemporaryDirectory("file-watch-service")
    try {
      block(baseDir)
    } finally {
      baseDir.delete()
    }
  }

  @volatile var changed: Boolean = false
  @volatile var startTime: Long  = 0
  @volatile var endTime: Long    = 0

  private def reset(): Unit = {
    Thread.sleep(200)
    changed = false
    startTime = System.nanoTime
  }

  private def reportChange(): Void = {
    endTime = System.nanoTime
    changed = true
    null
  }

  private def assertChanged() = {
    val deadline = 5.seconds.fromNow
    while (!changed) {
      if (deadline.isOverdue()) {
        failure("Changed did not become true within 5 seconds")
      }
      Thread.sleep(200)
    }
    printf("%s: %7.1f ms\n", getClass.getSimpleName, (endTime - startTime).nanoseconds.toUnit(MILLISECONDS))
    ok
  }

  private def watchFiles[T](files: File*)(block: => T): T = {
    val watcher = watchService.watch(files.map(_.toJava).asJava, (() => reportChange()).asJava)
    reset()
    try {
      block
    } finally {
      watcher.stop()
    }
  }

  "The file watch service" should {

    "detect new files" in withTempDir { dir =>
      watchFiles(dir) {
        (dir / "test").write("new file")
        assertChanged()
      }
    }

    "detect changes on files" in withTempDir { dir =>
      val testFile = dir / "test"
      testFile.write("new file")
      watchFiles(dir) {
        testFile.write("changed")
        assertChanged()
      }
    }

    "detect changes on files in new subdirectories" in withTempDir { dir =>
      val subDir = dir.createChild("subdir", asDirectory = true)
      watchFiles(dir) {
        (subDir / "test").write("new file")
        assertChanged()
      }
    }
  }

}
