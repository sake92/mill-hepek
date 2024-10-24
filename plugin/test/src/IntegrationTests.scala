package ba.sake.millhepek

import mill.api.PathRef
import mill.testkit.IntegrationTester

class IntegrationTests extends munit.FunSuite {

  test("integration") {
    pprint.log(sys.env("MILL_EXECUTABLE_PATH"))
    val resourceFolder = os.Path(sys.env("MILL_TEST_RESOURCE_DIR"))

    scala.util.Using.resource(
      new IntegrationTester(
        clientServerMode = false,
        workspaceSourcePath = resourceFolder / "simple",
        millExecutable = os.Path(sys.env("MILL_EXECUTABLE_PATH"))
      )
    ) { tester =>
      val res = tester.eval("hepek")
      assert(res.isSuccess)

      val hepekRes = tester.out("hepek").value[PathRef]
      val renderedFiles = os.walk(hepekRes.path).filter(os.isFile)
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
        .find(_.relativeTo(hepekRes.path).startsWith(os.RelPath("scripts") / "main.js"))
        .get
      val resourceScriptFileContent = os.read(resourceScriptFile)
      assertEquals(
        resourceScriptFileContent,
        """ console.log("Hello World!") """.trim
      )

    }
  }
}
