package actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

case class GetUserActor(name: String)
class UserManager(system: ActorSystem) extends Actor {

  var userMap: Map[String,ActorRef] = Map()
  override def receive: Receive = {
    case GetUserActor(name: String) => {
      if(userMap.contains(name)) {
        sender() ! userMap(name)
      }else {
        val userActor = system.actorOf(Props(new User(name)))
        userMap = userMap + (name -> userActor)
        sender() ! userActor
      }
    }
  }
}
