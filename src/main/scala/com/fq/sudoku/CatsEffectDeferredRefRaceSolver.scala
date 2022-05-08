package com.fq.sudoku

import cats.effect.{Deferred, IO, Ref}
import cats.implicits._
import com.fq.sudoku.Solver._

object CatsEffectDeferredRefRaceSolver extends Solver[IO] {
  override def solve(givens: List[Value.Given]): IO[List[Value]] =
    for {
      allCells <- Coord.allCoords.traverse(Cell.make)
      givensMap = givens.map(g => g.coord -> g).toMap
      values <- allCells.parTraverse(_.solve(givensMap, allCells))
    } yield values

  trait Cell {
    def coord: Coord
    protected[this] def deferredValue: Deferred[IO, Value]
    def getValue: IO[Value] = deferredValue.get
    def deduceSingleCandidate(allCells: List[Cell]): IO[Value]
    def solve(givensMap: Map[Coord, Value.Given], allCells: List[Cell]): IO[Value] =
      (givensMap.get(coord) match {
        case Some(givenValue) => IO.pure(givenValue)
        case None             => deduceSingleCandidate(allCells)
      }).flatTap(deferredValue.complete)
  }

  object Cell {
    def make(_coord: Coord): IO[Cell] =
      for {
        _deferredValue <- Deferred[IO, Value]
      } yield new Cell {
        override val coord: Coord = _coord

        override val deferredValue: Deferred[IO, Value] = _deferredValue

        override def deduceSingleCandidate(allCells: List[Cell]): IO[Candidate.Single] =
          for {
            refCandidate <- Ref.of[IO, Candidate](Candidate.initial(coord))
            peerCells = allCells.filter(cell => cell.coord.isPeerOf(coord))
            listOfSingleCandidateOrNever =
              peerCells.map(peerCell => refineToSingleCandidateOrNever(refCandidate, peerCell))
            singleCandidate <- raceMany(listOfSingleCandidateOrNever)
          } yield singleCandidate

        private def raceMany[T](listOfIOs: List[IO[T]]): IO[T] =
          listOfIOs.reduce((a, b) => a.race(b).map(_.merge))

        private def refineToSingleCandidateOrNever(
            refCandidate: Ref[IO, Candidate],
            peerCell: Cell
        ): IO[Candidate.Single] =
          for {
            peerValue <- peerCell.getValue
            singleCandidate <- refCandidate.modify {
              case multiple: Candidate.Multiple =>
                multiple.refine(peerValue) match {
                  case single: Candidate.Single     => (single, IO.pure(single))
                  case multiple: Candidate.Multiple => (multiple, IO.never)
                }
              case alreadySingle: Candidate.Single => (alreadySingle, IO.never)
            }.flatten
          } yield singleCandidate
      }
  }
}
