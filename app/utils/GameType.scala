package utils

import akka.actor.Props

/**
 * This class contains an enumeration on the games we have available.
 */

object GameType {
  implicit object bindableGameType 
    extends play.api.mvc.PathBindable.Parsing[GameType](
      withName(_),
      _.toString.toLowerCase,
      (key: String, e: Exception) => e.getMessage
    )

  def values: Set[GameType] = Set(Chat, Tictactoe, Connectfour)
  def withName(s: String): GameType = s.toLowerCase match {
    case "chat" => Chat
    case "tictactoe" => Tictactoe
    case "connectfour" => Connectfour
    case _ => throw new java.util.NoSuchElementException(s"Game $s not found.")
  }
}
sealed trait GameType {
  /**
   * Return the function that returns the HTML view for this game type.
   */
  def view: (String, String, play.api.mvc.RequestHeader) => play.api.templates.Html

  /**
   * The type of the room for the game
   */
   // TODO: Can we replace this with a type?
   def props: Props
}

/* DEFINE GAME TYPE CONFIGURATION HERE */

case object Chat extends GameType {
  override def view = views.html.chat.render
  override def props = Props[models.ChatRoom]
}
case object Tictactoe extends GameType {
  override def view = views.html.tictactoe.render
  override def props = Props[models.TicTacToeRoom]
}
case object Connectfour extends GameType {
  override def view = views.html.connectfour.render
  override def props = Props[models.ConnectFourRoom]
}
