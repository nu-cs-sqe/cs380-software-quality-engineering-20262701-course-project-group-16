# BettingHistory BVA

Immutable engine provenance, separate from action requests and PlayStats. A null
aggressor means no preflop aggression. A zero amount means no actual flop c-bet;
positive amounts record its original round target. The c-bettor is the final
preflop aggressor, so a positive amount requires that identity. This is an
explicit event marker, not a classification inferred from the current bet size.
GameState validates its identity, phase and backing commitment against the hand.

Both constructors and both public getters are covered below. Rates and display
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
