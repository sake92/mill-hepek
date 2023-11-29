package ba.sake.millhepek

import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.{Path => JPath}
import scala.jdk.CollectionConverters._
import mill._
import mill.scalalib._
import mill.api.Logger

trait MillHepekModule extends JavaModule {

  private val ModuleFieldName = "MODULE$"
  private val RenderableFQN = "ba.sake.hepek.core.Renderable"
  private val MultiRenderableFQN = "ba.sake.hepek.core.MultiRenderable"

  override def ivyDeps: T[Agg[Dep]] = T {
    super.ivyDeps() ++ Agg(
      ivy"ba.sake:hepek-core:0.2.0"
    )
  }

  def hepek = T {
    val log = T.ctx().log
    val destDir = T.dest

    // deps/JARs + user classes
    val fullClasspath =
      runClasspath().map(_.path).filter(os.exists)
    val classloader = new URLClassLoader(
      fullClasspath.map(_.toNIO.toUri.toURL).toArray[URL]
    )

    // only the user classes
    val userClassFilesFolder = compile().classes.path
    val userClassFiles = if (os.exists(userClassFilesFolder)) {
      os.walk(userClassFilesFolder)
        .filter(x => os.isFile(x, followLinks = false) && x.ext == "class")
    } else Seq.empty

    // do rendering
    val renderableClazz = classloader.loadClass(RenderableFQN)
    val multiRenderableClazz = classloader.loadClass(MultiRenderableFQN)
    val userClassNames = userClassFiles
      .map(_.relativeTo(userClassFilesFolder))
      .map(
        _.toString
          .dropRight(6) // remove ".class" suffix
          .replaceAll("\\\\|/", "\\.") // replace "\" and "/" with "."
      )

    userClassNames.foreach { className =>
      val clazz = classloader.loadClass(className)
      val mods = clazz.getModifiers
      val fieldNames = clazz.getDeclaredFields.map(_.getName).toSeq

      val isScalaObject =
        !Modifier.isAbstract(mods) && fieldNames.contains(ModuleFieldName)
      if (isScalaObject) {
        if (isSuperclassOf(renderableClazz, clazz)) {
          val objClazz =
            classloader.loadClass(className.dropRight(1)) // without $ at end
          val content =
            objClazz.getMethod("render").invoke(null).asInstanceOf[String]
          val relPath =
            objClazz.getMethod("relPath").invoke(null).asInstanceOf[JPath]
          writeRenderableObject(className, destDir, relPath, content, log)
        } else if (isSuperclassOf(multiRenderableClazz, clazz)) {
          val objClazz =
            classloader.loadClass(className.dropRight(1)) // without $ at end
          val renderables = objClazz
            .getMethod("renderables")
            .invoke(null)
            .asInstanceOf[java.util.List[_]]
          renderables.asScala.foreach { r =>
            val content =
              renderableClazz.getMethod("render").invoke(r).asInstanceOf[String]
            val relPath =
              renderableClazz.getMethod("relPath").invoke(r).asInstanceOf[JPath]
            writeRenderableObject(className, destDir, relPath, content, log)
          }
        }
      }
    }
    PathRef(T.dest)
  }

  private def isSuperclassOf(clazzParent: Class[_], clazz: Class[_]): Boolean =
    clazzParent.isAssignableFrom(clazz)

  private def writeRenderableObject(
      className: String,
      destDir: os.Path,
      relPath: JPath,
      content: String,
      log: Logger
  ): Unit = {
    val finalPath = destDir / os.RelPath(relPath)
    log.debug(s"Rendering class '${className}' to '${finalPath}'")
    os.write(finalPath, content, createFolders = true)
  }

}