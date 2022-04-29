package controllers

import akka.stream.Materializer
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc._

import java.nio.charset.StandardCharsets
import javax.inject._
import scala.concurrent.Future
import scala.util.control.NonFatal

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents)(implicit val materializer: Materializer) extends BaseController
  with StreamingParser {

  def streamMemory(): Action[Source[ByteString, _]] = Action.async(streamingFromMemory) { implicit request: Request[Source[ByteString, _]] =>
    stream(request)
  }

  def streamFile(): Action[Source[ByteString, _]] = Action.async(streamingFromFile) { implicit request: Request[Source[ByteString, _]] =>
    stream(request)
  }

  def stream[R <: Request[Source[ByteString, _]]](implicit request: R): Future[Result] = {
    val future = request.body.toMat(Sink.fold("")((current: String, incoming: ByteString) => current + incoming.decodeString(StandardCharsets.UTF_8)))(Keep.right).run()

    future.map { completedFuture =>
      Ok(Json.obj("message" -> completedFuture))
    }.recover {
      case NonFatal(_) =>
        InternalServerError(Json.obj("error" -> "didn't work"))
    }
  }


  def streamMultipleTimes(): Action[Files.TemporaryFile] = Action.async(parse.temporaryFile) { implicit request: Request[Files.TemporaryFile] =>
    def internalStream(a: String) = streamFromTemporaryFile { source =>
      source.toMat(Sink.fold(a)((current: String, incoming: ByteString) => current + incoming.decodeString(StandardCharsets.UTF_8)))(Keep.right)
    }

    val f = for {
      a <- internalStream("0")
      b <- internalStream("1")
      x <- Future.successful((a, b))
    } yield x
    f.map { completedFuture =>
      Ok(Json.obj(
        "message1" -> completedFuture._1,
        "message2" -> completedFuture._2
      ))
    }.recover {
      case NonFatal(_) =>
        InternalServerError(Json.obj("error" -> "didn't work"))
    }
  }



}
