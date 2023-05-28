package actors

import akka.actor.{Actor, ActorRef}
import akka.http.scaladsl.model.ws.TextMessage

case class SetOutGoingActor(actor: ActorRef)
case class User(name: String) extends Actor {

  var outgoingActorRef: ActorRef = null
  override def receive: Receive = {
    case message: String => {
      println(s"Message for $name is \"$message\" ")
      outgoingActorRef ! TextMessage.Strict(s"Echo: $message")
    }
    case SetOutGoingActor(actorRef: ActorRef)  => {
      outgoingActorRef = actorRef
    }
  }
}
