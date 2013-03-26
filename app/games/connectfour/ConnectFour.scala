package games.connectfour

case object ConnectFour extends common.GameType {
  override def view = views.html.connectfour()
  override def model = models.ConnectFourModel
}
