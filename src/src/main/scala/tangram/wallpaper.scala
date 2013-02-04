package tangram

import breeze.linalg._
import java.awt.image.BufferedImage
import java.util.Date

import math.min

///////////////////////////////////////////////////////////

//trait HasWallpaper {
//  def wallpaper: Wallpaper
//}

/**
 * A position in an image.
 */
case class Position(x: Int, y: Int) {
  require(x >= 0)
  require(y >= 0)
}

/**
 * A single pixel in a wallpaper.
 * It stores the source image from which the pixel is taken,
 * the position of the pixel in the source image, and the time the pixel
 * was inserted in the wallpaper.
 */
case class WallpaperPixel[R <% Renderable](
  image: R,
  position: Position,
  insertionTime: Date)

/**
 * A 2D array of WallpaperPixels which can be rendered.
 */
case class Wallpaper[R <% Renderable](
  pixels: DenseMatrix[Option[WallpaperPixel[R]]])

object Wallpaper {
  def apply[R <% Renderable](width: Int, height: Int): Wallpaper[R] = {
    val pixels = DenseMatrix.fill[Option[WallpaperPixel[R]]](height, width)(None)
    Wallpaper(pixels)
  }

  def apply[R <% Renderable](
    image: R): Wallpaper[R] = {
    val wallpaper = Wallpaper[R](image.size.width, image.size.height)
    wallpaper.insert(image, Position(0, 0))
  }

  /////////////////////////////////////////////////////////

  implicit class Wallpaper2Renderable[R <% Renderable](
    self: Wallpaper[R]) extends Renderable {
    override def size = self.size

    override def render: BufferedImage = {
      // Starting with a blank canvas, splat the images into the canvas in the
      // order in which they were inserted into the wallpaper.

      val onePixelPerImage = {
        // Pair pixels with their locations in the wallpaper.
        val pixelsWithPositions = for (
          row <- 0 until self.pixels.rows;
          column <- 0 until self.pixels.cols
        ) yield {
          for (pixel <- self.pixels(row, column)) yield (
            pixel,
            Position(row, column))
        }

        // Remove the |None|s.
        val flatPixels = pixelsWithPositions.flatten

        // There should be a "distinctBy" in the standard collection, but
        // since there's not, I'll emulate it here.
        // This is using reference comparison because we're comparing Java
        // types, but it should still work.
        val groups = flatPixels.groupBy(_._1.image).values
        groups.map(_.head).toSeq
      }

      // Splat the images into the wallpaper in the order of insertion.
      val wallpaper = new BufferedImage(
        size.width,
        size.height,
        BufferedImage.TYPE_INT_ARGB)
      val graphics = wallpaper.createGraphics
      for ((pixel, position) <- onePixelPerImage.sortBy(_._1.insertionTime))
        graphics.drawImage(
          pixel.image.render,
          null,
          position.x - pixel.position.x,
          position.y - pixel.position.y)

      wallpaper
    }
  }

  implicit class WallpaperOps[R <% Renderable](self: Wallpaper[R]) {
    //    val width = pixels.cols
    //    val height = pixels.rows
    //
    //    val lastInsertionTime = {
    //      val insertionTimes = pixels.data.flatten.map(_.insertionTime).sorted.reverse
    //      insertionTimes.headOption match {
    //        case Some(time) => time
    //        case None => 0
    //      }
    //    }

    def insert[R <% Renderable](image: R, position: Position): Wallpaper[R] = {
      val newPixel = WallpaperPixel(image, position, new Date())

      val cappedWidth = min(image.size.width, self.size.width - position.x)
      val cappedHeight = min(image.size.height, self.size.height - position.y)

//      // TODO: When the following issue is resolved, this code should be deleted:
//      // https://github.com/scalanlp/breeze/issues/16
//      // Warning: If this is a shallow copy, it doesn't do much.
//      def copyMatrix[A](denseMatrix: DenseMatrix[A]): DenseMatrix[A] = {
//        val data = denseMatrix.data.clone
//        new DenseMatrix(denseMatrix.rows, denseMatrix.cols, data)
//      }

      val patchedPixels = copy(self.pixels)
      patchedPixels(
        position.y until position.y + cappedHeight,
        position.x until position.x + cappedWidth) :=
        DenseMatrix.fill[Option[WallpaperPixel[R]]](cappedHeight, cappedWidth)(Some(newPixel))

      Wallpaper(patchedPixels)
    }
  }

//  def concatenateVertical(top: Wallpaper, bottom: Wallpaper): Wallpaper = {
//    require(top.width == bottom.width)
//
//    // TODO: This should go in Breeze.
//    val newPixels =
//      DenseMatrix.fill[Option[WallpaperPixel]](top.height + bottom.height, top.width)(None)
//    newPixels(0 until top.height, ::) := top.pixels
//
//    val updatedBottomPixels = bottom.pixels.map {
//      case None => None
//      case Some(pixel @ WallpaperPixel(_, position, _)) => {
//        val newPosition = Position(
//          position.x,
//          position.y + top.height)
//        Some(pixel.copy(position = newPosition))
//      }
//    }
//
//    newPixels(top.height until newPixels.rows, ::) := updatedBottomPixels
//
//    Wallpaper(newPixels)
//  }
//
//  def concatenateHorizontal(left: Wallpaper, right: Wallpaper): Wallpaper = {
//    require(left.height == right.height)
//
//    // TODO: This should go in Breeze.
//    val newPixels =
//      DenseMatrix.fill[Option[WallpaperPixel]](left.height, left.width + right.width)(None)
//    newPixels(::, 0 until left.width) := left.pixels
//
//    val updatedRightPixels = right.pixels.map {
//      case None => None
//      case Some(pixel @ WallpaperPixel(_, position, _)) => {
//        val newPosition = Position(
//          position.x + left.width,
//          position.y)
//        Some(pixel.copy(position = newPosition))
//      }
//    }
//
//    newPixels(::, left.width until newPixels.cols) := updatedRightPixels
//
//    Wallpaper(newPixels)
//  }

//  implicit def implicitRectangleLike(self: Wallpaper): RectangleLike = new RectangleLike {
//    override def size = RectangleSize(self.pixels.cols, self.pixels.rows)
//  }
}