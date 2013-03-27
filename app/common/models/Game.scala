package common.models 

import scala.util.Try

/**
 * The base trait that defines the necessary elements of a game.
 */
trait Game {
  /** Players are represented as integers. */
  type Player = Int

  /** Representation of a state in the game. */
  trait AbstractState {
    /** Returns true if the game has reached a stopping point. */
    def isFinal: Boolean

    /** The result of a player moving somewhere. */
    def transition(m: Move, p: Player): Try[State]
  }

  /** The abstract type of the internal game state. */
  type State <: AbstractState

  /** The abstract type of an action in the game. */
  type Move

  /** The number of players in this game. */
  def numPlayers: Int
  
  /** The initial state of the game. */
  def init: State
}
