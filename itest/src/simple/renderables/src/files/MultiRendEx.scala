package files.ex2

import java.nio.file.Paths
import scala.jdk.CollectionConverters._
import ba.sake.hepek.core.Renderable
import ba.sake.hepek.core.MultiRenderable

object MultiRendEx extends MultiRenderable {

  override def renderables = {
    Seq(1, 2).map { x =>
      new MultiRendExTemplate(x): Renderable
    }.asJava
  }

}

class MultiRendExTemplate(num: Int) extends Renderable {

  override def relPath =
    Paths.get(s"files/multi_rend_ex_${num}.txt")

  override def render =
    s"MultiRendEx content content ${num}"

}
