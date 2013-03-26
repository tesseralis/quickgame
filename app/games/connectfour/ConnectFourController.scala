package games.connectfour
case object ConnectFourController extends common.controllers.GameController {
  override def view = views.html.connectfour()
  override def model = models.ConnectFourModel
  override def gameType = ConnectFour
}
