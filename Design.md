# Overview

This is here to let me guide you and me through my thought process as I break
down the problem. Think of it as a Falkner-esque stream-of-conciousness
narrative of my brain dumping into your brain.

# Commands

I'd like to make the command for communicating unique and short enough for
users to type. My first thoughts are these: should the command be generic for
any future game, or should I just implement what is in the requirements. If
this were a longer-term project, I'd think the former would be the best.
However, since the requirements are for this one game, I think a nicer API
would be a single `/` command: `/ttt` (for tic-tack-toe) or `/tst` (for
tic-slack-toe).

## Gameplay

### Starting a game

Since there are no requirements around who starts and which symbol is used to
start the game, I will start with a simple version that makes these decisions
for the players:
  - "X" will always be the symbol for player 1
  - The challenger will always be player 1

In the future we can make this configurable via the game-initiation phase.

### Marking a spot

I'd really like to confirm that a user wants to make the move they asked to
make, so private to the player making the move it should display the updated
grid with a confirmation prompt allowing them to undo the move they requested.

### Canceling a game

The user should be able to drop out of a game.

### How to win, lose, or draw

1. Normal gameplay rules
2. If :user leaves the channel, they forfeit the game
3. If :user drops out of a game, they lose
4. If the game times-out, the last player to play wins

### Canceling a game

The user should be able to drop out of a game.

## Playing Grid

I'm thinking that a nice user experience trumps implementation overhead on the
back-end, so for actually making a move I'm playing with two ideas for
determining the location of where to mark the X or O.

Let's say we want to place an X marker on the top-right corner:

```
. . .    . . X
. . . => . . .
. . .    . . .
```

If we use an Cartesian-like grid, we can use a-c and 1-3 for the y, x axis
respectively.

```
a,1 a,2 a,3
b,1 b,2 b,3
c,1 c,2 c,3
```

Though, I think it might be nicer to only require one character to be entered

```
a b c    1 2 3
d e f or 4 5 6
g h i    7 8 9
```

Numbers might be better for non-alphabetic languages, and most people start
counting from 1 instead of 0.


# Server

## Game representation

The requirement is that there can only be one game per channel at a time. Which
means two things: we only need to identify the game based on the channel and
team, and that any old games for that channel can be discarded once a new game
takes place. Further, users are totally ephemeral.

### Data types

Here are my initial thoughts in pseudo math notation:

```
# A game is the product type of game_id, players, board
Game :: (GameId, Players, Board)

# GameId is the product type of team_id and channel_id
GameId :: (TeamId, ChannelId)
TeamId :: String
ChannelId :: String

# Players a product type of two players
Players :: (Player, Player)

# A player is the product type of user_id, name, and if they are the challenger
Player :: (UserId, Name, Mode)
UserId :: String
Name   :: String
Mode   :: 'challenger | 'opponent

# Game board is simply a list of Marks passed through a validator
Board :: Validator => List[List[Mark]]
Board grid =
  | isValid && isWinner = WinningBoard
  | isValid = ValidBoard
  | otherwise = InvalidBoard

# Value types
Mark :: symbol =
  | UnMarked = '.
  | Ex = 'x
  | Oh = 'o

```

### Util Functions

```
# Ex's and Oh's are chosen by challenger vs opponent
ExOh :: Mode -> Mark
ExOh 'challenger = Ex
ExOh 'opponent  = Oh

# Validation for a board
Validator :: Grid -> Boolean
```

## Persistence

There is one way to remove a game from the system: that game ends and a new
game begins. Otherwise, we'll be holding on to games indefinitely.

It would be reasonable to set a time limit for how long we keep game results
around so that we can clear up abandoned and finished games after a week or so.

The computational cost of reporting game state is so small that I don't plan on
storing anything more than what is in `Game`.

## Infrastructure

Normally, we'd want to back the persistence of these games by a datastore of
some kind. However, given the short-term goals and no requirements for longer
term outlook I'm going to be keeping data in memory. But, still I want to think
through what I'd actually use for this game, given its use case.

Use case:
  - Persist `Game`
  - `Game` is not long-lasting, once a new game is started the old one can be
    removed. Nothing else is needed to be persisted
  - We most-likely don't want to keep abandoned games around, so a TTL for a
    game is not a bad idea.

Assuming we would have millions of games played per month, because we are super
popular here are some thoughts...

### RDBMS

While an RDBMS is a perfectly fine place to store this data, the ephemeral
nature of these games makes it unkind to many many random deletes. Mutating the
tables in-place works fine, but abandoned games (that we'd like to reclaim
space from) would likely make swiss-cheese of our pages and require external
work to reclaim.

Our write load would most likely never hinder a single server. Replication and
fail-over strategies for HA would be needed.

### Cache

memcached is great for the TTL and ephemeral nature of our games, however is a
poor fit for HA for one main reason:
  - even with consistent hashing, a loss of a server means losing that
    partition of users

Initially I'd think that keeping everything in memory (even if partitioned out)
would be a concern, but millions of rows of ephemeral data is not much memory
based on our data types.

redis would be a better fit since we have async replication, we'd only lose the
games and updates that occurred before replication occurred, and it has the
same tradeoffs as memcached otherwise.

### Distributed Key-Value Stores

These are a great fit for our application, especially if they do garbage
collection. We could simply model a game as an append-only log of `Player` and
`Board` states. Replication is nice for HA and we don't read what we write
immediately, so we don't have consistency issues.

However, the operational cost and complexity for those unfamiliar with the
technology, paired with the volatility of many of the products makes for
longer-term maintenance headache potential.


# API

## Request/Response data types

Deriving the type of response to a specific request is well-formed and should
be simply described as a contract and transformations into the contract.

As documented an example request would look like:

```
token=jJGfagzhcGLFIXoPSSL4ssd3
team_id=T0001
team_domain=example
channel_id=C2147483705
channel_name=test
user_id=U2147483697
user_name=Steve
command=/tst
text=help
response_url=https://hooks.slack.com/commands/1234/5678
```

A response should be predicated on the `text`, for example a `Help` response
is static and only dependent on the `text == "help"` field of the incoming
request (since, by default the message is ephemeral and not in-channel).

A `mark` response is dependent on `user*`, `channel*` and `text*` values, since
we use those to hydrate a Game from persistent memory and respond with either a
Success or Failure response (depending on if the move was valid, if it ends in
them winning).

A response is also well formed, but there are many options that we can respond
with (attachment, ephemeral, in-channel, etc). So we should encode those values
appropriately.

```
Response ::= JSON(<Message>)
Message ::= (<ResponseType>, text, [<Attachment>])
Attachment ::= (title, text, fallback, pretext)
ResponseType ::= "in_channel" | "ephemeral"
```

## Grammar

Here is an EBNF form:

```
  <Command>      ::= <SlashCommand> " " [<Option>]
  <SlashCommand> ::= "/ttt" | "/tst"
  <Option>       ::= <Challenge> | <Mark> | "show" | "help" | "forfeit"
  <Challenge>    ::= "play" " " "@" username
  <Mark>         ::= "mark" " " 1-9
```

## help

```
/ttt help
```

Visibility: Ephemeral
Description: It will display the usage, potential commands and options

## show

```
/ttt show
```

Visibility: Ephemeral
Description: Shows the current game (or last game played) in the channel, if
the last game to be played is no longer available then it will read "no games
to show"

## play

```
/ttt play
```

If this is a new game and can be played, then:
  Visibility: In-Channel
  Description: "New game challenge started, by :user. Type `/tst play` to
               play!"

If there is already a game in the channel, then:
  Visibility: Ephemeral
  Description: "There is already a game going on between :user1 and :user2"

## mark

```
/ttt mark NUM
```

First:
  Visibility: Ephemeral
  Description: If the user tries to mark a space that is already marked or an
               invalid move, a message will come back saying "invalid move, try
               \"help\""

Second:
  Visibility: In-Channel
  Description: If valid play, then display ":user marked :num" and display
               the updated board


## forfeit

```
/ttt forfeit
```

First:
  Visibility: Ephemeral
  Description: If game is over, then this is an invalid command. "Game already
               over, :user beat :otheruser"
Second:
  Visibility: In-Channel
  Description: ":winner beat :loser"

