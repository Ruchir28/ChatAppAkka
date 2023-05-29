package actors

import akka.actor.{Actor, ActorRef, Props}
case class JoinRoom(roomId : String)
case class SendMessage(roomId: String, message: String)
class ChatRoomManager extends Actor {

  var roomIdMap: Map[String,ActorRef] = Map()
  override def receive: Receive = {
    case JoinRoom(roomId: String) => {
      println(s"Received Request by ${sender()} for Joining Room $roomId")
      if(roomIdMap.contains(roomId)) {
        roomIdMap.get(roomId).map(_ ! ActorJoinRoom(sender()))
      } else {
        val new_room = context.actorOf(Props(new ChatRoom(roomId)),roomId)
        roomIdMap = roomIdMap + (roomId -> new_room)
        new_room ! ActorJoinRoom(sender())
      }
    }
    case SendMessage(roomId,message) => {
      val roomActor = roomIdMap(roomId)
      roomActor ! BroadCastMessage(sender(),message)
    }

  }
}
