package play.dev.filewatch

import java.io.File

import sbt.io.PathFinder
import sbt.io.syntax._

import annotation.tailrec

/**
 * A polling Play watch service. Polls in the background.
 */
class PollingFileWatchService(val pollDelayMillis: Int) extends FileWatchService {

  // Work around for https://github.com/sbt/sbt/issues/1973
  def distinctPathFinder(pathFinder: PathFinder) = PathFinder {
    pathFinder.get.map(p => (p.getAbsolutePath, p)).toMap.values
  }

  def watch(filesToWatch: Seq[File], onChange: () => Unit) = {

    @volatile var stopped = false

    val thread = new Thread(new Runnable {
      def run() = {
        var state = WatchState.empty
        while (!stopped) {
          val (triggered, newState) = SourceModificationWatch.watch(distinctPathFinder(filesToWatch.allPaths),
            pollDelayMillis, state)(stopped)
          if (triggered) onChange()
          state = newState
        }
      }
    }, "sbt-play-watch-service")
    thread.setDaemon(true)
    thread.start()

    new FileWatcher {
      def stop() = stopped = true
    }
  }
}

/**
 * Copied from sbt.
 */
object SourceModificationWatch {
  @tailrec def watch(sourcesFinder: PathFinder, pollDelayMillis: Int, state: WatchState)(terminationCondition: => Boolean): (Boolean, WatchState) =
    {
      import state._

      val sourceFiles: Iterable[java.io.File] = sourcesFinder.get
      val sourceFilesPath: Set[String] = sourceFiles.map(_.getCanonicalPath)(collection.breakOut)
      val lastModifiedTime =
        (0L /: sourceFiles) { (acc, file) => math.max(acc, file.lastModified) }

      val sourcesModified =
        lastModifiedTime > lastCallbackCallTime ||
          previousFiles != sourceFilesPath

      val (triggered, newCallbackCallTime) =
        if (sourcesModified)
          (false, System.currentTimeMillis)
        else
          (awaitingQuietPeriod, lastCallbackCallTime)

      val newState = new WatchState(newCallbackCallTime, sourceFilesPath, sourcesModified, if (triggered) count + 1 else count)
      if (triggered)
        (true, newState)
      else {
        Thread.sleep(pollDelayMillis)
        if (terminationCondition)
          (false, newState)
        else
          watch(sourcesFinder, pollDelayMillis, newState)(terminationCondition)
      }
    }
}

final class WatchState(val lastCallbackCallTime: Long, val previousFiles: Set[String], val awaitingQuietPeriod: Boolean, val count: Int) {
  def previousFileCount: Int = previousFiles.size
}

object WatchState {
  def empty = new WatchState(0L, Set.empty[String], false, 0)
}

