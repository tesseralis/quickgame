package models

object TicTacToe {
  type Pos = (Int, Int)
  type Player = Int
  type Board = Map[Pos, Player]
  
  /**
   * Create an empty tic-tac-toe board
   */
  def empty: TicTacToe = Turn(Map.empty, 0)
  
  def nextPlayer(p: Player): Player = 1 - p
  def outOfBounds(pos: Pos) = {
    val (i, j) = pos
    i < 0 || i >= 3 || j < 0 || j >= 3
  }
}

import TicTacToe._

trait TicTacToe {
  def board: Board
}

case class Turn(board: Board, currentPlayer: Player) extends TicTacToe {
  def move(pos: Pos): TicTacToe = {
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
      copy(board = newBoard,
           currentPlayer = nextPlayer(currentPlayer))
    }
  }
}

case class Win(board: Board, player: Player) extends TicTacToe
case class Draw(board: Board) extends TicTacToe