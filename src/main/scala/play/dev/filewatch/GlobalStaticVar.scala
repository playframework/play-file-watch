package play.dev.filewatch

import scala.reflect.ClassTag

/**
 * Provides a global (cross classloader) static var.
 *
 * This does not leak classloaders (unless the value passed to it references a classloader that shouldn't be leaked). It
 * uses an MBeanServer to store an AtomicReference as an mbean, exposing the get method of the AtomicReference as an
 * mbean operation, so that the value can be retrieved.
 */
private[filewatch] object GlobalStaticVar {
  import java.lang.management._
  import java.util.concurrent.atomic.AtomicReference
  import javax.management._
  import javax.management.modelmbean._

  private def objectName(name: String) = {
    new ObjectName(":type=GlobalStaticVar,name=" + name)
  }

  /**
   * Set a global static variable with the given name.
   */
  def set(name: String, value: AnyRef): Unit = {

    val reference = new AtomicReference[AnyRef](value)

    // Now we construct a MBean that exposes the AtomicReference.get method
    val getMethod = classOf[AtomicReference[_]].getMethod("get")
    val getInfo   = new ModelMBeanOperationInfo("The value", getMethod)
    val mmbi      = new ModelMBeanInfoSupport(
      "GlobalStaticVar",
      "A global static variable",
      null,           // no attributes
      null,           // no constructors
      Array(getInfo), // the operation
      null
    ); // no notifications

    val mmb = new RequiredModelMBean(mmbi)
    mmb.setManagedResource(reference, "ObjectReference")

    // Register the Model MBean in the MBean Server
    ManagementFactory.getPlatformMBeanServer.registerMBean(mmb, objectName(name))
  }

  /**
   * Get a global static variable by the given name.
   */
  def get[T](name: String)(implicit ct: ClassTag[T]): Option[T] = {
    try {
      val value = ManagementFactory.getPlatformMBeanServer.invoke(objectName(name), "get", Array.empty, Array.empty)
      if (ct.runtimeClass.isInstance(value)) {
        Some(value.asInstanceOf[T])
      } else {
        throw new ClassCastException(
          s"Global static var $name is not an instance of ${ct.runtimeClass}, but is actually a ${Option(value)
              .fold("null")(_.getClass.getName)}"
        )
      }
    } catch {
      case _: InstanceNotFoundException =>
        None
    }
  }

  /**
   * Clear a global static variable with the given name.
   */
  def remove(name: String): Unit = {
    try {
      ManagementFactory.getPlatformMBeanServer.unregisterMBean(objectName(name))
    } catch {
      case _: InstanceNotFoundException =>
    }
  }
}
