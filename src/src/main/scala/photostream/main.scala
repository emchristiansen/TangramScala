package photostream

object Main extends App {
  val unusedImages = PhotoStream.getImages

  val wallpaper = Wallpaper(Display.wallpaperWidth, Display.wallpaperHeight)

  //  Run.updateRunner(4000, RandomStrategy.single, wallpaper, unusedImages)

  Run.updateRunner(40000, Block.full, wallpaper, unusedImages)

  //  Block.pairs(List(1, 2, 3, 4)).foreach(println)
}
