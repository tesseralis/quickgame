package games.connectfour.models

import scala.util.{Try, Success, Failure}
import akka.actor.Actor

import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.json.{Json, JsValue}
import common.{BoardGame, GameFormat}

object ConnectFourModel extends BoardGame with GameFormat {

  override type Board = IndexedSeq[Seq[Player]]

  override type Move = Int

  override type Player = Int

  override def players = 0 until 2

  override def playerInit = 0

  override def boardInit = (0 until 7) map { _ => List.empty }

  override def boardTransition(board: Board, player: Player, move: Move) = Try {
    require(board(move).length < 6, "Invalid board position")
    val newBoard = board updated (move, board(move) :+ player)
    if (winningMove(newBoard, move, player)) {
      Win(player, newBoard)
    } else if (newBoard.forall(_.size == 6)) {
      Draw(newBoard)
    } else {
      Turn(nextPlayer(player), newBoard)
    }
  }

  override def moveFromJson(data: JsValue) = data.asOpt[Int]
  override def stateToJson(state: State) = {
    val (stateString, player) = state match {
      case Turn(p, _) => ("gamestart", p)
      case Win(p, _) => ("gameend", p)
      case Draw(_) => ("draw", -1)
    }
    Json.obj(
      "kind" -> stateString,
      "player" -> player,
      "board" -> Json.toJson(state.board)
    )
  }

  def nextPlayer(p: Player): Player = 1 - p

  def winningMove(board: Board, col: Move, player: Player): Boolean = {
    val defaultBoard = (for {
      j <- 0 until 7
      i <- 0 until board(j).length
    } yield {
      (i, j) -> board(j)(i)
    }).toMap.withDefaultValue(-1)
    val row = board(col).length - 1

    val colWin = board(col).reverse.takeWhile(_ == player).length >= 4
    val rowWin = {
      val left = (1 to 3).takeWhile(k => defaultBoard((row, col-k)) == player).length
      val right = (1 to 3).takeWhile(k => defaultBoard((row, col+k)) == player).length
      (1 + left + right) >= 4
    }
    val diagWin = {
      val left = (1 to 3).takeWhile(k => defaultBoard((row-k, col-k)) == player).length
      val right = (1 to 3).takeWhile(k => defaultBoard((row+k, col+k)) == player).length
      (1 + left + right) >= 4
    }
    val diagWin2 = {
      val left = (1 to 3).takeWhile(k => defaultBoard((row-k, col+k)) == player).length
      val right = (1 to 3).takeWhile(k => defaultBoard((row+k, col-k)) == player).length
      (1 + left + right) >= 4
    }
    colWin || rowWin || diagWin || diagWin2
  }
}
