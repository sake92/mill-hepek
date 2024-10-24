package ex1

import java.nio.file.Paths
import ba.sake.hepek.core.Renderable

object RendEx extends Renderable {

  override def relPath =
    Paths.get("files/rend_ex.txt")

  override def render =
    "RendEx content"
}