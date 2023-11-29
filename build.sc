import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.1`
import $ivy.`io.chris-kipp::mill-ci-release::0.1.9`

import mill._
import mill.scalalib._
import mill.scalalib.scalafmt._
import mill.scalalib.publish._
import mill.scalalib.api.Util.scalaNativeBinaryVersion
import de.tobiasroeser.mill.integrationtest._
import io.kipp.mill.ci.release.CiReleaseModule

val millVersion = "0.11.5"
val scala213 = "2.13.10"
val pluginName = "mill-hepek"

def millBinaryVersion(millVersion: String) =
  scalaNativeBinaryVersion(millVersion)

object plugin extends ScalaModule with CiReleaseModule with ScalafmtModule {

  override def scalaVersion = scala213

  override def artifactName =
    s"${pluginName}_mill${millBinaryVersion(millVersion)}"

  override def pomSettings = PomSettings(
    description = "Mill plugin generated with mill-plugin.g8",
    organization = "ba.sake",
    url = "https://github.com/sake92/mill-hepek",
    licenses = Seq(License.`Apache-2.0`),
    versionControl =
      VersionControl.github(owner = "sake92", repo = "mill-hepek"),
    developers =
      Seq(Developer("sake92", "Sakib Hadziavdic", "https://github.com/sake92"))
  )

  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  )

  override def scalacOptions = Seq("-Ywarn-unused", "-deprecation")

}

object itest extends MillIntegrationTestModule {

  override def millTestVersion = millVersion

  override def pluginsUnderTest = Seq(plugin)

  def testBase = millSourcePath / "src"

  override def testInvocations: T[Seq[(PathRef, Seq[TestInvocation.Targets])]] =
    T {
      Seq(
        PathRef(testBase / "simple") -> Seq(
          TestInvocation.Targets(Seq("verify"), noServer = true)
        )
      )
    }
}
