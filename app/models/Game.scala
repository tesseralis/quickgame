package models 

import scala.util.Try
import play.api.libs.json.JsValue

/**
 * The base trait that defines the necessary elements of a game.
 */
trait Game {
  /** Players are represented as integers. */
  type Player = Int

  /** The abstract type of the internal game state. */
  type State

  /** The abstract type of an action in the game. */
  type Move

  /** The number of players in this game. */
  def numPlayers: Int
  
  /** The initial state of the game. */
  def init: State

  /** Returns true if the game has reached a stopping point. */
  def isFinal(q: State): Boolean

  /** The result of a player moving somewhere. */
  def transition(q: State, m: Move, p: Player): Try[State]

  // TODO Separate out this functionality.
  /** How to convert a move from JSON input. */
  def moveFromJson(input: JsValue): Option[Move]

  /** How to transform a state into JSON. */
  def stateToJson(input: State): JsValue
}
