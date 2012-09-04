package photostream

import Render.UpdateWallpaper

object FindNoneStrategy {
  def full: UpdateWallpaper = (wallpaper, images) => {
    val firstNoneLocation = {
      val locations = for (
      (row, y) <- wallpaper.pixels.view.zipWithIndex;
      (None, x) <- row.view.zipWithIndex) yield (x, y)
      
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