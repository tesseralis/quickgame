package common

import scala.util.{Try, Success, Failure}

trait BoardGame extends Game {
  type Board

  protected def boardTransition(board: Board, player: Player, move: Move):Try[State]

  protected def boardInit: Board

  sealed trait State {
    def board: Board
  }
  case class Turn(player: Player, board: Board) extends State
  case class Win(winner: Player, board: Board) extends State
  case class Draw(board: Board) extends State

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

}
