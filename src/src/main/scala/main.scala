import java.io.File

import java.awt.image._
import java.awt._
import javax.imageio.ImageIO

case class Position(val x: Int, val y: Int, val width: Int, val height: Int) {
  require(x >= 0)
  require(y >= 0)
  require(width > 0)
  require(height > 0)
}

case class WallpaperPixel(image: BufferedImage, position: Position, insertionTime: Int) {
  require(image.getWidth > 0)
  require(image.getHeight > 0)

  // Resize |image| so it has the size given by |position|.
  def render: BufferedImage = {
    val newImage = new BufferedImage(
      position.width, 
      position.height, 
      BufferedImage.TYPE_INT_ARGB)

    // TODO: I suspect this may not work.
    val graphics = newImage.createGraphics
    graphics.drawImage(image, 0, 0, position.width, position.height, null)
    newImage
  }
}

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

    // Pixels which are located at the upper left-hand corner of their images.
    // There is one for each image to splat.
    val originPixels = for (
      (row, y) <- pixels.zipWithIndex;
      (Some(pixel), x) <- row.zipWithIndex;
      if (pixel.position.x == x && pixel.position.y == y)
    ) yield pixel
      
    // Splat the images into the wallpaper in the order of insertion.
    val wallpaper = new BufferedImage(
      Display.wallpaperWidth, 
      Display.wallpaperHeight, 
      BufferedImage.TYPE_INT_ARGB)
    val graphics = wallpaper.createGraphics
    for (pixel <- originPixels.sortBy(_.insertionTime))
      graphics.drawImage(pixel.render, null, pixel.position.x, pixel.position.y)

    wallpaper
  }

  def insert(image: BufferedImage, position: Position): Wallpaper = {
    val newPixel = WallpaperPixel(image, position, lastInsertionTime + 1)

    val cappedWidth = position.width.min(width - position.x)
    val cappedHeight = position.height.min(height - position.y)

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
    //refreshWallpaper
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
    val imageDirectory = new File("/u/echristiansen/Dropbox/2012Summer/photo_stream/")
    assert(imageDirectory.exists)
    val files = imageDirectory.listFiles.filter(isImageFile)
    files.toStream.map(UnusedImage.apply)
  }
}

object Render {
  def updateWallpaper(
    wallpaper: Wallpaper, 
    images: Stream[UnusedImage]): Tuple2[Wallpaper, Stream[UnusedImage]] = {

    println("here")

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
	  headImage, 
	  Position(x, y, headImage.getWidth, headImage.getHeight))
	updateWallpaper(newWallpaper, tailImages)
      }
      case None => (wallpaper, images)
    }
  }
}

object Main extends App {
  val unusedImages = PhotoStream.getImages

  val wallpaper = Wallpaper(Display.wallpaperWidth, Display.wallpaperHeight)
  val (newWallpaper, _) = Render.updateWallpaper(wallpaper, unusedImages)

  Display.setWallpaper(newWallpaper.render)

  // @tailrec
  // def updateWallpaper(wallpaper: Wallpaper, unusedImages: Stream[UnusedImage]) {
    
  // }

//  println(PhotoStream.getImages.toList)
  // println(Toolkit.getDefaultToolkit.getScreenSize.width)
  // println(PhotoStream.getImages.head.getType)
  // println(BufferedImage.TYPE_INT_ARGB)
  // val wallpaper = Render.mkWallpaper(PhotoStream.getImages)
  // Display.setWallpaper(wallpaper)
}
