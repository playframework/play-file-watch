package play.dev.filewatch

/**
 * Proxy interface to a logger.
 */
trait LoggerProxy {
  def verbose(message: => String): Unit
  def debug(message: => String): Unit
  def info(message: => String): Unit
  def warn(message: => String): Unit
  def error(message: => String): Unit
  def trace(t: => Throwable): Unit
  def success(message: => String): Unit
}