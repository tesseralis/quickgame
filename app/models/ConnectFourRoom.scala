package models

import scala.util.{Try, Success, Failure}
import akka.actor.Actor

import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.json.{Json, JsValue}

object ConnectFourRoom {
  type Player = Int
  type Board = IndexedSeq[Seq[Player]]

  def nextPlayer(p: Player): Player = 1 - p

  def winningMove(board: Board, col: Int, player: Player): Boolean = {
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

  trait State {
    def board: Board
    def move(player: Player, col: Int): Try[State] = this match {
      case turn @ GameStart(board, currentPlayer) => Try {
        require(player == currentPlayer, "Wrong player.")
        require(board(col).length < 6, "Invalid board position")
        val newBoard = board updated (col, board(col) :+ currentPlayer)
        if (winningMove(newBoard, col, currentPlayer)) {
          GameEnd(newBoard, currentPlayer)
        } else if (newBoard.forall(_.size == 6)) {
          GameEnd(newBoard, -1)
        } else {
          turn.copy(board = newBoard, currentPlayer = nextPlayer(currentPlayer))
        }
      }
      case GameEnd(_, _) => Failure(new Exception("The game is completed."))
    }
    def winner = this match {
      case GameStart(_, _) => None
      case GameEnd(_, player) => Some(player)
    }
    def isFinal = this match {
      case GameStart(_, _) => false
      case _ => true
    }
  }

  case class GameStart(board: Board, currentPlayer: Player) extends State
  /** Contains the state of the board at end game and the winning player (-1 if none) */
  case class GameEnd(board: Board, player: Player) extends State
}

import ConnectFourRoom._

class ConnectFourRoom extends GameRoom[State, Int] {
  override def numPlayers = 2
  override def moveFromJson(data: JsValue) = data.asOpt[Int]
  override def stateToJson(state: State) = {
    val (stateString, player) = state match {
      case GameStart(_, p) => ("gamestart", p)
      case GameEnd(_, p) => ("gameend", p)
    }
    Json.obj(
      "kind" -> stateString,
      "player" -> player,
      "board" -> Json.toJson(state.board)
    )
  }
  override def move(state: State, idx: Int, mv: Int) = state.move(idx, mv)
  override def initState = GameStart((0 until 7) map { _ => List.empty }, 0)
  override def isFinal(state: State) = state.isFinal
}
