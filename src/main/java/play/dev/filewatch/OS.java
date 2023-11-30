package play.dev.filewatch;

import java.util.Locale;

enum OS {
  Windows,
  Linux,
  Mac,
  Other;

  static OS getCurrent() {
    String osName = System.getProperty("os.name");
    if (osName == null) return Other;
    osName = osName.toLowerCase(Locale.ENGLISH);
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
