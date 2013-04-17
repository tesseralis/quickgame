package common

/**
 * The base trait that defines the necessary elements of a game.
 */
trait Game {
  /** The abstract type of a player in the game. */
  type Player

  /** The abstract type of the internal game state. */
  type State

  /** The abstract type of an action in the game. */
  type Move

  /** The list of players in this game. */
  def players: Seq[Player]

  /** The number of players in this game. */
  def numPlayers: Int = players.length
  
  /** The initial state of the game. */
  def init: State

  /** Returns true if the game has reached a stopping point. */
  def isFinal(state: State): Boolean

  /** The result of a player moving somewhere. */
  def transition(state: State, player: Player, move: Move): util.Try[State]
}
