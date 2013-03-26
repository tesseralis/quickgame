package games.tictactoe

case object TicTacToeController extends common.controllers.GameController {
  import games.tictactoe._
  override def view = views.html.tictactoe()
  override def model = models.TicTacToeModel
  override def gameType = TicTacToe
}
