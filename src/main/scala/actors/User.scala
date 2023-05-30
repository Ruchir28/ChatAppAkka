package actors

import akka.actor.{Actor, ActorRef, Terminated}
import akka.http.scaladsl.model.ws.TextMessage
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global


import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

sealed trait UserCommand
case class SetOutGoingActor(actor: ActorRef) extends UserCommand
case class JoinChatRoom(chatRoomId: String) extends UserCommand
case class InvalidConfig(message: String) extends UserCommand

case class ChatGroupMessage(message: String,chatRoom: String) extends UserCommand
case class ReceiveChatRoomMessage(message: String, chatRoom: String,user: String) extends UserCommand

case class DirectMessage(message: String, toUser: String) extends UserCommand

case class ReceiveDirectMessage(message: String, fromUser: String) extends UserCommand
case class User(name: String) extends Actor {

  var outgoingActorRef: ActorRef = null
  override def receive: Receive = {
    case message: String => {
      println(s"Message for $name is \"$message\" ")
      outgoingActorRef ! TextMessage.Strict(s"Echo: $message")
    }

    case ReceiveChatRoomMessage(message: String, chatRoom: String,user: String) => {
      val msg = s"Message : \"$message\" in Chatroom: $chatRoom by $user"
      outgoingActorRef ! TextMessage.Strict(msg)
    }

    case ChatGroupMessage(message,chatRoom) => {
      println(s"Message by $name is \"$message\" to Chatroom: $chatRoom")
      val chatRoomManager = context.actorSelection(s"/user/ChatRoomManager")
      chatRoomManager ! SendMessage(chatRoom,message,name)
    }

    case JoinChatRoom(chatRoomId: String) => {
      println(s"$name requested to Join: $chatRoomId")
      val chatRoomManager = context.actorSelection(s"/user/ChatRoomManager")
      chatRoomManager ! JoinRoom(roomId = chatRoomId)
    }

    case DirectMessage(message,toUser) => {
      val userManager = context.actorSelection(s"/user/UserManager")
      implicit val timeout: Timeout = Timeout(3.seconds)
      val userActorFuture = userManager ? GetUserActor(toUser)
      userActorFuture.onComplete {
        case Success(userActor: ActorRef) => {
          userActor ! ReceiveDirectMessage(message,name)
        }
        case Failure(exception) => {
          self ! InvalidConfig("Error in Finding User")
        }
      }
    }

    case ReceiveDirectMessage(message,fromUser) => {
      println(s"Received \" $message \" from $fromUser")
      outgoingActorRef ! TextMessage.Strict(s"Message  \" $message \" from $fromUser")
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
