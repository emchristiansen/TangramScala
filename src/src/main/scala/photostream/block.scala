package photostream

import java.awt.image.BufferedImage
import math._
import Run.UpdateWallpaper
import java.awt.Color
import util._
import scala.actors.Futures._

///////////////////////////////////////////////////////////

sealed trait Block {
  // The ranges are exclusive.
  val legalSizesByWidth: Map[Int, Range]
  val legalSizesByHeight: Map[Int, Range]

  def splitTree(size: RectangleSize): SplitTree

  // TODO: Move this crap to a pimp pattern.
  def images: Seq[BufferedImage]
}

case class BlockLeaf(image: BufferedImage)(implicit constraints: Constraints) extends Block {
  import RectangleSize._

  override val (legalSizesByWidth, legalSizesByHeight) = {
    val legalSizes = for (
      width <- (image.getWidth * constraints.minRelativeSize).round.toInt to image.getWidth;
      height <- (image.getHeight * constraints.minRelativeSize).round.toInt to image.getHeight;
      if max(width, height) >= constraints.minAbsoluteSize;
      originalAspect = RectangleSize(image.getWidth, image.getHeight).aspect;
      newSize = RectangleSize(width, height);
      newAspect = newSize.aspect;
      if newAspect <= constraints.maxAspectWarp * originalAspect;
      if newAspect >= 1 / constraints.maxAspectWarp * originalAspect;
      padding = RectangleSize(2 * constraints.border.width, 2 * constraints.border.width)
    ) yield newSize + padding

    val legalSizesByWidth =
      legalSizes.groupBy(_.width).mapValues(_.map(_.height)).mapValues(heights =>
        Range(heights.min, heights.max + 1))
    val legalSizesByHeight =
      legalSizes.groupBy(_.height).mapValues(_.map(_.width)).mapValues(widths =>
        Range(widths.min, widths.max + 1))

    (legalSizesByWidth, legalSizesByHeight)
  }

  override def splitTree(size: RectangleSize) = {
    require(legalSizesByWidth(size.width).contains(size.height))

    SplitLeaf(BorderedResizedImage.resizeToFit(constraints.border, size, image))
  }

  override def images = Seq(image)
}

case class BlockNode(first: Block, second: Block, split: Split) extends Block {
  override val (legalSizesByWidth, legalSizesByHeight) = {
    def helper(firstSizes: Map[Int, Range], secondSizes: Map[Int, Range]): Map[Int, Range] = {
      val matches =
        firstSizes.keys.toSet.intersect(secondSizes.keys.toSet)
      val newRanges = for (matcher <- matches) yield {
        val firstRange = firstSizes(matcher)
        val secondRange = secondSizes(matcher)
        val newRange = Range(
          firstRange.start + secondRange.start,
          firstRange.end + secondRange.end - 1)
        (matcher, newRange)
      }
      newRanges.toMap
    }

    def switchDomain(sizes: Map[Int, Range]): Map[Int, Range] =
      if (sizes.isEmpty) Map[Int, Range]()
      else {
        val newDomain = Range(
          sizes.values.map(_.start).min,
          sizes.values.map(_.end).max)
        (for (x <- newDomain) yield {
          val matches = sizes.filter(_._2.contains(x))
          (x, Range(matches.map(_._1).min, matches.map(_._1).max + 1))
        }).toMap
      }

    split match {
      case VerticalSplit => {
        val legalSizesByWidth = helper(first.legalSizesByWidth, second.legalSizesByWidth)
        (legalSizesByWidth, switchDomain(legalSizesByWidth))
      }
      case HorizontalSplit => {
        val legalSizesByHeight = helper(first.legalSizesByHeight, second.legalSizesByHeight)
        (switchDomain(legalSizesByHeight), legalSizesByHeight)
      }
    }
  }

  override def splitTree(size: RectangleSize) = {
    require(legalSizesByWidth(size.width).contains(size.height))

    // Select two ints so that they fall into their respective ranges and sum to |sum|.
    // They are selected to be proportionally the same distance away from the centers
    // of their ranges (ideally the ints come from the centers of the ranges).
    def selectSizes(firstRange: Range, secondRange: Range, sum: Int): Tuple2[Int, Int] = {
      val firstBottom = firstRange.start
      val firstTop = firstRange.end - 1
      val secondBottom = secondRange.start
      val secondTop = secondRange.end - 1

      val alpha = (sum - firstBottom - secondBottom).toDouble /
        (firstTop + secondTop - firstBottom - secondBottom)

      val (first, second) = {
        val first = (alpha * firstTop + (1 - alpha) * firstBottom).round.toInt
        val second = (alpha * secondTop + (1 - alpha) * secondBottom).round.toInt

        // They both got rounded up.
        if (first + second == sum + 1) (first - 1, second)
        else (first, second)
      }

      //      assert(first + second == sum, List(firstRange, secondRange, sum, alpha, first, second).mkString(", "))
      assert(first + second == sum)
      assert(firstRange.contains(first))
      assert(secondRange.contains(second))

      (first, second)
    }

    split match {
      case VerticalSplit => {
        // The width of each tree is set, but the heights may vary.
        val topHeights = first.legalSizesByWidth(size.width)
        val bottomHeights = second.legalSizesByWidth(size.width)

        // We select a topHeight and a bottomHeight so their sum is |size.height|.
        val (topHeight, bottomHeight) = selectSizes(topHeights, bottomHeights, size.height)
        SplitNode(
          first.splitTree(RectangleSize(size.width, topHeight)),
          second.splitTree(RectangleSize(size.width, bottomHeight)),
          VerticalSplit)
      }
      case HorizontalSplit => {
        // The height of each tree is set, but the widths may vary.  
        val leftWidths = first.legalSizesByHeight(size.height)
        val rightWidths = second.legalSizesByHeight(size.height)

        val (leftWidth, rightWidth) = selectSizes(leftWidths, rightWidths, size.width)
        SplitNode(
          first.splitTree(RectangleSize(leftWidth, size.height)),
          second.splitTree(RectangleSize(rightWidth, size.height)),
          HorizontalSplit)
      }
    }
  }

  override def images = first.images ++ second.images
}

///////////////////////////////////////////////////////////

object Block {
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

    val IndexedSeq(firstIndex, secondIndex) =
      (new Random).shuffle(0 until indexed.size).take(2).sorted
    extractPair(indexed, firstIndex, secondIndex)
  }

  def dfsStrategy(blocks: IndexedSeq[Block], partitionSize: RectangleSize): Option[Tuple2[Block, IndexedSeq[Block]]] = {
    println(blocks.size)

    def isSolution(block: Block) =
      block.legalSizesByWidth.contains(partitionSize.width) &&
        block.legalSizesByWidth(partitionSize.width).contains(partitionSize.height)

    val (init, tail) = blocks.span(block => !isSolution(block))

    if (!tail.isEmpty) Some((tail.head, init ++ tail.tail))
    else if (blocks.size < 2) None
    else {
      val candidates = (new Random).shuffle(pairs(blocks))

      val solutions = for ((first, second, remaining) <- candidates.toStream) yield {
        def canFitInPartition(block: Block) = {
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
            dfsStrategy(IndexedSeq(block) ++ remaining, partitionSize) match {
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

  def dfsWrapper(blocks: IndexedSeq[Block], partitionSize: RectangleSize): Option[Tuple2[Block, IndexedSeq[Block]]] = {
    def runWithTimeout[T](timeoutMs: Long)(f: => T): Option[T] = {
      awaitAll(timeoutMs, future(f)).head.asInstanceOf[Option[T]]
    }

    val timeoutMS = 5 * 1000
    val maxAttempts = 8

    // TODO: Specifying parallelism manually is stupid.
    val parallelism = 2    
    
    val attempts = for (_ <- (0 until maxAttempts).toStream) yield {
      // This |flatten| removes the attempts that timed out.
      val possibleSolutions = (0 until parallelism).par.map(_ => runWithTimeout(timeoutMS)(
        dfsStrategy(blocks, partitionSize))).flatten.toIndexedSeq
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
    blocks: IndexedSeq[Block],
    partitionSize: RectangleSize): Option[Tuple2[Block, IndexedSeq[Block]]] = {
    println(blocks.size)

    def isSolution(block: Block) =
      block.legalSizesByWidth.contains(partitionSize.width) &&
        block.legalSizesByWidth(partitionSize.width).contains(partitionSize.height)

    val (init, tail) = blocks.span(block => !isSolution(block))

    if (!tail.isEmpty) Some((tail.head, init ++ tail.tail))
    else if (blocks.size < 2) None
    else {
      val (first, second, remaining) = randomPair(blocks)

      def canFitInPartition(block: Block) = {
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
    blocks: IndexedSeq[Block],
    partitionSize: RectangleSize): Option[Tuple2[Block, IndexedSeq[Block]]] = {
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

  def full: UpdateWallpaper = (wallpaper, images) => {
    val partitionSize = RectangleSize(wallpaper.width, wallpaper.height)
    //    val partitionSize = RectangleSize(1000, 500)

    implicit val global = Constraints(300, 0.5, 1.1, ImageBorder(1, Color.WHITE))

    val lookahead = 20

    dfsWrapper(images.take(lookahead).map(i => BlockLeaf(i.image)).toIndexedSeq, partitionSize) match {
      case None => sys.error("No solution found")
      case Some((solution, remainingBlocks)) => {
        println(solution)
        val splitTree = solution.splitTree(partitionSize)
        val wallpaper = splitTree.wallpaper

        // TODO: This loses the num misses information for the UnusedImages.
        val unusedImages = remainingBlocks.flatMap(_.images.map(x => UnusedImage(x, 0)))

        (wallpaper, unusedImages.toStream ++ images.drop(lookahead))
      }
    }
  }
}