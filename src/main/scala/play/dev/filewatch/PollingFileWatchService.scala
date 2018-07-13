package play.dev.filewatch

import java.io.File
import java.util.concurrent.TimeUnit

import sbt.internal.io.PlaySourceModificationWatch

import scala.concurrent.duration._

/**
 * A polling Play watch service. Polls in the background.
 */
class PollingFileWatchService(val pollDelay: FiniteDuration, val antiEntropy: FiniteDuration = 40.milliseconds) extends FileWatchService {

  @deprecated("Use pollDelay Duration instead", "1.1.8")
  lazy val pollDelayMillis: Int = pollDelay.toMillis.toInt

  def this(pollDelayMillis: Int) = this(Duration(pollDelayMillis, TimeUnit.MILLISECONDS))

  override def watch(filesToWatch: Seq[File], onChange: () => Unit): FileWatcher = {

    @volatile var stopped = false

    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        var state = PlaySourceModificationWatch.emptyWatchState(filesToWatch, pollDelay)
        while (!stopped) {
          val (triggered, newState) = PlaySourceModificationWatch.watch(state, pollDelay)(stopped)
          if (triggered) onChange()
          state = newState
        }
      }
    }, "play-polling-watch-service")
    thread.setDaemon(true)
    thread.start()

    new FileWatcher {
      override def stop(): Unit = stopped = true
    }
  }
}
