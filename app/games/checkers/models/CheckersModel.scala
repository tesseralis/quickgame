package games.checkers.models

import scala.util.{Try, Success, Failure}

import play.api.libs.json._

import common.{BoardGame, GameFormat}

object CheckersModel extends BoardGame with GameFormat {
  type Pos = Int

  object Direction extends Enumeration {
    val RD, LD, LU, RU = Value
    type Direction = Value
  }
  import Direction._

  override def numPlayers = 2

  override type Board = Map[Pos, Piece]

  case class Move(pos: Pos, direction: Direction)

  case class Piece(player: Player, isKing: Boolean = false) {
    override def toString =
      player + (if (isKing) "K" else "R")

    lazy val moves: Set[Direction] =
      if (isKing) Direction.values
      else ValueSet(Direction(player * 2), Direction(player * 2 + 1))

    def canMoveIn(dir: Direction): Boolean = moves contains dir
  }

  override def boardInit: Board = Map() ++
    (for (i <- 0 until 12) yield (i, Piece(0))) ++
    (for (i <- 20 until 32) yield (i, Piece(1)))

  override def boardTransition(board: Board, player: Player, move: Move) = Try {
    val Move(pos, direction) = move
    require(board contains pos, "There is no piece here.")
    val piece = board(pos)
    require(piece.player == player, "This is not your piece.")
    require(piece canMoveIn direction, "This piece is not a king.")

    val dest = neighbor(pos, direction)
    board.get(dest).map { target =>
      require(target.player != player, "You cannot jump your own piece.")
      val dest2 = neighbor(dest, direction)
      require(board.get(dest2).isEmpty, "You cannot jump more than one piece.")
      val newBoard = board - pos - dest + (dest2 -> kingMaybe(dest2, piece))
      if (playerCount(newBoard, nextPlayer(player)) == 0)
        Win(player, newBoard)
      else {
        if (piece.moves.exists(dir => canCapture(newBoard, dest2, dir)))
          Turn(player, newBoard)
        else
          Turn(nextPlayer(player), newBoard)
      }
    } getOrElse {
      // If the space is unoccupied, simply move there.
      Turn(nextPlayer(player), board - pos + (dest -> kingMaybe(dest, piece)))
    }
  }

  def canCapture(board: Board, source: Pos, dir: Direction): Boolean = {
    val jumped = neighbor(source, dir)
    val dest = neighbor(jumped, dir)

    (for {
      piece <- board.get(source)
      target <- board.get(jumped)
    } yield {
      piece.player != target.player && !board.contains(dest)
    }) getOrElse {
      false
    }

  }

  def nextPlayer(player: Player): Player = 1 - player

  def validPos(row: Int, col: Int): Boolean =
    row >= 0 && row < 8 && col >= 0 && col < 8

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
    val col = 2 * (pos % 4) + (if (row % 2 == 0) 0 else 1)
    (row, col)
  }
  def position(row: Int, col: Int) = {
    require(validPos(row, col))
    (row * 4) + (col - (if (row % 2 == 0) 0 else 1)) / 2
  }

  def kingMaybe(pos: Pos, piece: Piece): Piece = {
    val (row, col) = coord(pos)
    if (piece.isKing) piece else piece.copy(isKing = piece.player == (7 - row))
  }

  /* JSON Formatters */

  override def moveFromJson(data: JsValue) = for {
    index <- (data\"index").asOpt[Pos]
    dir <- Try(Direction withName (data\"direction").as[String]).toOption
  } yield Move(index, dir)
  override def stateToJson(state: State) = {
    val (stateString, player) = state match {
      case Turn(p, _) => ("turn", p)
      case Win(p, _) => ("win", p)
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
