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
  case class Game(id: GameId, players: Players, board: Board)

  /**
    * A game id is the product type of team_id and channel_id
    */
  case class GameId(teamId: TeamId, channelId: ChannelId)
  case class TeamId(id: Int)
  case class ChannelId(id: Int)

  /**
    * Players is a an ordered product of two players
    */
  case class Players(one: Player, two: Player)

  /**
    * A player is a product type of user_id, name, and which order they will play
    */
  case class Player(id: UserId, name: String, mode: Mode)
  case class UserId(id: Int)

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
