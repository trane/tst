package kuhnhausen

import com.twitter.finagle.http.ParamMap
import kuhnhausen.tst.Slack.User
import kuhnhausen.tst.TickSlackToe.InMemoryGameStore
import kuhnhausen.tst._
import org.scalatest._
import io.circe._
import io.finch.circe._
import io.circe.generic.auto._

class SlackSpec extends FlatSpec with Matchers {
  import tst.TestHelper._

  implicit val myStore = InMemoryGameStore.empty
  implicit val validator = FastValidator

  behavior of "Slack"
  val service = msgEndpoint.toService

  "help" should "be a HelpMessage" in {
    val req = request
    val res = result(service.apply(req))
    val exp = result(Slack.help)
    toJson(res) should be(toJson(exp))
  }

  "show" should "be a NoGameMessage when there is no game" in {
    val gameId = generateGameId
    val req = request(ParamMap(("text", "show"), ("team_id", gameId.teamId), ("channel_id", gameId.channelId)))
    val res = result(service.apply(req))
    val exp = result(Slack.show(gameId))
    toJson(res) should be(toJson(exp))
  }

  "show" should "be a ShowMessage when there is a game" in {
    val game = generateGame
    myStore.updateGame(game)
    val req = request(ParamMap(("text", "show"), ("team_id", game.id.teamId), ("channel_id", game.id.channelId)))
    val res = result(service.apply(req))
    val exp = result(Slack.show(game.id))
    toJson(res) should be(toJson(exp))
  }

  "play" should "return NewGameMessage if this is the first play command, and store pending game" in {
    val game = generatePendingGame
    val req = request(ParamMap(
      ("text", "play"),
      ("team_id", game.id.teamId),
      ("channel_id", game.id.channelId),
      ("user_id", game.player.id),
      ("user_name", game.player.name)
    ))
    val res = result(service.apply(req))
    val exp = Messages.NewGameMessage(game)
    result(myStore.getPending(game.id)).get should be(game)
    toJson(res) should be(toJson(exp))
  }

  "play" should "return GameStartedMessage if this is in response to an original play command" in {
    val game = generateGame
    myStore.updatePending(PendingGame(game.id, game.players.one))
    val req = request(ParamMap(
      ("text", "play"),
      ("team_id", game.id.teamId),
      ("channel_id", game.id.channelId),
      ("user_id", game.players.two.id),
      ("user_name", game.players.two.name)
    ))
    val res = result(service.apply(req))
    val exp = Messages.GameStartedMessage(game)
    result(myStore.getGame(game.id)).get should be(game)
    toJson(res) should be(toJson(exp))
  }

  "play" should "return GameInProgressMessage if a game has already started" in {
    val game = generateGame
    myStore.updateGame(game)
    val req = request(ParamMap(
      ("text", "play"),
      ("team_id", game.id.teamId),
      ("channel_id", game.id.channelId),
      ("user_id", game.players.one.id),
      ("user_name", game.players.one.name)
    ))
    val res = result(service.apply(req))
    val exp = Messages.GameInProgressMessage(game)
    toJson(res) should be(toJson(exp))
  }

  "mark" should "return successful MarkMessage if successful move and update the game" in {
    val game = generateGame
    myStore.updateGame(game)
    val req = request(ParamMap(
      ("text", "mark 4"),
      ("team_id", game.id.teamId),
      ("channel_id", game.id.channelId),
      ("user_id", game.players.one.id),
      ("user_name", game.players.one.name)
    ))
    val res = result(service.apply(req))
    res.contentString.contains("white_large_square") should be(true)
    result(myStore.getGame(game.id)) should not be(game)
  }

  "mark" should "return unsuccessful MarkMessage if bad move and not update the game" in {
    val game = generateGame.mark(4)
    myStore.updateGame(game)
    val req = request(ParamMap(
      ("text", "mark 4"),
      ("team_id", game.id.teamId),
      ("channel_id", game.id.channelId),
      ("user_id", game.players.one.id),
      ("user_name", game.players.one.name)
    ))
    val res = result(service.apply(req))
    res.contentString.contains("Invalid move") should be(true)
    result(myStore.getGame(game.id)).get should be(game)
  }

  "mark" should "return winning MarkMessage if winning move and update the game" in {
    /**
      * x x .
      * o o .
      * . . .
      */
    val game = generateGame.mark(1).mark(4).mark(2).mark(5)
    myStore.updateGame(game)
    val req = request(ParamMap(
      ("text", "mark 3"),
      ("team_id", game.id.teamId),
      ("channel_id", game.id.channelId),
      ("user_id", game.players.one.id),
      ("user_name", game.players.one.name)
    ))
    val res = result(service.apply(req))
    res.contentString.contains("won") should be(true)
    val newGame = result(myStore.getGame(game.id)).get
    newGame should not be(game)
    newGame.winner.get should be(game.players.one)
  }

  "forfeit" should "return a ForfeitMessage and update winners if the game is still going" in {
    val game = generateGame
    myStore.updateGame(game)
    val req = request(ParamMap(
      ("text", "forfeit"),
      ("team_id", game.id.teamId),
      ("channel_id", game.id.channelId),
      ("user_id", game.players.one.id),
      ("user_name", game.players.one.name)
    ))
    val res = result(service.apply(req))
    res.contentString.contains(s"${game.players.one.name}") should be(true)
    res.contentString.contains(s"forfeits the game") should be(true)
    val newGame = result(myStore.getGame(game.id)).get
    newGame should not be(game)
    newGame.winner.get should be(game.players.two)
  }

  "forfeit" should "return a ForfeitMessage error if the game is not going" in {
    val game = generateGame
    myStore.updateGame(game)
    val req = request(ParamMap(
      ("text", "forfeit"),
      ("team_id", game.id.teamId),
      ("channel_id", game.id.channelId),
      ("user_id", game.players.one.id),
      ("user_name", game.players.one.name)
    ))
    result(service.apply(req)) // forfeit
    val res = result(service.apply(req))
    println(res.contentString)
    res.contentString.contains(s"Game already over") should be(true)
  }
}

