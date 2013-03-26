package controllers

import utils.GameType
import models._

import akka.actor.Props

object Games {
  /** 
   * Enumeration of the available games we have.
   * Edit this to define the games available in the application.
   */
  def values: Set[GameType] = Set(TicTacToe, ConnectFour)

  /** Mapping from the a uri-string to the game name. */
  def fromString: Map[String, GameType] = values.map(g => g.toString.toLowerCase -> g).toMap

  // DEFINE GAME CONTROLS HERE
  // todo We should really make each game a sub-project and define the types there. 
  case object TicTacToe extends GameType {
    import games.tictactoe._
    override def view = views.html.tictactoe.render
    override def model = models.TicTacToe
  }
  case object ConnectFour extends GameType {
    import games.connectfour._
    override def view = views.html.connectfour.render
    override def model = models.ConnectFour
  }
}
