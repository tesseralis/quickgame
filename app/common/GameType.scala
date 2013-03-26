package common

/**
 * Interface between the core application and individual games.
 */
trait GameType {
  /**
   * The function that returns the HTML view for this game type.
   */
   def view: play.api.templates.Html

  /**
   * The definition of this game's actions.
   */
   def model: models.Game with models.GameFormat
}
