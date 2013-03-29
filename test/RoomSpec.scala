package test

import scala.concurrent.duration._

import org.scalatest._

import akka.actor._
import akka.testkit._

import common.models.Game1

import models.Room
import models.Room._

/*
 * Define a simple game to test the room on.
 */
object MockGame extends Game1[Int, Int] {
  override def numPlayers = 1
  override def init = 0
  override def isFinal(s: State) = false
  override def transition(s: State, p: Player, m: Move) =
    util.Success(0)
}

class RoomSpec(_system: ActorSystem) extends TestKit(_system) 
    with WordSpec
    with BeforeAndAfterAll
    with OneInstancePerTest {

  def this() = this(ActorSystem("RoomSpec"))

  val room = TestFSMRef(new Room(MockGame))

  override def afterAll {
    system.shutdown()
  }

  "A game room" should {

    "start properly" in {
      assert(room.stateData.members.isEmpty)
      info("Starts with no members.")

      assert(room.stateData.gamestate === MockGame.init)
      info("Starts with the initial game state.")

      assert(room.stateName === Idle)
      info("Starts in the Idle state.")
    }

    // TODO Make these shared tests between states

    "add and remove members" in {
      val client = TestProbe()
      client.send(room, Join("Nathan"))
      assert(room.stateData.members.size === 1)
      info("Successfully joined the room.")

      client.ref ! PoisonPill
      assert(room.stateData.members.isEmpty)
      info("Successfully left the room.")
    }

    "allow members to change their name" in {
      val client = TestProbe()
      client.send(room, Join("Nathan"))
      assert(room.stateData.members(client.ref).name === "Nathan")
      client.send(room, ChangeName("Sal"))
      assert(room.stateData.members(client.ref).name === "Sal")
    }

    "respond to update requests" in {
      val room = TestFSMRef(new Room(MockGame))
      val probe = TestProbe()
      probe.send(room, Join("Nathan"))
      probe.send(room, Update)
      probe.expectMsg(room.stateData)
    }
  }

  "A game room (when idle)" should {
  
    "add the first members as players" is (pending)

    "allow members to freely change player roles" is (pending)

    "allow members to chat" is (pending)

    "not start the game when there are not enough players" is (pending)

    "start the game when the command is called and there are enough players" is (pending)

    "not allow any other commands" is (pending)

  }

  "A game room (when playing)" should {

    "let a player make valid move" is (pending)

    "not allow a spectator to make a move" is (pending)

    "not allow a player to make an invalid move" is (pending)

    "pause the game if a player leaves" is (pending)
  }

  "A game room (when paused)" should {

    "resume when the start command is set" is (pending)

    "not allow any moves" is (pending)

  }

}
