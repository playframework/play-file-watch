/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.dev.filewatch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** A polling Play watch service. Polls in the background. */
public class PollingFileWatchService implements FileWatchService {
  private final int pollDelayMillis;

  private volatile boolean stopped;

  public PollingFileWatchService(int pollDelayMillis) {
    this.pollDelayMillis = pollDelayMillis;
  }

  private static Iterable<File> listRecursively(Iterable<File> files) {
    return StreamSupport.stream(files.spliterator(), false)
        .flatMap(
            file -> {
              try {
                return Files.walk(file.toPath()).filter(path -> !path.equals(file.toPath()));
              } catch (IOException e) {
                return Stream.empty();
              }
            })
        .map(Path::toFile)
        .collect(Collectors.toList());
  }

  @Override
  public FileWatcher watch(Iterable<File> filesToWatch, Runnable onChange) {
    stopped = false;

    var thread =
        new Thread(
            () -> {
              var state = WatchState.empty();
              while (!stopped) {
                var result =
                    SourceModificationWatch.watch(
                        () -> PollingFileWatchService.listRecursively(filesToWatch),
                        pollDelayMillis,
                        state,
                        () -> stopped);
                if (result.isTriggered()) {
                  onChange.run();
                }
                state = result.getState();
              }
            },
            "play-watch-service");
    thread.setDaemon(true);
    thread.start();

    return () -> stopped = true;
  }

  public int getPollDelayMillis() {
    return pollDelayMillis;
  }
}
