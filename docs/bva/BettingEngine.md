# BettingEngine BVA

Current full BET/RAISE and raised-round CALL boundaries are in
[FullBetting.md](FullBetting.md). Earlier increment tables below preserve the
original TDD history; their unsupported-wager scope is superseded by K01.

## Contract for review

`domain.model.BettingEngine.apply(GameState before, ActionRequest request)` returns
a `BettingResult` containing a new immutable state and one accepted engine action.
The engine does not own or update `PlayStats`; the application records an accepted
result after successful validation. Null inputs, an out-of-turn request, a
CHECK while chips are owed or a zero-cost CALL throw `IllegalArgumentException`. Unsupported behavior throws
`UnsupportedOperationException`. Neither failure produces a result or changes
the input state.

The original increments implemented FOLD/CHECK/CALL in unraised preflop and
CHECK/FOLD postflop. K01 extends them to full wagers and legal responses on all streets.
A CALL requires a positive amount owed and adds the smaller of that amount and
the available stack. Exhausting the stack sets ALL_IN and `becameAllIn=true`
while retaining CALL classification. FOLD is allowed on the player's turn even when
checking is possible. Decisions end when nobody remains pending or only one
participant remains. Full wagers and raised-round responses are now supported;
short all-in wager exceptions remain K02. The explicit `advanceStreet` operation supports closed
round transitions through SHOWDOWN; see the fourth increment below. Payouts,
short all-in wagers and numeric Swing controls remain later increments. The six original cases below verify the initial CALL and
out-of-turn/null-input rejection; the later tables extend the action boundaries.

## Shared state

The six original cases start from the real `GameInitializer`: two humans plus four bots,
six seats, 1,000 chips per player, dealer forced to seat 0, blinds 10/20, and a
seeded shuffled deck. Phase is PREFLOP; small blind is seat 1 with 990 chips;
big blind is seat 2 with 980 chips; actor is seat 3 with 1,000 chips and zero
round/hand contributions; pot is 30 and total chips are 6,000. Each player has
two cards and the remaining deck has 40 cards.

The actor's human/bot control type does not change the engine's betting rules.
An application or bot policy supplies the request using that player's actual ID.

| ID | Public methods under test | Input/state boundary | Expected result | Implemented status / JUnit method |
| --- | --- | --- | --- | --- |
| BE-CALL01 | BettingEngine(); apply; BettingResult.getState | Seat 3 calls, owing 20 with no previous contribution | New state: stack 980, round=20, hand=20, status ACTIVE; existing cards/config/identity retained; original state unchanged | Implemented, GREEN; `callMovesOnlyTheChipsOwedAndPreservesTheInput` |
| BE-CALL02 | BettingEngine(); apply; BettingResult.getState | Same call at the first decision | Pot 50; actor advances to seat 4; seat 3 removed from pending actors; current bet/full-raise size remain 20; total 6,000 | Implemented, GREEN; `callAdvancesTheActorAndConservesAllChips` |
| BE-CALL03 | BettingEngine(); apply; BettingResult.getAcceptedAction | Same accepted call | Matching hand/player IDs, PREFLOP/CALL, 20 new chips, contribution after=20, not all-in; no raise/three-bet/c-bet context; accepted statistics observation | Implemented, GREEN; `callProducesAnAcceptedObservationWithActualChipDelta` |
| BE-REJECT01 | BettingEngine(); apply | Seat 4 submits CALL while seat 3 must act | IllegalArgumentException; stacks, contributions, pot, actor, pending actors and cards remain unchanged | Implemented, GREEN; `outOfTurnCallIsRejectedWithoutChangingTheState` |
| BE-INPUT01 | BettingEngine(); apply | Null state and otherwise valid CALL request | IllegalArgumentException; no result | Implemented, GREEN; `nullStateIsRejected` |
| BE-INPUT02 | BettingEngine(); apply | Valid dealt state and null request | IllegalArgumentException; input state unchanged; no result | Implemented, GREEN; `nullRequestIsRejectedWithoutChangingTheState` |

Tests are in [BettingEngineTest](../../src/test/java/domain/model/BettingEngineTest.java).
The six tests were committed in the RED checkpoint before the production
implementation. They remain unchanged in this GREEN increment. These initial
cases do not establish complete betting-engine boundary or branch coverage.

## Fold/check and turn boundaries — second TDD increment

Unless stated otherwise, these cases use deterministic six-seat snapshots with
all 52 distinct cards accounted for, the same 1,000-chip / 10–20 settings, and
stable IDs `p0`–`p5`. Mid-match fixtures explicitly redistribute chips when a
seat has been eliminated or posts an all-in blind, preserving the 6,000 total.
`beforeBlindDecision(1)` calls seats 3, 4, 5 and 0 in order; the seat-2 version
also calls from seat 1. Tests snapshot all observable input values before an
operation to check that rejected or accepted actions retain the earlier state.

A FOLD keeps all chips already committed, including blinds, and adds no chips.
A CHECK requires zero chips owed and also adds no chips. Both remove the actor
from pending decisions and raising rights. The next actor is the next clockwise
pending active player. With no pending decisions, the actor is absent. If only
one participant remains, all decisions stop; ALL_IN players still count as
participants. These snapshots await separate street advancement or settlement:
no flop is dealt, no pot is awarded, and commitments/current bet are retained.

Every row below tests `BettingEngine()` and `apply`. Accepted rows also exercise
`BettingResult.getState` and `getAcceptedAction`; rejected rows return no result.
Parameterized case suffixes identify the input value in the corresponding test.

| ID | Input/system state | Expected output | Status / JUnit method |
| --- | --- | --- | --- |
| BE-FOLD01 | Initial actor, seat 3, folds with zero contribution | FOLDED; stack/cards/contributions retained; no chips added; actor 4; pending/raising exclude seat 3; pot 30; total 6,000; original snapshot retained; accepted PREFLOP/FOLD with no raise/c-bet context | Implemented, GREEN; `foldRetainsCardsAndChipsAndRemovesDecisionRights` |
| BE-FOLD02-SB | Seat 1 folds after four calls; previously posted 10 | Stack 990; round/hand contribution 10; pot 110 unchanged; accepted FOLD adds 0, contribution after 10 | Implemented, GREEN; `foldingFromABlindDoesNotRefundItsContribution(1)` |
| BE-FOLD02-BB | Seat 2 folds after five calls; previously posted 20 | Stack 980; round/hand contribution 20; pot 120 unchanged; accepted FOLD adds 0, contribution after 20 | Implemented, GREEN; `foldingFromABlindDoesNotRefundItsContribution(2)` |
| BE-CHECK01 | Five players called; BB at seat 2 owes 0 | Accepted CHECK adds 0; stack 980 and contributions 20 retained; pot 120, total 6,000; no actor/pending/raise rights; phase PREFLOP and cards retained; no dealing or payout | Implemented, GREEN; `bigBlindChecksWithoutAddingChipsOrDealingTheFlop` |
| BE-CHECK02-1 | Seat 3 has contributed 19 against a current bet of 20 | IllegalArgumentException; complete input snapshot retained | Implemented, GREEN; `checkIsRejectedWhenAnyChipsAreOwed(1)` |
| BE-CHECK02-10 | Seat 3 has contributed 10 against 20 | IllegalArgumentException; complete input snapshot retained | Implemented, GREEN; `checkIsRejectedWhenAnyChipsAreOwed(10)` |
| BE-CHECK02-20 | Seat 3 has contributed 0 against 20 | IllegalArgumentException; complete input snapshot retained | Implemented, GREEN; `checkIsRejectedWhenAnyChipsAreOwed(20)` |
| BE-CALL04 | BB calls while owing 0 after five calls | IllegalArgumentException; must CHECK instead; snapshot retained | Implemented, GREEN; `zeroCostCallIsRejectedWithoutChangingTheState` |
| BE-ORDER02-FOLD | Seat 4 folds while actor is seat 3 | IllegalArgumentException; snapshot retained | Implemented, GREEN; `foldAndCheckMustComeFromTheCurrentActor(FOLD)` |
| BE-ORDER02-CHECK | Seat 4 checks while actor is seat 3 | IllegalArgumentException; snapshot retained | Implemented, GREEN; `foldAndCheckMustComeFromTheCurrentActor(CHECK)` |
| BE-ORDER03-FOLD | Unknown player requests FOLD | IllegalArgumentException; snapshot retained | Implemented, GREEN; `anUnknownPlayerCannotAct(FOLD)` |
| BE-ORDER03-CHECK | Unknown player requests CHECK | IllegalArgumentException; snapshot retained | Implemented, GREEN; `anUnknownPlayerCannotAct(CHECK)` |
| BE-TURN01 | Seat 3 folds; seat 4 folded; seat 5 all-in; pending seats 0–3 | Next actor 0, skipping 4/5 and wrapping; pending 0/1/2; pot 50, total 6,000; input retained | Implemented, GREEN; `nextActorSkipsFoldedAndAllInSeats` |
| BE-TURN02 | Seat 3 folds; seat 4 eliminated; seat 5 pending | Actor 5; all six seat numbers retained; total 6,000 | Implemented, GREEN; `nextActorSkipsAnEliminatedSeat` |
| BE-TURN03 | Heads-up: dealer/SB seat 0 with 2,990 and commitment 10 folds; BB seat 5 has 2,980 and commitment 20 | One participant remains; no actor, pending decisions or raise rights; pot 30 retained without payout; total 6,000; phase PREFLOP; original retained | Implemented, GREEN; `lastFoldLeavesOneParticipantAndNoFurtherActor` |
| BE-TURN04 | BB is all-in for 20; seats 3/4/5/0 fold; SB still owes 10 | SB remains actor despite being the only active player; all-in opponent remains live; SB's funded call yields pot 40, total 6,000, no further actor | Implemented, GREEN; `anAllInOpponentDoesNotMakeTheRemainingActivePlayerAnUncontestedWinner` |
| BE-CLOSED01-FOLD | BB has checked; no pending decisions; requests FOLD | IllegalArgumentException; closed snapshot retained | Implemented, GREEN; `noActionIsAcceptedAfterTheRoundHasNoActor(FOLD)` |
| BE-CLOSED01-CHECK | Same closed round; requests CHECK | IllegalArgumentException; closed snapshot retained | Implemented, GREEN; `noActionIsAcceptedAfterTheRoundHasNoActor(CHECK)` |
| BE-CLOSED01-CALL | Same closed round; requests CALL | IllegalArgumentException; closed snapshot retained | Implemented, GREEN; `noActionIsAcceptedAfterTheRoundHasNoActor(CALL)` |
| BE-LIMIT01-FOLD | Current bet60, last full raise40; actor FOLD | Accepted FOLD, adds0; input retained | Passing; `raisedRoundsAcceptLegalResponsesAndRejectUnpaidChecks(FOLD)` |
| BE-LIMIT01-CHECK | Same raised round; actor CHECK while owing60 | IllegalArgumentException; input retained | Passing; `raisedRoundsAcceptLegalResponsesAndRejectUnpaidChecks(CHECK)` |
| BE-LIMIT01-CALL | Same raised round; actor CALL | Accepted CALL adds60; total6000; input retained | Passing; `raisedRoundsAcceptLegalResponsesAndRejectUnpaidChecks(CALL)` |
| BE-LIMIT02-BET | Preflop actor BET to60 | IllegalArgumentException; preflop requires RAISE over blinds | Passing; `preflopUsesRaiseRatherThanBet(BET)` |
| BE-LIMIT02-RAISE | Preflop actor RAISE to60 | Accepted RAISE adds60; input retained | Passing; `preflopUsesRaiseRatherThanBet(RAISE)` |
| BE-SEQUENCE01 | Real GameInitializer, dealer 0: seat 3 CALL, 4 FOLD, 5 CALL, 0 CALL, 1 CALL, 2 CHECK | BB retains final decision after SB call; check ends decisions; five live hands; every live contribution 20; pot 100; total 6,000 throughout; cards and earlier snapshot retained; no next street or payout | Implemented, GREEN; `controlledPreflopSequenceEndsAtPot100WithFiveLiveHands` |

Tests are in [BettingEngineFoldCheckTest](../../src/test/java/domain/model/BettingEngineFoldCheckTest.java).
The RED test run precedes production changes. No commits are made automatically;
RED/GREEN logs and patches are retained for review. Coverage/mutation adequacy and
additional betting contexts are separate later verification work.

## All-in CALL boundaries — third TDD increment

The implemented contract caps a CALL at the smaller of chips owed and the player's
available stack. A positive call that exhausts the stack is accepted as CALL,
with `becameAllIn=true` and status ALL_IN. A short call does not lower
`currentBet`, change `lastFullRaiseSize`, reopen raising rights or increase the
amount another player owes. An all-in player remains a hand participant but has
no further pending decisions or raising rights. At that original checkpoint the engine accepted only
unraised preflop decisions in this increment.

Unit fixtures use the same 1,000-chip / 10–20 configuration and a 6,000 total.
To represent different stacks in a later match hand, displaced chips are moved
to seat 0. Seat 3 initially owes 20 with no contribution and a pot of 30. For
seat 1, seats 3/4/5/0 have called: SB has already contributed 10, owes another
10, pot is 110, and pending seats are 1/2. Stack boundaries use 1 (minimum
active stack), one below the amount owed, exactly owed and one above owed.

Each row tests `BettingEngine()` / `apply`. Accepted rows also test both
`BettingResult` getters and the CALL observation's statistics bridge. Tests
assert the exact added chips, resulting round/hand contribution, remaining
stack, status, all-in flag, unchanged betting level and raise size, pending and
raising rights, next actor, conserved total and unchanged earlier snapshot.
Cards, identity and unaffected players must be retained. No payout or next
street is produced. Case IDs are included in JUnit display names.

| ID | Input/system state | Expected output | Status / JUnit method |
| --- | --- | --- | --- |
| BE-AIC01 | Seat 3 owes 20; stack 1; round/hand contribution 0 | Add 1; stack 0; contributions 1; ALL_IN; becameAllIn true; pot 31; actor 4 | Implemented, GREEN; `callUsesOnlyAvailableChipsAndRetainsItsBettingMeaning` |
| BE-AIC02 | Seat 3 owes 20; stack 19 | Add 19; stack 0; contributions 19; ALL_IN; flag true; pot 49; actor 4 | Implemented, GREEN; `callUsesOnlyAvailableChipsAndRetainsItsBettingMeaning` |
| BE-AIC03 | Seat 3 owes 20; stack 20 | Add 20; stack 0; contributions 20; ALL_IN; flag true; pot 50; actor 4 | Implemented, GREEN; `callUsesOnlyAvailableChipsAndRetainsItsBettingMeaning` |
| BE-AIC04 | Seat 3 owes 20; stack 21 | Add 20; stack 1; contributions 20; ACTIVE; flag false; pot 50; actor 4 | Implemented, GREEN; `callUsesOnlyAvailableChipsAndRetainsItsBettingMeaning` |
| BE-AIC05 | SB seat 1 owes 10; stack 1; previously contributed 10 | Add 1; stack 0; contributions 11; ALL_IN; flag true; pot 111; actor 2 | Implemented, GREEN; `callUsesOnlyAvailableChipsAndRetainsItsBettingMeaning` |
| BE-AIC06 | SB owes 10; stack 9; previously contributed 10 | Add 9; stack 0; contributions 19; ALL_IN; flag true; pot 119; actor 2 | Implemented, GREEN; `callUsesOnlyAvailableChipsAndRetainsItsBettingMeaning` |
| BE-AIC07 | SB owes 10; stack 10; previously contributed 10 | Add 10; stack 0; contributions 20; ALL_IN; flag true; pot 120; actor 2 | Implemented, GREEN; `callUsesOnlyAvailableChipsAndRetainsItsBettingMeaning` |
| BE-AIC08 | SB owes 10; stack 11; previously contributed 10 | Add 10; stack 1; contributions 20; ACTIVE; flag false; pot 120; actor 2 | Implemented, GREEN; `callUsesOnlyAvailableChipsAndRetainsItsBettingMeaning` |
| BE-AIC09-19 | Actor is seat 3; seat 4 with stack 19 requests CALL | IllegalArgumentException; no accepted result; seat 4 remains ACTIVE with stack 19, zero contribution; actor/pending rights/pot 30/total 6,000 unchanged | Implemented, GREEN; `anAllInCallStillRequiresTheCorrectTurn(19)` |
| BE-AIC09-20 | Actor is seat 3; seat 4 with stack 20 requests CALL | IllegalArgumentException; no accepted result; seat 4 remains ACTIVE with stack 20, zero contribution; actor/pending rights/pot 30/total 6,000 unchanged | Implemented, GREEN; `anAllInCallStillRequiresTheCorrectTurn(20)` |
| BE-AIC10-19 | Seat 3 has just made a short all-in CALL for 19; requests CALL again | IllegalArgumentException; zero stack, ALL_IN status, contribution 19, pot 49, next actor 4 and total 6,000 retained; no decision/raising rights for seat 3 | Implemented, GREEN; `anAllInPlayerCannotSubmitAnotherAction(19)` |
| BE-AIC10-20 | Seat 3 has just made an exact-stack CALL for 20; requests CALL again | IllegalArgumentException; zero stack, ALL_IN status, contribution 20, pot 50, next actor 4 and total 6,000 retained; no decision/raising rights for seat 3 | Implemented, GREEN; `anAllInPlayerCannotSubmitAnotherAction(20)` |
| BE-AIC11 | Actual GameInitializer with starting stacks 20 and blinds 10/20; BB already all-in from its forced post; seats 3/4/5/0/1 CALL | Calls add 20/20/20/20/10, all resolved CALL with all-in flag true; all six remain participants, stacks 0, contributions 20; pot/total 120; no actor or pending/raising rights; BB gets no extra decision; phase/cards and earlier snapshot retained | Implemented, GREEN; `AllInCallIntegrationTest.everyoneCanBecomeAllInWithoutAnExtraBlindDecision` |
| BE-AIC12 | Real default setup/dealing with seat 3 stack 19 and seat 0 stack 1,981; sequence 3 CALL, 4 FOLD, 5 CALL, 0 CALL, 1 CALL, 2 CHECK | Short caller remains ALL_IN with contribution 19; other live contributions 20; five live hands; BB retains its check option; final pot 99, total 6,000; current bet/full raise 20; no actor, payout or dealing; original snapshot/cards retained | Implemented, GREEN; `AllInCallIntegrationTest.shortAllInCallLeavesTheBettingLevelAndOtherDecisionsUnchanged` |

Unit tests: [BettingEngineAllInCallTest](../../src/test/java/domain/model/BettingEngineAllInCallTest.java).
Integration tests: [AllInCallIntegrationTest](../../src/test/java/integration/AllInCallIntegrationTest.java).
These cases extend the existing turn, null-input and zero-cost CALL rejection
cases; those tests remain regression checks. RED/GREEN snapshots and logs are
retained without automatic commits. Coverage/mutation adequacy and side-pot
settlement remain separate verification and implementation work.

## Street advancement — fourth TDD increment

`BettingEngine.advanceStreet(GameState before)` returns a new immutable
`GameState`, not a player `AcceptedAction`. It advances one closed round through
PREFLOP → FLOP → TURN → RIVER → SHOWDOWN. It never awards chips, declares a winner,
records a voluntary action or marks the hand COMPLETION.

Policy: burn the first remaining card before each community-card street, then
deal three flop cards or one turn/river card in deck order. River → SHOWDOWN
neither burns nor deals. Burned cards are removed from stored card locations.
Every rejection retains the complete observable input snapshot.

Advancement requires no pending decisions, at least two participants (including
all-in players), and no ACTIVE player owing chips. A short all-in contribution
below the current bet is allowed; a folded player's contribution also need not
match. New betting streets reset every round contribution/currentBet to zero,
preserve hand contributions/stacks/statuses/cards, and reset lastFullRaiseSize
to the big blind. Pending decisions and raising rights contain all ACTIVE IDs
only when at least two can act. Their first actor is clockwise after the dealer,
including heads-up order. Zero/one ACTIVE players get no artificial check decision;
repeat advancement to run out the board. SHOWDOWN preserves the completed river's
round accounting and has no actor, pending decisions or raising rights.

Unit fixtures use distinct ordered cards from `FoundationFixtures.cards()`.
Hole cards occupy indices 0–11; burn indices are 12, 16 and 18; the full board
is indices 13/14/15/17/19. Default stacks plus commitments total 6,000. Closed
preflop fixtures have each round/hand contribution 60, stack 940, current bet 60,
and last full raise 40; postflop fixtures have round 40, hand 100 and stack 900.
All old snapshots must remain unchanged. Parameterized IDs include each boundary.

Each row tests `BettingEngine()` / `advanceStreet`. Expected successful outputs
also retain hand identity, settings, seats, statuses, hole cards, hand commitments,
pot and total chips. No user-interface or bot implementation is involved.

| ID | Input/system state | Expected output | Status / JUnit method |
| --- | --- | --- | --- |
| BE-SA01 | Closed PREFLOP, 6 ACTIVE, deck 40, round/hand 60 each, last raise 40 | FLOP board indices 13/14/15; deck 36; round/currentBet 0; last raise 20; actor 1; all six pending/raise eligible; pot 360, total 6,000 | Implemented, GREEN; `advancementPreservesTheHandAndUsesTheCorrectBoardOrder` |
| BE-SA02 | Closed FLOP, board 3, deck 36, round 40/hand 100 each | TURN adds index 17 after burning 16; deck 34; round/currentBet 0, last raise 20; actor 1/all six pending; pot 600, total 6,000 | Implemented, GREEN; `advancementPreservesTheHandAndUsesTheCorrectBoardOrder` |
| BE-SA03 | Closed TURN, board 4, deck 34 | RIVER adds index 19 after burning 18; deck 32; round/currentBet 0, last raise 20; actor 1/all six pending; pot 600, total 6,000 | Implemented, GREEN; `advancementPreservesTheHandAndUsesTheCorrectBoardOrder` |
| BE-SA04 | Closed RIVER, board 5, deck 32, round/currentBet 40 and last raise 40 | SHOWDOWN; board/deck and round accounting retained; no actor/pending/raising rights; pot 600, total 6,000; no payout | Implemented, GREEN; `advancementPreservesTheHandAndUsesTheCorrectBoardOrder` |
| BE-SA05-0 | Closed PREFLOP, 0 cards remaining | IllegalArgumentException; no state/card changes | Implemented, GREEN; `dealingRequiresTheBurnCardAndAllCardsForThatStreet` |
| BE-SA05-1 | Closed PREFLOP, only 1 card remaining | IllegalArgumentException; no partial burn/deal | Implemented, GREEN; `dealingRequiresTheBurnCardAndAllCardsForThatStreet` |
| BE-SA05-3 | Closed PREFLOP, one below required deck size (3) | IllegalArgumentException; no state/card changes | Implemented, GREEN; `dealingRequiresTheBurnCardAndAllCardsForThatStreet` |
| BE-SA05-4 | Closed PREFLOP, exactly 4 cards | FLOP; burn 1/deal 3; remaining deck 0 | Implemented, GREEN; `dealingRequiresTheBurnCardAndAllCardsForThatStreet` |
| BE-SA05-5 | Closed PREFLOP, one above required size (5) | FLOP; burn 1/deal 3; remaining deck 1 | Implemented, GREEN; `dealingRequiresTheBurnCardAndAllCardsForThatStreet` |
| BE-SA05-40 | Closed PREFLOP, normal deck size 40 | FLOP; burn 1/deal 3; remaining deck 36 | Implemented, GREEN; `dealingRequiresTheBurnCardAndAllCardsForThatStreet` |
| BE-SA06-0 | Closed FLOP, deck 0 | IllegalArgumentException; unchanged | Implemented, GREEN; `dealingRequiresTheBurnCardAndAllCardsForThatStreet` |
| BE-SA06-1 | Closed FLOP, one below required size (1) | IllegalArgumentException; no partial burn | Implemented, GREEN; `dealingRequiresTheBurnCardAndAllCardsForThatStreet` |
| BE-SA06-2 | Closed FLOP, exactly 2 cards | TURN; burn 1/deal 1; remaining deck 0 | Implemented, GREEN; `dealingRequiresTheBurnCardAndAllCardsForThatStreet` |
| BE-SA06-3 | Closed FLOP, one above required size (3) | TURN; burn 1/deal 1; remaining deck 1 | Implemented, GREEN; `dealingRequiresTheBurnCardAndAllCardsForThatStreet` |
| BE-SA07-0 | Closed TURN, deck 0 | IllegalArgumentException; unchanged | Implemented, GREEN; `dealingRequiresTheBurnCardAndAllCardsForThatStreet` |
| BE-SA07-1 | Closed TURN, one below required size (1) | IllegalArgumentException; no partial burn | Implemented, GREEN; `dealingRequiresTheBurnCardAndAllCardsForThatStreet` |
| BE-SA07-2 | Closed TURN, exactly 2 cards | RIVER; burn 1/deal 1; remaining deck 0 | Implemented, GREEN; `dealingRequiresTheBurnCardAndAllCardsForThatStreet` |
| BE-SA07-3 | Closed TURN, one above required size (3) | RIVER; burn 1/deal 1; remaining deck 1 | Implemented, GREEN; `dealingRequiresTheBurnCardAndAllCardsForThatStreet` |
| BE-SA08-1 | Actual setup followed by five calls; BB still has its check option | IllegalArgumentException; pending BB decision and all values retained | Implemented, GREEN; `aPendingDecisionPreventsAdvancement(1)` |
| BE-SA08-6 | Actual setup with all six initial decisions pending | IllegalArgumentException; complete input retained | Implemented, GREEN; `aPendingDecisionPreventsAdvancement(6)` |
| BE-SA09-0 | Closed structural snapshot with zero participants | IllegalArgumentException; no dealing/payout | Implemented, GREEN; `anUncontestedOrEmptyHandDoesNotDealMoreCards(0)` |
| BE-SA09-1 | Only one participant remains after opponents folded | IllegalArgumentException; await settlement, no dealing/payout | Implemented, GREEN; `anUncontestedOrEmptyHandDoesNotDealMoreCards(1)` |
| BE-SA10-1 | Empty pending set but an ACTIVE player owes 1 | IllegalArgumentException; unchanged; an empty pending set alone is insufficient | Implemented, GREEN; `anActivePlayerOwingChipsPreventsAdvancement(1)` |
| BE-SA10-20 | Empty pending set but an ACTIVE player owes 20 | IllegalArgumentException; unchanged | Implemented, GREEN; `anActivePlayerOwingChipsPreventsAdvancement(20)` |
| BE-SA11-SETUP | Undealt SETUP snapshot | UnsupportedOperationException; unchanged | Implemented, GREEN; `setupAndTerminalPhasesCannotAdvance(SETUP)` |
| BE-SA11-SHOWDOWN | SHOWDOWN snapshot | UnsupportedOperationException; cannot skip settlement | Implemented, GREEN; `setupAndTerminalPhasesCannotAdvance(SHOWDOWN)` |
| BE-SA11-COMPLETION | COMPLETION snapshot | UnsupportedOperationException; no new hand started | Implemented, GREEN; `setupAndTerminalPhasesCannotAdvance(COMPLETION)` |
| BE-SA12 | Null state | IllegalArgumentException; no result | Implemented, GREEN; `nullStateIsRejected` |
| BE-SA13 | Dealer 0; seat 1 FOLDED, 2 ALL_IN, 3 ELIMINATED, 0/4/5 ACTIVE | FLOP actor 4; pending/raise IDs 0/4/5; all six seats and statuses retained; every round contribution 0 including folded/all-in; hand commitments/pot 300 retained; total 6,000 | Implemented, GREEN; `postflopActorsExcludeIneligibleSeats` |
| BE-SA14 | Heads-up: dealer/SB 0 and BB 5; other seats eliminated | FLOP actor 5; pending/raise IDs 0/5; pot 120, total 6,000 | Implemented, GREEN; `headsUpActionStartsWithTheNonDealer` |
| BE-SA15-0 | Six ALL_IN participants; no ACTIVE player; total 120 | FLOP with no actor/pending/raise rights; all six remain participants; pot/total 120 | Implemented, GREEN; `allInRunoutDoesNotCreateUnnecessaryChecks(0)` |
| BE-SA15-1 | Five ALL_IN participants and one matched ACTIVE player; total 6,000 | FLOP with no unnecessary check decision; all six remain live; pot 120, total 6,000 | Implemented, GREEN; `allInRunoutDoesNotCreateUnnecessaryChecks(1)` |
| BE-SA16 | Closed RIVER with 0 cards left | SHOWDOWN without burn/deal; board retained; round/currentBet/last raise 40; pot 600; total 6,000; no actor | Implemented, GREEN; `showdownDoesNotBurnOrDealAnotherCard` |
| BE-SAI01 | Real dealing and preflop sequence including short CALL 19, FOLD and BB CHECK | FLOP board/deck follow burn policy; actor 1, pending/raise seats 0/1/2/5; all-in seat 3 retains hand 19; folded 4 retains hand 0; all rounds/currentBet 0; pot 99, total 6,000; original snapshots/cards retained | Implemented, GREEN; `StreetAdvancementIntegrationTest.aClosedShortAllInRoundAdvancesWithoutLosingCommittedChips` |
| BE-SAI02-0 | Actual setup with stacks 20/blinds 10–20, followed by five exact-stack calls | Four advances reach SHOWDOWN with board deck offsets 1/2/3/5/7, deck 32, no actor/actions/payout; six ALL_IN participants, hand commitments 20; pot/total 120 | Implemented, GREEN; `StreetAdvancementIntegrationTest.anAllInRunoutReachesShowdownWithoutPlayerActionsOrPayout(0)` |
| BE-SAI02-1 | Real dealt cards in valid mid-match state: five ALL_IN and sole ACTIVE stack 5,880, all committed 20 | Same runout with no unnecessary checks; six participants; pot 120, total 6,000; sole active stack retained | Implemented, GREEN; `StreetAdvancementIntegrationTest.anAllInRunoutReachesShowdownWithoutPlayerActionsOrPayout(1)` |

Tests: [StreetAdvancementTest](../../src/test/java/domain/model/StreetAdvancementTest.java)
and [StreetAdvancementIntegrationTest](../../src/test/java/integration/StreetAdvancementIntegrationTest.java).
The API scaffold and RED tests precede production behavior. Preserve separate
RED/GREEN commits later using the review checkpoints; no commits are automatic.
Postflop CHECK/FOLD follow in the fifth increment. Hand evaluation, payouts and
statistics lifecycle wiring remain separate increments. These tests do not establish measured full-project
cyclomatic coverage or mutation adequacy.

## Postflop CHECK/FOLD — fifth TDD increment

This extension accepts CHECK/FOLD on FLOP/TURN/RIVER. CHECK requires zero chips
owed; FOLD retains previous round/hand investment. Both add zero chips, remove
the actor's pending/raising rights and emit the actual betting street. Turn
selection uses pending ACTIVE players; the final decision closes the round
without dealing or paying chips. An uncontested hand waits for settlement.
Postflop CALL/BET/RAISE remain outside this increment.

Fixtures use blinds 10/20 and default total 6,000. Unless overridden, each
player has invested 40 preflop and retains stack 960, round contribution 0,
hand contribution 40; actor is p1, current bet 0, full raise size 20, pot 240.
Bet-facing fixtures explicitly add round investments while deducting stacks.
The immutable BettingHistory carries the final preflop aggressor and an actual
flop c-bet's original round target; no history is reconstructed inside PlayStats.
An opening opportunity requires that aggressor, unopened flop betting, betting
rights and at least two ACTIVE players. Facing requires an actual recorded
c-bet, chips owed and the original betting level; facing a later raise is not
folding to the original c-bet. Turn/river actions carry no flop opportunity.
Only accepted results emit observations; rejected requests retain all values.

| ID | Public methods | Input/system state | Expected output | Status / JUnit method |
| --- | --- | --- | --- | --- |
| BE-PF01-FLOP | BettingEngine.apply; BettingResult.getState; getAcceptedAction | FLOP actor 1 CHECK with zero owed and no preflop aggressor | ACTIVE; chips/contributions/cards retained; actor 2; five pending; accepted FLOP/CHECK adds 0; no stats opportunities | Implemented, GREEN; `zeroCostActionsPreserveChipsAndReportTheActualStreet` |
| BE-PF02-FLOP | BettingEngine.apply; BettingResult.getState; getAcceptedAction | FLOP actor 1 FOLD with zero owed | FOLDED; previous hand investment 40 retained; actor 2; accepted FLOP/FOLD adds 0; pot 240/total 6,000 | Implemented, GREEN; `zeroCostActionsPreserveChipsAndReportTheActualStreet` |
| BE-PF01-TURN | BettingEngine.apply; BettingResult.getState; getAcceptedAction | TURN actor 1 CHECK with zero owed and no preflop aggressor | ACTIVE; chips/contributions/cards retained; actor 2; five pending; accepted TURN/CHECK adds 0; no stats opportunities | Implemented, GREEN; `zeroCostActionsPreserveChipsAndReportTheActualStreet` |
| BE-PF02-TURN | BettingEngine.apply; BettingResult.getState; getAcceptedAction | TURN actor 1 FOLD with zero owed | FOLDED; previous hand investment 40 retained; actor 2; accepted TURN/FOLD adds 0; pot 240/total 6,000 | Implemented, GREEN; `zeroCostActionsPreserveChipsAndReportTheActualStreet` |
| BE-PF01-RIVER | BettingEngine.apply; BettingResult.getState; getAcceptedAction | RIVER actor 1 CHECK with zero owed and no preflop aggressor | ACTIVE; chips/contributions/cards retained; actor 2; five pending; accepted RIVER/CHECK adds 0; no stats opportunities | Implemented, GREEN; `zeroCostActionsPreserveChipsAndReportTheActualStreet` |
| BE-PF02-RIVER | BettingEngine.apply; BettingResult.getState; getAcceptedAction | RIVER actor 1 FOLD with zero owed | FOLDED; previous hand investment 40 retained; actor 2; accepted RIVER/FOLD adds 0; pot 240/total 6,000 | Implemented, GREEN; `zeroCostActionsPreserveChipsAndReportTheActualStreet` |
| BE-PF03-FLOP-1 | BettingEngine.apply | FLOP actor 1 CHECK owing 1 | IllegalArgumentException; complete snapshot unchanged | Implemented, GREEN; `checkIsRejectedWhenChipsAreOwed(FLOP,1)` |
| BE-PF03-FLOP-20 | BettingEngine.apply | FLOP actor 1 CHECK owing 20 | IllegalArgumentException; complete snapshot unchanged | Implemented, GREEN; `checkIsRejectedWhenChipsAreOwed(FLOP,20)` |
| BE-PF04-FLOP | BettingEngine.apply; BettingResult.getState; getAcceptedAction | FLOP actor 1 FOLD with round 10/hand 50, current bet 20 | Stack 950 and commitments retained; pot 270, total 6,000; next actor 2; accepted adds 0/contribution after 10 | Implemented, GREEN; `foldRetainsPreviouslyCommittedChips(FLOP)` |
| BE-PF05-FLOP-CHECK | BettingEngine.apply | FLOP actor 1; seat 4 requests CHECK | IllegalArgumentException; complete input unchanged | Implemented, GREEN; `postflopActionsStillRequireTheCorrectTurn(FLOP,CHECK)` |
| BE-PF05-FLOP-FOLD | BettingEngine.apply | FLOP actor 1; seat 4 requests FOLD | IllegalArgumentException; complete input unchanged | Implemented, GREEN; `postflopActionsStillRequireTheCorrectTurn(FLOP,FOLD)` |
| BE-PF06-FLOP | BettingEngine.apply; BettingResult.getState | FLOP only pending p1; p0 still in raising-rights set; p1 CHECK | No actor/pending/raise rights; current phase/cards/pot retained; no automatic dealing | Implemented, GREEN; `theLastCheckClosesTheRound(FLOP)` |
| BE-PF07-FLOP | BettingEngine.apply; BettingResult.getState; advanceStreet | FLOP p1 FOLD leaving only p0 participating | All decisions stop; winner stack unchanged/pot 240 unawarded; advancement rejected pending settlement | Implemented, GREEN; `oneRemainingParticipantGetsNoFurtherDecisionOrPrematurePayout(FLOP)` |
| BE-PF03-TURN-1 | BettingEngine.apply | TURN actor 1 CHECK owing 1 | IllegalArgumentException; complete snapshot unchanged | Implemented, GREEN; `checkIsRejectedWhenChipsAreOwed(TURN,1)` |
| BE-PF03-TURN-20 | BettingEngine.apply | TURN actor 1 CHECK owing 20 | IllegalArgumentException; complete snapshot unchanged | Implemented, GREEN; `checkIsRejectedWhenChipsAreOwed(TURN,20)` |
| BE-PF04-TURN | BettingEngine.apply; BettingResult.getState; getAcceptedAction | TURN actor 1 FOLD with round 10/hand 50, current bet 20 | Stack 950 and commitments retained; pot 270, total 6,000; next actor 2; accepted adds 0/contribution after 10 | Implemented, GREEN; `foldRetainsPreviouslyCommittedChips(TURN)` |
| BE-PF05-TURN-CHECK | BettingEngine.apply | TURN actor 1; seat 4 requests CHECK | IllegalArgumentException; complete input unchanged | Implemented, GREEN; `postflopActionsStillRequireTheCorrectTurn(TURN,CHECK)` |
| BE-PF05-TURN-FOLD | BettingEngine.apply | TURN actor 1; seat 4 requests FOLD | IllegalArgumentException; complete input unchanged | Implemented, GREEN; `postflopActionsStillRequireTheCorrectTurn(TURN,FOLD)` |
| BE-PF06-TURN | BettingEngine.apply; BettingResult.getState | TURN only pending p1; p0 still in raising-rights set; p1 CHECK | No actor/pending/raise rights; current phase/cards/pot retained; no automatic dealing | Implemented, GREEN; `theLastCheckClosesTheRound(TURN)` |
| BE-PF07-TURN | BettingEngine.apply; BettingResult.getState; advanceStreet | TURN p1 FOLD leaving only p0 participating | All decisions stop; winner stack unchanged/pot 240 unawarded; advancement rejected pending settlement | Implemented, GREEN; `oneRemainingParticipantGetsNoFurtherDecisionOrPrematurePayout(TURN)` |
| BE-PF03-RIVER-1 | BettingEngine.apply | RIVER actor 1 CHECK owing 1 | IllegalArgumentException; complete snapshot unchanged | Implemented, GREEN; `checkIsRejectedWhenChipsAreOwed(RIVER,1)` |
| BE-PF03-RIVER-20 | BettingEngine.apply | RIVER actor 1 CHECK owing 20 | IllegalArgumentException; complete snapshot unchanged | Implemented, GREEN; `checkIsRejectedWhenChipsAreOwed(RIVER,20)` |
| BE-PF04-RIVER | BettingEngine.apply; BettingResult.getState; getAcceptedAction | RIVER actor 1 FOLD with round 10/hand 50, current bet 20 | Stack 950 and commitments retained; pot 270, total 6,000; next actor 2; accepted adds 0/contribution after 10 | Implemented, GREEN; `foldRetainsPreviouslyCommittedChips(RIVER)` |
| BE-PF05-RIVER-CHECK | BettingEngine.apply | RIVER actor 1; seat 4 requests CHECK | IllegalArgumentException; complete input unchanged | Implemented, GREEN; `postflopActionsStillRequireTheCorrectTurn(RIVER,CHECK)` |
| BE-PF05-RIVER-FOLD | BettingEngine.apply | RIVER actor 1; seat 4 requests FOLD | IllegalArgumentException; complete input unchanged | Implemented, GREEN; `postflopActionsStillRequireTheCorrectTurn(RIVER,FOLD)` |
| BE-PF06-RIVER | BettingEngine.apply; BettingResult.getState | RIVER only pending p1; p0 still in raising-rights set; p1 CHECK | No actor/pending/raise rights; current phase/cards/pot retained; no automatic dealing | Implemented, GREEN; `theLastCheckClosesTheRound(RIVER)` |
| BE-PF07-RIVER | BettingEngine.apply; BettingResult.getState; advanceStreet | RIVER p1 FOLD leaving only p0 participating | All decisions stop; winner stack unchanged/pot 240 unawarded; advancement rejected pending settlement | Implemented, GREEN; `oneRemainingParticipantGetsNoFurtherDecisionOrPrematurePayout(RIVER)` |
| BE-PF08 | BettingEngine.apply; BettingResult.getState | FLOP actor 0; 1 FOLDED, 2 ALL_IN, 3 ELIMINATED; only 0/4 pending; 5 ACTIVE already acted | CHECK 0 advances to 4; CHECK 4 closes decisions; no turn for inactive/already-acted seats; pot 200, total 6,000 | Implemented, GREEN; `actorSelectionUsesOnlyPendingActivePlayers` |
| BE-PF09-FLOP | BettingEngine.apply | Closed FLOP; p1 requests CHECK | IllegalArgumentException; state unchanged | Implemented, GREEN; `aClosedPostflopRoundAcceptsNoMoreActions(FLOP)` |
| BE-PF09-TURN | BettingEngine.apply | Closed TURN; p1 requests CHECK | IllegalArgumentException; state unchanged | Implemented, GREEN; `aClosedPostflopRoundAcceptsNoMoreActions(TURN)` |
| BE-PF09-RIVER | BettingEngine.apply | Closed RIVER; p1 requests CHECK | IllegalArgumentException; state unchanged | Implemented, GREEN; `aClosedPostflopRoundAcceptsNoMoreActions(RIVER)` |
| BE-PF10 | BettingEngine.apply | Unknown player requests postflop FOLD | IllegalArgumentException; state unchanged | Implemented, GREEN; `anUnknownPlayerCannotAct` |
| BE-PC01-CHECK | BettingEngine.apply; BettingResult.getState; getAcceptedAction | FLOP final preflop aggressor p1 can open; requests CHECK | Opening c-bet opportunity true, facing false; no preflop raise/three-bet context; history retained | Implemented, GREEN; `anEligibleAggressorGetsExplicitOpeningOpportunityContext(CHECK)` |
| BE-PC01-FOLD | BettingEngine.apply; BettingResult.getState; getAcceptedAction | FLOP final preflop aggressor p1 can open; requests FOLD | Opening c-bet opportunity true, facing false; no preflop raise/three-bet context; history retained | Implemented, GREEN; `anEligibleAggressorGetsExplicitOpeningOpportunityContext(FOLD)` |
| BE-PC02 | BettingEngine.apply; BettingResult.getAcceptedAction | FLOP actor p1; preflop aggressor p0; no bet yet | No opening/faced c-bet opportunity for p1 | Implemented, GREEN; `anotherPlayerIsNotTheContinuationBettor` |
| BE-PC03 | BettingEngine.apply; BettingResult.getAcceptedAction | FLOP aggressor p1 has no betting/raising rights | Accepted CHECK has no opening opportunity | Implemented, GREEN; `anAggressorWithoutBettingRightsCannotOpen` |
| BE-PC04 | BettingEngine.apply; BettingResult.getState; getAcceptedAction | FLOP actor p1 faces explicitly recorded c-bet 20 from p0; FOLD | Facing opportunity true; opening false; accepted FLOP/FOLD; history retained through apply | Implemented, GREEN; `foldFacingAnActualCBetCarriesTheOpportunityAndRetainsHistory` |
| BE-PC05 | BettingEngine.apply; BettingResult.getAcceptedAction | FLOP p0 opening bet 20, preflop aggressor p5, no recorded c-bet; p1 FOLD | No c-bet opportunity; current bet size alone is insufficient | Implemented, GREEN; `anUnrecordedOpeningBetCreatesNoFacedCBetContext` |
| BE-PC06 | BettingEngine.apply; BettingResult.getAcceptedAction | Recorded p0 c-bet 20, then p2 raises to 60; p1 FOLD | No fold-to-original-c-bet opportunity when facing the subsequent raise | Implemented, GREEN; `anInterveningRaiseIsNotTheOriginalContinuationBet` |
| BE-PC07 | BettingEngine.apply; BettingResult.getAcceptedAction | Preflop aggressor p1 faces another opening bettor p0; FOLD | No opportunity to open with a c-bet; no actual c-bet faced | Implemented, GREEN; `anAggressorFacingAnotherOpeningBetCannotCBet` |
| BE-PC08-TURN | BettingEngine.apply; BettingResult.getAcceptedAction | TURN bet 20 and recorded past flop c-bet 20; p1 FOLD | No flop opportunity on later streets despite equal amounts | Implemented, GREEN; `aPastCBetIsNotACurrentTurnOrRiverCBet(TURN)` |
| BE-PC08-RIVER | BettingEngine.apply; BettingResult.getAcceptedAction | RIVER bet 20 and recorded past flop c-bet 20; p1 FOLD | No flop opportunity on later streets despite equal amounts | Implemented, GREEN; `aPastCBetIsNotACurrentTurnOrRiverCBet(RIVER)` |
| BE-PF11-FLOP-CALL | BettingEngine.apply | FLOP unopened round; request CALL | IllegalArgumentException; state/history retained | Passing; `invalidPostflopActionsAreRejectedWithoutChanges(FLOP,CALL)` |
| BE-PF11-FLOP-BET | BettingEngine.apply | FLOP round already opened to20; request BET | IllegalArgumentException; state/history retained | Passing; `invalidPostflopActionsAreRejectedWithoutChanges(FLOP,BET)` |
| BE-PF11-FLOP-RAISE | BettingEngine.apply | FLOP unopened round; request RAISE | IllegalArgumentException; state/history retained | Passing; `invalidPostflopActionsAreRejectedWithoutChanges(FLOP,RAISE)` |
| BE-PF11-TURN-CALL | BettingEngine.apply | TURN unopened round; request CALL | IllegalArgumentException; state/history retained | Passing; `invalidPostflopActionsAreRejectedWithoutChanges(TURN,CALL)` |
| BE-PF11-TURN-BET | BettingEngine.apply | TURN round already opened to20; request BET | IllegalArgumentException; state/history retained | Passing; `invalidPostflopActionsAreRejectedWithoutChanges(TURN,BET)` |
| BE-PF11-TURN-RAISE | BettingEngine.apply | TURN unopened round; request RAISE | IllegalArgumentException; state/history retained | Passing; `invalidPostflopActionsAreRejectedWithoutChanges(TURN,RAISE)` |
| BE-PF11-RIVER-CALL | BettingEngine.apply | RIVER unopened round; request CALL | IllegalArgumentException; state/history retained | Passing; `invalidPostflopActionsAreRejectedWithoutChanges(RIVER,CALL)` |
| BE-PF11-RIVER-BET | BettingEngine.apply | RIVER round already opened to20; request BET | IllegalArgumentException; state/history retained | Passing; `invalidPostflopActionsAreRejectedWithoutChanges(RIVER,BET)` |
| BE-PF11-RIVER-RAISE | BettingEngine.apply | RIVER unopened round; request RAISE | IllegalArgumentException; state/history retained | Passing; `invalidPostflopActionsAreRejectedWithoutChanges(RIVER,RAISE)` |
| BE-PFI01 | BettingEngine.apply; advanceStreet; BettingResult getters | Real setup; preflop CALL/FOLD/CALL/CALL/CALL/CHECK; all five remaining players CHECK all postflop streets | SHOWDOWN after 21 accepted actions; exact actor order and action streets; board 5/deck 32; pot 100/stacks 5,900/total 6,000; no payout | Implemented, GREEN; `CheckThroughHandIntegrationTest.setupAndPlayerActionsReachShowdownWithConservedChips(false)` |
| BE-PFI02 | BettingEngine.apply; advanceStreet; BettingResult getters | Same setup; seat 2 folds on flop and remaining players check through | SHOWDOWN after 19 accepted actions; folded seat 2 retains 20 and never acts later; same pot/cards/total; input snapshots unchanged | Implemented, GREEN; `CheckThroughHandIntegrationTest.setupAndPlayerActionsReachShowdownWithConservedChips(true)` |

Tests: [PostflopActionsTest](../../src/test/java/domain/model/PostflopActionsTest.java)
and [CheckThroughHandIntegrationTest](../../src/test/java/integration/CheckThroughHandIntegrationTest.java).
BVA cases are written before production behavior. RED/GREEN logs and ordered
patches are retained without automatic commits; actual Git-history evidence
requires the team's later commits. Coverage and mutation adequacy remain
separate measured requirements.
