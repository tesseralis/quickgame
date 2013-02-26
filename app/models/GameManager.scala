package models

import scala.util._
import scala.concurrent.duration._
import scala.concurrent._

import akka.actor._

import akka.util.Timeout
import akka.pattern.ask

import play.api._
import play.api.libs.json._
import play.api.libs.concurrent._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits._

import play.api.Play.current


object GameManager {
  case class CreateGame(g: GameType)
  case class JoinGame(g: GameType, id: String, username: String)
  case class ContainsGame(g: GameType, id: String)

  implicit val timeout = Timeout(10.seconds)

  type WebSocket[A] = (Iteratee[A, _], Enumerator[A])
  /** Create a new random ID string. */
  def generateId(isNew: String => Boolean): String = {
    val id = Random.alphanumeric.take(5).mkString
    if (isNew(id)) id else generateId(isNew)
  }

  def errorWebSocket(error: String) = {
    val iteratee = Done[JsValue, Unit]((), Input.EOF)
    val enumerator = Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))

    (iteratee, enumerator)
  }
}

import GameManager._

trait GameManager {
  /* Create a new game of the specified type. */
  def create(g: GameType): Future[String]

  /* Check whether the given game exists. */
  def contains(g: GameType, id: String): Future[Boolean]

  /* Join the given game. */
  def join(g: GameType, id: String, username: String): Future[WebSocket[JsValue]]
}

class GameManagerImpl extends GameManager {
  val ctx = TypedActor.context

  var games: Map[(GameType, String), ActorRef] = Map.empty

  override def create(g: GameType) = Future {
    val id = generateId(id => !games.contains((g, id)))
    games += ((g, id) -> ctx.actorOf(Props(new ChatRoom(id))))
    id
  }

  override def contains(g: GameType, id: String) = Future { games.contains((g, id)) }

  override def join(g: GameType, id: String, username: String) =
    games.get((g, id)) map { room =>
      (room ? Join(username)) map {
        case Connected(enumerator) => {
          val iteratee = Iteratee.foreach[JsValue] { event =>
            room ! Talk(username, (event \ "text").as[String])
          }.mapDone { _ =>
            room ! Quit(username)
          }
          (iteratee, enumerator)
        }
        case CannotConnect(error) => errorWebSocket(error)
      }
    } getOrElse {
      Future(errorWebSocket(s"Could not find $g/$id"))
    }
}
