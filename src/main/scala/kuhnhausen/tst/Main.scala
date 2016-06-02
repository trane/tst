package kuhnhausen.tst

import io.finch._

import com.twitter.finagle.param.Stats
import com.twitter.server.TwitterServer
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Await
import io.finch.circe._
import io.circe._
import io.circe.generic.auto._
import kuhnhausen.tst.TickSlackToe.InMemoryGameStore

object Main extends TwitterServer {
  import Messages._
  import Slack._
  import Endpoints._
  implicit val myStore = InMemoryGameStore.empty
  implicit val validator = FastValidator

  val api: Endpoint[Message] = post("tst" :: messageResp) {msg: Message =>
    Ok(msg)
  }

  def main(): Unit = {
    val server = Http.server
        .configured(Stats(statsReceiver))
        .serve(":8080", api.toService)

    onExit { server.close() }

    Await.ready(adminHttpServer)
  }
}
