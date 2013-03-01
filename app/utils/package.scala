import play.api.libs.iteratee.{Iteratee, Enumerator}

package object utils {
  /** Convenience alias for Play's representation of a Websocket */
  type WebSocket[A] = (Iteratee[A, _], Enumerator[A])

  /** Create a new random ID string. */
  @scala.annotation.tailrec
  def generateId(length: Int, isNew: String => Boolean = _ => true): String = {
    // todo Case sensitive urls is very bad
    val id = scala.util.Random.alphanumeric.take(length).mkString
    if (isNew(id)) id else generateId(length, isNew)
  }

  /**
   * Interface between the core application and individual games.
   */
  trait GameType {
    /**
     * The function that returns the HTML view for this game type.
     */
    def view: (String, String, play.api.mvc.RequestHeader) => play.api.templates.Html

    /**
     * The properties of the actor model for this game.
     * todo: I'd like to just store the type for the game, but that's not possible because of type
     * erasure. Oh well, this way, I suppose we can specify the configuration of the properties.
     */
     def props: akka.actor.Props
  }
}
