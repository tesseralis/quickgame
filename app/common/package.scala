package object common {
  import models.{Game, GameFormat}

  /**
   * Marker trait for a type of game.
   */
  trait GameType

  /**
   * An adapter that lets the application controller know what
   * model and view to use with the game and the corresponding type.
   */
  trait GameAdapter {
    def view: play.api.templates.Html

    def model: Game with GameFormat

    def gameType: GameType
  }

}
