import actors.{ChatGroupMessage, ChatRoomManager, DirectMessage, GetUserActor, InvalidConfig, JoinChatRoom, SetOutGoingActor, UserCommand, UserManager}
import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.{handleWebSocketMessages, path}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.OverflowStrategy
import akka.util.Timeout
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}


object Main extends App{
  implicit val actorSystem = ActorSystem("Main")


  val userManager = actorSystem.actorOf(Props(new UserManager(actorSystem)),"UserManager")
  val chatRoomManager = actorSystem.actorOf(Props[ChatRoomManager],"ChatRoomManager")

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

    def ParseMessages(inputJson: String): UserCommand = {
      val json = Json.parse(inputJson)
      val command: String = (json \ "command").as[String]
      command match {
        case "JoinRoom" => {
          val roomId = (json \ "roomId").as[String]
          if(roomId.isEmpty || roomId.equals("")) {
            InvalidConfig(s"Invlaid Room Id: $roomId")
          } else {
            JoinChatRoom(roomId)
          }
        }
        case "Message" => {
          val message = (json \ "message").as[String]
          val roomId =  (json \ "roomId").as[String]
          ChatGroupMessage(message = message, chatRoom = roomId)
        }
        case "DirectMessage" => {
          val message = (json \ "message").as[String]
          val userName = (json \ "userName").as[String]
          DirectMessage(message = message, toUser = userName)
        }
        case value: Any => {
          InvalidConfig(s"Invalid Command $value from user")
        }
      }
    }

    val incomingMessages: Sink[Message, _] = Flow[Message]
      .collect{ case TextMessage.Strict(text) => text }
      .map(ParseMessages)
      .map {command => userActorFuture.map(_ ! command)}
      .to(Sink.ignore)

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
