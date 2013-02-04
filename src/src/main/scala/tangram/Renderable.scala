package tangram

import java.awt.image.BufferedImage

///////////////////////////////////////////////////////////

/**
 * Anything which can be rendered to a BufferedImage.
 */
trait Renderable {
  /**
   * The size of the rendered image.
   */
  def size: RectangleSize
  
  def render: BufferedImage
}