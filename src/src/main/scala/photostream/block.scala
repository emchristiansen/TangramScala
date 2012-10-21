package photostream

import java.awt.image.BufferedImage
import math._
import Run.UpdateWallpaper

///////////////////////////////////////////////////////////

case class Global(minSize: Int, maxAspectWarp: Double, borderWidth: Int) {
  require(minSize > 0)
  require(maxAspectWarp >= 1)
  require(borderWidth >= 0)
}

///////////////////////////////////////////////////////////

sealed trait Block {
  //  val legalSizes: Set[RectangleSize]

  // The ranges are exclusive.
  val legalSizesByWidth: Map[Int, Range]
  val legalSizesByHeight: Map[Int, Range]
}

case class BlockLeaf(image: BufferedImage)(implicit global: Global) extends Block {
  import RectangleSize._

  override val (legalSizesByWidth, legalSizesByHeight) = {
    val legalSizes = for (
      width <- 1 to image.getWidth;
      height <- 1 to image.getHeight;
      if max(width, height) >= global.minSize;
      originalAspect = RectangleSize(image.getWidth, image.getHeight).aspect;
      newSize = RectangleSize(width, height);
      newAspect = newSize.aspect;
      if newAspect <= global.maxAspectWarp * originalAspect;
      if newAspect >= 1 / global.maxAspectWarp * originalAspect;
      padding = RectangleSize(2 * global.borderWidth, 2 * global.borderWidth)
    ) yield newSize + padding

    val legalSizesByWidth =
      legalSizes.groupBy(_.width).mapValues(_.map(_.height)).mapValues(heights =>
        Range(heights.min, heights.max + 1))
    val legalSizesByHeight =
      legalSizes.groupBy(_.height).mapValues(_.map(_.width)).mapValues(widths =>
        Range(widths.min, widths.max + 1))

    (legalSizesByWidth, legalSizesByHeight)
  }
}

// TODO: Bug
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
//        println("a", legalSizesByWidth)
        (legalSizesByWidth, switchDomain(legalSizesByWidth))
      }
      case HorizontalSplit => {
        val legalSizesByHeight = helper(first.legalSizesByHeight, second.legalSizesByHeight)
//        println("b", legalSizesByHeight)
        (switchDomain(legalSizesByHeight), legalSizesByHeight)
      }
    }
  }

  //  override val legalSizes = {
  //    def group(
  //      matchingField: RectangleSize => Int,
  //      otherField: RectangleSize => Int,
  //      constructor: (Int, Int) => RectangleSize) = {
  //      val first = leftOrTop.legalSizes.groupBy(matchingField)
  //      val second = rightOrBottom.legalSizes.groupBy(matchingField)
  //
  //      val matches = first.keys.toSet.intersect(second.keys.toSet)
  //
  //      for (
  //        matcher <- matches;
  //        firstOther <- first(matcher).map(otherField);
  //        secondOther <- second(matcher).map(otherField)
  //      ) yield constructor(matcher, firstOther + secondOther)
  //    }
  //
  //    split match {
  //      case VerticalSplit => group(
  //        (s: RectangleSize) => s.width,
  //        (s: RectangleSize) => s.height,
  //        (w: Int, h: Int) => RectangleSize(w, h))
  //
  //      case HorizontalSplit => group(
  //        (s: RectangleSize) => s.height,
  //        (s: RectangleSize) => s.width,
  //        (h: Int, w: Int) => RectangleSize(w, h))
  //    }
  //  }
}

///////////////////////////////////////////////////////////

object Block {
  def pairs[A](seq: Seq[A]): Seq[Tuple3[A, A, Seq[A]]] = {
    val indexed = seq.toIndexedSeq
    for (
      i <- 0 until indexed.size;
      j <- i + 1 until indexed.size
    ) yield {
      val left = indexed(i)
      val right = indexed(j)

      val remaining = indexed.slice(0, i) ++
        indexed.slice(i + 1, j) ++
        indexed.slice(j + 1, indexed.size)

      assert(remaining.size + 2 == seq.size)

      (left, right, remaining)
    }
  }

  def full: UpdateWallpaper = (wallpaper, images) => {
    val partitionSize = RectangleSize(wallpaper.width, wallpaper.height)
    //    val partitionSize = RectangleSize(1000, 500)

    implicit val global = Global(300, 1.1, 1)

    val lookahead = 20

    def helper(blocks: Seq[Block]): Option[Tuple2[Block, Seq[Block]]] = {
      println(blocks.size)

      def isSolution(block: Block) =
        block.legalSizesByWidth.contains(partitionSize.width) &&
          block.legalSizesByWidth(partitionSize.width).contains(partitionSize.height)

      val (init, tail) = blocks.span(block => !isSolution(block))

      if (!tail.isEmpty) Some((tail.head, init ++ tail.tail))
      else if (blocks.size < 2) None
      else {
        val candidates = pairs(blocks)

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
            //            println(block.legalSizesByHeight.keys.toList.sorted)
            if (!canFitInPartition(block)) None
            else {
              helper(Seq(block) ++ remaining) match {
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

    helper(images.take(lookahead).toList.map(i => BlockLeaf(i.image))) match {
      case None => sys.error("No solution found")
      case Some((solution, remainingBlocks)) => {
        println(solution)
        sys.error("TODO")
      }
    }
  }
}