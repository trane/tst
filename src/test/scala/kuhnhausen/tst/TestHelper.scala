package kuhnhausen.tst

import java.util.UUID

import com.twitter.util.{Await, Future}
import io.circe._
import io.circe.syntax._
import io.finch._
import com.twitter.finagle.http.{Response, ParamMap, Method, Request}
import kuhnhausen.tst.Messages.Message

object TestHelper {
  implicit val validator = FastValidator

  val baseParams = ParamMap(
    ("token", "jJGfagzhcGLFIXoPSSL4ssd3"),
    ("team_id","T0001"),
    ("team_domain","example"),
    ("channel_id","C2147483705"),
    ("channel_name","test"),
    ("user_id","U2147483697"),
    ("user_name","Steve"),
    ("command","/tst"),
    ("text","help"),
    ("response_url","https://hooks.slack.com/commands/1234/5678")
  )

  def request(paramMap: ParamMap): Request = {
    val params = baseParams ++ paramMap
    Request(Method.Get, s"/test${params.toString}")
  }

  def request: Request =
    request(ParamMap())

  val msgEndpoint: Endpoint[Message] = get("test" :: Endpoints.messageResp) {msg: Message =>
    Ok(msg)
  }

  def result[A](future: Future[A]): A =
    Await.result[A](future)

  def toJson(msg: Message): String =
    Encoder[Message].apply(msg).noSpaces

  def toJson(res: Response): String =
    res.contentString

  def randomString: String =
    UUID.randomUUID().toString

  def generateGameId: GameId =
    GameId(randomString, randomString)

  def generateChallenger: Player =
    Player(randomString, randomString, Challenger)

  def generateOpponent: Player =
    Player(randomString, randomString, Opponent)

  def generateGame: Game =
    Game(generateGameId, Players(generateChallenger, generateOpponent), Board.empty)

  def generatePendingGame: PendingGame =
    PendingGame(generateGameId, generateChallenger)
}
