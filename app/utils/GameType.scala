package utils

/**
 * Interface between the core application and individual games.
 */
trait GameType {
  /**
   * The function that returns the HTML view for this game type.
   */
  def view: (String, play.api.mvc.RequestHeader) => play.api.templates.Html

  /**
   * The properties of the actor model for this game.
   * todo: I'd like to just store the type for the game, but that's not possible because of type
   * erasure. Oh well, this way, I suppose we can specify the configuration of the properties.
   */
   def props: akka.actor.Props
}
