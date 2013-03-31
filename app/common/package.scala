package object common {

  /**
   * Marker trait for a type of game.
   * This allows us to make a check for the existence of a type of game
   * during route time, as well as having a non-string identifier for
   * each game.
   */
  trait GameType

  /**
   * Convenience type defining what our game model needs.
   */
  type GameModel = Game with GameFormat

  /**
   * They type of our view.
   */
  type GameView = play.api.templates.Html

  /**
   * An adapter that lets the application controller know what
   * model and view to use with the game and the corresponding type.
   */
  trait GameAdapter extends GameType {

    def view: GameView

    def model: GameModel
  }

}
