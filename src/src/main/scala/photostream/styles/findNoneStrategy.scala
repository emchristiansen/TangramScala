package photostream.styles

import photostream._
import Run.UpdateWallpaper
import java.awt.Color
import photostream.UnusedImage

object FindNoneStrategy {
  val border = ImageBorder(2, Color.black)
  
  def full: UpdateWallpaper = (wallpaper, images) => {
    val firstNoneLocation = {
      val locations = for (
        ((y, x), None) <- wallpaper.pixels.iterator.toSeq
      ) yield (x, y)

      locations.headOption
    }

    firstNoneLocation match {
      case Some((x, y)) => {
        val UnusedImage(headImage, _) #:: tailImages = images
        val newWallpaper = wallpaper.insert(
          BorderedResizedImage(
            border,
            ResizedImage(headImage, headImage.getWidth, headImage.getHeight)),
          Position(x, y))
        full(newWallpaper, tailImages)
      }
      case None => (wallpaper, images)
    }
  }
}