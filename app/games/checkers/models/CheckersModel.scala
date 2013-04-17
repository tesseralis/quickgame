package games.checkers.models

import scala.util.{Try, Success, Failure}

import play.api.libs.json._

import common.{Game, GameFormat}

object CheckersModel extends Game with GameFormat {

  override type Player = Int

  override def players = 0 until 2

  /**
   * Defines all posible move direction of checkers,
   * in clockwise order.
   * Right-Down, Left-Down, Left-Up, Right-Up
   */
  object Direction extends Enumeration {
    val RD, LD, LU, RU = Value
    type Direction = Value
  }
  import Direction._

  /**
   * Represent the position of the board as its grid position.
   */
  type Pos = (Int, Int)

  /**
   * Representation of a checkers piece.
   * Contains information on the player and whether the piece is a king.
   */
  case class Piece(player: Player, isKing: Boolean = false) {
    override def toString =
      player + (if (isKing) "K" else "R")

    /**
     * The list of possible moves of this piece.
     * A regular piece may only move forwards, while a king can move
     * in all four diagonals.
     */
    lazy val moves: Set[Direction] =
      if (isKing) Direction.values
      else ValueSet(Direction(player * 2), Direction(player * 2 + 1))

    /**
     * Can this piece move in the specified direction?
     */
    def canMoveIn(dir: Direction): Boolean = moves contains dir
  }

  /**
   * Store the board as a mapping from each position to the player that
   * owns the piece at that position (if it exists).
   */
  type Board = Map[Pos, Piece]

  sealed trait State {
    def board: Board
  }
  case class Turn(player: Player, board: Board) extends State
  case class Win(winner: Player, board: Board) extends State
  case class Draw(board: Board) extends State

  /**
   * A player moves by choosing a piece and a direction.
   */
  case class Move(pos: Pos, direction: Direction)

  def boardInit: Board = Map() ++
    (for (i <- 0 until 12) yield (toPos(i), Piece(0))) ++
    (for (i <- 20 until 32) yield (toPos(i), Piece(1)))

  def boardTransition(board: Board, player: Player, move: Move) = Try {
    val Move(pos, direction) = move
    require(board contains pos, "There is no piece here.")
    val piece = board(pos)
    require(piece.player == player, "This is not your piece.")
    require(piece canMoveIn direction, "This piece is not a king.")

    val dest = neighbor(pos, direction)
    require(validPos(dest))
    board.get(dest).map { target =>
      require(target.player != player, "You cannot jump your own piece.")
      val dest2 = neighbor(dest, direction)
      require(validPos(dest2))
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
      require(!capturesAvailable(board, player), "You must take an available capture")
      Turn(nextPlayer(player), board - pos + (dest -> kingMaybe(dest, piece)))
    }
  }

  final override def isFinal(state: State) = state match {
    case Turn(_, _) => false
    case _ => true
  }

  final override def init = Turn(0, boardInit)

  final override def transition(state: State, player: Player, move: Move) = state match {
    case Turn(current, board) => Try {
      require(player == current, s"It is not $player's turn")
      boardTransition(board, player, move).get
    }
    case _ => Failure(new IllegalStateException("The game has ended."))
  }

  def capturesAvailable(board: Board, player: Player): Boolean =
    board.exists { case (pos, piece) =>
      piece.player == player && piece.moves.exists(dir => canCapture(board, pos, dir))
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

  def validPos(pos: Pos): Boolean = {
    val (row, col) = pos
    row >= 0 && row < 8 && col >= 0 && col < 8 && ((row + col) % 2 == 0)
  }

  def neighbor(pos: Pos, dir: Direction): Pos = {
    val (row, col) = pos
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

  def toPos(index: Int): Pos = {
    val row = index / 4
    val col = 2 * (index % 4) + (if (row % 2 == 0) 0 else 1)
    (row, col)
  }
  def toIndex(pos: Pos) = {
    val (row, col) = pos
    (row * 4) + (col - (if (row % 2 == 0) 0 else 1)) / 2
  }

  def kingMaybe(pos: Pos, piece: Piece): Piece = {
    val (row, col) = pos
    if (piece.isKing) piece else piece.copy(isKing = piece.player == (7 - row))
  }

  /* JSON Formatters */

  override def moveFromJson(data: JsValue) = for {
    index <- (data\"index").asOpt[Int]
    pos = toPos(index)
    dir <- Try(Direction withName (data\"direction").as[String]).toOption
  } yield Move(pos, dir)
  override def stateToJson(state: State) = {
    val (stateString, player) = state match {
      case Turn(p, _) => ("turn", p)
      case Win(p, _) => ("win", p)
      case Draw(_) => ("draw", -1)
    }
    val jsonBoard = JsArray(for (k <- 0 until 32) yield {
      JsString(state.board.get(toPos(k)).map(_.toString).getOrElse(""))
    })
    Json.obj(
      "kind" -> stateString,
      "player" -> player,
      "board" -> jsonBoard
    )
  }
}
