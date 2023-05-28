package actors

import akka.actor.{Actor, ActorRef}
import utils.{JoinRoom, LeaveRoom, BroadCastMessage}

class ChatRoom extends Actor {

  var users: Set[ActorRef] = Set.empty
  override def receive: Receive = {
    case JoinRoom(user: ActorRef) => {
      users += user
    }
    case LeaveRoom(user: ActorRef) => {
      users -= user
    }
    case BroadCastMessage(sender: ActorRef,message: String) => {
      users.foreach(user => user ! message)
    }
  }
}
