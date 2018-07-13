package sbt.internal.io {

  import java.io.File

  import scala.concurrent.duration.FiniteDuration

  object PlaySourceModificationWatch {

    // This needs to be here because most of the classes used by this method are
    // private/internal to sbt-io.
    def emptyWatchState(files: Seq[File], initialDelay: FiniteDuration): WatchState = {
      val sbtPollingWatchService = new sbt.io.PollingWatchService(initialDelay)
      val sources = getParentDirs(files).map(Source(_))
      val emptyState = WatchState.empty(sbtPollingWatchService, sources)
      emptyState
    }

    // Internally, sbt-io handle the directories recursively.
    // This is just a small optimization.
    private def getParentDirs(files: Seq[File]): Seq[File] = {
      files.map {
        case dir if dir.isDirectory => dir
        case file if file.isFile => file.getParentFile
      }.distinct
    }

    def watch(
      state: WatchState,
      delay: FiniteDuration
    )(terminationCondition: => Boolean): (Boolean, WatchState) = {
      SourceModificationWatch.watch(delay, state)(terminationCondition)
    }
  }
}

