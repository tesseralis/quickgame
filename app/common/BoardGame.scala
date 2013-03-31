package common

import scala.util.{Try, Success, Failure}

/**
 * A subclass that defines a turn-based board game and defines game 
 * initialization and transitions based on the board.
 */
trait BoardGame extends Game {
  /**
   * The representation for a board in this game.
   */
  type Board

  /**
   * The state of the board when the game begins.
   */
  protected def boardInit: Board

  /** 
   * Pick the next state given the board and current player.
   */
  protected def boardTransition(board: Board, player: Player, move: Move): Try[State]

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

  /* Start the game with the first player and the initial board state. */
  final override def init = Turn(0, boardInit)

  /* Override transition to choose turn or check for the current player. */
  final override def transition(state: State, player: Player, move: Move) = state match {
    case Turn(current, board) => Try {
      require(player == current, s"It is not $player's turn")
      boardTransition(board, player, move).get
    }
    case _ => Failure(new IllegalStateException("The game has ended."))
  }

}
