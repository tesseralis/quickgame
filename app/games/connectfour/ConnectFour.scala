package games.connectfour
import common.{GameType, GameAdapter}

case object ConnectFour extends GameType with GameAdapter {
  override def view = views.html.connectfour()
  override def model = models.ConnectFourModel
}
