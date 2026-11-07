# Completed statistics and continuation BVA

Settlement is explicit: comparison still returns AWAITING_SETTLEMENT until
`SetupSession.settleHand()` succeeds. The application first prepares pot payouts,
then copies completed PlayStats collectors, replays this hand's accepted log and
completes each copy with verified participation/payout facts. Only then are the
paid state and all totals published. Pending trackers validate live delivery;
completed totals consume the accepted ledger once per paid hand.

| ID | Methods | State/input | Expected output | Implemented test |
| --- | --- | --- | --- | --- |
| CS01 | settleHand; getStatistics; nextHand; getSettlement; start | Before start, unfinished, result, paid, second hand, new match | Reject early/duplicate settlement and early continuation; six stable IDs, carried stacks, new hand ID, cumulative1→2 hands, immutable old snapshots, new match resets | Yes; CompletedStatisticsIntegrationTest.settlementCompletesOnceAndNextHandKeepsIdentityChipsAndTotals |
| CS02 | submitAction; settleHand; statistics snapshots | Real six-player call/check showdown; shared-board tie | Pending totals0; paid hands1, callersVPIP100/BB0, WTSD100/W$SD100 from true tied payouts, total6000 | Yes; CompletedStatisticsIntegrationTest.realShowdownCountsCallsSeeingFlopAndTiedPayouts |
| CS03 | settleHand; statistics snapshots | One preflop fold, later flop folds and uncontested finish | Preflop folder WTSD unavailable, postflop folder WTSD0; nobody has a showdown payout | Yes; CompletedStatisticsIntegrationTest.preflopAndPostflopFoldersHaveDifferentFlopParticipation |
| CS04 | PlayStats.copy; recordAction; completeHand | Completed ID plus pending call copied, each branch continues independently | Preserve totals/pending actions/completed-ID protection without aliasing mutable totals/collections | Yes; PlayStatsCopyTest.copyKeepsTotalsPendingActionsAndCompletedIdsIndependent |
| NH01 | NextHand.start | Paid six-seat hand | Move dealer0→1, blinds2/3, actor4, preserve IDs/stacks, fresh two-card hands and40-card deck | Yes; NextHandTest.rotatesDealerAndDealsOnlySurvivingFixedSeatsWithCarriedStacks |
| NH02 | NextHand.start; InitialDeal.deal | Heads-up; dealer3 has5, BB0 has5995 | Cap SB5 all-in, BB20; no unnecessary decision;48 remaining cards; four eliminated seats retain IDs and get no cards | Yes; NextHandTest.headsUpShortSmallBlindRunsOutWithoutInventingAnAction |
| NH03 | NextHand.start | Heads-up; SB3 has5985, BB0 has15 | BB all-in15; lone active SB must respond to5 owed | Yes; NextHandTest.loneActiveSmallBlindStillRespondsToShortAllInBigBlind |
| NH04 | NextHand.start | Unfinished state or one winner holding6000 | Reject continuation without replacing players | Yes; NextHandTest.refusesUnfinishedHandAndFinishedMatch |

TDD: all four initial completed-statistics/copy cases failed at the missing
boundary; all four continuation cases also failed before implementation.
StatisticsView contains completed numeric values only, with unavailable rates
represented by OptionalDouble, and no cards. This is in-memory match tracking;
closing the game discards it. Unsupported BET/RAISE never creates a statistic.
