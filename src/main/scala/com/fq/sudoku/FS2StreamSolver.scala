package com.fq.sudoku

import cats.effect.{IO, Resource}
import cats.implicits.toTraverseOps
import com.fq.sudoku.Solver._
import fs2.Stream
import fs2.concurrent.Topic

object FS2StreamSolver extends Solver[IO] {
  override def solve(givens: List[Value.Given]): IO[List[Value]] =
    valuesStream(givens).compile.toList

  def valuesStream(givens: List[Value.Given]): Stream[IO, Value] =
    for {
      updatesTopic <- Stream.eval(Topic[IO, Value])
      givenCoords = givens.map(_.coord).toSet
      missingCoords = Coord.allCoords.filterNot(givenCoords.contains)
      givenValuesStream = Stream.emits(givens)
      missingValueStreamsResource = missingCoords.traverse(missingValueStreamResource(updatesTopic))
      missingValueStreams <- Stream.resource(missingValueStreamsResource)
      missingValuesStream = missingValueStreams.reduce(_ merge _)
      valuesStream = givenValuesStream ++ missingValuesStream
      publishedValuesStream = valuesStream.evalTap(updatesTopic.publish1)
      value <- publishedValuesStream
    } yield value

  def missingValueStreamResource(
      updatesTopic: Topic[IO, Value]
  )(coord: Coord): Resource[IO, Stream[IO, Candidate.Single]] =
    updatesTopic
      .subscribeAwait(81)
      .map { updatesStream =>
        updatesStream
          .filter(_.coord.isPeerOf(coord))
          .mapAccumulate[Candidate, Candidate](Candidate.initial(coord)) {
            case (multiple: Candidate.Multiple, peerValue) =>
              val nextCandidate = multiple.refine(peerValue)
              (nextCandidate, nextCandidate)
            case (single: Candidate.Single, _) => (single, single)
          }
          .collectFirst { case (_, single: Candidate.Single) => single }
      }
}
