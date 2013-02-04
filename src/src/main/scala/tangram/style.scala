package tangram

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.File

///////////////////////////////////////////////////////////

/**
 * Takes a Wallpaper and an ImageStream and returns an updated Wallpaper
 * and ImageStream.
 */
trait DisplayStyle[R] {
  def updateWallpaper: DisplayStyle.UpdateWallpaper[R]
}

object DisplayStyle {
  type UpdateWallpaper[R] = (Wallpaper[R], ImageStream) => (Wallpaper[R], ImageStream)
}