package actors

import akka.actor.Actor
import utils.JoinRoom

class ChatRoomActor extends Actor{
  override def receive: Receive = {
    case JoinRoom(id: Int)
  }
}
