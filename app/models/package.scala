import play.api.libs.iteratee._
import play.api.libs.json._

package object models {

  case class Connected(iteratee: Iteratee[JsValue, _], enumerator: Enumerator[JsValue])
  case class CannotConnect(msg: String)
}
