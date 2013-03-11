package models

import scala.util.{Try, Success, Failure}
import play.api.libs.json._

object CheckersRoom {
  type Pos = (Int, Int)
  object Player extends Enumeration {
    val Black, Red = Value
  }
  type Player = Player.Value

  case class Piece(player: Player, isKing: Boolean = false) {
    override def toString = "" + player.id + (if (isKing) "K" else "")
  }

  type Board = Map[Pos, Piece]

  trait State {
    def board: Board
    def move(player: Player, pos: Pos): Try[State] = this match {
      case turn @ Turn(board, currentPlayer) => Try {
        turn
      }
      case _ => Failure(new Exception("This game is completed."))
    }
    def winner = this match {
      case Turn(_, _) => None
      case Win(_, winner) => Some(winner.id)
      case Draw(_) => Some(-1)
    }
  }

  case class Turn(board: Board, currentPlayer: Player) extends State
  case class Win(board: Board, player: Player) extends State
  case class Draw(board: Board) extends State
}

import CheckersRoom._

class CheckersRoom extends GameRoom[State, Pos] {
  override def maxPlayers = 2
  override def moveFromJson(data: JsValue) = for {
    row <- (data\"row").asOpt[Int]
    col <- (data\"col").asOpt[Int]
  } yield (row, col)
  override def stateToJson(state: State) = {
    val (stateString, player) = state match {
      case Turn(_, p) => ("turn", p.id)
      case Win(_, p) => ("win", p.id)
      case Draw(_) => ("draw", -1)
    }
    val jsonBoard = JsArray(for (i <- 0 until 8) yield {
      JsArray(for (j <- 0 until 8) yield {
        JsString(state.board.get((i, j)).map(_.toString).getOrElse(""))
      })
    })
    Json.obj(
      "kind" -> stateString,
      "player" -> player,
      "board" -> jsonBoard
    )
  }

  override def move(state: State, idx: Int, mv: Pos) = state.move(Player(idx), mv)
  override def initState = Turn(Map.empty, Player.Black)
  override def winner(state: State) = state.winner
}
