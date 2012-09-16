package photostream

import Render.UpdateWallpaper

object FindNoneStrategy {
  def full: UpdateWallpaper = (wallpaper, images) => {
    val asdf = wallpaper.pixels.iterator

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
          ResizedImage(headImage, headImage.getWidth, headImage.getHeight),
          Position(x, y))
        full(newWallpaper, tailImages)
      }
      case None => (wallpaper, images)
    }
  }
}