package games.tictactoe

import utils.GameType

case object TicTacToe extends GameType {
  import games.tictactoe._
  override def view = views.html.tictactoe.render
  override def model = models.TicTacToeModel
}
