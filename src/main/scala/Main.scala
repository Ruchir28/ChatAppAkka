import actors.{ChatRoom, GetUserActor, SetOutGoingActor, User, UserManager}
import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.{handleWebSocketMessages, path}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}


object Main extends App{
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()


  val userManager = actorSystem.actorOf(Props(new UserManager(actorSystem)))


  //  val chatRoom = actorSystem.actorOf(Props[ChatRoom],"ChatRoom1")
//
//  val user1 = actorSystem.actorOf(Props(new User("John")),"USER1")
//  val user2 = actorSystem.actorOf(Props(new User("Anik")),"USER2")
//
//  chatRoom ! JoinRoom(user1)
//  chatRoom ! JoinRoom(user2)
//
//
//  chatRoom ! BroadCastMessage(user1,"Hello There !!")

  def websocketFlow(username: String) : Flow[Message, Message, _] = {

    implicit val timeout: Timeout = Timeout(3.seconds)
    val userActorFuture: Future[ActorRef] = (userManager ? GetUserActor(username)).mapTo[ActorRef]

    val outgoingMessages: Source[Message, _] = Source.actorRef[TextMessage.Strict](
      bufferSize = 10,
      overflowStrategy = OverflowStrategy.dropHead
    ).mapMaterializedValue { outgoingActorRef =>
      // ask the UserManagerActor for the UserActor
      userActorFuture.onComplete {
        case Success(userActor) => userActor ! SetOutGoingActor(outgoingActorRef)
        case Failure(e) => println(s"Error retrieving user actor: $e")
      }
      NotUsed
    }

    val incomingMessages: Sink[Message, _] = Flow[Message].map {
      case TextMessage.Strict(text) => userActorFuture.map(_ ! text)
      case _ => // handle other types of messages, or ignore them
    }.to(Sink.ignore)

    Flow.fromSinkAndSourceCoupled(incomingMessages, outgoingMessages)
  }

  val route =
    path("ws" / Segment) { username =>
      handleWebSocketMessages(websocketFlow(username))
    } ~
      path("test") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Test Page</h1>"))
        }
      }


  Http().newServerAt("localhost", 8080).bind(route)

  println("Server Started at port 8080")


}
