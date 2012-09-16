package photostream

import Render.UpdateWallpaper

object BestSplatStrategy {
  def single: UpdateWallpaper = (wallpaper, images) => {
    def agePunishmentFunction(age: Int): Double = math.pow(2, age)

    val middleWeight = 4

    val insertionTimes = wallpaper.pixels.map({
      case Some(WallpaperPixel(_, _, insertionTime)) => insertionTime
      case None => 0
    })

    val ageArray = {
      val ages = (insertionTimes - wallpaper.lastInsertionTime) * (-1)
      ages.map(agePunishmentFunction)
    }

    val UnusedImage(image, _) #:: tailImages = images

    val integralImage = IntegralImage(ageArray)

    case class InsertionScore(val x: Int, val y: Int, val score: Double)

    val insertionScores = for (
      y <- 0 until wallpaper.height;
      x <- 0 until wallpaper.width
    ) yield {
      val yEnd = (y + image.getHeight).min(wallpaper.height)
      val xEnd = (x + image.getWidth).min(wallpaper.width)

      val wholeScore = integralImage.sumRectangle(y, yEnd, x, xEnd)
      val middleScore = {
        // There's a special bonus for covering pixels using the 
        // center of the image.
        val yQuarter = (yEnd - y) / 4
        val xQuarter = (xEnd - x) / 4
        integralImage.sumRectangle(
          y + yQuarter,
          yEnd - yQuarter,
          x + xQuarter,
          xEnd - xQuarter)
      }

      InsertionScore(x, y, wholeScore + middleWeight * middleScore)
    }

    val highestScore = insertionScores.maxBy(_.score)
    println(highestScore)
    val newWallpaper = wallpaper.insert(
      ResizedImage(image, image.getWidth, image.getHeight),
      Position(highestScore.x, highestScore.y))
    (newWallpaper, tailImages)
  }
}