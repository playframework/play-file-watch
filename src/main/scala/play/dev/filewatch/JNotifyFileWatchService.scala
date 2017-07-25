package play.dev.filewatch

import java.io.File
import java.net.URLClassLoader
import java.util.zip.ZipFile

import better.files.{ File => ScalaFile, _ }

import scala.io.Codec
import scala.util.Try

private[play] class JNotifyFileWatchService(delegate: JNotifyFileWatchService.JNotifyDelegate) extends FileWatchService {
  def watch(filesToWatch: Seq[File], onChange: () => Unit) = {
    val listener = delegate.newListener(onChange)
    val registeredIds = filesToWatch.map { file =>
      delegate.addWatch(file.getAbsolutePath, listener)
    }
    new FileWatcher {
      def stop() = registeredIds.foreach(delegate.removeWatch)
    }
  }
}

private object JNotifyFileWatchService {

  import java.lang.reflect.{ InvocationHandler, Method, Proxy }

  /**
   * Captures all the reflection invocations in one place.
   */
  class JNotifyDelegate(classLoader: ClassLoader, listenerClass: Class[_], addWatchMethod: Method, removeWatchMethod: Method) {
    def addWatch(fileOrDirectory: String, listener: AnyRef): Int = {
      addWatchMethod.invoke(null,
        fileOrDirectory, // The file or directory to watch
        15: java.lang.Integer, // flags to say watch for all events
        true: java.lang.Boolean, // Watch subtree
        listener).asInstanceOf[Int]
    }
    def removeWatch(id: Int): Unit = {
      try {
        removeWatchMethod.invoke(null, id.asInstanceOf[AnyRef])
      } catch {
        case _: Throwable =>
        // Ignore, if we fail to remove a watch it's not the end of the world.
        // http://sourceforge.net/p/jnotify/bugs/12/
        // We match on Throwable because matching on an IOException didn't work.
        // http://sourceforge.net/p/jnotify/bugs/5/
      }
    }
    def newListener(onChange: () => Unit): AnyRef = {
      Proxy.newProxyInstance(classLoader, Seq(listenerClass).toArray, new InvocationHandler {
        def invoke(proxy: AnyRef, m: Method, args: Array[AnyRef]): AnyRef = {
          onChange()
          null
        }
      })
    }

    @throws[Throwable]("If we were not able to successfully load JNotify")
    def ensureLoaded(): Unit = {
      removeWatchMethod.invoke(null, 0.asInstanceOf[java.lang.Integer])
    }
  }

  // Tri state - null means no attempt to load yet, None means failed load, Some means successful load
  @volatile var watchService: Option[Try[JNotifyFileWatchService]] = None

  def apply(targetDirectory: File): Try[FileWatchService] = {

    watchService match {
      case None =>
        val ws = scala.util.control.Exception.allCatch.withTry {

          val classloader = GlobalStaticVar.get[ClassLoader]("FileWatchServiceJNotifyHack").getOrElse {
            val jnotifyJarFile = this.getClass.getClassLoader.asInstanceOf[java.net.URLClassLoader].getURLs
              .map(_.getFile)
              .find(_.contains("/jnotify"))
              .map(new File(_))
              .getOrElse(sys.error("Missing JNotify?"))

            val jnotifyTarget = targetDirectory.toScala / "jnotify"
            val nativeLibrariesDirectory = jnotifyTarget / "native_libraries"

            if (!nativeLibrariesDirectory.exists) {
              // Unzip native libraries from the jnotify jar to target/jnotify/native_libraries
              unzipTo(jnotifyJarFile.toScala, jnotifyTarget)
            }

            val libs = (nativeLibrariesDirectory / (System.getProperty("sun.arch.data.model") + "bits"))
              .toJava.getAbsolutePath

            // Hack to set java.library.path
            System.setProperty("java.library.path", {
              Option(System.getProperty("java.library.path")).map { existing =>
                existing + java.io.File.pathSeparator + libs
              }.getOrElse(libs)
            })
            val fieldSysPath = classOf[ClassLoader].getDeclaredField("sys_paths")
            fieldSysPath.setAccessible(true)
            fieldSysPath.set(null, null)

            // Create classloader just for jnotify
            val loader = new URLClassLoader(Array(jnotifyJarFile.toURI.toURL), null)

            GlobalStaticVar.set("FileWatchServiceJNotifyHack", loader)

            loader
          }

          val jnotifyClass = classloader.loadClass("net.contentobjects.jnotify.JNotify")
          val jnotifyListenerClass = classloader.loadClass("net.contentobjects.jnotify.JNotifyListener")
          val addWatchMethod = jnotifyClass.getMethod("addWatch", classOf[String], classOf[Int], classOf[Boolean], jnotifyListenerClass)
          val removeWatchMethod = jnotifyClass.getMethod("removeWatch", classOf[Int])

          val d = new JNotifyDelegate(classloader, jnotifyListenerClass, addWatchMethod, removeWatchMethod)

          // Try it
          d.ensureLoaded()

          new JNotifyFileWatchService(d)
        }
        watchService = Some(ws)
        ws
      case Some(ws) => ws
    }
  }

  /**
   * Unzips a zip file. This is copied from better files, however it doesn't require each file in the zip file to
   * have a parent directory listed, it does this by using createIfNotExists(createParents = true) instead of just
   * createChild().
   */
  private def unzipTo(thisFile: ScalaFile, destination: ScalaFile)(implicit codec: Codec): destination.type = {
    import scala.collection.JavaConverters._
    for {
      zipFile <- new ZipFile(thisFile.toJava, codec).autoClosed
      entry <- zipFile.entries().asScala
      file = (destination / entry.getName).createIfNotExists(entry.isDirectory, createParents = true)
      if !entry.isDirectory
    } zipFile.getInputStream(entry) > file.newOutputStream
    destination
  }

}
