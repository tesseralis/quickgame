package games.connectfour

case object ConnectFour extends utils.GameType {
  override def view = views.html.connectfour.render
  override def model = models.ConnectFourModel
}
