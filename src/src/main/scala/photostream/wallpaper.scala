package photostream

import breeze.linalg.DenseMatrix
import java.awt.image.BufferedImage

import math.min

case class Position(val x: Int, val y: Int) {
  require(x >= 0)
  require(y >= 0)
}

case class WallpaperPixel(image: BorderedResizedImage, position: Position, insertionTime: Int)

case class Wallpaper(pixels: DenseMatrix[Option[WallpaperPixel]]) {
  val width = pixels.cols
  val height = pixels.rows

  val lastInsertionTime = {
    val insertionTimes = pixels.data.flatten.map(_.insertionTime).sorted.reverse
    insertionTimes.headOption match {
      case Some(time) => time
      case None => 0
    }
  }

  def render: BufferedImage = {
    // Starting with a blank canvas, splat the images into the canvas in the
    // order in which they were inserted into the wallpaper.

    val onePixelPerImage = {
      // Remove the |None|s.
      val flatPixels = pixels.data.flatten

      // There should be a "distinctBy" in the standard collection, but
      // since there's not, I'll emulate it here.
      // This is using reference comparison because we're comparing Java
      // types, but it should still work.
      val groups = flatPixels.groupBy(_.image).values
      groups.map(_.head).toSeq
    }

    // Splat the images into the wallpaper in the order of insertion.
    val wallpaper = new BufferedImage(
      Display.wallpaperWidth,
      Display.wallpaperHeight,
      BufferedImage.TYPE_INT_ARGB)
    val graphics = wallpaper.createGraphics
    for (pixel <- onePixelPerImage.sortBy(_.insertionTime))
      graphics.drawImage(pixel.image.render, null, pixel.position.x, pixel.position.y)

    wallpaper
  }

  def insert(image: BorderedResizedImage, position: Position): Wallpaper = {
    val newPixel = WallpaperPixel(image, position, lastInsertionTime + 1)

    val cappedWidth = min(image.width, width - position.x)
    val cappedHeight = min(image.height, height - position.y)

    // TODO: When the following issue is resolved, this code should be deleted:
    // https://github.com/scalanlp/breeze/issues/16
    // Warning: If this is a shallow copy, it doesn't do much.
    def copyMatrix[A](denseMatrix: DenseMatrix[A]): DenseMatrix[A] = {
      val data = denseMatrix.data.clone
      new DenseMatrix(denseMatrix.rows, denseMatrix.cols, data)
    }

    val patchedPixels = copyMatrix(pixels)
    patchedPixels(
      position.y until position.y + cappedHeight, 
      position.x until position.x + cappedWidth) :=
        DenseMatrix.fill[Option[WallpaperPixel]](cappedHeight, cappedWidth)(Some(newPixel))

    Wallpaper(patchedPixels)
  }
}

object Wallpaper {
  def apply(width: Int, height: Int): Wallpaper = {
    val pixels = DenseMatrix.fill[Option[WallpaperPixel]](height, width)(None)
    Wallpaper(pixels)
  }

  def apply(image: BorderedResizedImage): Wallpaper = {
    val wallpaper = Wallpaper(image.width, image.height)
    wallpaper.insert(image, Position(0, 0))
  }

  def concatenateVertical(top: Wallpaper, bottom: Wallpaper): Wallpaper = {
    require(top.width == bottom.width)

    // TODO: This should go in Breeze.
    val newPixels =
      DenseMatrix.fill[Option[WallpaperPixel]](top.height + bottom.height, top.width)(None)
    newPixels(0 until top.height, ::) := top.pixels
    newPixels(top.height until newPixels.rows, ::) := bottom.pixels

    Wallpaper(newPixels)
  }

  def concatenateHorizontal(left: Wallpaper, right: Wallpaper): Wallpaper = {
    require(left.height == right.height)

    // TODO: This should go in Breeze.
    val newPixels =
      DenseMatrix.fill[Option[WallpaperPixel]](left.height, left.width + right.width)(None)
    newPixels(::, 0 until left.width) := left.pixels
    newPixels(::, left.width until newPixels.cols) := right.pixels

    Wallpaper(newPixels)
  }
}