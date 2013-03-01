package utils


/** Defines the interface for a game. */
trait GameType {
  /**
   * Return the function that returns the HTML view for this game type.
   */
  def view: (String, String, play.api.mvc.RequestHeader) => play.api.templates.Html

  /**
   * The type of the room for the game
   */
   // TODO: Can we replace this with a type?
   def props: akka.actor.Props
}

/* DEFINE GAME TYPE CONFIGURATION HERE */

