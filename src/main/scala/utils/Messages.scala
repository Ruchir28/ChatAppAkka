package utils

import akka.actor.ActorRef

case class JoinRoom(user: ActorRef)
case class LeaveRoom(user: ActorRef)
case class BroadCastMessage(sender: ActorRef,message: String)
