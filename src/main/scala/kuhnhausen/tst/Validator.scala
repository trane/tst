package kuhnhausen.tst

import scala.annotation.tailrec

/**
  * A Validator is a contract so that different options for implementation can be used.
  */
trait Validator {
  /**
    * Ensures the board is valid
    *   A valid board follows these rules:
    *     - There can never be more Oh's than Ex's
    *     - An empty board is valid
    */
  def isValid(board: Grid): Boolean

  /**
    * A winning board is one that has 3-in-a-row of the same mark in:
    *   - horizontal
    *   - vertical
    *   - diagonal
    */
  def isWinner(board: Grid): Boolean
}

/**
  * Companion object with two helpful utility functions:
  *   isValidBoard and isWinningBoard
  */
object Validator {

  /**
    * Ensures the board is valid
    *   A valid board follows these rules:
    *     - There can never be more Oh's than Ex's
    *     - An empty board is valid
    */
  def isValidBoard(board: Grid): Boolean = {
    val exsAndOhs = countExsAndOhs(board)
    exsAndOhs <= 1 && exsAndOhs >= 0
  }

  /**
    * A winning board is one that has 3-in-a-row of the same mark in:
    *   - horizontal
    *   - vertical
    *   - diagonal
    */
  def isWinningBoard(board: Grid): Boolean =
    horizontal(board) || vertical(board) || diagonal(board)

  /**
    * Increments when it finds an "x" and decrements when it finds an "o".
    * Since we only care that the number of "x"s is at most 1 more than "o"s and
    * "o"s can never be more than "x"s, we can add all of them up:
    *   - if there are more "o"s than "x"s, the number will be < 0
    *   - if there are more than 1 "x"s than "o"s the number will be > 1
    *   - otherwise it is valid
    */
  private def countExsAndOhs(board: Grid): Int =
    board.foldRight(0)((row, rowSum) =>
      row.foldRight(0)((mark, count) => mark match {
        case Ex => count + 1
        case Oh => count - 1
        case Unmarked => count
      }) + rowSum
    )


  private def horizontal(board: Grid): Boolean =
    test(board)

  /**
    * For fun, let's transpose the matrix and treat it like it's a horizontal board
    */
  private def vertical(board: Grid): Boolean = {
    @tailrec
    def transpose(board: Grid, acc: Grid): Grid = {
      board match {
        case Nil :: Nil => acc
        case a :: b :: c :: Nil => transpose(List(a.tail, b.tail, c.tail), List(a.head, b.head, c.head) :: acc)
      }
    }
    test(transpose(board, List(List())))
  }

  /**
    * Again, another approach is pattern matching
    */
  private def diagonal(board: Grid): Boolean = board match {
    case List(a, _, c) :: List(_, e, _) :: List(g, _, i) :: Nil => allSame(List(a,e,i)) ||
                                                                   allSame(List(g,e,c))
    case _ => false
  }

  /**
    * Another pattern matching fun
    */
  private def patternMatchingFun(board: Grid): Boolean = board match {
    case List(a, b, c) :: List(d, e, f) :: List(g, h, i) :: Nil =>
      allSame(List(a,e,i)) || allSame(List(g,e,c)) // diagonal
      allSame(List(a,b,c)) || allSame(List(d,e,f)) || allSame(List(g,h,i)) // horizontal
      allSame(List(a,d,g)) || allSame(List(b,e,h)) || allSame(List(c,f,i)) // vertical
    case _ => false
  }

  /**
    * Tests if a Grid contains a valid horizontal row
    */
  private def test(board: Grid): Boolean =
    board.filter(allSame).size == 1

  /**
    * Checks if all board in the list are the same
    */
  private def allSame(board: List[Mark]): Boolean = board match {
    case Ex :: Ex :: Ex :: Nil => true
    case Oh :: Oh :: Oh :: Nil => true
    case _ => false
  }
}

/**
  * The FastValidator uses a bit more memory, but has O(1) validation
  *
  * It creates two sets:
  *   - Set of all valid game boards
  *   - Set of all winning game boards from a subset of valid game boards
  *
  * So, set lookup is O(1) for an immutable [[Grid]]
  */
case object FastValidator extends Validator {
  private[this] val valids: Set[Grid] = boards.filter(Validator.isValidBoard).toSet
  private[this] val winners: Set[Grid] = valids.filter(Validator.isWinningBoard)
  private[this] val board: List[Mark] = List(Ex, Oh, Unmarked)

  def isValid(board: Grid): Boolean =
    valids(board)

  def isWinner(board: Grid): Boolean =
    winners(board)

  /**
    * All permutations of rows: 3^3 possibilities
    */
  private[this] def rows: Grid =
    for {
      a <- board
      b <- board
      c <- board
    } yield List(a, b, c)

  /**
    * All permutations of three row matrices: 9^3 possibilities
    */
  private[this] def boards: List[Grid] =
    for {
      a <- rows
      b <- rows
      c <- rows
    } yield List(a, b, c)
}
