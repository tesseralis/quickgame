package test

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import games.checkers.models.CheckersModel._

class CheckersSpec extends FlatSpec with ShouldMatchers {
  // TODO Copy these tests for the other player...
  "Checkers" should "let move a piece forward to an empty space" in {
    val state = Turn(0, Map(12 -> Piece(0), 31 -> Piece(1)))
    transition(state, 0, Move(12, Direction.LD)).get.board(16) should be (Piece(0))
    transition(state, 0, Move(12, Direction.RD)).get.board(17) should be (Piece(0))
  }

  it should "not let a regular piece move backwards" in {
    val state = Turn(0, Map(12 -> Piece(0), 31 -> Piece(1)))
    transition(state, 0, Move(12, Direction.LU)) should be ('failure)
    transition(state, 0, Move(12, Direction.RU)) should be ('failure)
  }

  it should "let a King move backwards" in {
    val state = Turn(0, Map(12 -> Piece(0, true), 31 -> Piece(1)))
    transition(state, 0, Move(12, Direction.LU)).get.board(8) should equal (Piece(0, true))
    transition(state, 0, Move(12, Direction.RU)).get.board(9) should equal (Piece(0, true))
  }

  it should "turn a piece into a king upon reaching opposite end" in {
    val state = Turn(0, Map(25 -> Piece(0), 31 -> Piece(1)))
    transition(state, 0, Move(25, Direction.LD)).get.board(28) should be (Piece(0, true))
  }

  it should "not do anything when a king reaches the end" in {
    val state = Turn(0, Map(25 -> Piece(0, true), 4 -> Piece(0, true), 31 -> Piece(1)))
    transition(state, 0, Move(25, Direction.LD)).get.board(28) should be (Piece(0, true))
    transition(state, 0, Move(4, Direction.LU)).get.board(0) should be (Piece(0, true))
  }

  it should "not let a piece move beyond the board boundaries" in {
    val state = Turn(0, Map(16 -> Piece(0), 31 -> Piece(1)))
    transition(state, 0, Move(16, Direction.LD)) should be ('failure)
  }

  it should "not let a piece jump more than one opponent" in {
    val state = Turn(0, Map(12 -> Piece(0), 17 -> Piece(1), 21 -> Piece(1)))
    transition(state, 0, Move(12, Direction.RD)) should be ('failure)
  }
  it should "not let jumping a piece go out of bounds" in {
    val state = Turn(0, Map(12 -> Piece(0), 16 -> Piece(1)))
    transition(state, 0, Move(12, Direction.LD)) should be ('failure)
  }

  it should "let a piece jump another piece" in {
    val state = Turn(0, Map(12 -> Piece(0), 17 -> Piece(1), 31 -> Piece(1)))
    val state1 = transition(state, 0, Move(12, Direction.RD)).get
    state1.board.get(17) should be (None)
    state1.board(21) should be (Piece(0))
  }

  it should "change the player when no piece is captured" in {
    val state = Turn(0, Map(12 -> Piece(0), 31 -> Piece(1)))
    val expected = Turn(0, Map(16 -> Piece(0), 31 -> Piece(1)))
    transition(state, 0, Move(12, Direction.LD)).get should equal (expected)
  }

  it should "remain on the same turn if a second jump is possible" in {
    val state = Turn(0, Map(12 -> Piece(0), 17 -> Piece(1), 25 -> Piece(1)))
    val expected = Turn(0, Map(21 -> Piece(0), 25 -> Piece(1)))
    transition(state, 0, Move(12, Direction.RD)).get should equal (expected)
  }

  it should "change the player when no more captures are available" in {
    val state = Turn(0, Map(12 -> Piece(0), 17 -> Piece(1), 31 -> Piece(1)))
    val expected = Turn(0, Map(21 -> Piece(0), 31 -> Piece(1)))
    transition(state, 0, Move(12, Direction.RD)).get should equal (expected)
  }

  it should "force a player to take a capture" in {
    val state = Turn(0, Map(12 -> Piece(0), 17 -> Piece(1), 31 -> Piece(1)))
    transition(state, 0, Move(12, Direction.LD)) should be ('failure)
  }

  it should "end the game in a win if opponent has no more pieces" in {
    val state = Turn(0, Map(12 -> Piece(0), 17 -> Piece(1)))
    val expected = Win(0, Map(21 -> Piece(0)))
    transition(state, 0, Move(12, Direction.RD)).get should be (expected)
  }
}
