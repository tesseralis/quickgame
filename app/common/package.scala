package object common {

  /**
   * Marker trait for a type of game.
   */
  trait GameType

  /**
   * Convenience type defining what our game model needs.
   */
  type GameModel = Game with GameFormat

  /**
   * An adapter that lets the application controller know what
   * model and view to use with the game and the corresponding type.
   */
  trait GameAdapter {
    def view: play.api.templates.Html

    def model: GameModel

    def gameType: GameType
  }

}
