package games.tictactoe.models

import scala.util.{Try, Success, Failure}
import play.api.libs.json.{Json, JsValue, JsArray, JsNumber} 
import models._

object TicTacToe extends Game with GameFormat {
  type Board = Map[Move, Player]

  override def numPlayers = 2

  override def init = Turn(Map.empty, 0)

  override type Move = (Int, Int)

  sealed trait State extends AbstractState {
    def board: Board
    override def transition(pos: Move, player: Player): Try[State] = this match {
      case turn @ Turn(board, currentPlayer) => Try {
        require(player == currentPlayer, "Wrong player.")
        require(!board.contains(pos), "Invalid board position.")
        require(!outOfBounds(pos), "Moveition out of bounds.")

        val board1 = board updated (pos, currentPlayer)
        if (winningMove(board1, pos, currentPlayer)) {
          Win(board1, currentPlayer)
        } else if (board1.size == 3 * 3) {
          Draw(board1)
        } else {
          Turn(board1, nextPlayer(currentPlayer))
        }
      }
      case _ => Failure(new Exception("The game is completed."))
    }
    override def isFinal = this match {
      case Turn(_, _) => false
      case _ => true
    }
  }

  case class Turn(board: Board, currentPlayer: Player) extends State
  case class Win(board: Board, player: Player) extends State
  case class Draw(board: Board) extends State


  /* JSON Readers and Writers */
  override def moveFromJson(data: JsValue) = for {
    row <- (data\"row").asOpt[Int]
    col <- (data\"col").asOpt[Int]
  } yield (row, col)
  override def stateToJson(state: State) = {
    val (stateString, player) = state match {
      case Turn(_, p) => ("turn", p)
      case Win(_, p) => ("win", p)
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
  def outOfBounds(pos: Move) = {
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
