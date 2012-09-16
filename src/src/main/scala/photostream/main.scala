package photostream

import java.awt.Color
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File

import scala.Array.canBuildFrom
import scala.Option.option2Iterable
import scala.annotation.tailrec

import breeze.linalg.DenseMatrix
import javax.imageio.ImageIO

case class ResizedImage(
  val originalImage: BufferedImage,
  val width: Int,
  val height: Int) {
  require(originalImage.getWidth > 0)
  require(originalImage.getHeight > 0)
  require(width > 0)
  require(height > 0)

  def render: BufferedImage = {
    val newImage = new BufferedImage(
      width,
      height,
      BufferedImage.TYPE_INT_ARGB)

    // TODO: I suspect this may not work.
    val graphics = newImage.createGraphics
    graphics.drawImage(originalImage, 0, 0, width, height, null)
    newImage
  }
}

case class Position(val x: Int, val y: Int) {
  require(x >= 0)
  require(y >= 0)
}

case class WallpaperPixel(image: ResizedImage, position: Position, insertionTime: Int)

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

  def insert(image: ResizedImage, position: Position): Wallpaper = {
    val newPixel = WallpaperPixel(image, position, lastInsertionTime + 1)

    val cappedXEnd = width.min(position.x + image.width)
    val cappedYEnd = height.min(position.y + image.height)

    // TODO: When the following issue is resolved, this code should be deleted:
    // https://github.com/scalanlp/breeze/issues/16
    // Warning: If this is a shallow copy, it doesn't do much.
    def copyMatrix[A](denseMatrix: DenseMatrix[A]): DenseMatrix[A] = {
      val data = denseMatrix.data.clone
      new DenseMatrix(denseMatrix.rows, denseMatrix.cols, data)
    }

    val patchedPixels = copyMatrix(pixels)
    patchedPixels(position.y until cappedYEnd, position.x until cappedXEnd) :=
      DenseMatrix.fill[Option[WallpaperPixel]](image.height, image.width)(Some(newPixel))

    Wallpaper(patchedPixels)
  }
}

object Wallpaper {
  def apply(width: Int, height: Int): Wallpaper = {
    val pixels = DenseMatrix.fill[Option[WallpaperPixel]](height, width)(None)
    Wallpaper(pixels)
  }

  def apply(image: ResizedImage): Wallpaper = {
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

object Display {
  // TODO: Make this reflect the size of the desktop, which is actually smaller
  // than the screen resolution, owing to possible bars at the top or bottom.
  val (wallpaperWidth, wallpaperHeight) = {
    val screenSize = Toolkit.getDefaultToolkit.getScreenSize
    (screenSize.width, screenSize.height)
  }

  def refreshWallpaper {
    val command = "gsettings set org.gnome.desktop.background picture-uri file://%s".format(Runtime.wallpaperFile)
    Runtime.runSystemCommand(command)
  }

  def setWallpaper(image: BufferedImage) {
    require(image.getWidth == wallpaperWidth)
    require(image.getHeight == wallpaperHeight)
    ImageIO.write(image, "png", Runtime.wallpaperFile)

    // TODO
    refreshWallpaper
  }
}

object Runtime {
  // TODO: Make system agnostic
  val wallpaperFile = new File("/tmp/photostream_wallpaper.png")

  def runSystemCommand(command: String): String = {
    println("running system command: %s".format(command))
    try {
      val out = sys.process.Process(command).!!
      println("successfully ran system command")
      out
    } catch {
      case e: Exception => throw new Exception("system command failed: %s\nException was %s\n".format(command, e.toString))
    }
  }
}

case class UnusedImage(image: BufferedImage, numMisses: Int)

object UnusedImage {
  def apply(file: File): UnusedImage = UnusedImage(ImageIO.read(file), 0)
}

object PhotoStream {
  def isImageFile(file: File): Boolean = {
    val extensions = Seq(".png", ".jpg", ".jpeg", ".bmp")
    extensions.map(extension => file.toString.endsWith(extension)).contains(true)
  }

  def getImages: Stream[UnusedImage] = {
    val imageDirectory = new File("/u/echristiansen/Dropbox/scala/12Summer/photostream/data")
    assert(imageDirectory.exists)
    val files = imageDirectory.listFiles.filter(isImageFile)
    files.toStream.map(UnusedImage.apply)
  }
}

object Preprocess {
  def addBorder(
    borderWidth: Int,
    borderColor: Color)(
      unusedImage: UnusedImage): UnusedImage = {
    val UnusedImage(image, numMisses) = unusedImage
    val newWidth = image.getWidth + 2 * borderWidth
    val newHeight = image.getHeight + 2 * borderWidth
    val newImage = new BufferedImage(
      newWidth,
      newHeight,
      BufferedImage.TYPE_INT_ARGB)
    val graphics = newImage.createGraphics
    graphics.setColor(borderColor)
    graphics.fillRect(0, 0, newWidth, newHeight)
    graphics.drawImage(image, borderWidth, borderWidth, null)
    UnusedImage(newImage, numMisses)
  }
}

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

// TODO: Rename
object Render {
  type UpdateWallpaper = (Wallpaper, Stream[UnusedImage]) => Tuple2[Wallpaper, Stream[UnusedImage]]

  def updateRunner(
    minDelayInMilliseconds: Int,
    updater: UpdateWallpaper,
    wallpaper: Wallpaper,
    images: Stream[UnusedImage]) {

    @annotation.tailrec
    def refresh(wallpaper: Wallpaper, images: Stream[UnusedImage]) {
      val (newWallpaper, newImages) = updater(wallpaper, images)
      // The updater must do something.
      assert(newWallpaper != wallpaper)
      assert(newImages != images)
      Display.setWallpaper(newWallpaper.render)
      Thread.sleep(minDelayInMilliseconds)
      refresh(newWallpaper, newImages)
    }

    refresh(wallpaper, images)
  }
}

object Main extends App {
  val unusedImages = PhotoStream.getImages
  val decoratedImages = unusedImages.map(Preprocess.addBorder(1, Color.white))

  val wallpaper = Wallpaper(Display.wallpaperWidth, Display.wallpaperHeight)

  Render.updateRunner(40000, RecursiveSplitStrategy.full, wallpaper, decoratedImages)
}
