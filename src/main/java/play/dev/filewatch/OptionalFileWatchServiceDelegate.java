/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.dev.filewatch;

import java.io.File;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Watch service that delegates to a try. This allows it to exist without reporting an exception
 * unless it's used.
 */
public class OptionalFileWatchServiceDelegate implements FileWatchService {

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private final Optional<FileWatchService> watchService;

  public OptionalFileWatchServiceDelegate(FileWatchService watchService) {
    this.watchService = Optional.ofNullable(watchService);
  }

  @Override
  public FileWatcher watch(Iterable<File> filesToWatch, Supplier<Void> onChange) {
    return watchService.map(ws -> ws.watch(filesToWatch, onChange)).get();
  }
}
