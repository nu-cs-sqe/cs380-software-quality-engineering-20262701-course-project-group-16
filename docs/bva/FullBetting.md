# K01 full betting BVA

Requests carry round targets. The engine owns chip deductions, minimum full
amounts, stack limits, pending decisions and immutable rejection. The original K01 checkpoint covered full
bets/raises and passive responses. [K02](ShortAllInBetting.md) now extends this to
short all-ins and cumulative reopening; shared legal-limit presentation remains K05.

| ID | Methods | System state / inputs | Expected output | Test / status |
| --- | --- | --- | --- | --- |
| K01-B | BettingEngine.apply | FLOP/TURN/RIVER, unopened, stack960; targets19/20/21/960/961 | Reject below20/above960; legal target moves that many chips, sets full increment and all-in when exact stack | FullBettingTest.openingBetsRespectMinimumAndStackBoundaries; passing; 15 cases |
| K01-R | apply | Every street, current60, last full increment40, own10; targets60/61/99/100/101 | Reject below100; deduct target minus10, update increment to target minus60 | raisesUseRoundTargetsAndTheLastFullIncrement; passing; 20 cases |
| K01-CF | apply | Every street, raised target60, own10 | CALL adds50; FOLD spends0; CHECK rejects | callsAndFoldsWorkFacingRaisedRounds; passing; 4 cases |
| K01-P | apply | Player checks, another bets20, next raises40 | Restore all other ACTIVE decisions/raise rights; clockwise actor; conserve6000 | aFullBetReopensDecisionsForPlayersWhoAlreadyChecked; passing |
| K01-H | apply; history/context getters | Four accepted preflop raises | Opening/three/four/later classifications and latest actual aggressor preserved | raiseClassificationAndLatestAggressorFollowAcceptedPreflopRaises; passing |
| K01-I | apply | Wrong BET/RAISE kind, no rights, no active opponent | IllegalArgumentException; input remains usable | wrongWagerKindsRightsAndAbsentOpponentsRejectWithoutChangingState; passing |
| K01-M | apply | Raise current60/own10 to960 vs961, stack950 | Add950/all-in vs rejection | raiseToMaximumUsesOnlyTheRemainingStackAndRejectsOneMore; passing |
| K01-A | apply | Owes50, only20 left; request CALL vs target30 RAISE | Accepted all-in CALL20; RAISE below the current target rejected | aShortCallIsStillACallAndARaiseMustExceedTheCurrentTarget; passing |
| K01-S | apply | Open60, raise120, next targets179/180 | Full increment remains60; next minimum180 | anOpeningBetChangesTheFollowingMinimumRaise; passing |

Expected chip values are literal or hand-derived; assertions do not use engine
helpers to calculate their expected answers. Existing fold/check, short-blind,
privacy, settlement and statistics cases remain regression protection.

Two real-component session tests play raised hands through all streets, comparison,
settlement and completed statistics. One hand reaches pot 1,480 with four tied
winners receiving 370 each; another checks actual three-bet/c-bet observations.
They use a controlled Random source, real engine/evaluator/payouts and no mock
library. Existing isolated coordinator tests retain their injected stubs.

TDD: all 45 boundary cases and both integration cases failed before implementation,
then passed. Additional mutation-driven regressions are recorded with the quality
results. Reference: [Poker TDA minimum-raise rules](https://www.pokertda.com/view-poker-tda-rules/).

Additional regression boundaries cover affordable three-bet targets 60/99/100/101,
opening stacks 19/20/21 against the big blind 20, rejection before dealing,
matched-player/aggressor c-bet context, the nominal short-blind target with two
ACTIVE players versus one, and independently open rights when a round closes.
These tests address reported coverage/mutation gaps; they are not claimed as
additional feature RED cycles.
