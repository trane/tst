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

  // return a PlayMessage or NotAllowedMessage if user doesn't have permissions to start/accept a game
  def play(gameId: GameId, user: User)(implicit store: GameStore): Future[Message] =
    pendingOrGame(gameId, user).map(either => either match {
      case Left(PendingGame(_, p)) if !validUser(user, p) => NotAllowedMessage
      case Right(game) if !validUser(user, game) => NotAllowedMessage
      case _ => PlayMessage(either)
    })

  // return a MarkMessage, or NotAllowedMessage if request is from invalid user, or NoGameMessage if not game exists
  def mark(gameId: GameId, user: User, num: Int)(implicit store: GameStore): Future[Message] =
    store.getGame(gameId).map(maybeGame =>
      maybeGame.map(game =>
        if (validUser(user, game)) MarkMessage(game.mark(num))
        else NotAllowedMessage
      ) getOrElse (NoGameMessage)
    )

  // return ForfeitMessage if valid to forfeit, NotAllowedMessage if from invalid user, or NoGameMessage if no game exists
  def forfeit(gameId: GameId, user: User)(implicit store: GameStore): Future[Message] =
    store.getGame(gameId).map(maybeGame =>
      maybeGame.map(game =>
        game.players.get(user.id).map(player =>
          ForfeitMessage(game.copy(winner = Some(game.players.other(player))), player)
        ) getOrElse (NotAllowedMessage)
      ) getOrElse (NoGameMessage)
    )

  def unknown(text: Text): Future[Message] =
    Future.value[Message](UnrecognizedMessage(text.text))

  /**
   * If a game exists, return Right(:game)
   * Otherwise, if there is a pending game, return Right(:game) with players completed
   * Otherwise, this is a new game pending game and return Left(:pendinggame)
   */
  def pendingOrGame(gameId: GameId, user: User)(implicit store: GameStore): Future[Either[PendingGame, Game]] = {
    store.getGame(gameId).flatMap(game => game match {
      case None => {
        store.getPending(gameId).flatMap(pend => pend match {
          case None => store.updatePending(
            PendingGame(gameId, Player(user.id, user.name, Challenger))
          ).map(Left(_))
          case Some(p) => store.updateGame(
            Game(gameId, Players(p.player, Player(user.id, user.name, Opponent)), Board.empty)
          ).map(Right(_))
        })
      }
      case Some(g) => Future.value(Right(g))
    })
  }

}
