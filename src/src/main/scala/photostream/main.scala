package photostream

import java.awt.Color
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File

import scala.Option.option2Iterable
import scala.annotation.tailrec

import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.RealMatrix

import javax.imageio.ImageIO

case class ResizedImage(
  val originalImage: BufferedImage,
  val width: Int,
  val height: Int) {
  require(originalImage.getWidth > 0)
  require(originalImage.getHeight > 0)  
  
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

case class Wallpaper(pixels: IndexedSeq[IndexedSeq[Option[WallpaperPixel]]]) {
  val height = pixels.size
  require(height > 0)

  val width = pixels.head.size
  require(width > 0)
  require(pixels.map(_.size == width).reduce(_ && _))

  val lastInsertionTime = {
    val insertionTimes = pixels.flatten.flatten.map(_.insertionTime).sorted.reverse
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
      val flatPixels = pixels.flatten.flatten
      
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

    val cappedWidth = image.width.min(width - position.x)
    val cappedHeight = image.height.min(height - position.y)

    def patchRow(row: IndexedSeq[Option[WallpaperPixel]]): IndexedSeq[Option[WallpaperPixel]] = {
      val patch = IndexedSeq.fill(cappedWidth)(Some(newPixel))
      row.patch(position.x, patch, cappedWidth)
}

    val patchedRows = pixels.drop(position.y).take(cappedHeight).map(patchRow)
    val patchedPixels = pixels.patch(position.y, patchedRows, cappedHeight)

    Wallpaper(patchedPixels)
  }
}

object Wallpaper {
  def apply(width: Int, height: Int): Wallpaper = {
    val pixels = IndexedSeq.fill(height)(IndexedSeq.fill(width)(None))
    Wallpaper(pixels)
  }
  
  def apply(image: ResizedImage): Wallpaper = {
    val wallpaper = Wallpaper(image.width, image.height)
    wallpaper.insert(image, Position(0, 0))
  }
  
  def concatenateVertical(top: Wallpaper, bottom: Wallpaper): Wallpaper = {
    require(top.width == bottom.width)
    Wallpaper(top.pixels ++ bottom.pixels)
  }
  
  def concatenateHorizontal(left: Wallpaper, right: Wallpaper): Wallpaper = {
    require(left.height == right.height)
    Wallpaper(left.pixels.zip(right.pixels).map({case (l, r) => l ++ r}))
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

case class IntegralImage(val matrix: RealMatrix) {
  val integralImage = new Array2DRowRealMatrix(
    matrix.getRowDimension, 
    matrix.getColumnDimension)  

  def getElseZero(row: Int, column: Int): Double = {
    if (row < 0 || column < 0) 0
    else integralImage.getEntry(row, column)
  }

  for (
    row <- 0 until matrix.getRowDimension;
    column <- 0 until matrix.getColumnDimension) {
    val up = getElseZero(row - 1, column)
    val left = getElseZero(row, column - 1)
    val upLeft = getElseZero(row - 1, column - 1)
    val sumHere = up + left - upLeft + matrix.getEntry(row, column)
    integralImage.setEntry(row, column, sumHere)
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
