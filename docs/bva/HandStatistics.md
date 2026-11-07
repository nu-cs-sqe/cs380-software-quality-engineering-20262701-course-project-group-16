# Pending hand statistics BVA

`HandStatistics` is an application adapter. Its package-private constructor and
`record` boundary are tested with real PlayStats collectors; `start(GameState)`
creates collectors for the initialized seats. `SetupSession` uses the real
adapter through a package-private factory seam; its public constructors stay
unchanged. No mocking framework is added.

Analysis and tests preceded implementation. The first run executed 28 cases:
25 failed at the missing connection and three existing zero-rate cases passed.
The GREEN run passed all 28. Completing a collector in an adapter test supplies
model-only facts to observe recorded actions; production never completes a hand
before settlement.

## Adapter cases — HandStatisticsTest (12 invocations)

| ID | Method(s) | State / input boundary | Expected output | Implemented / test |
| --- | --- | --- | --- | --- |
| HS01 | constructor; record | Two players, one positive preflop CALL | Only that player's pending record changes; no lifetime total until model-test completion | Yes; routesAcceptedCallsToTheOwningPlayerWithoutCompletingTheHand |
| HS02 | record | Accepted THREE_BET with opportunity context | Preserve VPIP/PFR/three-bet context | Yes; preservesTheEnginesThreeBetContext |
| HS03 | record | FLOP bet/call, TURN raise, RIVER fold | Preserve street/action; model-test AF 2 and AFq 50% | Yes; preservesPostflopActionKindsForAggregation |
| HS04-CHECK/FOLD/FORCED_POST | record | Zero voluntary preflop chips (forced blind may add chips) | No VPIP/PFR; three cases | Yes; nonVoluntaryActionsDoNotCreateVpipOrPfr |
| HS05-hand/player | record | Wrong hand ID or unknown player ID | Reject before changing pending data; two cases | Yes; rejectsForeignObservationsWithoutChangingStatistics |
| HS06 | record | Second action after a fold | Retain PlayStats rejection | Yes; keepsCollectorValidationForActionsAfterFolding |
| HS07 | constructor; record | Caller clears the supplied registry afterward | Copied registry still routes to the intended collector | Yes; copiesThePlayerRegistryWhileRetainingItsCollectors |
| HS08 | start; record | All six dealt seats, including bots | Begin each collector; each rejects a later action after folding | Yes; startsCollectorsForEveryDealtPlayer |
| HS09 | start; record | SETUP state, no cards dealt | No participant accepts a voluntary action | Yes; undealtSeatsCannotRecordVoluntaryActions |

## Session integration — SessionStatisticsIntegrationTest (16 invocations)

Real initialization, betting, advancement, adapter and PlayStats run together.
Controlled Random subclasses choose legal dealer/shuffle values. A capturing
consumer observes deliveries; injected start/record/comparison failures test
recovery before mutation. These are integration tests, not isolated unit tests.

| ID | Method(s) | State / input boundary | Expected output | Implemented / test |
| --- | --- | --- | --- | --- |
| SS-S01 | package constructor | Missing statistics factory | IllegalArgumentException | Yes; rejectsAMissingStatisticsFactory |
| SS-S02 | start; queries; viewer controls; playBotTurns | Blinds, repeated queries, reveal/hide/handoff and zero bot actions | Six pending collectors, no accepted observation | Yes; blindsQueriesAndViewerChangesDoNotRecordActions |
| SS-S03 | submitAction | First human calls 20 | One identical event in collector/log, pot 50, lifetime totals unchanged | Yes; acceptedHumanActionIsDeliveredOnceWithoutAggregatingAPendingHand |
| SS-S04-null/unknown/outOfTurn/checkOwing/bet/raise | submitAction | Six invalid requests after one valid call | Reject, preserve state/log/collector; hide private cards | Yes; rejectedRequestsDoNotReachTheCollectors |
| SS-S05 | submitAction; playBotTurns | Five all-in calls, board runout and later action attempt | Exactly five CALL events; no fake CHECK/blind/result events or aggregation | Yes; allInRunoutRecordsOnlyTheFiveRealCallsAndRejectsFurtherActions |
| SS-S06 | submitAction | Five folds, uncontested result | Exactly five fold events; no completed-hand aggregation | Yes; uncontestedResultRecordsEachFoldOnceAndDoesNotCompleteStatistics |
| SS-S07 | playBotTurns | One human, three initial bot calls, repeat pump | Record each real bot once and stop at the human | Yes; botsDeliverRealActionsOnceAndStopRecordingAtTheHuman |
| SS-S08 | submitAction | Final river comparison fails, then retry | Retain 23 observations on failure; commit final action once as event 24 | Yes; comparisonFailureAndRetryDeliverTheFinalActionOnlyOnce |
| SS-S09 | submitAction | Recorder rejects before mutation, then retry | Preserve old state/log/collector; successful retry counts once | Yes; collectorFailureLeavesTheDecisionAvailableForASingleRetry |
| SS-S10 | start | Invalid count / collector startup failure / valid restart | Failed restart keeps old state and collector; success creates a fresh hand | Yes; failedRestartKeepsTheCollectorAndSuccessfulRestartCreatesAFreshOne |
| SS-S11 | start | Collector creation fails before first match | No published match/log; retry can start normally | Yes; failedStatisticsStartDoesNotPublishAnInitialMatch |

The factory receives initialized state before session publication. Recorders
must reject before changing their pending collection. Delivery follows successful
advancement/comparison and precedes publication of state/result/log. Calls run
on the Swing event thread; this is not a concurrent/distributed exactly-once
protocol. Payout-dependent aggregation and persistent profiles remain future work.
