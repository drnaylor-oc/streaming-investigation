package controllers

import akka.stream.IOResult
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.RunnableGraph
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.Files
import play.api.libs.streams.Accumulator
import play.api.mvc.BaseController
import play.api.mvc.BodyParser
import play.api.mvc.Request

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait StreamingParser {
  self: BaseController =>

  implicit val materializer: Materializer
  implicit val executionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

  def streamingFromMemory: BodyParser[Source[ByteString, _]] = BodyParser { _ =>
    Accumulator.source[ByteString].map(Right.apply)
  }

  def streamingFromFile: BodyParser[Source[ByteString, _]] = parse.temporaryFile.map { tempFile =>
    FileIO.fromPath(tempFile.path)
  }

  def streamFromTemporaryFile[A](block: Source[ByteString, Future[IOResult]] => RunnableGraph[Future[A]])(implicit request: Request[Files.TemporaryFile]): Future[A] =
    block(FileIO.fromPath(request.body.path)).run()

}
