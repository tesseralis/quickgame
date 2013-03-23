package models

import scala.util.{Try, Success, Failure}
import play.api.libs.json._

object Checkers extends Game with GameFormat {
  type Pos = Int
  override def numPlayers = 2

  override def init = Turn(initBoard, 0)

  case class Piece(player: Player, isKing: Boolean = false) {
    override def toString = "" + player + (if (isKing) "K" else "R")
  }
  object Direction extends Enumeration {
    val LU, RU, RD, LD = Value
    type Direction = Value
  }
  import Direction._

  def nextPlayer(player: Player): Player = 1 - player

  def validPos(pos: Pos): Boolean = {
    val (row, col) = coord(pos)
    row >= 0 && row < 8 && col >= 0 && col < 8
  }

  def neighbor(pos: Pos, dir: Direction): Pos = {
    val (row, col) = coord(pos)
    val (nrow, ncol) = neighbor(row, col, dir)
    position(nrow, ncol)
  }
  def neighbor(row: Int, col: Int, dir: Direction): (Int, Int) = {
    dir match {
      case LU => (row - 1, col - 1)
      case RU => (row - 1, col + 1)
      case RD => (row + 1, col + 1)
      case LD => (row + 1, col - 1)
      case _ => (row, col)
    }
  }

  def playerCount(board: Board, player: Player): Int =
    board.values.filter(_.player == player).size

  def coord(pos: Pos): (Int, Int) = {
    val row = pos / 4
    val col = (pos % 4) + (if (row % 2 == 0) 0 else 1)
    (row, col)
  }
  def position(row: Int, col: Int) =
    (row * 4) + (col - (if (row % 2 == 0) 0 else 1)) / 2

  type Board = Map[Pos, Piece]

  def kingMaybe(pos: Pos, piece: Piece): Piece = {
    val (row, col) = coord(pos)
    if (piece.isKing) piece else piece.copy(isKing = piece.player == (7 - col))
  }

  def initBoard: Board = Map() ++
    (for (i <- 0 until 12) yield (i, Piece(0))) ++
    (for (i <- 20 until 32) yield (i, Piece(1)))


  case class Move(pos: Pos, direction: Direction)

  trait State extends AbstractState {
    def board: Board

    override def isFinal = this match {
      case Turn(_, _) => false
      case _ => true
    }
    // todo: Player must take a capture.
    override def transition(move: Move, player: Player): Try[State] = this match {
      case Turn(board, currentPlayer) => Try {
        val Move(pos, direction) = move
        require(player == currentPlayer, "Wrong player.")
        require(board contains pos, "There is no piece here.")
        val piece = board(pos)
        require(piece.player == currentPlayer, "You don't own this piece.")
        require(piece.isKing || (direction.id / 2 != player), "This piece is not a king.")

        val dest = neighbor(pos, direction)
        require(validPos(dest), "You cannot move in that direction.")
        board.get(dest) match {
          case None => // No piece, so move here.
            Turn(board - pos + (dest -> kingMaybe(dest, piece)), nextPlayer(currentPlayer))
          case Some(piece) =>
            require(piece.player != currentPlayer)
            val dest2 = neighbor(dest, direction)
            require(validPos(dest2), "You cannot jump in that direction.")
            require(board.get(dest2).isEmpty, "You cannot jump more than one piece.")
            val newBoard = board - pos - dest + (dest2 -> kingMaybe(dest2, piece))
            if (playerCount(newBoard, nextPlayer(currentPlayer)) == 0)
              Win(newBoard, currentPlayer)
            else
              Turn(newBoard, currentPlayer)
        }
      }
      case _ => Failure(new Exception("This game is completed."))
    }
  }


  case class Turn(board: Board, currentPlayer: Player) extends State
  case class Win(board: Board, player: Player) extends State
  case class Draw(board: Board) extends State

  override def moveFromJson(data: JsValue) = for {
    index <- (data\"index").asOpt[Pos]
    dir <- Try(Direction withName (data\"direction").as[String]).toOption
  } yield Move(index, dir)
  override def stateToJson(state: State) = {
    val (stateString, player) = state match {
      case Turn(_, p) => ("turn", p)
      case Win(_, p) => ("win", p)
      case Draw(_) => ("draw", -1)
    }
    val jsonBoard = JsArray(for (k <- 0 until 32) yield {
      JsString(state.board.get(k).map(_.toString).getOrElse(""))
    })
    Json.obj(
      "kind" -> stateString,
      "player" -> player,
      "board" -> jsonBoard
    )
  }
}
