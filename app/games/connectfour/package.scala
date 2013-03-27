package games
import common.{GameType, GameAdapter}

package object connectfour {
  case object ConnectFour extends GameType

  object Adapter extends GameAdapter {
    override def gameType = ConnectFour
    override def view = views.html.connectfour()
    override def model = models.ConnectFourModel
  }
}
