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
  override def numPlayers = 2
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
      val client = TestProbe()
      client.send(room, Join("Nathan"))
      client.send(room, Update)
      client.expectMsg(room.stateData)
    }
  }

  "A game room (when idle)" should {
  
    "add the first [numPlayers] members as players and the rest as spectators" in {
      val player0 = TestProbe()
      player0.send(room, Join("player0"))
      assert(room.stateData.members(player0.ref).role === Player(0))
      
      for (i <- 1 until MockGame.numPlayers) {
        val player = TestProbe()
        player.send(room, Join("player" + i))
        assert(room.stateData.members(player.ref).role === Player(i))
      }
      for (i <- 0 until 10) {
        val spectator = TestProbe()
        spectator.send(room, Join("spectator" + i))
        assert(room.stateData.members(spectator.ref).role === Spectator)
      }
      info("Correct roles for people inserted in order.")

      player0.ref ! PoisonPill
      val player1 = TestProbe()
      player1.send(room, Join("player100"))
      assert(room.stateData.members(player1.ref).role === Player(0))
      info("Correct roles for players inserted out of order.")
    }

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
