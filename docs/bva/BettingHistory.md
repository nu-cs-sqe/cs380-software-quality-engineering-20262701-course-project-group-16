# BettingHistory BVA

Immutable engine provenance, separate from action requests and PlayStats. A null
aggressor means no preflop aggression. A zero amount means no actual flop c-bet;
positive amounts record its original round target. The c-bettor is the final
preflop aggressor, so a positive amount requires that identity. This is an
explicit event marker, not a classification inferred from the current bet size.
GameState validates its identity, phase and backing commitment against the hand.

The constructors and public getters are covered below. Rates and display
strings are not stored here. Whitespace suffixes describe exact input values.

| ID | Public methods | Input/system state | Expected output | Status / JUnit method |
| --- | --- | --- | --- | --- |
| BH-01 | BettingHistory; getFinalPreflopAggressorId; getFlopCBetAmount | Default constructor | Both optionals empty | Implemented, GREEN; `defaultHistoryIsEmpty` |
| BH-02-0 | BettingHistory; getFinalPreflopAggressorId; getFlopCBetAmount | Aggressor p1; recorded c-bet amount 0 | Aggressor retained; amount unavailable | Implemented, GREEN; `explicitHistoryRetainsIdentityAndOptionalAmount(0)` |
| BH-02-1 | BettingHistory; getFinalPreflopAggressorId; getFlopCBetAmount | Aggressor p1; recorded c-bet amount 1 | Aggressor retained; amount retained exactly | Implemented, GREEN; `explicitHistoryRetainsIdentityAndOptionalAmount(1)` |
| BH-02-20 | BettingHistory; getFinalPreflopAggressorId; getFlopCBetAmount | Aggressor p1; recorded c-bet amount 20 | Aggressor retained; amount retained exactly | Implemented, GREEN; `explicitHistoryRetainsIdentityAndOptionalAmount(20)` |
| BH-02-9223372036854775807 | BettingHistory; getFinalPreflopAggressorId; getFlopCBetAmount | Aggressor p1; recorded c-bet amount 9223372036854775807 | Aggressor retained; amount retained exactly | Implemented, GREEN; `explicitHistoryRetainsIdentityAndOptionalAmount(9223372036854775807)` |
| BH-03 | BettingHistory; getFinalPreflopAggressorId; getFlopCBetAmount | Explicit null aggressor, amount 0 | Both optionals empty | Implemented, GREEN; `explicitNoAggressorIsAllowedWithoutACBet` |
| BH-04-empty | BettingHistory | Aggressor empty string, amount 0 | IllegalArgumentException | Implemented, GREEN; `blankAggressorsAreRejected(empty)` |
| BH-04-space | BettingHistory | Aggressor one space, amount 0 | IllegalArgumentException | Implemented, GREEN; `blankAggressorsAreRejected(space)` |
| BH-04-tab | BettingHistory | Aggressor tab, amount 0 | IllegalArgumentException | Implemented, GREEN; `blankAggressorsAreRejected(tab)` |
| BH-05-minus-one | BettingHistory | Aggressor p1; amount -1 | IllegalArgumentException | Implemented, GREEN; `negativeAmountsAreRejected(-1)` |
| BH-05-minimum-long | BettingHistory | Aggressor p1; amount -9223372036854775808 | IllegalArgumentException | Implemented, GREEN; `negativeAmountsAreRejected(-9223372036854775808)` |
| BH-06-1 | BettingHistory | No aggressor; c-bet amount 1 | IllegalArgumentException; a c-bet needs the final preflop aggressor | Implemented, GREEN; `aCBetCannotExistWithoutItsAggressor(1)` |
| BH-06-20 | BettingHistory | No aggressor; c-bet amount 20 | IllegalArgumentException; a c-bet needs the final preflop aggressor | Implemented, GREEN; `aCBetCannotExistWithoutItsAggressor(20)` |

Tests: [BettingHistoryTest](../../src/test/java/domain/model/BettingHistoryTest.java).

## K03: exact raise counts and all-in observations

| ID | Public methods | Input/system state | Expected output | Status / JUnit method |
| --- | --- | --- | --- | --- |
| BH-07-one | BettingHistory; getPreflopRaiseCount; BettingEngine.apply | Prior count 1; current wager 60, legal raise to 100 | THREE_BET with an opportunity; prior snapshot unchanged, count 2 and new aggressor in result | Implemented; `exactSnapshotHistoryClassifiesTheNextRaiseAtTheSamePrice` |
| BH-07-two | Same | Prior count 2; same wager and target | FOUR_BET without a three-bet opportunity; count 3 | Implemented; same test |
| BH-07-three | Same | Prior count 3; same wager and target | LATER_RAISE without a three-bet opportunity; count 4 | Implemented; same test |
| BH-08-id-negative | BettingHistory | Aggressor p0, count -1 | IllegalArgumentException | Implemented; `anExactRaiseCountRequiresConsistentAggressorHistory` |
| BH-08-id-zero | BettingHistory | Aggressor p0, count 0 | IllegalArgumentException | Implemented; same test |
| BH-08-absent-positive | BettingHistory | Absent aggressor, count 1 | IllegalArgumentException | Implemented; same test |
| BH-08-absent-negative | BettingHistory | Absent aggressor, count -1 | IllegalArgumentException | Implemented; same test |
| BH-09 | BettingHistory; getPreflopRaiseCount | Empty exact history and legacy one-raise history | Counts 0 and 1 respectively | Implemented; `emptyExactHistoryAndLegacyHistoryRetainTheirMeaning` |
| BH-10 | SetupSession.submitAction; settleHand; getStatistics | Final preflop aggressor c-bets its last 5 chips; a short CALL adds 4, then a short RAISE targets 9 | Original bettor records 100% c-bet; direct caller and raiser record 0% fold-to-c-bet; players responding to the raise have no fold-to-c-bet denominator | Implemented; `shortContinuationBetCountsOnlyDirectResponsesAtSettlement` |
| BH-11 | Same | Two short all-in raises cumulatively reopen the first raiser, who raises again then checks the flop | Short three-bettor records 100% three-bet; later raisers and callers do not gain a three-bet opportunity from reopening; final aggressor records a declined c-bet | Implemented; extended `cumulativeReopeningRunsThroughTheRealSessionAndSettlement` |
| BH-12 | GameState constructor | Known aggressor, count 5,981 or Long.MAX_VALUE; 6,000 chips, BB 20 | Reject before a subsequent action can overflow the count | Implemented; `snapshotRejectsMoreRaisesThanAvailableChipsAllow` |

The exact-constructor tests first failed to compile because it was private.
After exposing the signature, all four inconsistent count cases failed until
validation was added. Both impossible-count cases also failed before adding the
GameState bound. The live-play integration cases verify existing behavior
through settlement and were already green in that intermediate run.
