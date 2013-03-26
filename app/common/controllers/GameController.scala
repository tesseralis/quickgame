package common.controllers

import common.GameType
import common.models.{Game, GameFormat}
import play.api.templates.Html

trait GameController {
  def view: Html

  def model: Game with GameFormat

  def gameType: GameType
}
