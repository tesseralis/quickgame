package games
import common.{GameType, GameAdapter}

package object connectfour {
  case object ConnectFour extends GameType with GameAdapter {
    override def view = views.html.connectfour()
    override def model = models.ConnectFourModel
  }
}
