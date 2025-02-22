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

  override def ivyDeps = Task {
    super.ivyDeps() ++ Agg(
      ivy"ba.sake:hepek-core:0.2.0"
    )
  }

  def hepek = Task {
    // scala ~requires all classes/objects to have a package
    // so we arbitrarily choose the "files" toplevel package..

    // generate hepek files
    val hepekGenerateFolder = hepekGenerate().path
    val hepekGenerateFilesFolder =
      os.list(hepekGenerateFolder)
        .find(f => os.isDir(f) && f.relativeTo(hepekGenerateFolder).startsWith(os.RelPath("files")))
        .toSeq

    // collect resources/public folders
    val publicFolders = resources()
      .filter(r => os.isDir(r.path) && os.exists(r.path))
      .flatMap { r =>
        os.list(r.path)
          .filter(f => os.isDir(f) && f.relativeTo(r.path) == os.RelPath("public"))
      }

    val destFolder = millSourcePath / "hepek_output"
    val allFolders = publicFolders ++ hepekGenerateFilesFolder
    allFolders.foreach { folder =>
      os.copy(
        folder,
        destFolder,
        createFolders = true,
        replaceExisting = true,
        mergeFolders = true
      )
    }
    PathRef(destFolder)
  }

  private def hepekGenerate: Task[PathRef] = Task.Anon {
    val log = Task.log
    val destFolder = Task.dest

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
          writeRenderableObject(className, destFolder, relPath, content, log)
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
            writeRenderableObject(className, destFolder, relPath, content, log)
          }
        }
      }
    }
    PathRef(Task.dest)
  }

  private def isSuperclassOf(clazzParent: Class[_], clazz: Class[_]): Boolean =
    clazzParent.isAssignableFrom(clazz)

  private def writeRenderableObject(
      className: String,
      destFolder: os.Path,
      relPath: JPath,
      content: String,
      log: Logger
  ): Unit = {
    val finalPath = destFolder / os.RelPath(relPath)
    log.debug(s"Rendering '${className}' to '${finalPath}'")
    os.write(finalPath, content, createFolders = true)
  }

  // generate handy resources
  override def generatedSources: T[Seq[PathRef]] = Task {
    val dest = Task.dest
    val publicFolders = resources()
      .filter(r => os.isDir(r.path) && os.exists(r.path))
      .flatMap { r =>
        os.list(r.path)
          .filter(f => os.isDir(f) && f.relativeTo(r.path) == os.RelPath("public"))
      }
    
    var res = ""
    var indent = 0
    def writeResource(path: os.Path, parentPath: String): Unit = {
      val fileName = path.wrapped.getFileName.toString
      val pathName = if (parentPath.isEmpty) fileName else s"${parentPath}/${fileName}"
      if (os.isDir(path)) {
        res += (" " * indent) + s"object ${fileName} {\n"
        indent += 2
        os.list(path).foreach(p => writeResource(p, pathName))
        indent -= 2
        res += (" " * indent) + "}\n\n"
      } else {
        res += (" " * indent) + s"""val `${fileName}` = Resource("${pathName}")\n"""
      }
    }
    publicFolders.headOption.toSeq.flatMap(p => os.list(p)).foreach(p => writeResource(p, ""))

    os.write(
      dest / "public_resources.scala",
      s"""package files
         |import ba.sake.hepek.Resource
         |
         |${res}
         |""".stripMargin
    )
    super.generatedSources() ++ Seq(PathRef(Task.dest))
  }

}
