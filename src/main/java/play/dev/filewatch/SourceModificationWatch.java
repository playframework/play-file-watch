/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.dev.filewatch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class SourceModificationWatch {

  private SourceModificationWatch() {}

  private static Set<File> listFiles(Iterable<File> sourcesFinder) {
    return StreamSupport.stream(sourcesFinder.spliterator(), false).collect(Collectors.toSet());
  }

  private static long findLastModifiedTime(File file) {
    try {
      return Files.getLastModifiedTime(file.toPath()).toInstant().toEpochMilli();
    } catch (IOException e) {
      return 0L;
    }
  }

  private static long findLastModifiedTime(Set<File> files) {
    if (!files.isEmpty()) {
      return files.stream()
          .max(Comparator.comparingLong(SourceModificationWatch::findLastModifiedTime))
          .map(SourceModificationWatch::findLastModifiedTime)
          .get();
    } else {
      return 0L;
    }
  }

  public static class Result {
    private final boolean triggered;

    private final WatchState state;

    public boolean isTriggered() {
      return triggered;
    }

    public WatchState getState() {
      return state;
    }

    public Result(boolean triggered, WatchState state) {
      this.triggered = triggered;
      this.state = state;
    }
  }

  public static Result watch(
      final Supplier<Iterable<File>> sourcesFinder,
      final int pollDelayMillis,
      WatchState state,
      final Supplier<Boolean> terminationCondition) {
    while (true) {
      var filesToWatch = listFiles(sourcesFinder.get());

      var sourceFilesPath =
          filesToWatch.stream()
              .map(
                  file -> {
                    try {
                      return file.getCanonicalPath();
                    } catch (IOException e) {
                      return null;
                    }
                  })
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
      var lastModifiedTime = findLastModifiedTime(filesToWatch);

      var sourcesModified =
          lastModifiedTime > state.getLastCallbackCallTime()
              || !state.getPreviousFiles().equals(sourceFilesPath);

      var triggered = sourcesModified ? false : state.isAwaitingQuietPeriod();
      var newCallbackCallTime =
          sourcesModified ? System.currentTimeMillis() : state.getLastCallbackCallTime();

      var newState =
          new WatchState(
              newCallbackCallTime,
              sourceFilesPath,
              sourcesModified,
              triggered ? state.getCount() + 1 : state.getCount());
      if (triggered) {
        return new Result(true, newState);
      }
      try {
        Thread.sleep(pollDelayMillis);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      if (terminationCondition.get()) {
        return new Result(false, newState);
      }
      state = newState;
    }
  }
}
