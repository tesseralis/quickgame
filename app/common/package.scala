package object common {
  import models.{Game, GameFormat}

  /**
   * Marker trait for individual games.
   */
  trait GameType

  trait GameAdapter {
    def view: play.api.templates.Html

    def model: Game with GameFormat

    def gameType: GameType
  }

}
