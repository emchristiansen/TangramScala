package tangram

import java.awt.image.BufferedImage
import java.net.URL
import java.util.Date
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.imageio.ImageIO
import java.net.SocketTimeoutException
import shapeless._

///////////////////////////////////////////////////////////

/**
 * Holds something downloaded and cached.
 */
case class CachedObject[A](document: A, date: Date)

object CachedObject {
  implicit class CachedObjectOps[A](self: CachedObject[A]) {
    val shelfLifeInSeconds = 60 * 60

    def isFresh: Boolean = {
      val now = new Date()
      (now.getTime - self.date.getTime) / 1000 < shelfLifeInSeconds
    }
  }
}

/**
 * A URL that points to a document.
 */
case class DocumentURL(url: URL)

/**
 * A URL that points to an image.
 */
case class ImageURL(url: URL)

/**
 * Specifies legal type relations for use in the type-safe |WebCache|, which
 * is backed by a shapeless HMap.
 */
class WebCacheRelations[K, V]

/**
 * This object specifies legal type relations for the WebCache.
 * Take a look at shapeless for similar code:
 * https://github.com/milessabin/shapeless
 */
object WebCacheRelations {
  /**
   * This says mappings from DocumentURL => CachedObject[Document]
   * are legal.
   */
  implicit val webCacheRelationDocumentURL2Document =
    new WebCacheRelations[DocumentURL, CachedObject[Document]]

  /**
   * This says mappings from ImageURL => CachedObject[BufferedImage]
   * are legal.
   */
  implicit val webCacheRelationImageURL2BufferedImage =
    new WebCacheRelations[ImageURL, CachedObject[BufferedImage]]
}

/**
 * Caches web content locally to avoid lots of web activity.
 * Pages eventually expire.
 */
case class WebCache(cache: HMap[WebCacheRelations])

object WebCache {
  /**
   * Expresses repeated attempts at evaluating a failure-prone function
   * as an infinite stream.
   */
  def tryForever[A](function: () => Option[A]): Stream[Option[A]] =
    function() #:: tryForever(function)

  implicit class WebCacheOps(self: WebCache) {
    /**
     * Default method for downloading a document without using the cache.
     * It's okay if this method fails.
     */
    implicit val getDocumentNoCache: DocumentURL => Document = url =>
      Jsoup.connect(url.url.toString).get
    
    /**
     * Default method for downloading an image without using the cache.
     * It's okay if this method fails.
     */
    implicit val getBufferedImageNoCache: ImageURL => BufferedImage = url =>
      ImageIO.read(url.url)    
      
    /**
     * Attempts to fetch the document indicated by the URL, either from
     * local cache or from the web.
     * Returns the document, along with the updated web cache.
     */
    def get[K, V](
      url: K)(
        implicit getter: K => V, 
        webCacheRelations: WebCacheRelations[K, CachedObject[V]]): Option[(V, WebCache)] = {
      self.cache.get(url) match {
        case Some(cached) if cached.isFresh => Some((cached.document, self))
        case _ => {
          println(s"Getting ${url}")

          def tryToDownload = try {
            Some(getter(url))
          } catch {
            case _: SocketTimeoutException => {
              // Sleep 1s then give up.
              Thread.sleep(1000)
              None
            }
          }

          // Try to download the document 8 times, then give up.
          val documentOption: Option[V] =
            tryForever(tryToDownload _).take(8).flatten.headOption

          for (document <- documentOption) yield {
            val cachedObject = CachedObject(document, new Date())
            val updatedCache = self.cache + (url -> cachedObject)
            (document, WebCache(updatedCache))
          }
        }
      }
    }
  }
}