/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.dev.filewatch;

import java.io.File;
import java.util.function.Supplier;

/** A service that can watch files */
public interface FileWatchService {

  /**
   * Watch the given sequence of files or directories.
   *
   * @param filesToWatch The files to watch.
   * @param onChange A callback that is executed whenever something changes.
   * @return A watcher
   */
  FileWatcher watch(Iterable<File> filesToWatch, Supplier<Void> onChange);

  static FileWatchService defaultWatchService(int pollDelayMillis, LoggerProxy logger) {
    return defaultWatchService(pollDelayMillis, logger, false);
  }

  static FileWatchService defaultWatchService(
      int pollDelayMillis, LoggerProxy logger, Boolean disableFileHashCheck) {
    FileWatchService watchService;
    switch (OS.getCurrent()) {
        // If Windows or Linux, use JDK7 Watch Service (assume JDK7+)
      case Windows:
      case Linux:
        {
          watchService = jdk7(logger, disableFileHashCheck);
          break;
        }
        // If macOS, use the mac implementation
      case Mac:
        {
          try {
            watchService = mac(logger, disableFileHashCheck);
          } catch (Throwable e) {
            logger.warn(() -> "Error loading Mac OS X watch service: " + e.getMessage());
            logger.trace(() -> e);
            watchService = polling(pollDelayMillis);
          }
          break;
        }
        // Fall back to polling watch service
      default:
        watchService = polling(pollDelayMillis);
        break;
    }
    final FileWatchService delegate = watchService;

    return new FileWatchService() {

      @Override
      public FileWatcher watch(Iterable<File> filesToWatch, Supplier<Void> onChange) {
        return delegate.watch(filesToWatch, onChange);
      }

      @Override
      public String toString() {
        return delegate.toString();
      }
    };
  }

  static FileWatchService jdk7(LoggerProxy logger, boolean disableFileHashCheck) {
    return def(logger, false, disableFileHashCheck);
  }

  static FileWatchService jdk7(LoggerProxy logger) {
    return jdk7(logger, false);
  }

  static FileWatchService mac(LoggerProxy logger, boolean disableFileHashCheck) {
    return def(logger, true, disableFileHashCheck);
  }

  static FileWatchService mac(LoggerProxy logger) {
    return mac(logger, false);
  }

  static FileWatchService polling(int pollDelayMillis) {
    return new PollingFileWatchService(pollDelayMillis);
  }

  static FileWatchService optional(FileWatchService watchService) {
    return new OptionalFileWatchServiceDelegate(watchService);
  }

  static FileWatchService def(LoggerProxy logger, boolean isMac, boolean disableFileHashCheck) {
    return new DefaultFileWatchService(logger, isMac, disableFileHashCheck);
  }

  static FileWatchService def(LoggerProxy logger, boolean isMac) {
    return def(logger, isMac, false);
  }
}
