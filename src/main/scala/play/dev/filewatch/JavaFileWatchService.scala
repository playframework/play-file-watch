package play.dev.filewatch

import better.files.{ File => ScalaFile, _ }
import java.io.File

import scala.util.control.NonFatal

/**
 * Implementation of the file watch service that uses the JDK7 WatchService API.
 */
class JavaFileWatchService(logger: LoggerProxy) extends FileWatchService {

  import java.nio.file._
  import StandardWatchEventKinds._

  def watch(filesToWatch: Seq[File], onChange: () => Unit) = {
    val dirsToWatch = filesToWatch.filter { file =>
      if (file.isDirectory) {
        true
      } else if (file.isFile) {
        // JDK7 WatchService can't watch files
        logger.warn("JDK7 WatchService only supports watching directories, but an attempt has been made to watch the file: " + file.getCanonicalPath)
        logger.warn("This file will not be watched. Either remove the file from playMonitoredFiles, or configure a different WatchService, eg:")
        logger.warn("PlayKeys.fileWatchService := play.runsupport.FileWatchService.jnotify(target.value)")
        false
      } else false
    }

    val watcher = FileSystems.getDefault.newWatchService()

    def watchDir(dir: File) = {
      dir.toPath.register(watcher, Array[WatchEvent.Kind[_]](ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY),
        // This custom modifier exists just for polling implementations of the watch service, and means poll every 2 seconds.
        // For non polling event based watchers, it has no effect.
        com.sun.nio.file.SensitivityWatchEventModifier.HIGH)
    }

    // Get all sub directories
    val allDirsToWatch = allSubDirectories(dirsToWatch)
    allDirsToWatch.foreach(watchDir)

    val thread = new Thread(new Runnable {
      def run() = {
        try {
          while (true) {
            val watchKey = watcher.take()

            val events = watchKey.pollEvents()

            import scala.collection.JavaConversions._
            // If a directory has been created, we must watch it and its sub directories
            events.foreach { event =>

              if (event.kind == ENTRY_CREATE) {
                val file = watchKey.watchable.asInstanceOf[Path].resolve(event.context.asInstanceOf[Path]).toFile

                if (file.isDirectory) {
                  allSubDirectories(Seq(file)).foreach(watchDir)
                }
              }
            }

            onChange()

            watchKey.reset()
          }
        } catch {
          case NonFatal(e) => // Do nothing, this means the watch service has been closed, or we've been interrupted.
        } finally {
          // Just in case it wasn't closed.
          watcher.close()
        }
      }
    }, "sbt-play-watch-service")
    thread.setDaemon(true)
    thread.start()

    new FileWatcher {
      def stop() = {
        watcher.close()
      }
    }

  }

  private def allSubDirectories(dirs: Seq[File]) = {
    dirs.iterator.flatMap { dir =>
      dir.toScala.collectChildren(child => child.isDirectory && !child.isHidden)
    }.map(_.toJava).toSeq.distinct
  }
}
