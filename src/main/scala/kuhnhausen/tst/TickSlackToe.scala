package kuhnhausen.tst
import com.twitter.util.Future
import scala.collection.mutable

object TickSlackToe {
  /**
   * Basic contract for a store for games
   */
  trait GameStore {
    def getGame(id: GameId): Future[Option[Game]]
    def updateGame(game: Game): Future[Game]
    def getPending(id: GameId): Future[Option[PendingGame]]
    def updatePending(game: PendingGame): Future[PendingGame]
  }

  /**
   * An in memory version of the store
   */
  case class InMemoryGameStore(games: mutable.Map[GameId, Game],
                               pending: mutable.Map[GameId, PendingGame]) extends GameStore {
    def getGame(id: GameId): Future[Option[Game]] = Future.value {
      games.get(id)
    }

    /**
      * Only save valid games (games with boards that are valid)
      */
    def updateGame(game: Game): Future[Game] = Future.value {
      game.board match {
        case b: InvalidBoard => game
        case b => {
          games.update(game.id, game)
          game
        }
      }
      games.update(game.id, game)
      game
    }
    def getPending(id: GameId): Future[Option[PendingGame]] = Future.value {
      pending.get(id)
    }
    def updatePending(game: PendingGame): Future[PendingGame] = Future.value {
      pending.update(game.id, game)
      game
    }
  }

  object InMemoryGameStore {
    val empty = InMemoryGameStore(mutable.Map.empty[GameId, Game], mutable.Map.empty[GameId, PendingGame])
  }
}
