/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.dev.filewatch;

import io.methvin.watcher.DirectoryWatcher;
import io.methvin.watchservice.MacOSXListeningWatchService;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Implementation of the file watch service that uses a native implementation for Mac and otherwise
 * uses the JDK's WatchService implementation.
 */
public class DefaultFileWatchService implements FileWatchService {

  private final LoggerProxy logger;
  private final boolean isMac;
  private final boolean disableFileHashCheck;

  public DefaultFileWatchService(LoggerProxy logger, boolean isMac, boolean disableFileHashCheck) {
    this.logger = logger;
    this.isMac = isMac;
    this.disableFileHashCheck = disableFileHashCheck;
  }

  public DefaultFileWatchService(LoggerProxy logger) {
    this(logger, false, false);
  }

  public DefaultFileWatchService(LoggerProxy logger, boolean isMac) {
    this(logger, isMac, false);
  }

  @Override
  public FileWatcher watch(Iterable<File> filesToWatch, Consumer<Optional<Path>> onChange) {
    var dirsToWatch =
        StreamSupport.stream(filesToWatch.spliterator(), false)
            .filter(
                file -> {
                  if (file.isDirectory()) {
                    return true;
                  } else if (file.isFile()) {
                    logger.warn(
                        () ->
                            "An attempt has been made to watch the file: "
                                + file.getAbsolutePath());
                    logger.warn(
                        () ->
                            "DefaultFileWatchService only supports watching directories. The file"
                                + " will not be watched.");
                    return false;
                  } else {
                    return false;
                  }
                });

    try {
      var watchService =
          isMac ? new MacOSXListeningWatchService() : FileSystems.getDefault().newWatchService();
      var directoryWatcher =
          DirectoryWatcher.builder()
              .paths(dirsToWatch.map(File::toPath).collect(Collectors.toList()))
              .listener(dirChangeEvent -> onChange.accept(Optional.of(dirChangeEvent.path())))
              .fileHashing(!disableFileHashCheck)
              .watchService(watchService)
              .build();

      var thread =
          new Thread(
              () -> {
                try {
                  directoryWatcher.watch();
                } catch (VirtualMachineError | ThreadDeath | LinkageError e) {
                  throw e;
                } catch (Throwable ignored) {
                  // Do nothing, this means the watch service has been closed, or we've been
                  // interrupted.
                }
              },
              "play-watch-service");
      thread.setDaemon(true);
      thread.start();

      return () -> {
        try {
          directoryWatcher.close();
        } catch (IOException e) {
          logger.error(e::getMessage);
        }
      };
    } catch (IOException e) {
      logger.error(e::getMessage);
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
