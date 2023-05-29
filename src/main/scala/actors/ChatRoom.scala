package actors

import akka.actor.{Actor, ActorRef}

case class ActorJoinRoom(user: ActorRef)
case class LeaveRoom(user: ActorRef)
case class BroadCastMessage(sender: ActorRef, message: String)
class ChatRoom(roomId: String) extends Actor {

  var users: Set[ActorRef] = Set.empty
  override def receive: Receive = {
    case ActorJoinRoom(user: ActorRef) => {
      println(s"User -> $user Joined Room")
      users = users + user
    }
    case LeaveRoom(user: ActorRef) => {
      users = users - user
    }
    case BroadCastMessage(sender: ActorRef,message: String) => {
      println(s"Received a Message Request for ChatRoom $message")
      users.foreach(user => user ! ReceiveMessage(message = message, chatRoom = roomId))
    }
  }
}


// USER -> ROOM MANAGER(ROOM_ID)

// ROOM MANAGHER