/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.dev.filewatch;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

/** A service that can watch files */
public interface FileWatchService {

  /**
   * Watch the given sequence of files or directories.
   *
   * @param filesToWatch The files to watch.
   * @param onChange A callback that is executed whenever something changes.
   * @return A watcher
   */
  FileWatcher watch(Iterable<File> filesToWatch, Runnable onChange);

  /**
   * @deprecated Use {@link #watch(Iterable, Runnable)} instead
   */
  @Deprecated(since = "3.0.0", forRemoval = true)
  default FileWatcher watch(List<File> filesToWatch, Callable<Void> onChange) {
    return watch(
        filesToWatch,
        () -> {
          try {
            onChange.call();
          } catch (RuntimeException e) {
            throw e;
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  /**
   * @deprecated Renamed to {@link #detect(int, LoggerProxy)}, the first argument targetDirectory of
   *     type File is not used anymore
   */
  @Deprecated(since = "3.0.0", forRemoval = true)
  static FileWatchService defaultWatchService(
      File targetDirectory, int pollDelayMillis, LoggerProxy logger) {
    return detect(pollDelayMillis, logger, false);
  }

  /**
   * @deprecated Renamed to {@link #detect(int, LoggerProxy, Boolean)}, the first argument
   *     targetDirectory of type File is not used anymore
   */
  @Deprecated(since = "3.0.0", forRemoval = true)
  static FileWatchService defaultWatchService(
      File targetDirectory, int pollDelayMillis, LoggerProxy logger, Boolean disableFileHashCheck) {
    return detect(pollDelayMillis, logger, disableFileHashCheck);
  }

  /**
   * @deprecated Renamed to {@link #detect(int, LoggerProxy)}
   */
  @Deprecated(since = "3.0.0", forRemoval = true)
  static FileWatchService defaultWatchService(int pollDelayMillis, LoggerProxy logger) {
    return detect(pollDelayMillis, logger, false);
  }

  /**
   * @deprecated Renamed to {@link #detect(int, LoggerProxy, Boolean)}
   */
  @Deprecated(since = "3.0.0", forRemoval = true)
  static FileWatchService defaultWatchService(
      int pollDelayMillis, LoggerProxy logger, Boolean disableFileHashCheck) {
    return detect(pollDelayMillis, logger, disableFileHashCheck);
  }

  static FileWatchService detect(int pollDelayMillis, LoggerProxy logger) {
    return detect(pollDelayMillis, logger, false);
  }

  static FileWatchService detect(
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
      public FileWatcher watch(Iterable<File> filesToWatch, Runnable onChange) {
        return delegate.watch(filesToWatch, onChange);
      }

      @Override
      public String toString() {
        return delegate.toString();
      }
    };
  }

  static FileWatchService jdk7(LoggerProxy logger, boolean disableFileHashCheck) {
    return defaultFileWatchService(logger, false, disableFileHashCheck);
  }

  static FileWatchService jdk7(LoggerProxy logger) {
    return jdk7(logger, false);
  }

  static FileWatchService mac(LoggerProxy logger, boolean disableFileHashCheck) {
    return defaultFileWatchService(logger, true, disableFileHashCheck);
  }

  static FileWatchService mac(LoggerProxy logger) {
    return mac(logger, false);
  }

  static FileWatchService polling(int pollDelayMillis) {
    return new PollingFileWatchService(pollDelayMillis);
  }

  /** This method was previously named "default" */
  static FileWatchService defaultFileWatchService(
      LoggerProxy logger, boolean isMac, boolean disableFileHashCheck) {
    return new DefaultFileWatchService(logger, isMac, disableFileHashCheck);
  }

  /** This method was previously named "default" */
  static FileWatchService defaultFileWatchService(LoggerProxy logger, boolean isMac) {
    return defaultFileWatchService(logger, isMac, false);
  }
}
