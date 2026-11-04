# BettingEngine BVA — first TDD increment

## Contract for review

`domain.model.BettingEngine.apply(GameState before, ActionRequest request)` returns
a `BettingResult` containing a new immutable state and one accepted engine action.
The engine does not own or update `PlayStats`; the application records an accepted
result after successful validation. Rejection throws `IllegalArgumentException`
and produces no result or input-state changes.

This increment covers the first legal preflop CALL and out-of-turn/null-input
rejection. It does not yet implement general calling, folds, checks, all-ins,
round completion, raises, street progression or Swing wiring. Those require
their own subsequent BVA and RED/GREEN increments.

## Shared state

Every case starts from the real `GameInitializer`: two humans plus four bots,
six seats, 1,000 chips per player, dealer forced to seat 0, blinds 10/20, and a
seeded shuffled deck. Phase is PREFLOP; small blind is seat 1 with 990 chips;
big blind is seat 2 with 980 chips; actor is seat 3 with 1,000 chips and zero
round/hand contributions; pot is 30 and total chips are 6,000. Each player has
two cards and the remaining deck has 40 cards.

The actor's human/bot control type does not change the engine's betting rules.
An application or bot policy supplies the request using that player's actual ID.

| ID | Public methods under test | Input/state boundary | Expected result | Implemented status / JUnit method |
| --- | --- | --- | --- | --- |
| BE-CALL01 | BettingEngine(); apply; BettingResult.getState | Seat 3 calls, owing 20 with no previous contribution | New state: stack 980, round=20, hand=20, status ACTIVE; existing cards/config/identity retained; original state unchanged | Test written, RED; `callMovesOnlyTheChipsOwedAndPreservesTheInput` |
| BE-CALL02 | BettingEngine(); apply; BettingResult.getState | Same call at the first decision | Pot 50; actor advances to seat 4; seat 3 removed from pending actors; current bet/full-raise size remain 20; total 6,000 | Test written, RED; `callAdvancesTheActorAndConservesAllChips` |
| BE-CALL03 | BettingEngine(); apply; BettingResult.getAcceptedAction | Same accepted call | Matching hand/player IDs, PREFLOP/CALL, 20 new chips, contribution after=20, not all-in; no raise/three-bet/c-bet context; accepted statistics observation | Test written, RED; `callProducesAnAcceptedObservationWithActualChipDelta` |
| BE-REJECT01 | BettingEngine(); apply | Seat 4 submits CALL while seat 3 must act | IllegalArgumentException; stacks, contributions, pot, actor, pending actors and cards remain unchanged | Test written, RED; `outOfTurnCallIsRejectedWithoutChangingTheState` |
| BE-INPUT01 | BettingEngine(); apply | Null state and otherwise valid CALL request | IllegalArgumentException; no result | Test written, RED; `nullStateIsRejected` |
| BE-INPUT02 | BettingEngine(); apply | Valid dealt state and null request | IllegalArgumentException; input state unchanged; no result | Test written, RED; `nullRequestIsRejectedWithoutChangingTheState` |

Tests are in [BettingEngineTest](../../src/test/java/domain/model/BettingEngineTest.java).
Production files currently contain API scaffolding only. RED is deliberate;
none of these behaviors is claimed implemented yet. Save this test-first
checkpoint before adding behavior so Git history preserves the TDD sequence.
