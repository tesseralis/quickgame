package games.tictactoe.models

import scala.util.{Try, Success, Failure}
import play.api.libs.json.{Json, JsValue, JsArray, JsNumber} 
import common.{BoardGame, GameFormat}

object TicTacToeModel extends BoardGame with GameFormat {
  type Pos = (Int, Int)

  override type Board = Map[Pos, Player]

  override def numPlayers = 2

  override def boardInit = Map.empty

  override type Move = Pos

  override def boardTransition(board: Board, player: Player, move: Move) = Try {
    require(!board.contains(move), "Invalid board position.")
    require(!outOfBounds(move), "Moveition out of bounds.")

    val board1 = board updated (move, player)
    if (winningMove(board1, move, player)) {
      Win(player, board1)
    } else if (board1.size == 3 * 3) {
      Draw(board1)
    } else {
      Turn(nextPlayer(player), board1)
    }
  }

  /* JSON Readers and Writers */
  override def moveFromJson(data: JsValue) = for {
    row <- (data\"row").asOpt[Int]
    col <- (data\"col").asOpt[Int]
  } yield (row, col)
  override def stateToJson(state: State) = {
    val (stateString, player) = state match {
      case Turn(p, _) => ("turn", p)
      case Win(p, _) => ("win", p)
      case Draw(_) => ("draw", -1)
    }
    val jsonBoard = JsArray(for (i <- 0 until 3) yield {
      JsArray(for (j <- 0 until 3) yield {
        JsNumber(BigDecimal(state.board.get((i, j)).getOrElse(-1)))
      })
    })
    Json.obj(
      "kind" -> stateString,
      "player" -> player,
      "board" -> jsonBoard
    )
  }

  /* Helper functions */
  def nextPlayer(p: Player): Player = 1 - p
  def outOfBounds(pos: Pos) = {
    val (i, j) = pos
    i < 0 || i >= 3 || j < 0 || j >= 3
  }

  def winningMove(board: Board, move: Move, player: Player): Boolean = {
    val defaultBoard = board withDefaultValue -1
    val (row, col) = move
    (0 until 3).forall(defaultBoard(_, col) == player) ||
      (0 until 3).forall(defaultBoard(row, _) == player) ||
      (0 until 3).forall(k => defaultBoard(k, k) == player) ||
      (0 until 3).forall(k => defaultBoard(k, 2-k) == player)
  }
}
