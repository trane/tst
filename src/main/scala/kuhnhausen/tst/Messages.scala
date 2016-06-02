package kuhnhausen.tst
import io.circe._
import io.circe.generic.auto._

object Messages {

  /**
    * There are two types of messages, ephemeral (only displayed to the user issuing the command)
    * and in_channel (visible by all users)
    */
  sealed trait SlackResponseType
  case object EphemeralResponse extends SlackResponseType
  case object InChannelResponse extends SlackResponseType

  /**
    * Represents the base contract of response messages to Slack
    */
  trait Message {
    def text: String
    def attachments: List[Attachment]
    def response_type: SlackResponseType
  }

  /**
    * There can be at most 20 attachments in a message
    */
  case class Attachment(title: String, text: String, fallback: String, pretext: String)


  implicit final val encodeSlackResponseType: Encoder[SlackResponseType] = Encoder.instance[SlackResponseType](rt =>
      Json.string(
          rt match {
            case t: EphemeralResponse.type => "ephemeral"
            case t: InChannelResponse.type => "in_channel"
          }
      )
  )

  implicit final val encodeMessage: Encoder[Message] = Encoder.instance[Message](msg =>
    Json.obj(
      ("text", Json.string(msg.text)),
      ("attachments", Encoder[List[Attachment]].apply(msg.attachments)),
      ("response_type", Encoder[SlackResponseType].apply(msg.response_type))
    )
  )


  object Attachment {
    def fromGame(game: Game): Attachment =
      Attachment(game.title, game.text, game.fallback, game.pretext)

    def help: Attachment =
      Attachment("Game board positions",
        ":one::two::three:\n:four::five::six:\n:seven::eight::nine:",
        "1 2 3\n4 5 6\n7 8 9",
        "Select the place you want to mark with the \"mark [num]\" command, e.g. /tst mark 4")
  }

  /**
    * Always display the same help message
    */
  case object HelpMessage extends Message {
    val text = "Help for Tic-Slack-Toe"
    val attachments = List(Attachment.help)
    val response_type = EphemeralResponse
  }

  case class UnrecognizedMessage(cmd: String) extends Message {
    val text = s"Unknown command: $cmd, try 'help'"
    val attachments = List()
    val response_type = EphemeralResponse
  }

  case object NoGameMessage extends Message {
    val text = "No game in channel"
    val attachments = List()
    val response_type = EphemeralResponse
  }

  case class GameInProgressMessage(game: Game) extends Message {
    val text = "Game in progress"
    val attachments = List(Attachment.fromGame(game))
    val response_type = EphemeralResponse
  }

  case class GameStartedMessage(game: Game) extends Message {
    val text = "Game started, challenge accepted"
    val attachments = List(Attachment.fromGame(game))
    val response_type = InChannelResponse
  }

  case class NewGameMessage(pending: PendingGame) extends Message {
    val text = s"New game challenge by ${pending.player.name}. Play against them by type `/tst play`"
    val attachments = List()
    val response_type = InChannelResponse
  }

  case object NotAllowedMessage extends Message {
    val text = s"You are not in the current game, type 'show' and wait for that game to finish"
    val attachments = List()
    val response_type = EphemeralResponse
  }

  /**
    * Display the current board for this game
    */
  case class ShowMessage(game: Game) extends Message {
    val text = ""
    val attachments = List(Attachment.fromGame(game))
    val response_type = EphemeralResponse
  }

  /**
    * If user tries an invalid move, display helpful message, otherwise
    * display the updated in_channel message with the new board.
    */
  case class MarkMessage(game: Game) extends Message {
    val (text, attachments, response_type) = game.board match {
      case t: InvalidBoard => ("Invalid move", List(), EphemeralResponse)
      case t: WinningBoard => (s"${game.players.two.name} won the game", List(Attachment.fromGame(game)), InChannelResponse)
      case t: ValidBoard => ("", List(Attachment.fromGame(game)), InChannelResponse)
    }
  }

  case object NotYourTurnMessage extends Message {
    val text = "It is not your turn, wait for opponent to make a move"
    val attachments = List()
    val response_type = EphemeralResponse
  }

  /**
    * If game is over, then this is invalid, display "Game already over"
    * and then winning text.
    * Otherwise display winning text.
    */
  case class ForfeitMessage(player: Player) extends Message {
    val text = s"${player.name} forfeits the game"
    val attachments = List()
    val response_type = InChannelResponse
  }

  case object GameOverMessage extends Message {
    val text = "Game already over"
    val attachments = List()
    val response_type = EphemeralResponse
  }

}
