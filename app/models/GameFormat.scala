package models

import play.api.libs.json.JsValue

trait GameFormat { this: Game =>
  /** How to convert a move from JSON input. */
  def moveFromJson(input: JsValue): Option[Move]

  /** How to transform a state into JSON. */
  def stateToJson(input: State): JsValue
}
