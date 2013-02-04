package tangram.styles

import java.awt.image.BufferedImage

import scala.Predef.Map.apply
import scala.Range.apply
import scala.collection.Seq.apply
import scala.math.max

import SplitLeaf.apply
import SplitNode.apply
import photostream.{BorderedResizedImage, Constraints, RectangleSize}
import photostream.RectangleSize.{apply, implicitRectangleLike, implicitSemiVectorSpace}

///////////////////////////////////////////////////////////

sealed trait BlockTree

case class BlockLeaf(image: BufferedImage) extends BlockTree

case class BlockNode(
  first: BlockTree,
  second: BlockTree,
  split: Split) extends BlockTree

///////////////////////////////////////////////////////////

trait RenderableTree {
  // The ranges are exclusive.
  val legalSizesByWidth: Map[Int, Range]
  val legalSizesByHeight: Map[Int, Range]

  def splitTree(size: RectangleSize): SplitTree

  def images: Seq[BufferedImage]
}

object BlockTree {
  implicit def blockTree2RenderableTree(self: BlockTree): RenderableTree = self match {
    case self: BlockLeaf => self
    case self: BlockNode => self
  }
}

object BlockLeaf {
  implicit def blockLeaf2RenderableTree(self: BlockLeaf)(implicit constraints: Constraints) = new RenderableTree {
    import self._

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
}

object BlockNode {
  implicit def blockNode2RenderableTree(self: BlockNode)(implicit constraints: Constraints) = new RenderableTree {
    import self._

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
}