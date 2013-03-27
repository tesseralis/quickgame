package test

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import games.checkers.models.CheckersModel._

class CheckersSpec extends FlatSpec with ShouldMatchers {
  // TODO Copy these tests for the other player...
  "Checkers" should "let move a piece forward to an empty space" in {
    val state = Turn(Map(12 -> Piece(0), 31 -> Piece(1)), 0)
    state.transition(Move(12, Direction.LD), 0).get.board(16) should be (Piece(0))
    state.transition(Move(12, Direction.RD), 0).get.board(17) should be (Piece(0))
  }

  it should "not let a regular piece move backwards" in {
    val state = Turn(Map(12 -> Piece(0), 31 -> Piece(1)), 0)
    state.transition(Move(12, Direction.LU), 0) should be ('failure)
    state.transition(Move(12, Direction.RU), 0) should be ('failure)
  }

  it should "let a King move backwards" in {
    val state = Turn(Map(12 -> Piece(0, true), 31 -> Piece(1)), 0)
    state.transition(Move(12, Direction.LU), 0).get.board(8) should equal (Piece(0, true))
    state.transition(Move(12, Direction.RU), 0).get.board(9) should equal (Piece(0, true))
  }

  it should "turn a piece into a king upon reaching opposite end" in {
    val state = Turn(Map(25 -> Piece(0), 31 -> Piece(1)), 0)
    state.transition(Move(25, Direction.LD), 0).get.board(28) should be (Piece(0, true))
  }

  it should "not do anything when a king reaches the end" in {
    val state = Turn(Map(25 -> Piece(0, true), 4 -> Piece(0, true), 31 -> Piece(1)), 0)
    state.transition(Move(25, Direction.LD), 0).get.board(28) should be (Piece(0, true))
    state.transition(Move(4, Direction.LU), 0).get.board(0) should be (Piece(0, true))
  }

  it should "not let a piece move beyond the board boundaries" in {
    val state = Turn(Map(16 -> Piece(0), 31 -> Piece(1)), 0)
    state.transition(Move(16, Direction.LD), 0) should be ('failure)
  }

  it should "not let a piece jump more than one opponent" in {
    val state = Turn(Map(12 -> Piece(0), 17 -> Piece(1), 21 -> Piece(1)), 0)
    state.transition(Move(12, Direction.RD), 0) should be ('failure)
  }
  it should "not let jumping a piece go out of bounds" in {
    val state = Turn(Map(12 -> Piece(0), 16 -> Piece(1)), 0)
    state.transition(Move(12, Direction.LD), 0) should be ('failure)
  }

  it should "let a piece jump another piece" in {
    val state = Turn(Map(12 -> Piece(0), 17 -> Piece(1), 31 -> Piece(1)), 0)
    val state1 = state.transition(Move(12, Direction.RD), 0).get
    state1.board.get(17) should be (None)
    state1.board(21) should be (Piece(0))
  }

  it should "change the player when no piece is captured" in {
    val state = Turn(Map(12 -> Piece(0), 31 -> Piece(1)), 0)
    val expected = Turn(Map(16 -> Piece(0), 31 -> Piece(1)), 1)
    state.transition(Move(12, Direction.LD), 0).get should equal (expected)
  }

  it should "remain on the same turn if a piece jumps another piece" in {
    val state = Turn(Map(12 -> Piece(0), 17 -> Piece(1), 31 -> Piece(1)), 0)
    val expected = Turn(Map(21 -> Piece(0), 31 -> Piece(1)), 0)
    state.transition(Move(12, Direction.RD), 0).get should equal (expected)
  }

  it should "force a player to take a capture" in {
    val state = Turn(Map(12 -> Piece(0), 17 -> Piece(1), 31 -> Piece(1)), 0)
    state.transition(Move(12, Direction.LD), 0) should be ('failure)
  }

  it should "end the game in a win if opponent has no more pieces" in {
    val state = Turn(Map(12 -> Piece(0), 17 -> Piece(1)), 0)
    val expected = Win(Map(21 -> Piece(0)), 0)
    state.transition(Move(12, Direction.RD), 0).get should be (expected)
  }
}
