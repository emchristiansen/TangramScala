package tangram

import java.awt.image.BufferedImage
import java.net.URL
import java.util.Date
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.imageio.ImageIO
import java.net.SocketTimeoutException
import shapeless._
import scala.util.Try

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

trait DirectFetcher[V] {
  def fetch: Option[V]
}

/**
 * A URL that points to a document.
 */
case class DocumentURL(url: URL)

object DocumentURL {
  /**
   * Default method for downloading a document without using the cache.
   */
  implicit class DocumentURLDirectFetcher(url: DocumentURL) extends DirectFetcher[Document] {
    override def fetch = Try(Jsoup.connect(url.url.toString).get).toOption
  }
}

/**
 * A URL that points to an image.
 */
case class ImageURL(url: URL)

object ImageURL {
  /**
   * Default method for downloading an image without using the cache.
   */
  implicit class ImageURL2DirectFetcher(url: ImageURL) extends DirectFetcher[BufferedImage] {
    override def fetch = Try(ImageIO.read(url.url)).toOption
  }
}

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
    
  /**
   * Adds delays in the evaluation of a Stream.
   */
  def addDelays[A](timeInMs: Int, stream: Stream[A]) = stream match {
    case head #:: tail => head #:: {
      Thread.sleep(timeInMs)
      tail
    }
    case Stream.Empty => Stream.Empty
  }

  implicit class WebCacheOps(self: WebCache) {
    /**
     * Attempts to fetch the document indicated by the URL, either from
     * local cache or from the web.
     * Returns the document, along with the updated web cache.
     */
    def get[K <% DirectFetcher[V], V](
      url: K)(
        implicit webCacheRelations: WebCacheRelations[K, CachedObject[V]]): Option[(V, WebCache)] = {
      self.cache.get(url) match {
        case Some(cached) if cached.isFresh => Some((cached.document, self))
        case _ => {
          println(s"Getting ${url}")

          def tryToDownload = url.fetch match {
            case some@Some(_) => some
            case None => 
          } 

          // Try to download the document 8 times, then give up.
          val documentOption: Option[V] =
            addDelays(1000, tryForever(() => url.fetch)).take(8).flatten.headOption

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