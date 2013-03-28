package common.models

/**
 * The base class for all game state machine definitions.
 */
abstract class Game1[S, M] {
  /** For now, represent players as integers */
  type Player = Int
  
  type State = S
  
  type Move = M

  /** The number of players in this game. */
  def numPlayers: Int
  
  /** The initial state of the game. */
  def init: State

  def isFinal(state: State): Boolean

  def transition(state: State, player: Player, move: Move): scala.util.Try[State]
}
