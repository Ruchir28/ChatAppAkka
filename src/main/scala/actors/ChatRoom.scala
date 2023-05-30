package actors

import akka.actor.{Actor, ActorRef}

case class ActorJoinRoom(user: ActorRef)
case class LeaveRoom(user: ActorRef)
case class BroadCastMessage(sender: ActorRef, message: String,user: String)
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
    case BroadCastMessage(sender: ActorRef,message: String,user: String) => {
      if(!users.contains(sender)) {
        sender ! InvalidConfig("Bsdk Bakchodi karbe!! User Not Found in this Room")
      } else {
        println(s"Received a Message Request for ChatRoom $message")
        users.foreach(userActor => userActor ! ReceiveChatRoomMessage(message = message, chatRoom = roomId, user))
      }
    }
  }
}