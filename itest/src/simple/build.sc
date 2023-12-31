import $ivy.`org.scalameta::munit:0.7.29`
import $file.plugins

import mill._, scalalib._
import munit.Assertions._
import ba.sake.millhepek._

object renderables extends MillHepekModule with ScalaModule {
  def scalaVersion = "3.3.1"

  def ivyDeps = Agg(
    ivy"ba.sake::hepek:0.22.0"
  )

  object test extends ScalaTests with TestModule.Munit {
    def ivyDeps = Agg(ivy"org.scalameta::munit:0.7.29")
  }
}

def verify() = T.command {
  val res = renderables.hepek()
  val renderedFiles = os.walk(res.path).filter(os.isFile)
  assertEquals(renderedFiles.size, 5)

  // Renderable
  val rendExFile =
    renderedFiles.find(_.toNIO.getFileName.toString == "rend_ex.txt").get
  val rendExFileContent = os.read(rendExFile)
  assertEquals(rendExFileContent, "RendEx content")

  // MultiRenderable
  val multiRendExFile1 =
    renderedFiles
      .find(_.toNIO.getFileName.toString == "multi_rend_ex_1.txt")
      .get
  val multiRendExFile1Content = os.read(multiRendExFile1)
  assertEquals(multiRendExFile1Content, "MultiRendEx content content 1")
  val multiRendExFile2 =
    renderedFiles
      .find(_.toNIO.getFileName.toString == "multi_rend_ex_2.txt")
      .get
  val multiRendExFile2Content = os.read(multiRendExFile2)
  assertEquals(multiRendExFile2Content, "MultiRendEx content content 2")

  // resource files copied
  val resourceScriptFile = renderedFiles
    .find(_.relativeTo(res.path).startsWith(os.RelPath("scripts") / "main.js"))
    .get
  val resourceScriptFileContent = os.read(resourceScriptFile)
  assertEquals(
    resourceScriptFileContent,
    """ console.log("Hello World!") """.trim
  )
}
