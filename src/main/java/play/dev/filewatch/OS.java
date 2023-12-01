package play.dev.filewatch;

import java.util.Locale;

enum OS {
  Windows,
  Linux,
  Mac,
  Other;

  static OS getCurrent() {
    final var osName = System.getProperty("os.name", "unknown").toLowerCase(Locale.ENGLISH);
    if (osName.contains("darwin") || osName.contains("mac")) {
      return Mac;
    } else if (osName.contains("windows")) {
      return Windows;
    } else if (osName.contains("linux")) {
      return Linux;
    }
    return Other;
  }
}
