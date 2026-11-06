# Foundation contract

This contract covers immutable state, match initialization, seat order, action messages and
player-specific views. `GameSetup` prepares the table; Member A supplies the shared card/deck model and shuffling/dealing primitives.
The application initializer and Swing setup session now connect setup and dealing. Hand evaluation and closed-state winner/tie resolution are implemented through the
existing HandEvaluator interface; see [showdown](showdown.md). Settlement stays separate. The betting engine will validate actions, produce
accepted results, advance streets, handle pots and settle hands. CI builds and tests pushes and pull requests.

## Settings and seats

| Setting | Initial value |
| --- | --- |
| Starting chips | 1,000 per player |
| Small / big blind | 10 / 20 |
| Seats | Six, numbered 0–5 |
| Humans | 1–6 |
| Bots | Six minus human count |
| Antes | None |
| Blind increases | None |

`new GameConfig()` supplies these defaults. Custom test/match settings must satisfy
`0 < smallBlind < bigBlind <= startingChips` and `startingChips <= Long.MAX_VALUE / 6`.
Seats, antes and blind progression are fixed in this version. Configuration is immutable.

Humans take the first seats and bots fill the remainder. For two humans, seats 0–1 are
human and seats 2–5 are bots. IDs are generated automatically and are separate from seat
indices. Match participant IDs remain stable across hands; persistent profile IDs will
be a separate concern. Every new match gets new participant IDs and a distinct first-hand
ID. The future engine must assign a new unique hand ID for every subsequent hand.

All six seat records stay in state after elimination. An eliminated player has no chips,
cards or commitments. A player who has committed all their chips remains `ALL_IN` until
settlement; they are not eliminated while their hand is live.

## Setup and position selection

```java
GameSetup setup = new GameSetup(new GameConfig(), new Random());
GameState initial = setup.create(2);
```

Inject a seeded `Random`, or override its `nextInt(bound)` in tests, to control the dealer.
At match creation all six seats are occupied; the implementation makes one
`nextInt(6)` draw. Standard `Random` supplies uniform bounded selection. No statistical
frequency assertions or uncontrolled randomness are needed in tests.

The moving-button policy selects the next remaining seat clockwise after each hand.
`SeatOrder.nextDealerAfter(previousSeat)` also accepts a dealer who was just eliminated.
Use `positionsFor(newDealer)` only when at least two players remain. The engine ends the
match when one player holds all chips rather than starting another hand.

| Remaining players | Small blind | Big blind | First preflop actor | First postflop actor |
| --- | --- | --- | --- | --- |
| 3 or more | Next remaining after dealer | Next remaining after SB | Next eligible after BB | Next eligible after dealer |
| 2 | Dealer | Other player | Dealer if eligible | Other player if eligible |

Actor searches skip folded, all-in and eliminated players, returning `OptionalInt.empty()`
if nobody can act. Position searches skip only eliminated players. Seat order determines
who is eligible, not whether another decision is needed; the engine uses pending actions
and amounts owed to decide whether a betting round is finished.

`GameSetup` returns `SETUP`: blinds are posted and the future first actor is identified,
but hole cards, community cards and remaining-deck contents are empty. No betting may be
accepted yet. The application's `application.GameInitializer.start(humanCount)` calls this
setup operation, creates and shuffles a fresh Deck, and deals two rounds clockwise
after the dealer. It returns a `PREFLOP` snapshot with two hole cards per player
and the immutable 40-card remainder in deal order, preserving the table's chips,
statuses, positions and actor. Its injected constructor accepts GameConfig and
separate dealer/deck Random sources; see the [initializer BVA](../bva/GameInitializer.md).
The Swing interface calls this coordinator through SetupSession and SetupView.

## Current state and chip accounting

`GameState` contains `GameConfig`, `handId`, `GamePhase`, six `PlayerState` records,
`HandPositions`, an optional current actor ID, community cards, remaining deck and
`BettingProgress`. Construction copies collections and rejects inconsistent state.

`Player` continues to hold immutable identity, seat, control type and remaining stack.
`PlayerState` adds hole cards, `ACTIVE`/`FOLDED`/`ALL_IN`/`ELIMINATED` status,
`roundContribution` and `handContribution`. A chip transfer creates a new player/state
snapshot; it does not modify identity objects behind existing snapshots.

`handContribution` includes `roundContribution`. While chips remain unawarded:

```
totalPot = sum(player.handContribution)
sum(player.remainingStack) + totalPot = config.initialTotalChips
```

`getPot()` derives its result from contributions. The state never maintains another
independently updated pot counter. `COMPLETION` requires zero commitments after payouts
and refunds; the previous hand's positions remain available as historical information.
Integer chip arithmetic must remain within `long`; invalid constructors throw
`IllegalArgumentException` without changing an earlier snapshot.

`currentBet` is the highest actual round contribution, not a whole-hand contribution.
`lastFullRaiseSize` starts at the big blind. `BettingProgress` stores IDs who still need
to act and IDs whose raising rights remain open. Both sets contain only active players;
raising rights need not be a subset of pending actors. The engine updates these explicitly
after full raises and incomplete all-in raises. For a later short big blind, the engine
must separately apply the configured preflop bring-in; actual posted chips alone do not
determine the full amount required to enter betting.

The old small `GameState` constructor is replaced by the full snapshot constructor.
`getCurrentActorSeat()` now returns `OptionalInt`; `getCurrentActorId()` returns
`Optional<String>`. The earlier state tests/BVA are migrated to this API. `Player` and
the statistics classes keep their existing APIs.

## Requests and accepted results

```java
ActionRequest call = new ActionRequest(playerId, ActionType.CALL);
ActionRequest raise = new ActionRequest(playerId, ActionType.RAISE, 60);
```

Only `BET` and `RAISE` carry a positive target amount. The target is the player's total
contribution for this round: **raise to**, not **raise by**. The caller never supplies a
separate chip deduction. With prior contribution 10 and current bet 20, a call adds 10;
a raise to 60 adds 50. These calculations and minimum-raise/stack/turn checks belong to
the future betting engine, not to the request data object.

After legal validation, the engine constructs `AcceptedAction` with hand/player IDs,
street, resolved type, actual chips added, resulting round contribution, whether this
action made the player all-in, and explicit `StatsContext`. Its constructor checks
structural consistency; constructing it is not itself a legality check against a game.

There is no generic `ALL_IN` action type. Resolve it to `CALL`, `BET` or `RAISE`.
Forced blinds use `FORCED_POST`, including a blind that exhausts a stack. They are never
voluntary investments. `toStatsAction()` bridges an accepted result to the existing
statistics input. The engine routes it to the correct player's active hand and checks
hand/player identity before recording. Rejected requests produce no accepted result.

Three-bet and c-bet eligibility are engine-provided context. The stats component does
not infer raise levels from sizes or reconstruct action legality. Hand completion,
contested showdown payouts and uncalled refunds remain distinct engine events; see
[statistics requirements](../requirements/player-statistics.md).

## Card contract and private information

The shared immutable value class is `domain.model.Card`, with `Rank` and `Suit` enums.
Cards compare by rank and suit, so separately constructed equal values identify the
same physical card. Member A's `Deck` creates the 52 combinations and supplies shuffle
and deal operations. The duplicate `domain.Card` enum was removed during integration.
PokerHandEvaluator handles rank ordering, kickers and ace-low straights.

The internal state holds all hole cards and the remaining deck. Cards cannot occur in
more than one stored location. Burned/discarded cards may be absent from the snapshot;
the engine is responsible for moving cards correctly. Phase board counts are 0 before
the flop, 3 on the flop, 4 on the turn, and 5 on the river/showdown.

```java
GameView botInput = state.viewFor(botPlayerId);
```

The [setup integration cases](../bva/setup-integration.md) compose `Deck` with these
snapshots. They transfer the undealt order into the immutable state and verify duplicate
rejection by value. GameInitializer and the Swing SetupSession now use these real components; see
[setup design](game-setup.md).

`GameView` contains public state and `PlayerView` records. Only the selected viewer's
hole cards are included; every opponent's hole-card list is empty. The view exposes no
remaining deck and retains no internal-state reference. Pass these views to both bots
and interface code. Future showdown reveals can be an explicit public result event.
On a shared computer, the interface must also hide the previous human's view before
handing control to the next human.

## Shared initialization acceptance case

For two humans plus four bots, 1,000 chips each, blinds 10/20 and forced dealer seat 0:

```
dealer = 0
smallBlind = 1; stack = 990; roundContribution = handContribution = 10
bigBlind = 2; stack = 980; roundContribution = handContribution = 20
firstPreflopActor = 3
currentBet = 20
lastFullRaiseSize = 20
totalPot = 30
remainingStacks = 5970
remainingStacks + totalPot = 6000
```

This is executable case **GU-12** in `GameSetupTest`; the setup BVA also covers human
counts 0, 1, 2, 5, 6 and 7. Each class has [BVA cases](../bva/README.md) mapped to JUnit IDs.

## Fix null handling and validate inputs (November 6)

Use Optional for an absent actor, active statistics hand or session match, and
empty immutable lists for hidden/undealt cards. The Swing layer preserves the
session's Optional instead of converting it to null or passing nullable players
and cards to rendering helpers. GameState's existing nullable actor constructor
is retained for compatibility with engine callers; it converts to Optional at
entry, and its public actor getters return Optional/OptionalInt.

Player and other public constructors still reject invalid inputs once at their
boundary. Removing those guards would weaken the established contract rather
than prevent null flow. Internal code uses validated identities/types and explicit
absence; Swing's setLocationRelativeTo(null) remains a documented JDK API request
to center the window, not a missing application value.
