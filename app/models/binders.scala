package models

import play.api.mvc.PathBindable

package object binders {
  implicit object bindableGameType extends PathBindable.Parsing[GameType](
    GameType.withName(_), _.toString.toLowerCase, (key: String, e: Exception) => e.getMessage
  )
}
