package tangram.styles

import tangram._
import java.awt.image.BufferedImage
import math._
import java.awt.Color
import util._
import scala.actors.Futures._
import tangram.BorderedResizedImage
import tangram.Constraints
import tangram.RectangleSize
import tangram.RectangleSize.implicitRectangleLike
import tangram.RectangleSize.implicitSemiVectorSpace

import scala.Option.option2Iterable
import RectangleSize._

///////////////////////////////////////////////////////////

object BlockStyle extends DisplayStyle {
  def extractPair[A](
    indexed: IndexedSeq[A],
    firstIndex: Int,
    secondIndex: Int): Tuple3[A, A, IndexedSeq[A]] = {
    require(firstIndex < secondIndex)

    val left = indexed(firstIndex)
    val right = indexed(secondIndex)

    val remaining = indexed.slice(0, firstIndex) ++
      indexed.slice(firstIndex + 1, secondIndex) ++
      indexed.slice(secondIndex + 1, indexed.size)

    assert(remaining.size + 2 == indexed.size)

    (left, right, remaining)
  }

  def pairs[A](indexed: IndexedSeq[A]): IndexedSeq[Tuple3[A, A, IndexedSeq[A]]] = {
    for (
      i <- 0 until indexed.size;
      j <- i + 1 until indexed.size
    ) yield {
      extractPair(indexed, i, j)
    }
  }

  def randomPair[A](indexed: IndexedSeq[A]): Tuple3[A, A, IndexedSeq[A]] = {
    require(indexed.size >= 2)

    val List(firstIndex, secondIndex) =
      (new Random).shuffle((0 until indexed.size).toList).take(2).sorted
    extractPair(indexed, firstIndex, secondIndex)
  }

  def dfsStrategy(blocks: IndexedSeq[BlockTree])(
    implicit constraints: Constraints): Option[Tuple2[BlockTree, IndexedSeq[BlockTree]]] = {
    def isSolution(block: BlockTree) =
      block.legalSizesByWidth.contains(constraints.wallpaperSize.width) &&
        block.legalSizesByWidth(constraints.wallpaperSize.width).contains(constraints.wallpaperSize.height)

    val (init, tail) = blocks.span(block => !isSolution(block))

    if (!tail.isEmpty) Some((tail.head, init ++ tail.tail))
    else if (blocks.size < 2) None
    else {
      val candidates = (new Random).shuffle(pairs(blocks))

      val solutions = for ((first, second, remaining) <- candidates.toStream) yield {
        def canFitInPartition(block: BlockTree) = {
          val canFitForWidth = for (
            (width, heights) <- block.legalSizesByWidth;
            if width <= constraints.wallpaperSize.width
          ) yield heights.start <= constraints.wallpaperSize.height

          canFitForWidth.find(_ == true).isDefined
        }

        def solution(split: Split) = {
          val block = BlockNode(first, second, split)
          if (!canFitInPartition(block)) None
          else {
            dfsStrategy(IndexedSeq(block) ++ remaining) match {
              case solution @ Some(_) => solution
              case None => None
            }
          }
        }

        lazy val verticalSolution = solution(VerticalSplit)
        lazy val horizontalSolution = solution(HorizontalSplit)

        val favorVertical = (new util.Random).nextBoolean
        if (favorVertical && verticalSolution.isDefined) verticalSolution
        else if ((!favorVertical) && horizontalSolution.isDefined) horizontalSolution
        else if (verticalSolution.isDefined) verticalSolution
        else if (horizontalSolution.isDefined) horizontalSolution
        else None
      }

      solutions.dropWhile(!_.isDefined).headOption match {
        case Some(solution) => solution
        case None => None
      }
    }
  }

  def dfsWrapper(
    images: IndexedSeq[BufferedImage])(
      implicit constraintsStream: Stream[Constraints]): Option[Tuple2[BlockTree, IndexedSeq[BlockTree]]] = {
    //    def runWithTimeout[T](timeoutMs: Long)(f: => T): Option[T] = {
    //      awaitAll(timeoutMs, future(f)).head.asInstanceOf[Option[T]]
    //    }

    val timeoutMS = 5 * 1000
    val maxAttempts = 8
    // TODO: Specifying parallelism manually is stupid.
    // TODO: There's a bug where setting higher parallelism creates threads
    // that possibly run forever. My guess: if you never demand the result
    // of |runWithTimeout|, the thread may never be killed.
    val parallelism = 1

    val attempts = for ((constraints, index) <- constraintsStream.zipWithIndex.take(maxAttempts)) yield {
      println("Attempt %d".format(index))
      val blocks = images.map(image => BlockLeaf(image))
      val futures = (0 until parallelism).map(_ => future(dfsStrategy(blocks)(constraints)))
      // This |flatten| removes the attempts that timed out.
      val possibleSolutions =
        awaitAll(timeoutMS, futures: _*).flatten.map(_.asInstanceOf[Option[Tuple2[BlockTree, IndexedSeq[BlockTree]]]])
      //      val possibleSolutions = (0 until parallelism).par.map(_ => runWithTimeout(timeoutMS)(
      //        dfsStrategy(blocks)(constraints))).toIndexedSeq.flatten
      // This |flatten| removes the attempts that finished in time but failed.
      // We take the first solution if it exists.
      possibleSolutions.flatten.headOption
    }

    (attempts.zipWithIndex.dropWhile(_._1 == None).headOption: @unchecked) match {
      case Some((solution @ Some(_), index)) => {
        println("Found a solution on attempt %d".format(index))
        solution
      }
      case None => {
        println("No solution found after %d attempts".format(maxAttempts))
        None
      }
    }
  }

  // Build a Block by making random legal decisions. If we find a solution, 
  // return it. Otherwise try again.
  def rolloutStrategy(
    blocks: IndexedSeq[BlockTree],
    partitionSize: RectangleSize): Option[Tuple2[BlockTree, IndexedSeq[BlockTree]]] = {
    println(blocks.size)

    def isSolution(block: BlockTree) =
      block.legalSizesByWidth.contains(partitionSize.width) &&
        block.legalSizesByWidth(partitionSize.width).contains(partitionSize.height)

    val (init, tail) = blocks.span(block => !isSolution(block))

    if (!tail.isEmpty) Some((tail.head, init ++ tail.tail))
    else if (blocks.size < 2) None
    else {
      val (first, second, remaining) = randomPair(blocks)

      def canFitInPartition(block: BlockTree) = {
        val canFitForWidth = for (
          (width, heights) <- block.legalSizesByWidth;
          if width <= partitionSize.width
        ) yield heights.start <= partitionSize.height

        canFitForWidth.find(_ == true).isDefined
      }

      def solution(split: Split) = {
        val block = BlockNode(first, second, split)
        if (!canFitInPartition(block)) None
        else {
          rolloutStrategy(IndexedSeq(block) ++ remaining, partitionSize) match {
            case solution @ Some(_) => solution
            case None => None
          }
        }
      }

      if ((new Random).nextBoolean) solution(VerticalSplit)
      else solution(HorizontalSplit)
    }
  }

  def rolloutHelper(
    blocks: IndexedSeq[BlockTree],
    partitionSize: RectangleSize): Option[Tuple2[BlockTree, IndexedSeq[BlockTree]]] = {
    val maxAttempts = 512
    // TODO: Specifying parallelism manually is stupid.
    val parallelism = 8

    // Note this is evaluated lazily.
    val attempts = for (_ <- (0 until maxAttempts).toStream) yield {
      val possibleSolutions = (0 until parallelism).par.map(_ => rolloutStrategy(blocks, partitionSize))
      // Drop the failed attempts and take the first solution if it exists.
      possibleSolutions.toIndexedSeq.flatten.headOption
    }

    (attempts.zipWithIndex.dropWhile(_._1 == None).headOption: @unchecked) match {
      case Some((solution @ Some(_), index)) => {
        println("Found a solution on attempt %d".format(index))
        solution
      }
      case None => {
        println("No solution found after %d attempts".format(maxAttempts))
        None
      }
    }
  }

  def updateWallpaper = (wallpaper, images) => {
    val partitionSize = RectangleSize(wallpaper.width, wallpaper.height)

    implicit val constraintsStream = Constraints(
      300,
      0.75,
      1.05,
      partitionSize,
      ImageBorder(1, Color.WHITE)).relaxations

    val lookahead = 20

    //    dfsWrapper(images.take(lookahead).map(i => Blockeaf(i.image)).toIndexedSeq, partitionSize) match {
    dfsWrapper(images.take(lookahead).map(i => i.image).toIndexedSeq) match {
      case None => sys.error("No solution found")
      case Some((solution, remainingBlocks)) => {
        val splitTree = solution.splitTree(partitionSize)
        val wallpaper = splitTree.wallpaper

        // TODO: This loses the num misses information for the UnusedImages.
        val unusedImages = remainingBlocks.flatMap(_.images.map(x => UnusedImage(x, 0)))

        (wallpaper, unusedImages.toStream ++ images.drop(lookahead))
      }
    }
  }
}