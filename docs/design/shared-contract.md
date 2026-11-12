# Foundation contract

This contract covers immutable state, match initialization, seat order, action messages and
player-specific views. `GameSetup` prepares the table; Member A supplies the shared card/deck model and shuffling/dealing primitives.
The application initializer and Swing setup session now connect setup and dealing. Hand evaluation and closed-state winner/tie resolution are implemented through the
existing HandEvaluator interface; see [showdown](showdown.md). Settlement stays separate. The betting engine validates actions and produces
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

All six seat records stay in state after elimination or departure. An eliminated
player has no chips, cards or commitments. A departed seat keeps its stack but
receives no cards or commitments in future hands. An all-in player, including
one who leaves, remains `ALL_IN` and eligible until settlement.

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
`SeatOrder.nextDealerAfter(previousSeat)` accepts an eliminated or departed
previous dealer. Positions skip undealt seats. A paid match starts another
hand only when at least two funded nondeparted seats remain.

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

`currentBet` is the round target, not a whole-hand contribution. It normally equals
the highest actual round contribution. A short preflop big blind retains the nominal
bring-in while at least two players can still act; a lone active player need only
match an opposing all-in commitment. `lastFullRaiseSize` starts at the big blind. `BettingProgress` stores IDs who still need
to act and IDs whose raising rights remain open. Both sets contain only active players;
raising rights need not be a subset of pending actors. The engine updates these explicitly
after full bets/raises. Full wagers restore decisions and raising rights for every
other ACTIVE player, including players who already checked. Short all-ins create
responses for players who owe more without automatically restoring their raise rights.
The engine retains immutable last-decision targets for ACTIVE players. A cumulative
increase of at least the last full increment reopens that player's rights; an
intermediate CALL resets only that player's decision level. Forced blinds are not
voluntary decisions, so the unacted big blind retains its option.

`BettingProgress` keeps its four-argument constructor for legacy snapshots. Its new
five-argument overload accepts `Map<String, Long> lastDecisionTargets`; it copies the
map and rejects missing IDs/values or levels outside 0..currentBet. GameState validates
that recorded players can act. Engine transitions retain/update this provenance;
callers constructing a mid-round snapshot must supply it to support cumulative
reopening after prior actions. A new street or hand starts with an empty map.

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
engine operations, not to the request data object. `BettingEngine` supports
FOLD/CHECK/CALL, opening postflop BET and full RAISE on every betting street,
including responses to raised preflop rounds. A full opening bet is at least the
big blind. A raise target must exceed the current target by at least the last
full increment; it cannot exceed the actor's stack plus their round contribution.
Preflop uses RAISE over the blinds, not BET. An undersized wager is accepted only
when its target uses the entire remaining stack and the player may raise. It leaves
the full increment unchanged. An unacted player facing a short opening of 5 must
raise by at least 20 to 25, not merely complete to 20 (default blinds).
[K01 BVA](../bva/FullBetting.md) and [K02 BVA](../bva/ShortAllInBetting.md)
record the boundaries.
A CALL adds `min(currentBet - roundContribution, remainingStack)`
when chips are owed. A short or exact-stack call leaves zero chips, sets ALL_IN
and produces an accepted CALL with `becameAllIn=true`. It removes the caller's
decision/raising rights while retaining their participation, cards and commitments.
The betting level and full raise size do not change. No street transition or pot
settlement occurs automatically. See the [engine BVA](../bva/BettingEngine.md).

After legal validation, the engine constructs `AcceptedAction` with hand/player IDs,
street, resolved type, actual chips added, resulting round contribution, whether this
action made the player all-in, and explicit `StatsContext`. Its constructor checks
structural consistency; constructing it is not itself a legality check against a game.

`GameState.getBettingHistory()` returns immutable engine provenance. The original
constructor supplies empty history for unraised hands. The new overload accepts
`BettingHistory(finalPreflopAggressorId, flopCBetAmount)`: null means no aggressor,
zero means no recorded actual c-bet, and a positive amount records that bet's
original flop target. Its bettor is the final preflop aggressor. Unknown or
eliminated current-hand aggressors, pre-deal aggression, preflop c-bets and
insufficient backing commitments are rejected. Completed hands may retain
historical values after payouts/elimination. Callers constructing later snapshots
must pass the same provenance explicitly; engine operations already do so.

Every accepted preflop raise, including a short all-in raise, increments the
recorded raise count and replaces the final preflop aggressor. The legacy
two-argument constructor seeds one raise when an aggressor is present. Use the
three-argument constructor to supply the exact count when constructing a
snapshot. Zero requires no aggressor; a positive count requires one. GameState
rejects counts above total match chips minus the big blind, since every raise
increases the target by at least one chip.

The prior count determines whether the next raise is an opening raise,
three-bet, four-bet, or later raise. Reopening determines whether the actor
may raise again. A three-bet opportunity requires one prior raise and a legal
raise option, including a short all-in raise.

For FLOP actions, an opening c-bet opportunity requires that actor to be the
recorded aggressor, unopened betting, betting rights, enough chips to increase
the wager (including a short all-in) and another ACTIVE player.
Facing a c-bet requires an explicitly recorded actual c-bet, chips owed and the
original betting level. A subsequent raise is not a direct fold to the original
c-bet. Ordinary opening bets and equal-sized turn/river bets create no flop
opportunity. These facts populate accepted `StatsContext`; action requests do
not contain caller-supplied statistics flags. An accepted flop BET by that
aggressor records the actual c-bet target. The engine uses recorded actions,
not contribution sizes alone. See the [K03 boundary cases](../bva/BettingHistory.md#k03-exact-raise-counts-and-all-in-observations).

`BettingEngine.getLegalActions(state)` publishes read-only current-actor
options: legal action types, capped call cost and optional BET/RAISE limits.
The wager's minimum full target is nominal (empty if it overflows a long),
maximum is the actor's round contribution plus stack, and the short-all-in
target is present only when the exact stack is the sole legal increase.
Queries and submissions share wager validation. See [legal options BVA](../bva/LegalActions.md).

There is no generic `ALL_IN` action type. Resolve it to `CALL`, `BET` or `RAISE`.
Forced blinds use `FORCED_POST`, including a blind that exhausts a stack. They are never
voluntary investments. `toStatsAction()` bridges an accepted result to the existing
statistics input. The engine routes it to the correct player's active hand and checks
hand/player identity before recording. Rejected requests produce no accepted result.

Three-bet and c-bet eligibility are engine-provided context. The stats component does
not infer raise levels from sizes or reconstruct action legality. Hand completion,
contested showdown payouts and uncalled refunds remain distinct engine events; see
[statistics requirements](../requirements/player-statistics.md).

## Street advancement and burn cards

`BettingEngine.advanceStreet(before)` returns a new `GameState` directly, without
an `AcceptedAction` or statistics action. Each call advances one closed round:
PREFLOP → FLOP → TURN → RIVER → SHOWDOWN. Pending decisions must be empty, at
least two participants must remain, and every ACTIVE player's round commitment
must match the current bet. Folded and short all-in players need not match it.
An uncontested hand awaits settlement instead of further dealing.

Burn the first remaining card, then deal three flop cards or one turn/river
card. Burned cards leave the stored card locations. These operations require
four remaining cards before the flop and two before turn/river; insufficient
decks are rejected before changing any state. Entering SHOWDOWN burns/deals no
cards, so an empty remaining deck is valid there.

Starting a new betting street resets every `roundContribution` and `currentBet`
to zero, and `lastFullRaiseSize` to the configured big blind. `handContribution`,
stacks, statuses, seats, positions, hole cards and hand ID are preserved. With
at least two ACTIVE players, their IDs become the pending/raising sets and the
first actor is clockwise after the dealer; heads-up this is the non-dealer.
With zero/one ACTIVE players and multiple participants, no artificial check
decision is created. Repeated advances run out the board for all-in hands.

SHOWDOWN has no actor, pending decisions or raising rights. It preserves the
river's round accounting and whole-hand commitments until a later evaluation
and settlement operation. No winner, payout, profile aggregate or new hand is
produced by advancement. SETUP, SHOWDOWN and COMPLETION cannot advance using
this method. All earlier snapshots remain unchanged on success or rejection.

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


## Played-hand session follow-up

SetupSession now owns action submission, automatic closed-round advancement,
uncontested/showdown comparison and a temporary legal check/call bot policy.
It publishes immutable display/result/action snapshots and never exposes the
remaining deck or awards the pot. See the [played-hand contract](played-hand.md)
and [session BVA](../bva/SessionBetting.md).

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
