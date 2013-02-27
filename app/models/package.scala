import play.api.libs.iteratee._
import play.api.libs.json._

package object models {
  
  case class Join(username: String)
  case class Quit(username: String)
  case class Talk(username: String, text: String)
  case class NotifyJoin(username: String)

  case class Connected(enumerator: Enumerator[JsValue])
  case class CannotConnect(msg: String)
}
