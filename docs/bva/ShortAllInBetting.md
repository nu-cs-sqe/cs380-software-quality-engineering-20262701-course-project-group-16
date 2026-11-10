# K02 short all-ins and reopening BVA

An all-in wager can be below the full minimum only when the target uses every
remaining chip and the actor has raising rights. Pending responses and raise
eligibility are separate. Preserve the last full increment after a short wager;
compare cumulative increases against each player's own last decision level.
This follows [Poker TDA rules 45/49](https://www.pokertda.com/view-poker-tda-rules/).
Checked players must face a full cumulative opening amount before raising;
players who have not acted retain rights. A response CALL stays CALL when all-in.

| ID | Input / boundary | Expected behavior | Test / status |
| --- | --- | --- | --- |
| K02-O | Opening stack 1/19/20/21, BB20, commit entire stack | Accept BET/all-in; minimum full increment20/20/20/21 | openingAllInsPreserveTheFullMinimum; passing |
| K02-R | Open100; next all-in target101/199/200/201 | Accept RAISE; retain full increment100 for short raises, update to101 at201; original bettor reopens only from200 | fullAndShortRaisesUpdateDifferentRoundFacts; passing |
| K02-C | A bet100, B all-in125, C call125, D all-in199/200/201 | A reopens at200, C stays closed; everyone owing more still responds | cumulativeReopeningUsesEachPlayersOwnLastDecision; passing |
| K02-U | First actor all-in5; unacted player raises24/25 | Reject24; accept25 with full increment20, not completion to20 | aPlayerWhoHasNotActedMayRaiseAShortOpeningByTheFullMinimum; passing |
| K02-CH | A check, B all-in5, C all-in19/20/21 | A reopens only from20; increment stays20 | checkedPlayersReopenOnlyAfterFacingAFullCumulativeBet; passing |
| K02-D | A bet100, B call100, C all-in125, D call125, E all-in200 | A/B reopens; D still closed at125; calling200 resets only the caller | callingAnIntermediateShortRaiseResetsOnlyThatPlayersReopeningLevel; passing |
| K02-I | Opening19 or raised199 when chips remain | Reject unchanged; no partial voluntary undersized wager | anUndersizedWagerWithChipsBehindIsRejectedWithoutChanges; baseline regression |
| K02-ST | Preflop raise40 followed by all-in50 | Accepted RAISE/THREE_BET context, increment20, opener not reopened | shortPreflopRaisesStillProduceUsableAcceptedStatistics; passing |

Tests exercise real immutable states and engine operations. Expectations use
literal chip totals and independently stated action sequences. Existing K01
full wagers, forced blinds, short/exact CALLs and round/street tests remain
regression protection. Advanced statistics coverage is K03, UI numeric limits
are K05, and broader state measurements remain K06.

## Application and immutable-state checks

Two real-session integration tests arrange conserved unequal starting stacks,
then use real setup/dealing, engine, evaluator, payout and statistics components.
Raise to40 → all-in50 → call50 → all-in60 sequence reopens the40 bettor; a full raise80
leads to pot 300 and verified side-pot/tie payouts. A second hand uses short
raises21/22 and capped CALLs, reaches pot 121 and runs out without extra decisions.
The tests use controlled Random sources and no mock library.

ReopeningProgressTest checks immutable copied maps, decision levels below/at/above
0..currentBet, absent IDs/values, rejection of unknown/inactive players, unchanged
input snapshots and clearing on street advancement. These are additional state
regressions, not claimed as separate feature RED cycles.

TDD first observed 13 failures among 18 engine cases (five already supported),
plus both application cases failing at unsupported short wagers. They all passed
after implementation. Full-suite/code-review follow-up found that a valid
unopened snapshot with lastFull40/BB20 retained40 after BET20/21. One additional
short-opening case failed (expected20, actual40), then passed when opening
classification used BB and retained that floor after an undersized opening.

Mutation review added three regressions: an exact-stack match is CALL rather than
RAISE, a minimum full raise reopens legacy snapshots without stored decision
levels, and an opening bet uses the BB minimum even when chips remain behind.
Real-session and street-completion loops have finite action limits so broken
progression fails an assertion instead of hanging the test.
