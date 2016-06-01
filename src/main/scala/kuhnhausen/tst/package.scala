package kuhnhausen

/**
  * Tic-Slack-Toe (tst) contains the shared Value Types and Algebraic Data Types
  * for use in the rest of the application
  */
package object tst {
  type Grid = List[List[Mark]]

  /**
    * A game is the product type of GameId, Players, Board
    */
  case class Game(id: GameId, players: Players, board: Board, winner: Option[Player] = None)(implicit validator: Validator) {
    /**
      * Creates a new Game with players, board, and winner updated (if needed)
      */
    def mark(square: Int): Game =
      board.update(square, players.one.mode.mark) match {
        case b: WinningBoard => copy(board = b, winner = Some(players.one))
        case b: ValidBoard => copy(players = players.switch, board = b)
        case b: InvalidBoard => copy(board = b)
      }

    /**
      * Takes a board and converts it to a Slack interpretable string
      *
      * List(List(Ex, Oh, Oh), List(Ex, Ex, Oh), List(Oh, Ex, Ex)) =>
      *   ":x::o::o:\n:x::x::o:\n:o::x::x:"
      */
    def text: String =
      board.board.map(
        _.map(Game.emojiMap).mkString
      ).mkString("\n")

    /**
      * Converts a board to an ascii string representation of the board
      *
      * List(List(Ex, Unmarked, Oh), List(Ex, Ex, Oh), List(Oh, Ex, Unmarked)) =>
      *
      *   "  x  |  .  |  o  \n-----+-----+-----\n   x  |  x  |  o  \n  -----+-----+-----  o  |  x  |  .  \n"
      */
    def fallback: String =
      board.board.map(
        _.map(Game.textMap).mkString("|")
      ).mkString(Game.textSeparator)

    def title: String =
      players.toString

    def pretext: String =
      s"It's ${players.one.name}'s turn"
  }

  object Game {
    // Let's build the board with emojies!
    val emojiMap = Map[Mark, String](
      Ex -> ":x:",
      Oh -> ":o:",
      Unmarked -> ":white_large_square:"
    )

    // fallback text in the case we can't display emoji
    val textMap = Map[Mark, String](
      Ex -> "  x  ",
      Oh -> "  o  ",
      Unmarked -> "  .  "
    )
    val textSeparator = "\n-----+-----+-----\n"
  }

  case class PendingGame(id: GameId, player: Player)

  /**
    * A game id is the product type of team_id and channel_id
    */
  case class GameId(teamId: String, channelId: String)

  /**
    * Players is a an ordered product of two players
    */
  case class Players(one: Player, two: Player) {
    def get(userId: String): Option[Player] = userId match {
      case one.id => Some(one)
      case two.id => Some(two)
      case _ => None
    }

    def other(player: Player): Player =
      if (player == one) two
      else one

    /**
      * Create a new Players with the order switched
      */
    def switch: Players =
      Players(two, one)

    override def toString: String = one.mode match {
      case t: Challenger.type => s"${one.name} (X's) vs ${two.name} (O's)"
      case _ => s"${two.name} (X's) vs ${one.name} (O's)"
    }
  }

  /**
    * A player is a product type of user_id, name, and which order they will play
    */
  case class Player(id: String, name: String, mode: Mode)

  /**
    * ADT that describes the valid player mode mark types
    *   - Challenger is always "x"
    *   - Opponent is always "o"
    */
  sealed trait Mode {
    val mark: Mark
  }
  case object Challenger extends Mode {
    val mark = Ex
  }
  case object Opponent extends Mode {
    val mark = Oh
  }

  /**
    * ADT to describe the two states of a board:
    *   - Invalid boards: contains invalid marks, e.g. start with 'o, 3 'o and 1 'x, etc
    *   - Valid boards: board that contains only valid marks
    *   - Winning board: a valid board that has a winner (x's or o's)
    */
  sealed trait Board {
    val board: Grid

    /**
      * Creates a new Board with the Mark set at the square
      */
    def update(square: Int, mark: Mark)(implicit validator: Validator): Board = {
      val row = (square - 1) / 3
      val index = (square - 1) % 3
      Board(board.updated(row, board(row).updated(index, mark)))
    }
  }
  case class ValidBoard(board: Grid) extends Board
  case class WinningBoard(board: Grid) extends Board
  case class InvalidBoard(board: Grid) extends Board

  /**
    * Companion object for initializing an empty board and a smart constructor
    */
  object Board {
    val length = 3
    val height = 3

    // An empty board is a 3x3 board of Unmarked squares
    val empty: ValidBoard = ValidBoard(List.fill(height)(List.fill(length)(Unmarked)))

    /**
      * Smart constructor, returns proper Board type based on a given Validator
      */
    def apply(grid: Grid)(implicit validator: Validator) = {
      if (validator.isValid(grid))
        if (validator.isWinner(grid))
          WinningBoard(grid)
        else
          ValidBoard(grid)
      else
        InvalidBoard(grid)
    }
  }

  /**
    * ADT to describe the three Marks possible:
    *   - X
    *   - O
    *   - not marked
    */
  sealed trait Mark
  case object Ex extends Mark
  case object Oh extends Mark
  case object Unmarked extends Mark
}
