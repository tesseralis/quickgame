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
  def move(pos: Pos): Try[TicTacToeModel]
}

case class Turn(board: Board, currentPlayer: Player) extends TicTacToeModel {
  def move(pos: Pos): Try[TicTacToeModel] = Try {
    assert(board.contains(pos) || outOfBounds(pos))
    val newBoard = board updated (pos, currentPlayer)
    val (i, j) = pos
    // TODO Diagonals
    if ((0 until 3).forall(newBoard(_, j) == currentPlayer) ||
        (0 until 3).forall(newBoard(i, _) == currentPlayer)) {
      Win(newBoard, currentPlayer)
    } else if (newBoard.size == 3 * 3) {
      Draw(newBoard)
    }else {
      copy(board = newBoard, currentPlayer = nextPlayer(currentPlayer))
    }
  }
}

case class Win(board: Board, player: Player) extends TicTacToeModel {
  override def move(pos: Pos) = Failure(new Exception("The game is over"))
}
case class Draw(board: Board) extends TicTacToeModel {
  override def move(pos: Pos) = Failure(new Exception("The game is over"))
}