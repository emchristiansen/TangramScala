package tangram

import java.awt.image.BufferedImage
import scala.collection.immutable.Stream.consWrapper
import tangram.stream.ImageFinder
import tangram.stream.ImageURL
import tangram.ImageURL.ImageURL2DirectFetcher
import tangram.stream.WebCache

///////////////////////////////////////////////////////////

/**
 * Holds the caching data for an ImageFinder, allowing it to be viewed as an
 * ImageStream.
 */
case class CachedImageStream[I <% ImageFinder](
  imageFeed: I,
  webCache: WebCache,
  oldImages: Set[ImageURL])

object CachedImageStream {
  /**
   * Creates a CachedImageStream with an empty cache.
   */
  def apply[I <% ImageFinder](imageFinder: I): CachedImageStream[I] =
    CachedImageStream(imageFinder, WebCache(), Set[ImageURL]())
  
  implicit class CachedImageStream2RetryingImageStream[I <% ImageFinder](
    self: CachedImageStream[I]) extends RetryingImageStream {
    override def waitBetweenAttemptsInSeconds = 60

    override def noWaitingImageStream: Stream[Option[BufferedImage]] =
      self.imageFeed.getImageURLs(self.webCache) match {
        // We couldn't fetch any image URLs.
        case (Nil, newWebCache) =>
          None #:: self.copy(webCache = newWebCache).noWaitingImageStream
        // We successfully fetched new image URLS.
        case (imageURLs, newWebCache) =>
          imageURLs.filterNot({
            // Remove all previously seen image URLs.
            self.oldImages
          }).headOption match {
            // We have seen all these image URLs.
            case None =>
              None #:: self.copy(webCache = newWebCache).noWaitingImageStream
            // There's a new URL.
            case Some(imageURL) => {
              // We mark this url as seen regardless of whether we can 
              // download the image.
              val newOldImages = self.oldImages + imageURL

              // We don't cache the downloaded image, since we should never
              // download the same image twice.
              def streamTail = self.copy(
                webCache = newWebCache,
                oldImages = newOldImages).noWaitingImageStream
              imageURL.fetch match {
                // We couldn't download the image.
                case None => None #:: streamTail
                // We did download the image.	
                case someImage => someImage #:: streamTail
              }
            }
          }
      }
  }
}