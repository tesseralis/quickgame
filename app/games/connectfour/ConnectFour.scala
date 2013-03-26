package games.connectfour

case object ConnectFour extends common.GameType {
  override def view = views.html.connectfour.render
  override def model = models.ConnectFourModel
}
