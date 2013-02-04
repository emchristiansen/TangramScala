package tangram

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.File

///////////////////////////////////////////////////////////

/**
 * Takes a Wallpaper and an ImageStream and returns an updated Wallpaper
 * and ImageStream.
 */
trait DisplayStyle {
  def updateWallpaper: DisplayStyle.UpdateWallpaper
}

object DisplayStyle {
  type UpdateWallpaper = (Wallpaper, ImageStream) => (Wallpaper, ImageStream)
}