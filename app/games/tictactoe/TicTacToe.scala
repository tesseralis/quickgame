package games.tictactoe

import common.{GameType, GameAdapter}

case object TicTacToe extends GameType with GameAdapter {
  override def view = views.html.tictactoe()
  override def model = models.TicTacToeModel
}
