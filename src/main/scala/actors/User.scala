package actors

import akka.actor.{Actor, ActorRef, Terminated}
import akka.http.scaladsl.model.ws.TextMessage

sealed trait UserCommand
case class SetOutGoingActor(actor: ActorRef) extends UserCommand
case class JoinChatRoom(chatRoomId: String) extends UserCommand
case class InvalidConfig(message: String) extends UserCommand

case class ChatGroupMessage(message: String,chatRoom: String) extends UserCommand
case class User(name: String) extends Actor {

  var outgoingActorRef: ActorRef = null
  override def receive: Receive = {
    case message: String => {
      println(s"Message for $name is \"$message\" ")
      outgoingActorRef ! TextMessage.Strict(s"Echo: $message")
    }
    case ChatGroupMessage(message,chatRoom) => {
      println(s"Message by $name is \"$message\" to Chatroom: $chatRoom")
      outgoingActorRef ! TextMessage.Strict(s"Echo: $message")
    }

    case JoinChatRoom(chatRoomId: String) => {
      println(s"$name requested to Join: $chatRoomId")
    }
    case InvalidConfig(message) => outgoingActorRef ! TextMessage.Strict(s"Invalid Config: $message")

    case SetOutGoingActor(actorRef: ActorRef)  => {
      outgoingActorRef = actorRef
      context.watch(outgoingActorRef)
    }

    case Terminated(actor) => {
      if (actor == outgoingActorRef) {
        println(s"User $name Disconnected From Chat ")
      }
    }
  }
}
