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
  val legalSizes: Set[RectangleSize]
}

case class BlockLeaf(image: BufferedImage)(implicit global: Global) extends Block {
  import RectangleSize._

  override val legalSizes = (for (
    width <- 1 to image.getWidth;
    height <- 1 to image.getHeight;
    if max(width, height) >= global.minSize;
    originalAspect = RectangleSize(image.getWidth, image.getHeight).aspect;
    newSize = RectangleSize(width, height);
    newAspect = newSize.aspect;
    if newAspect <= global.maxAspectWarp * originalAspect;
    if newAspect >= 1 / global.maxAspectWarp * originalAspect;
    padding = RectangleSize(global.borderWidth, global.borderWidth)
  ) yield newSize + padding).toSet
}

case class BlockNode(leftOrTop: Block, rightOrBottom: Block, split: Split) extends Block {
  override val legalSizes = {
    def group(
      matchingField: RectangleSize => Int,
      otherField: RectangleSize => Int,
      constructor: (Int, Int) => RectangleSize) = {
      val first = leftOrTop.legalSizes.groupBy(matchingField)
      val second = rightOrBottom.legalSizes.groupBy(matchingField)

      val matches = first.keys.toSet.intersect(second.keys.toSet)

      for (
        matcher <- matches;
        firstOther <- first(matcher).map(otherField);
        secondOther <- second(matcher).map(otherField)
      ) yield constructor(matcher, firstOther + secondOther)
    }

    split match {
      case VerticalSplit => group(
        (s: RectangleSize) => s.width,
        (s: RectangleSize) => s.height,
        (w: Int, h: Int) => RectangleSize(w, h))

      case HorizontalSplit => group(
        (s: RectangleSize) => s.height,
        (s: RectangleSize) => s.width,
        (h: Int, w: Int) => RectangleSize(w, h))
    }
  }
}

///////////////////////////////////////////////////////////

object Block {
  def pairs[A](seq: Seq[A]): Seq[Tuple3[A, A, Seq[A]]] = {
    val indexed = seq.toIndexedSeq
    for (
      i <- 0 until indexed.size;
      j <- i + i until indexed.size
    ) yield {
      val left = indexed(i)
      val right = indexed(j)

      val remaining = indexed.slice(0, i) ++
        indexed.slice(i + 1, j) ++
        indexed.slice(j + 1, indexed.size)

      (left, right, remaining)
    }
  }

  def full: UpdateWallpaper = (wallpaper, images) => {
    val partitionSize = RectangleSize(wallpaper.width, wallpaper.height)

    implicit val global = Global(200, 1.3, 1)

    val lookahead = 20

    def helper(blocks: Seq[Block]): Option[Tuple2[Block, Seq[Block]]] = {
      blocks.map(_.legalSizes.size).foreach(println)

      val (init, tail) = blocks.span(!_.legalSizes.contains(partitionSize))

      if (!tail.isEmpty) Some((tail.head, init ++ tail.tail))
      else if (blocks.size < 2) None
      else {
        val candidates = pairs(blocks)

        val solutions = for ((first, second, remaining) <- candidates.toStream) yield {
          def solution(split: Split) = {
            val block = BlockNode(first, second, split)
            if (block.legalSizes.isEmpty) None
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