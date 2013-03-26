package games.tictactoe

import common.GameType

case object TicTacToe extends GameType {
  import games.tictactoe._
  override def view = views.html.tictactoe()
  override def model = models.TicTacToeModel
}
