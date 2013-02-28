package models

import scala.util.{Try, Success, Failure}
import akka.actor.Actor

import play.api.Logger
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.json.{Json, JsValue}

object ConnectFourRoom {
  type Player = Int
  type Board = IndexedSeq[Seq[Player]]

  def nextPlayer(p: Player): Player = 1 - p

  def winningMove(board: Board, move: Int, player: Player): Boolean = {
    // TODO Actual winning move.
    false
  }

  trait State {
    def board: Board
    def move(player: Player, col: Int): Try[State] = this match {
      case turn @ GameStart(board, currentPlayer) => Try {
        require(player == currentPlayer, "Wrong player.")
        require(board(col).length < 6, "Invalid board position")
        val newBoard = board updated (col,  board(col) :+ currentPlayer)
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
  }

  case class GameStart(board: Board, currentPlayer: Player) extends State
  /** Contains the state of the board at end game and the winning player (-1 if none) */
  case class GameEnd(board: Board, player: Player) extends State
}

import ConnectFourRoom._

class ConnectFourRoom extends GameRoom[State, Int] {
  def maxPlayers = 2
  def parseMove(data: JsValue) = data.asOpt[Int]
  def encodeState(input: State) = {
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
  def move(state: State, idx: Int, mv: Int) = state.move(idx, mv)
  def initState = GameStart((0 until 7) map { _ => List.empty }, 0)
}
