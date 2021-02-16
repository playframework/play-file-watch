package play.dev.filewatch

import better.files._
import org.specs2.mutable.Specification

import scala.concurrent.duration._

class JavaFileWatchServiceSpec extends FileWatchServiceSpec {
  // the mac impl consistently fails because it takes more than 5s so skip this one on mac
  args(skipAll = FileWatchService.os == FileWatchService.OS.Mac)

  override def watchService: FileWatchService =
    FileWatchService.jdk7(FileWatchServiceSpecLoggerProxy, disableFileHashCheck = false)
}

class JavaFileWatchServiceHashCheckDisabledSpec extends FileWatchServiceSpec {
  // the mac impl consistently fails because it takes more than 5s so skip this one on mac
  args(skipAll = FileWatchService.os == FileWatchService.OS.Mac)

  override def watchService: FileWatchService =
    FileWatchService.jdk7(FileWatchServiceSpecLoggerProxy, disableFileHashCheck = true)
}

class MacFileWatchServiceSpec extends FileWatchServiceSpec {
  // this only works on mac
  args(skipAll = FileWatchService.os != FileWatchService.OS.Mac)

  override def watchService: FileWatchService =
    FileWatchService.mac(FileWatchServiceSpecLoggerProxy, disableFileHashCheck = false)
}

class MacFileWatchServiceHashCheckDisabledSpec extends FileWatchServiceSpec {
  // this only works on mac
  args(skipAll = FileWatchService.os != FileWatchService.OS.Mac)

  override def watchService: FileWatchService =
    FileWatchService.mac(FileWatchServiceSpecLoggerProxy, disableFileHashCheck = true)
}

class JNotifyFileWatchServiceSpec extends FileWatchServiceSpec {
  private val jnotifyDir = File("./target/jnotify").createIfNotExists(asDirectory = true)

  override def watchService: FileWatchService = FileWatchService.jnotify(jnotifyDir.toJava)
}

class PollingFileWatchServiceSpec extends FileWatchServiceSpec {
  override def watchService: FileWatchService = FileWatchService.polling(200)
}

object FileWatchServiceSpecLoggerProxy extends LoggerProxy {
  override def verbose(message: => String): Unit = ()
  override def debug(message: => String): Unit   = ()
  override def info(message: => String): Unit    = ()
  override def warn(message: => String): Unit    = ()
  override def error(message: => String): Unit   = ()
  override def trace(t: => Throwable): Unit      = ()
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

  @volatile var changed: Boolean = false
  @volatile var startTime: Long  = 0
  @volatile var endTime: Long    = 0

  private def reset(): Unit = {
    Thread.sleep(200)
    changed = false
    startTime = System.nanoTime
  }

  private def reportChange(): Unit = {
    endTime = System.nanoTime
    changed = true
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
