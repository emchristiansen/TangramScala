package tangram

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.File

///////////////////////////////////////////////////////////

trait DisplayStyle {
  def updateWallpaper: DisplayStyle.UpdateWallpaper
}

object DisplayStyle {
  type UpdateWallpaper = (Wallpaper, Stream[UnusedImage]) => Tuple2[Wallpaper, Stream[UnusedImage]]
}