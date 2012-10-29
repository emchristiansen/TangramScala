package photostream.styles

import photostream._
import Run.UpdateWallpaper
import java.awt.Color
import breeze.linalg.DenseMatrix
import photostream.UnusedImage
import photostream.WallpaperPixel

// TODO: Generalize to arbitrary numeric types. 
case class IntegralImage(val matrix: DenseMatrix[Double]) {
  val integralImage = DenseMatrix.zeros[Double](matrix.rows, matrix.cols)

  def getElseZero(row: Int, column: Int): Double = {
    if (row < 0 || column < 0) 0
    else integralImage(row, column)
  }

  for (
    row <- 0 until matrix.rows;
    column <- 0 until matrix.cols
  ) {
    val up = getElseZero(row - 1, column)
    val left = getElseZero(row, column - 1)
    val upLeft = getElseZero(row - 1, column - 1)
    val sumHere = up + left - upLeft + matrix(row, column)
    integralImage(row, column) = sumHere
  }

  // |endRow| and |endColumn| are exclusive.
  def sumRectangle(
    startRow: Int,
    endRow: Int,
    startColumn: Int,
    endColumn: Int): Double = {
    val lowerRight = getElseZero(endRow - 1, endColumn - 1)
    val upperRight = getElseZero(startRow - 1, endColumn - 1)
    val lowerLeft = getElseZero(endRow - 1, startColumn - 1)
    val upperLeft = getElseZero(startRow - 1, startColumn - 1)
    lowerRight - upperRight - lowerLeft + upperLeft
  }
}

object BestSplatStrategy {
  val border = ImageBorder(2, Color.black)
  
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
      BorderedResizedImage(
        border,
        ResizedImage(image, image.getWidth, image.getHeight)),
      Position(highestScore.x, highestScore.y))
    (newWallpaper, tailImages)
  }
}