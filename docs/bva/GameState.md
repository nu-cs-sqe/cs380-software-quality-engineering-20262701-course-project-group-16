# GameState boundary value analysis

Each case maps to the JUnit method with the same ID (hyphens become underscores).
Grouped assertions exercise the public getters at the stated boundary; no mutable state is exposed.

| ID | Public methods | State/input boundary | Expected result | Implemented |
| --- | --- | --- | --- | --- |
| GS-01 | GameState(...); getConfig(); getHandId(); getPhase(); getPlayers(); getPlayerStates(); getPositions(); getDealerSeat(); getSmallBlindSeat(); getBigBlindSeat(); getCurrentActorId(); getCurrentActorSeat(); getCommunityCards(); getRemainingDeck(); getBettingProgress(); getCurrentBet(); getLastFullRaiseSize(); getPot(); getTotalChips() | Six-seat setup before dealing | Immutable settings/IDs; seat order; expected positions/actor; zero pot and 6000 total | Yes |
| GS-02 | GameState; getPot; getTotalChips; getCommittedChips | Blinds 10/20 deducted from stacks | Pot=30; total=6000; commitments counted once | Yes |
| GS-03 | GameState; getPot; getTotalChips | Round contribution 10 already included in hand contribution 30 | Pot=30, not 40; total=6000 | Yes |
| GS-04 | GameState | null config | IllegalArgumentException | Yes |
| GS-05 | GameState | null hand ID | IllegalArgumentException | Yes |
| GS-06 | GameState | empty hand ID | IllegalArgumentException | Yes |
| GS-07 | GameState | blank hand ID | IllegalArgumentException | Yes |
| GS-08 | GameState | null phase | IllegalArgumentException | Yes |
| GS-09 | GameState | null players | IllegalArgumentException | Yes |
| GS-10 | GameState | null positions | IllegalArgumentException | Yes |
| GS-11 | GameState | null community | IllegalArgumentException | Yes |
| GS-12 | GameState | null deck | IllegalArgumentException | Yes |
| GS-13 | GameState | null betting progress | IllegalArgumentException | Yes |
| GS-14 | GameState | five player records | IllegalArgumentException | Yes |
| GS-15 | GameState | seven player records | IllegalArgumentException | Yes |
| GS-16 | GameState | null player record | IllegalArgumentException | Yes |
| GS-17 | GameState | duplicate seat | IllegalArgumentException | Yes |
| GS-18 | GameState | duplicate ID | IllegalArgumentException | Yes |
| GS-19 | GameState | incorrect clockwise blind assignment | IllegalArgumentException | Yes |
| GS-20 | GameState | one chip missing | IllegalArgumentException | Yes |
| GS-21 | GameState | one chip created | IllegalArgumentException | Yes |
| GS-22 | GameState | current bet below maximum contribution | IllegalArgumentException | Yes |
| GS-23 | GameState | current bet above maximum contribution | IllegalArgumentException | Yes |
| GS-24 | GameState | full raise below configured big blind | IllegalArgumentException | Yes |
| GS-25 | GameState | unknown pending player | IllegalArgumentException | Yes |
| GS-26 | GameState | unknown raise-eligible player | IllegalArgumentException | Yes |
| GS-27 | GameState | folded pending player | IllegalArgumentException | Yes |
| GS-28 | GameState | all-in pending player | IllegalArgumentException | Yes |
| GS-29 | GameState | folded raise-eligible player | IllegalArgumentException | Yes |
| GS-30 | GameState | unknown actor | IllegalArgumentException | Yes |
| GS-31 | GameState | blank actor | IllegalArgumentException | Yes |
| GS-32 | GameState | actor missing with pending decisions | IllegalArgumentException | Yes |
| GS-33 | GameState | actor not pending | IllegalArgumentException | Yes |
| GS-34 | GameState | actor exists with no pending decisions | IllegalArgumentException | Yes |
| GS-35 | GameState; getPlayers; getPlayerStates | Reversed input order then caller clears list | Stored players remain seat-sorted and intact | Yes |
| GS-36 | getPlayers; getPlayerStates | Mutating returned player lists | UnsupportedOperationException | Yes |
| GS-37 | getCurrentActorId; getCurrentActorSeat | Round closed with no pending actions | Both actor optionals unavailable | Yes |
| GS-38 | GameState; getPlayers; getPositions | Heads-up seats 0 and 4 retain all six seat records | Dealer also SB; eliminated seats not renumbered | Yes |
| GS-39 | GameState; getTotalChips | Cross-player totals exceed long even though each player is valid | IllegalArgumentException | Yes |
| GS-40 | GameState; getTotalChips | Maximum safe configured six-stack total | Accept without overflow | Yes |
| GS-41 | getCommittedChips | Invalid seat -1 | IllegalArgumentException | Yes |
| GS-42 | getCommittedChips | Invalid seat 6 | IllegalArgumentException | Yes |
| GS-43 | getCommittedChips | Valid endpoint seats 0 and 5 | Return zero commitments | Yes |
| GS-44 | GameState; getPhase; getCommunityCards; getRemainingDeck | PREFLOP board size 0 | Accept complete dealt-in snapshot | Yes |
| GS-45 | GameState; getPhase; getCommunityCards; getRemainingDeck | FLOP board size 3 | Accept complete dealt-in snapshot | Yes |
| GS-46 | GameState; getPhase; getCommunityCards; getRemainingDeck | TURN board size 4 | Accept complete dealt-in snapshot | Yes |
| GS-47 | GameState; getPhase; getCommunityCards; getRemainingDeck | RIVER board size 5 | Accept complete dealt-in snapshot | Yes |
| GS-48 | GameState; getPhase; getCommunityCards; getRemainingDeck | SHOWDOWN board size 5 | Accept complete dealt-in snapshot | Yes |
| GS-49 | GameState | SETUP has invalid board size 3 | IllegalArgumentException | Yes |
| GS-50 | GameState | PREFLOP has invalid board size 3 | IllegalArgumentException | Yes |
| GS-51 | GameState | FLOP has invalid board size 2 | IllegalArgumentException | Yes |
| GS-52 | GameState | FLOP has invalid board size 4 | IllegalArgumentException | Yes |
| GS-53 | GameState | TURN has invalid board size 3 | IllegalArgumentException | Yes |
| GS-54 | GameState | TURN has invalid board size 5 | IllegalArgumentException | Yes |
| GS-55 | GameState | RIVER has invalid board size 4 | IllegalArgumentException | Yes |
| GS-56 | GameState | RIVER has invalid board size 6 | IllegalArgumentException | Yes |
| GS-57 | GameState | SHOWDOWN has invalid board size 4 | IllegalArgumentException | Yes |
| GS-58 | GameState | COMPLETION has invalid board size 2 | IllegalArgumentException | Yes |
| GS-59 | GameState | same card in two hole-card sets | IllegalArgumentException | Yes |
| GS-60 | GameState | same card in hole cards and deck | IllegalArgumentException | Yes |
| GS-61 | GameState | same card in board and deck | IllegalArgumentException | Yes |
| GS-62 | GameState | same card in board and hole cards | IllegalArgumentException | Yes |
| GS-63 | GameState | duplicate board card | IllegalArgumentException | Yes |
| GS-64 | GameState | duplicate deck card | IllegalArgumentException | Yes |
| GS-65 | GameState | null board element | IllegalArgumentException | Yes |
| GS-66 | GameState | null deck element | IllegalArgumentException | Yes |
| GS-67 | GameState | dealt street without all hole cards | IllegalArgumentException | Yes |
| GS-68 | GameState | setup already contains dealt hole cards | IllegalArgumentException | Yes |
| GS-69 | GameState | showdown still has actor | IllegalArgumentException | Yes |
| GS-70 | GameState | completion has unawarded commitments | IllegalArgumentException | Yes |
| GS-71 | GameState; getPot; getTotalChips | Completed match: one winner, all pots awarded | Pot zero; winner holds all 6000 chips | Yes |
| GS-72 | GameState; getCommunityCards; getRemainingDeck | Caller mutates supplied board/deck and returned lists | Defensive copy; returned lists unmodifiable | Yes |
| GS-73 | GameState | Completion snapshot whose unawarded pot overflows long | IllegalArgumentException; no partially constructed state | Yes |

`viewFor(String)` is covered by GV-01–GV-08 in [GameView.md](GameView.md).
This analysis replaces the earlier basic snapshot analysis after the full state API migration.

## Explicit betting-history extension

The original constructor supplies empty history for unraised hands. The new
constructor accepts immutable engine provenance; callers must preserve it when
constructing later snapshots. These cases also cover getBettingHistory.

| ID | Public methods | Input/system state | Expected output | Status / JUnit method |
| --- | --- | --- | --- | --- |
| GS-H01 | GameState(original constructor); getBettingHistory | Original constructor without explicit history | Empty history; previous constructor behavior preserved | Implemented, GREEN; `existingStateConstructorRemainsCompatible` |
| GS-H02 | GameState(..., BettingHistory); getBettingHistory | Explicit null history | IllegalArgumentException | Implemented, GREEN; `nullStateHistoryIsRejected` |
| GS-H03 | GameState(..., BettingHistory); getBettingHistory | History references unknown aggressor | IllegalArgumentException | Implemented, GREEN; `unknownAggressorIsRejected` |
| GS-H04 | GameState(..., BettingHistory); getBettingHistory | SETUP with recorded preflop aggressor | IllegalArgumentException; no accepted action before dealing | Implemented, GREEN; `setupCannotHaveAnAggressor` |
| GS-H05 | GameState(..., BettingHistory); getBettingHistory | PREFLOP with aggressor p1 and no c-bet, then with c-bet 20 | First accepted and getter retains p1; latter rejected | Implemented, GREEN; `preflopHistoryCannotAlreadyContainACBet` |
| GS-H06-19 | GameState(..., BettingHistory); getBettingHistory | FLOP with recorded c-bet 20 and aggressor round contribution 19 | IllegalArgumentException; insufficient actual commitment | Implemented, GREEN; `flopHistoryMustBeBackedByCommittedChips(19)` |
| GS-H06-20 | GameState(..., BettingHistory); getBettingHistory | FLOP with recorded c-bet 20 and aggressor round contribution 20 | Retain c-bet 20 | Implemented, GREEN; `flopHistoryMustBeBackedByCommittedChips(20)` |
| GS-H06-21 | GameState(..., BettingHistory); getBettingHistory | FLOP with recorded c-bet 20 and aggressor round contribution 21 | Retain c-bet 20 | Implemented, GREEN; `flopHistoryMustBeBackedByCommittedChips(21)` |
| GS-H07 | GameState(..., BettingHistory); getBettingHistory | Aggressor seat already ELIMINATED before completion | IllegalArgumentException | Implemented, GREEN; `anEliminatedAggressorIsRejectedBeforeCompletion` |
| GS-H08 | GameState(..., BettingHistory); getBettingHistory | COMPLETION after historical aggressor eliminated and all chips awarded | Historical aggressor/c-bet retained even though current commitments are zero | Implemented, GREEN; `completedHistoryCanOutlivePlayerElimination` |
| GS-H09-FLOP | GameState(..., BettingHistory); getBettingHistory; BettingEngine.advanceStreet | Advance closed FLOP with explicit p0/c-bet 20 | New TURN retains both provenance values; old state unchanged | Implemented, GREEN; `advancingDoesNotLoseBettingHistory(FLOP)` |
| GS-H09-TURN | GameState(..., BettingHistory); getBettingHistory; BettingEngine.advanceStreet | Advance closed TURN with explicit p0/c-bet 20 | New RIVER retains both provenance values; old state unchanged | Implemented, GREEN; `advancingDoesNotLoseBettingHistory(TURN)` |
| GS-H09-RIVER | GameState(..., BettingHistory); getBettingHistory; BettingEngine.advanceStreet | Advance closed RIVER with explicit p0/c-bet 20 | New SHOWDOWN retains both provenance values; old state unchanged | Implemented, GREEN; `advancingDoesNotLoseBettingHistory(RIVER)` |
| GS-H10-TURN | GameState(..., BettingHistory); getBettingHistory | TURN history c-bet 41 but aggressor hand commitment 40 | IllegalArgumentException; inconsistent historic investment | Implemented, GREEN; `historicalCBetRequiresEnoughHandCommitment(TURN)` |
| GS-H10-RIVER | GameState(..., BettingHistory); getBettingHistory | RIVER history c-bet 41 but aggressor hand commitment 40 | IllegalArgumentException; inconsistent historic investment | Implemented, GREEN; `historicalCBetRequiresEnoughHandCommitment(RIVER)` |

Tests: [BettingHistoryTest](../../src/test/java/domain/model/BettingHistoryTest.java).
