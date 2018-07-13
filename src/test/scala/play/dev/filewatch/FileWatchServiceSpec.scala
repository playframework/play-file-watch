package play.dev.filewatch

import better.files._
import org.specs2.mutable.Specification

import scala.concurrent.duration._

class JavaFileWatchServiceSpec extends FileWatchServiceSpec {
  // the mac impl consistently fails because it takes more than 5s so skip this one on mac
  args(skipAll = FileWatchService.os == FileWatchService.OS.Mac)

  override def watchService: FileWatchService = FileWatchService.jdk7(FileWatchServiceSpecLoggerProxy)
}

class PollingFileWatchServiceSpec extends FileWatchServiceSpec {
  // Underlying this uses JDK file watch, which fails for Mac just like the test above.
  args(skipAll = FileWatchService.os == FileWatchService.OS.Mac)
  override def watchService: FileWatchService = FileWatchService.polling(100)
}

class MacFileWatchServiceSpec extends FileWatchServiceSpec {
  // this only works on mac
  args(skipAll = FileWatchService.os != FileWatchService.OS.Mac)

  override def watchService: FileWatchService = FileWatchService.mac(FileWatchServiceSpecLoggerProxy)
}

class JNotifyFileWatchServiceSpec extends FileWatchServiceSpec {
  private val jnotifyDir = File("./target/jnotify").createIfNotExists(asDirectory = true)

  override def watchService: FileWatchService = FileWatchService.jnotify(jnotifyDir.toJava)
}

object FileWatchServiceSpecLoggerProxy extends LoggerProxy {
  override def verbose(message: => String): Unit = ()
  override def debug(message: => String): Unit = ()
  override def info(message: => String): Unit = ()
  override def warn(message: => String): Unit = ()
  override def error(message: => String): Unit = ()
  override def trace(t: => Throwable): Unit = ()
  override def success(message: => String): Unit = ()
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

  val defaultSleepTimeInMillis: Long = 500
  val defaultDeadlineDuration: FiniteDuration = 5.seconds

  @volatile var changed: Boolean = false
  @volatile var startTime: Long = 0
  @volatile var endTime: Long = 0

  private def reset(): Unit = {
    Thread.sleep(defaultSleepTimeInMillis)
    changed = false
    startTime = System.nanoTime
  }

  private def reportChange(): Unit = {
    endTime = System.nanoTime
    changed = true
  }

  private def assertChanged() = {
    val deadline = defaultDeadlineDuration.fromNow
    while (!changed) {
      if (deadline.isOverdue()) {
        failure(s"Changed did not become true within $defaultDeadlineDuration")
      }
      Thread.sleep(defaultSleepTimeInMillis)
    }
    printf("%s: %7.1f ms\n", getClass.getSimpleName, (endTime - startTime).nanoseconds.toUnit(MILLISECONDS))
    ok
  }

  private def watchFiles[T](files: File*)(block: => T): T = {
    val watcher = watchService.watch(files.map(_.toJava), () => reportChange())
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

    "detect new directories" in withTempDir { dir =>
      watchFiles(dir) {
        dir.createChild("subdir", asDirectory = true)
        assertChanged()
      }
    }

    "detect deleting files" in withTempDir { dir =>
      (dir / "to-be-deleted").write("to be deleted")
      watchFiles(dir) {
        (dir / "to-be-deleted").delete()
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

    "detect changes on files in subdirectories" in withTempDir { dir =>
      val subDir = dir.createChild("subdir", asDirectory = true)
      watchFiles(dir) {
        (subDir / "test").write("new file")
        assertChanged()
      }
    }

    "detect changes on files in new subdirectories" in withTempDir { dir =>
      watchFiles(dir) {
        val subDir = dir.createChild("subdir", asDirectory = true)
        (subDir / "test").write("new file")
        assertChanged()
      }
    }
  }

}
