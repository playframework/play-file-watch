/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.dev.filewatch;

import java.util.function.Supplier;

/** Proxy interface to a logger. */
public interface LoggerProxy {
  void verbose(final Supplier<String> message);

  void debug(final Supplier<String> message);

  void info(final Supplier<String> message);

  void warn(final Supplier<String> message);

  void error(final Supplier<String> message);

  void trace(final Supplier<Throwable> t);

  void success(final Supplier<String> message);
}
