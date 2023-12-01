/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.dev.filewatch;

/** A watcher, that watches files. */
public interface FileWatcher {

  /** Stop watching the files. */
  void stop();
}
