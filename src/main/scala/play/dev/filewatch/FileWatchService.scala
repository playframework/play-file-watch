package play.dev.filewatch

import java.io.File
import java.util.concurrent.Callable
import java.util.{ Locale, List => JList }

import scala.collection.JavaConverters._
import scala.util.Try

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
    val buffer: Seq[java.io.File] = filesToWatch.asScala.toList
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
  private[filewatch] sealed trait OS {

  }
  private[filewatch] object OS {
    case object Windows extends OS
    case object Linux extends OS
    case object Mac extends OS
    case object Other extends OS
  }
  import OS._

  private[filewatch] val os: OS = {
    sys.props.get("os.name").fold(Other: OS) { name =>
      name.toLowerCase(Locale.ENGLISH) match {
        case mac if mac.contains("darwin") || mac.contains("mac") => Mac
        case windows if windows.contains("windows") => Windows
        case linux if linux.contains("linux") => Linux
        case _ => Other
      }
    }
  }

  def defaultWatchService(targetDirectory: File, pollDelayMillis: Int, logger: LoggerProxy): FileWatchService = new FileWatchService {
    lazy val delegate = os match {
      // If Windows or Linux, use JDK7 Watch Service (assume JDK7+)
      case (Windows | Linux) => jdk7(logger)
      // If mac OS, use the mac implementation
      case Mac => try mac(logger) catch {
        case e: Throwable =>
          logger.warn("Error loading Mac OS X watch service: " + e.getMessage)
          logger.trace(e)
          polling(pollDelayMillis)
      }
      // Fall back to polling watch service
      case _ => polling(pollDelayMillis)
    }

    def watch(filesToWatch: Seq[File], onChange: () => Unit) = delegate.watch(filesToWatch, onChange)

    override def toString = delegate.toString
  }

  def jnotify(targetDirectory: File): FileWatchService = optional(JNotifyFileWatchService(targetDirectory))

  def jdk7(logger: LoggerProxy): FileWatchService = default(logger, isMac = false)

  def mac(logger: LoggerProxy): FileWatchService = default(logger, isMac = true)

  def polling(pollDelayMillis: Int): FileWatchService = new PollingFileWatchService(pollDelayMillis)

  def optional(watchService: Try[FileWatchService]): FileWatchService = new OptionalFileWatchServiceDelegate(watchService)

  def default(logger: LoggerProxy, isMac: Boolean): FileWatchService = new DefaultFileWatchService(logger, isMac)
}

/**
 * Watch service that delegates to a try. This allows it to exist without reporting an exception unless it's used.
 */
class OptionalFileWatchServiceDelegate(val watchService: Try[FileWatchService]) extends FileWatchService {
  def watch(filesToWatch: Seq[File], onChange: () => Unit) = {
    watchService.map(ws => ws.watch(filesToWatch, onChange)).get
  }
}

