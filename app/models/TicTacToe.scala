package models

import scala.util.{Try, Success, Failure}

object TicTacToeModel {
  type Pos = (Int, Int)
  type Player = Int
  type Board = Map[Pos, Player]
  
  /**
   * Create an empty tic-tac-toe board
   */
  def empty: TicTacToeModel = Turn(Map.empty, 0)
  
  def nextPlayer(p: Player): Player = 1 - p
  def outOfBounds(pos: Pos) = {
    val (i, j) = pos
    i < 0 || i >= 3 || j < 0 || j >= 3
  }
}

import TicTacToeModel._

trait TicTacToeModel {
  def board: Board
  def move(player: Player, pos: Pos): Try[TicTacToeModel] = this match {
    case turn @ Turn(board, currentPlayer) => Try {
      require(player == currentPlayer, "Wrong player.")
      require(!board.contains(pos), "Invalid board position.")
      require(!outOfBounds(pos), "Position out of bounds.")
      val newBoard = board updated (pos, currentPlayer)
      val (i, j) = pos
      // TODO Diagonals
      //if ((0 until 3).forall(newBoard(_, j) == currentPlayer) ||
      //    (0 until 3).forall(newBoard(i, _) == currentPlayer)) {
      if (newBoard.get((1, 1)).map(_ == currentPlayer).getOrElse(false)) {
        Win(newBoard, currentPlayer)
      } else if (newBoard.size == 3 * 3) {
        Draw(newBoard)
      } else {
        turn.copy(board = newBoard, currentPlayer = nextPlayer(currentPlayer))
      }
    }
    case _ => Failure(new Exception("The game is completed."))
  }
}

case class Turn(board: Board, currentPlayer: Player) extends TicTacToeModel
case class Win(board: Board, player: Player) extends TicTacToeModel
case class Draw(board: Board) extends TicTacToeModel
