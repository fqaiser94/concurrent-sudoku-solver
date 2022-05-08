package com.fq.sudoku

import com.fq.sudoku.Solver.Value
import com.fq.sudoku.Solver.Coord._
import munit.FunSuite

class UtilsTest extends FunSuite {

  test("boardToString") {
    assertEquals(
      Solver.toString(allCoords.map(Value.Given(_, 1))),
      """+-------+-------+-------+
        || 1 1 1 | 1 1 1 | 1 1 1 |
        || 1 1 1 | 1 1 1 | 1 1 1 |
        || 1 1 1 | 1 1 1 | 1 1 1 |
        |+-------+-------+-------+
        || 1 1 1 | 1 1 1 | 1 1 1 |
        || 1 1 1 | 1 1 1 | 1 1 1 |
        || 1 1 1 | 1 1 1 | 1 1 1 |
        |+-------+-------+-------+
        || 1 1 1 | 1 1 1 | 1 1 1 |
        || 1 1 1 | 1 1 1 | 1 1 1 |
        || 1 1 1 | 1 1 1 | 1 1 1 |
        |+-------+-------+-------+""".stripMargin
    )
  }

  test("boardToString can handle missing values") {
    assertEquals(
      Solver.toString(List.empty),
      """+-------+-------+-------+
        || _ _ _ | _ _ _ | _ _ _ |
        || _ _ _ | _ _ _ | _ _ _ |
        || _ _ _ | _ _ _ | _ _ _ |
        |+-------+-------+-------+
        || _ _ _ | _ _ _ | _ _ _ |
        || _ _ _ | _ _ _ | _ _ _ |
        || _ _ _ | _ _ _ | _ _ _ |
        |+-------+-------+-------+
        || _ _ _ | _ _ _ | _ _ _ |
        || _ _ _ | _ _ _ | _ _ _ |
        || _ _ _ | _ _ _ | _ _ _ |
        |+-------+-------+-------+""".stripMargin
    )
  }

}
