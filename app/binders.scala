import play.api.mvc.PathBindable.Parsing
/**
 * This package defines the implicit binders used in the routes object.
 */
package object binders {
  implicit object bindableGameType extends Parsing[utils.GameType](
    controllers.Application.gamesAvailable.map(g => g.toString.toLowerCase -> g).toMap,
    _.toString.toLowerCase,
    (key: String, e: Exception) => e.getMessage
  )
}
