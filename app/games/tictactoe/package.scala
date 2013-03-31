package games

import common.{GameType, GameAdapter}

package object tictactoe {

  case object TicTacToe extends GameType with GameAdapter {
    override def view = views.html.tictactoe()
    override def model = models.TicTacToeModel
  }
}
