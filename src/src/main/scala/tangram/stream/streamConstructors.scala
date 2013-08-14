package tangram.stream

import scala.collection.immutable.Stream.consWrapper

///////////////////////////////////////////////////////////

/**
 * A processor of PossibleImageURLs and PossibleImages that throws an exception
 * at the first sign of trouble.
 */
object PessimisticProcessor extends ProcessPossibleImageURLs with ProcessPossibleImages {
  override def apply(possibleImageURLs: PossibleImageURLs): ImageURLs =
    possibleImageURLs map {
      case Right(imageURL) => imageURL
      case Left(_) => throw new Error
    }

  override def apply(possibleImages: PossibleImages): Images =
    possibleImages map {
      case Right(image) => image
      case Left(_) => throw new Error
    }
}

/**
 * A processor of PossibleImageURLs and PossibleImages that retries failed
 * computations forever, waiting |waitTime| seconds between each attempt.
 */
case class OptimisticProcessor(waitTime: Seconds) extends ProcessPossibleImageURLs with ProcessPossibleImages {
  
}

///**
// * An image stream that might not always provide an image.
// */
//trait FailingImageStream extends ImageStream {
//  def failingImageStream: Stream[Option[Image]]
//
//  /////////////////////////////////////////////////////////
//
//  override def imageStream = failingImageStream.flatten
//}
//
///**
// * An image stream that sleeps then retries when it cannot get the next
// * image.
// */
//trait RetryingImageStream extends FailingImageStream {
//  def waitBetweenAttemptsInSeconds: Int
//
//  /**
//   * An image stream that does not sleep between retries.
//   */
//  // TODO: This can probably be turned into a mixin.
//  def noWaitingImageStream: Stream[Option[BufferedImage]]
//
//  /////////////////////////////////////////////////////////  
//
//  override def failingImageStream = noWaitingImageStream map {
//    // If the image was successfully retrieved, we insert no
//    // delay.
//    case Some(image) => Some(image)
//
//    // If the image was not retrieved, we sleep before returning None.
//    case None => {
//      println(s"No new images in stream, sleeping ${waitBetweenAttemptsInSeconds}s")
//      Thread.sleep(waitBetweenAttemptsInSeconds * 1000)
//      None
//    }
//  }
//}

