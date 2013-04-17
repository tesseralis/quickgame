package test

import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import games.checkers.models.CheckersModel._

class CheckersSpec extends FlatSpec with ShouldMatchers {
  // TODO Copy these tests for the other player...
  "Checkers" should "let move a piece forward to an empty space" in {
    val state = Turn(0, Map((3, 1) -> Piece(0), (7, 7) -> Piece(1)))
    transition(state, 0, Move((3, 1), Direction.LD)).get.board((4, 0)) should be (Piece(0))
    transition(state, 0, Move((3, 1), Direction.RD)).get.board((4, 2)) should be (Piece(0))
  }

  it should "not let a regular piece move backwards" in {
    val state = Turn(0, Map((3, 1) -> Piece(0), (7, 7) -> Piece(1)))
    transition(state, 0, Move((3, 1), Direction.LU)) should be ('failure)
    transition(state, 0, Move((3, 1), Direction.RU)) should be ('failure)
  }

  it should "let a King move backwards" in {
    val state = Turn(0, Map((3, 1) -> Piece(0, true), (7, 7) -> Piece(1)))
    transition(state, 0, Move((3, 1), Direction.LU)).get.board((2, 0)) should equal (Piece(0, true))
    transition(state, 0, Move((3, 1), Direction.RU)).get.board((2, 2)) should equal (Piece(0, true))
  }

  it should "turn a piece into a king upon reaching opposite end" in {
    val state = Turn(0, Map((6, 2) -> Piece(0), (7, 7) -> Piece(1)))
    transition(state, 0, Move((6, 2), Direction.LD)).get.board((7, 1)) should be (Piece(0, true))
  }

  it should "not do anything when a king reaches the end" in {
    val state = Turn(0, Map((6, 2) -> Piece(0, true), (1, 1) -> Piece(0, true), (7, 7) -> Piece(1)))
    transition(state, 0, Move((6, 2), Direction.LD)).get.board((7, 1)) should be (Piece(0, true))
    transition(state, 0, Move((1, 1), Direction.LU)).get.board((0, 0)) should be (Piece(0, true))
  }

  it should "not let a piece move beyond the board boundaries" in {
    val state = Turn(0, Map((4, 0) -> Piece(0), (7, 7) -> Piece(1)))
    transition(state, 0, Move((4, 0), Direction.LD)) should be ('failure)
  }

  it should "not let a piece jump more than one opponent" in {
    val state = Turn(0, Map((3, 1) -> Piece(0), (4, 2) -> Piece(1), (5, 3) -> Piece(1)))
    transition(state, 0, Move((3, 1), Direction.RD)) should be ('failure)
  }
  it should "not let jumping a piece go out of bounds" in {
    val state = Turn(0, Map((3, 1) -> Piece(0), (4, 0) -> Piece(1)))
    transition(state, 0, Move((3, 1), Direction.LD)) should be ('failure)
  }

  it should "let a piece jump another piece" in {
    val state = Turn(0, Map((3, 1) -> Piece(0), (4, 2) -> Piece(1), (7, 7) -> Piece(1)))
    val state1 = transition(state, 0, Move((3, 1), Direction.RD)).get
    state1.board.get((4, 2)) should be (None)
    state1.board((5, 3)) should be (Piece(0))
  }

  it should "change the player when no piece is captured" in {
    val state = Turn(0, Map((3, 1) -> Piece(0), (7, 7) -> Piece(1)))
    transition(state, 0, Move((3, 1), Direction.LD)).get.asInstanceOf[TurnState].player should be (1)
  }

  it should "remain on the same turn if a second jump is possible" in {
    val state = Turn(0, Map((3, 1) -> Piece(0), (4, 2) -> Piece(1), (6, 2) -> Piece(1)))
    transition(state, 0, Move((3, 1), Direction.RD)).get.asInstanceOf[TurnState].player should be (0)
  }

  it should "change the player when no more captures are available" in {
    val state = Turn(0, Map((3, 1) -> Piece(0), (4, 2) -> Piece(1), (7, 7) -> Piece(1)))
    val expected = Turn(1, Map((5, 3) -> Piece(0), (7, 7) -> Piece(1)))
    transition(state, 0, Move((3, 1), Direction.RD)).get should equal (expected)
  }

  it should "force a player to take a capture" in {
    val state = Turn(0, Map((3, 1) -> Piece(0), (4, 2) -> Piece(1), (7, 7) -> Piece(1)))
    transition(state, 0, Move((3, 1), Direction.LD)) should be ('failure)
  }

  it should "require a player to use the same piece in a chain of moves" in {
    val state = Turn(0, Map((3, 1) -> Piece(0), (4, 2) -> Piece(1), (6, 2) -> Piece(1), (5, 1) -> Piece(0)))
    val chain = transition(state, 0, Move((3, 1), Direction.RD)).get
    transition(chain, 0, Move((5, 1), Direction.RD)) should be ('failure)
     
  }

  it should "end the game in a win if opponent has no more pieces" in {
    val state = Turn(0, Map((3, 1) -> Piece(0), (4, 2) -> Piece(1)))
    val expected = Win(0, Map((5, 3) -> Piece(0)))
    transition(state, 0, Move((3, 1), Direction.RD)).get should be (expected)
  }
}
