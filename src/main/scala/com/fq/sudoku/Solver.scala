package com.fq.sudoku

import com.fq.sudoku.Solver.Value

trait Solver[F[_]] {
  def solve(givens: List[Value.Given]): F[List[Value]]
}

object Solver {
  case class Coord(row: Int, col: Int) {
    def isPeerOf(that: Coord): Boolean =
      (inSameRowAs(that) || inSameColAs(that) || inSameBoxAs(that)) && notThis(that)
    private def notThis(that: Coord): Boolean = this != that
    private def inSameRowAs(that: Coord): Boolean = this.row == that.row
    private def inSameColAs(that: Coord): Boolean = this.col == that.col
    private def inSameBoxAs(that: Coord): Boolean =
      (this.row / 3) == (that.row / 3) && (this.col / 3) == (that.col / 3)
  }

  object Coord {
    val rowIndices: List[Int] = (0 to 8).toList
    val colIndices: List[Int] = (0 to 8).toList

    val allCoords: List[Coord] = for {
      row <- rowIndices
      col <- colIndices
    } yield Coord(row, col)
  }

  /** Value -----> Given (can be constructed by user)
    *          \
    * Candidate ----> Single (cannot be constructed, only refined to from Multiple)
    *          \
    *           ----> Multiple (cannot be constructed except as initial)
    */
  sealed trait Value {
    val coord: Coord
    val value: Int
  }

  sealed trait Candidate {
    val coord: Coord
  }

  object Value {
    case class Given(coord: Coord, value: Int) extends Value
  }

  object Candidate {
    class Single private[Candidate] (override val coord: Coord, override val value: Int)
        extends Value
        with Candidate

    class Multiple private[Candidate] (override val coord: Coord, candidates: Set[Int])
        extends Candidate {
      def refine(peerValue: Value): Candidate = {
        val newValues =
          candidates -- Option.when(coord.isPeerOf(peerValue.coord))(peerValue.value)
        newValues.toList match {
          case Nil                    => throw new IllegalStateException() // unreachable
          case singleCandidate :: Nil => new Single(coord, singleCandidate)
          case multipleCandidates     => new Multiple(coord, multipleCandidates.toSet)
        }
      }
    }

    def initial(coord: Coord): Multiple = new Multiple(coord, (1 to 9).toSet)
  }

  def toString(list: List[Value]): String = {
    val board: Map[Coord, Int] = list.map(cs => cs.coord -> cs.value).toMap

    val newLine = "\n"
    val rowSep = s"+-------+-------+-------+"

    Coord.rowIndices
      .map(row =>
        Coord.colIndices
          .map(col =>
            board.get(Coord(row, col)) match {
              case None        => "_"
              case Some(value) => value
            }
          )
          .grouped(3)
          .map(_.mkString(" "))
          .toList
          .mkString("| ", " | ", " |")
      )
      .grouped(3)
      .map(_.grouped(3).toList.map(_.mkString(newLine)))
      .toList
      .map(_.mkString + s"$newLine$rowSep")
      .mkString(s"$rowSep$newLine", newLine, "")
  }
}
