import play.api.mvc.PathBindable.Parsing
import common.GameType
/**
 * This package defines the implicit binders used in the routes object.
 */
package object binders {
  implicit object bindableGameType extends Parsing[GameType](
    controllers.Application.gameTypes.map(g => g.toString.toLowerCase -> g).toMap,
    _.toString.toLowerCase,
    (key: String, e: Exception) => e.getMessage
  )
}
