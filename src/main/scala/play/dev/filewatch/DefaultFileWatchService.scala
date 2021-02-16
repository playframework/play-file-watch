package play.dev.filewatch

import java.io.File
import java.nio.file.FileSystems

import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryChangeListener
import io.methvin.watcher.DirectoryWatcher
import io.methvin.watchservice.MacOSXListeningWatchService

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/**
 * Implementation of the file watch service that uses a native implementation for Mac and otherwise uses the JDK's
 * WatchService implementation.
 */
class DefaultFileWatchService(logger: LoggerProxy, isMac: Boolean, disableFileHashCheck: Boolean)
    extends FileWatchService {

  def this(logger: LoggerProxy) = this(logger, isMac = false, disableFileHashCheck = false)
  def this(logger: LoggerProxy, isMac: Boolean) = this(logger, isMac, disableFileHashCheck = false)

  def watch(filesToWatch: Seq[File], onChange: () => Unit) = {
    val dirsToWatch = filesToWatch.filter { file =>
      if (file.isDirectory) {
        true
      } else if (file.isFile) {
        logger.warn("An attempt has been made to watch the file: " + file.getCanonicalPath)
        logger.warn("DefaultFileWatchService only supports watching directories. The file will not be watched.")
        false
      } else false
    }

    val watchService = if (isMac) new MacOSXListeningWatchService() else FileSystems.getDefault.newWatchService()
    val directoryWatcher =
      DirectoryWatcher
        .builder()
        .paths(dirsToWatch.map(_.toPath).asJava)
        .listener(new DirectoryChangeListener {
          override def onEvent(event: DirectoryChangeEvent): Unit = onChange()
        })
        .fileHashing(!disableFileHashCheck)
        .watchService(watchService)
        .build()

    val thread = new Thread(
      new Runnable {
        override def run(): Unit = {
          try {
            directoryWatcher.watch()
          } catch {
            case NonFatal(_) => // Do nothing, this means the watch service has been closed, or we've been interrupted.
          }
        }
      },
      "play-watch-service"
    )
    thread.setDaemon(true)
    thread.start()

    new FileWatcher {
      override def stop(): Unit = directoryWatcher.close()
    }
  }
}
