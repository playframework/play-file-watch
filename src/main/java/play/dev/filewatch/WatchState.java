/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.dev.filewatch;

import java.util.Collections;
import java.util.Set;

public class WatchState {

  private final long lastCallbackCallTime;
  private final Set<String> previousFiles;
  private final boolean awaitingQuietPeriod;
  private final int count;

  public WatchState(
      long lastCallbackCallTime,
      Set<String> previousFiles,
      boolean awaitingQuietPeriod,
      int count) {
    this.lastCallbackCallTime = lastCallbackCallTime;
    this.previousFiles = previousFiles;
    this.awaitingQuietPeriod = awaitingQuietPeriod;
    this.count = count;
  }

  public long getLastCallbackCallTime() {
    return lastCallbackCallTime;
  }

  public Set<String> getPreviousFiles() {
    return previousFiles;
  }

  public boolean isAwaitingQuietPeriod() {
    return awaitingQuietPeriod;
  }

  public int getCount() {
    return count;
  }

  public int getPreviousFileCount() {
    return previousFiles.size();
  }

  public static WatchState empty() {
    return new WatchState(0L, Collections.emptySet(), false, 0);
  }
}
