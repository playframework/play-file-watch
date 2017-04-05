package play.dev.filewatch

import java.io.File
import java.util.concurrent.Callable
import java.util.{ List => JList, Locale }

import scala.collection.JavaConverters._
import scala.util.{ Properties, Try }

/**
 * A service that can watch files
 */
trait FileWatchService {
  /**
   * Watch the given sequence of files or directories.
   *
   * @param filesToWatch The files to watch.
   * @param onChange A callback that is executed whenever something changes.
   * @return A watcher
   */
  def watch(filesToWatch: Seq[File], onChange: () => Unit): FileWatcher

  /**
   * Watch the given sequence of files or directories.
   *
   * @param filesToWatch The files to watch.
   * @param onChange A callback that is executed whenever something changes.
   * @return A watcher
   */
  def watch(filesToWatch: JList[File], onChange: Callable[Void]): FileWatcher = {
    val buffer: Seq[java.io.File] = filesToWatch.asScala
    val function: () => Unit = () => { onChange.call }
    watch(buffer, function)
  }

}

/**
 * A watcher, that watches files.
 */
trait FileWatcher {

  /**
   * Stop watching the files.
   */
  def stop(): Unit
}

object FileWatchService {
  private sealed trait OS
  private case object Windows extends OS
  private case object Linux extends OS
  private case object OSX extends OS
  private case object Other extends OS

  private val os: OS = {
    sys.props.get("os.name").map { name =>
      name.toLowerCase(Locale.ENGLISH) match {
        case osx if osx.contains("darwin") || osx.contains("mac") => OSX
        case windows if windows.contains("windows") => Windows
        case linux if linux.contains("linux") => Linux
        case _ => Other
      }
    }.getOrElse(Other)
  }

  def defaultWatchService(targetDirectory: File, pollDelayMillis: Int, logger: LoggerProxy): FileWatchService = new FileWatchService {
    lazy val delegate = os match {
      // If Windows or Linux and JDK7, use JDK7 watch service
      case (Windows | Linux) if Properties.isJavaAtLeast("1.7") => new JavaFileWatchService(logger)
      // If Windows, Linux or OSX, use JNotify but fall back to SBT
      case (Windows | Linux | OSX) => JNotifyFileWatchService(targetDirectory).recover {
        case e =>
          logger.warn("Error loading JNotify watch service: " + e.getMessage)
          logger.trace(e)
          new PollingFileWatchService(pollDelayMillis)
      }.get
      case _ => new PollingFileWatchService(pollDelayMillis)
    }

    def watch(filesToWatch: Seq[File], onChange: () => Unit) = delegate.watch(filesToWatch, onChange)
  }

  def jnotify(targetDirectory: File): FileWatchService = optional(JNotifyFileWatchService(targetDirectory))

  def jdk7(logger: LoggerProxy): FileWatchService = new JavaFileWatchService(logger)

  def sbt(pollDelayMillis: Int): FileWatchService = new PollingFileWatchService(pollDelayMillis)

  def optional(watchService: Try[FileWatchService]): FileWatchService = new OptionalFileWatchServiceDelegate(watchService)
}

/**
 * Watch service that delegates to a try. This allows it to exist without reporting an exception unless it's used.
 */
class OptionalFileWatchServiceDelegate(val watchService: Try[FileWatchService]) extends FileWatchService {
  def watch(filesToWatch: Seq[File], onChange: () => Unit) = {
    watchService.map(ws => ws.watch(filesToWatch, onChange)).get
  }
}

