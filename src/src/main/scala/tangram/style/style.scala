package tangram.style

import tangram.style.Wallpaper

///////////////////////////////////////////////////////////

/**
 * Takes a Wallpaper and an ImageStream and returns an updated Wallpaper
 * and ImageStream.
 */
trait DisplayStyle[R, I] {
  def updateWallpaper: DisplayStyle.UpdateWallpaper[R, I]
}

object DisplayStyle {
  /**
   * A mapping from a Wallpaper and ImageStream to a Wallpaper and ImageStream.
   */
  type UpdateWallpaper[R, I] = (Wallpaper[R], I) => (Wallpaper[R], I)
}