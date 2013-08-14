package tangram

import nebula.imageProcessing._
import tangram.stream.ImageURL

///////////////////////////////////////////////

package object stream {
  case class Seconds(seconds: Int) extends Box[Int] {
    override def get = seconds
  }
  
  /**
   * An error coupled with the computation that returned the error.
   * The computation can be retried in the hope it will eventually
   * succeed. This might model a web request that could sometimes fail for
   * no good reason.
   */
  type TryAgainError[A] = (StreamError, () => A)
  
  /**
   * Either the result of a successful computation or the TryAgainError
   * or that computation.
   */
  type TryAgain[A] = Either[TryAgainError[A], A]
  
  /**
   * Either an ImageURL or an Error.
   */
  type PossibleImageURL = TryAgain[ImageURL]
  
  type PossibleImageURLs = Stream[PossibleImageURL]
  
  type ImageURLs = Stream[ImageURL]
  
  type ProcessPossibleImageURLs = PossibleImageURLs => ImageURLs
  
  ////////////////////////////////////
  
  type PossibleImage = TryAgain[Image]
  
  type PossibleImages = Stream[PossibleImage]
  
  type PossiblyFetchImages = ImageURLs => PossibleImages
  
  ////////////////////////////////////
  
  type Images = Stream[Image]
  
  type ProcessPossibleImages = PossibleImages => Images
}