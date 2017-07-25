package play.dev.filewatch

import better.files._

import org.specs2.mutable.Specification
import scala.concurrent.duration._

class JavaFileWatchServiceSpec extends FileWatchServiceSpec {
  override def watchService = FileWatchService.jdk7(FileWatchServiceSpecLoggerProxy)
}

class PollingFileWatchServiceSpec extends FileWatchServiceSpec {
  override def watchService = FileWatchService.polling(200)
}

class JNotifyFileWatchServiceSpec extends FileWatchServiceSpec {
  private val jnotifyDir = File("./target/jnotify").createIfNotExists(asDirectory = true)
  override def watchService = FileWatchService.jnotify(jnotifyDir.toJava)
}

object FileWatchServiceSpecLoggerProxy extends LoggerProxy {
  override def verbose(message: => String) = ()
  override def debug(message: => String) = ()
  override def info(message: => String) = ()
  override def warn(message: => String) = ()
  override def error(message: => String) = ()
  override def trace(t: => Throwable) = ()
  override def success(message: => String) = ()
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

  @volatile var changed = false

  private def assertChanged() = {
    val deadline = 5.seconds.fromNow
    while (!changed) {
      if (deadline.isOverdue()) {
        failure("Changed did not become true within 5 seconds")
      }
      Thread.sleep(200)
    }
    ok
  }

  private def reset() = {
    Thread.sleep(50)
    changed = false
  }

  private def watchFiles[T](files: File*)(block: => T): T = {
    changed = false
    val watcher = watchService.watch(files.map(_.toJava), () => changed = true)
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
      watchFiles(dir) {
        val subDir = dir.createChild("subdir", asDirectory = true)
        assertChanged()
        reset()
        (subDir / "test").write("new file")
        assertChanged()
      }
    }
  }

}
