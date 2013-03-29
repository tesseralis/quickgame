package common.models

import scala.util.{Try, Success, Failure}

trait BoardGame extends Game {
  type Board

  def boardTransition(board: Board, player: Player, move: Move):Try[State]

  sealed trait State extends AbstractState {
    def board: Board

    override def transition(player: Player, move: Move) = this match {
      case Turn(current, board) => Try {
        require(player == current, s"It is not $player's turn")
        boardTransition(board, player, move).get
      }
      case _ => Failure(new IllegalStateException("The game has ended."))
    }

    override def isFinal = this match {
      case Turn(_, _) => false
      case _ => true
    }
  }
  case class Turn(player: Player, board: Board) extends State
  case class Win(winner: Player, board: Board) extends State
  case class Draw(board: Board) extends State
}
