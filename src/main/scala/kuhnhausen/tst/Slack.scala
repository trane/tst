package kuhnhausen.tst

import io.finch._
import com.twitter.finagle.http.{ParamMap, Method, Request, RequestBuilder}
import com.twitter.io.{ Buf, Reader }
import com.twitter.util.Future
import io.circe.generic.auto._
import io.finch.circe._
import kuhnhausen.tst.TickSlackToe.{InMemoryGameStore, GameStore}
import kuhnhausen.tst.Messages._

/**
  * A Slack Slash command request comes in looking something like this:
  *
  * token=jJGfagzhcGLFIXoPSSL4ssd3
  * team_id=T0001
  * team_domain=example
  * channel_id=C2147483705
  * channel_name=test
  * user_id=U2147483697
  * user_name=Steve
  * command=/tst
  * text=help
  * response_url=https://hooks.slack.com/commands/1234/5678
  */
object Slack {

  case class Token(token: String)
  case class Team(id: String, domain: String)
  case class Channel(id: String, name: String)
  case class User(id: String, name: String)
  case class Command(command: String)
  case class Text(text: String)
  case class ResponseURL(response_url: String)

  implicit val myStore = InMemoryGameStore.empty
  implicit val validator = FastValidator

  case class SlackRequest(token: Token, team: Team, channel: Channel,
                          user: User, cmd: Command, text: Text, url: ResponseURL) {
    def gameId: GameId =
      GameId(team.id, channel.id)
  }

  def slackRequestToMessage(req: SlackRequest)(implicit store: GameStore): Future[Message] =
    req.text.text.split(" ").toList match {
      case "help" :: Nil        => help
      case "show" :: Nil        => show(req.gameId)
      case "play" :: Nil        => play(req.gameId, req.user)
      case "mark" :: num :: Nil => mark(req.gameId, req.user, num.toInt)
      case "forfeit" :: Nil     => forfeit(req.gameId, req.user)
      case _                    => unknown(req.text)
    }

  def validUser(user: User, game: Game): Boolean =
    game.players.one.id == user.id

  def validUser(user: User, player: Player): Boolean =
    player.id == user.id

  // smart constructor for creating a help message
  def help: Future[Message] =
    Future.value(HelpMessage)

  // return a ShowMessage or NoGameMessage if no game exists
  def show(gameId: GameId)(implicit store: GameStore): Future[Message] =
    store.getGame(gameId).map(maybeGame =>
      maybeGame.map(game =>
        ShowMessage(game)
      ) getOrElse (NoGameMessage)
    )

  // return a MarkMessage, or NotAllowedMessage if request is from invalid user, or NoGameMessage if not game exists
  def mark(gameId: GameId, user: User, num: Int)(implicit store: GameStore): Future[Message] =
    store.getGame(gameId).flatMap(_ match {
      case None => Future.value(NoGameMessage)
      case Some(game) if game.winner.isDefined => Future.value(GameOverMessage)
      case Some(game) if user.id == game.players.one.id => store.updateGame(game.mark(num)).map(MarkMessage(_))
      case Some(game) if user.id == game.players.two.id => Future.value(NotYourTurnMessage)
      case _ => Future.value(NotAllowedMessage)
    })

  // return ForfeitMessage if valid to forfeit, NotAllowedMessage if from invalid user, or NoGameMessage if no game exists
  def forfeit(gameId: GameId, user: User)(implicit store: GameStore): Future[Message] =
    store.getGame(gameId).flatMap(maybeGame =>
      maybeGame.map(game =>
        game.players.get(user.id).map(player => game.winner match {
          case None => store.updateGame(
                         game.copy(winner = Some(game.players.other(player)))
                       ).map(_ => ForfeitMessage(player))
          case Some(winner) => Future.value(GameOverMessage)
        }) getOrElse (Future.value(NotAllowedMessage))
      ) getOrElse (Future.value(NoGameMessage))
    )

  def unknown(text: Text): Future[Message] =
    Future.value[Message](UnrecognizedMessage(text.text))

  /**
    * If there is no PendingGame and no Game
    *   create a PendingGame and return NewGameMessage
    * If there is no PendingGame, but there is a Game
    *   return GameInProgressMessage
    * If there is a PendingGame
    *   create a new Game with this player (even if it's someone playing with themselves)
    *   return GameStartedMessage
    */
  def play(gameId: GameId, user: User)(implicit store: GameStore): Future[Message] =
    store.getPending(gameId).flatMap(_ match {
      case Some(pg) => store.updateGame(
        Game(gameId, Players(pg.player, Player(user.id, user.name, Opponent)), Board.empty)
      ).map(GameStartedMessage(_))
      case None => store.getGame(gameId).flatMap(_ match {
        case None => store.updatePending(
          PendingGame(gameId, Player(user.id, user.name, Challenger))
        ).map(NewGameMessage(_))
        case Some(g) => Future.value(GameInProgressMessage(g))
      })
    })

}
