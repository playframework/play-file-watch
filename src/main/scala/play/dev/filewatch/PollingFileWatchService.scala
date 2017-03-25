package play.dev.filewatch

import java.io.File

import better.files.{ File => ScalaFile, _ }

import annotation.tailrec

/**
 * A polling Play watch service. Polls in the background.
 */
class PollingFileWatchService(val pollDelayMillis: Int) extends FileWatchService {

  def watch(filesToWatch: Seq[File], onChange: () => Unit) = {

    @volatile var stopped = false

    val thread = new Thread(new Runnable {
      def run() = {
        var state = WatchState.empty
        while (!stopped) {
          val (triggered, newState) = SourceModificationWatch.watch(() => filesToWatch.iterator.flatMap(_.toScala.listRecursively),
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
  type PathFinder = () => Iterator[ScalaFile]

  @tailrec def watch(sourcesFinder: PathFinder, pollDelayMillis: Int, state: WatchState)(terminationCondition: => Boolean): (Boolean, WatchState) =
    {
      if (pollDelayMillis < 1000) {
        throw new IllegalArgumentException(
          "pollDelayMillis must be at least 1000 since many filesystems only support second-level granularity for last-modification time")
      }

      import state._

      val sourceFilesPath: Set[String] = sourcesFinder().map(_.toJava.getCanonicalPath).toSet
      val lastModifiedTime =
        (0L /: sourcesFinder()) { (acc, file) => math.max(acc, file.lastModifiedTime.toEpochMilli) }

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

