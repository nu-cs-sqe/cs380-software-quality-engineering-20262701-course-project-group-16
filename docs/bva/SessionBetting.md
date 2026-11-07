# SetupSession betting/progression BVA

Extends [SetupSession.md](SetupSession.md). One session owns one authoritative
GameState. It accepts requests through the real BettingEngine, advances closed
rounds, and uses the real ShowdownResolver. Results await settlement; chips are
not awarded. Display snapshots have no internal deck or other players' cards.
`SetupView.getCurrentActorSeat()` retains its int API and uses -1 for no actor.

BVA was written before implementation. The first RED run executed 29 cases with
28 expected failures and the existing null-initializer rejection passing. The
GREEN run passed those cases plus the 20 original session/privacy cases. Later
fault/retry and mutation-driven checks extend the suite; they are not claimed as
separate failing-before-implementation cycles.

| ID | Method(s) | System state and input | Expected output | Test / implementation status |
| --- | --- | --- | --- | --- |
| SS-B01-0/1/2 | SetupSession(initializer, engine, resolver) | Each collaborator null in turn | IllegalArgumentException | rejectsNullCollaborators; passing |
| SS-B02 | getAvailableActions; getAcceptedActions; getResult; playBotTurns | No match | Empty query results; bot execution rejects with IllegalStateException | emptySessionHasNoActionsHistoryOrResult; passing |
| SS-B03-FOLD/CHECK/CALL | submitAction | No match; each action | IllegalStateException, no match created | cannotActWithoutAMatch; passing |
| SS-B04 | submitAction; getAcceptedActions; getView | Initial seat 3 calls20 | Deduct20 once; pot 50; next actor/viewer 4; cards hidden; one accepted observation; old snapshot unchanged | callUpdatesOneStateAndAppendsExactlyOneObservation; passing |
| SS-B05-null/unknown/outOfTurn/checkOwing/bet/raise | submitAction | Null request, missing player, wrong actor, checking while owing, unsupported bet/raise | Appropriate rejection, same hand/contributions/actor/history/result; conceal private cards | rejectionDoesNotChangeStateOrHistory; passing |
| SS-B06 | getAvailableActions; getAcceptedActions | Initial decision and big blind's final preflop option | FOLD/CALL vs FOLD/CHECK; immutable set; no committed action from queries | availableActionsUseEngineValidationWithoutCommittingActions; passing |
| SS-B07 | submitAction; getView | Five calls followed by BB CHECK; pending count1→0 | Retain BB option until CHECK; burn/deal flop; actor 1; round contributions0; hand contributions20; pot 120 | lastPreflopDecisionAdvancesToFlopAndPreservesCommitments; passing |
| SS-B08 | submitAction; getResult; getAvailableActions; playBotTurns | Five folds leave one participant | Uncontested BB result awaiting settlement; no drawing/payout; no actor/actions | lastFoldResolvesUncontestedWithoutDrawingOrPaying; passing |
| SS-B09 | submitAction; getResult; getAcceptedActions | Six20-chip stacks; BB forced all-in; other five call all-in | Automatic runout to five board cards; exactly five CALL observations; pot 120/stacks 0; no fake CHECKs | allInCallsRunTheBoardWithoutInventingCheckActions; passing |
| SS-B10 | submitAction; getAvailableActions | Only SB can act against all-in BB, still owing10 | Must respond; CALL leads to contested showdown, not an uncontested win | loneActivePlayerStillMustRespondToAnAllInOpponent; passing |
| SS-B11-FOLD/CHECK/CALL | submitAction; getResult; getAcceptedActions | Result already produced; another action | IllegalStateException; same result/history; immutable log | completedComparisonRejectsMoreActions; passing |
| SS-B12-1/2/6 | playBotTurns; getAcceptedActions; getView | 1/2/6 humans | Bots check/call only; stop at human 0/0/3; hidden cards; immutable observations returned once; repeat call at human does nothing | botsStopAtTheNextHumanAndReturnOnlyCommittedActions; passing |
| SS-B13 | playBotTurns; getResult | Sole human folds; remaining actors are bots | Bots finish supported streets and comparison; pot 100; total6000 | botsFinishWhenTheOnlyHumanFolds; passing |
| SS-B14 | start; getResult; getAcceptedActions | Invalid restart after result, then valid restart | Invalid preserves result/log while hiding cards; successful restart clears both and creates new hand | failedRestartPreservesResultAndHistoryButSuccessfulRestartClearsThem; passing |
| SS-B15 | submitAction; getResult; getAcceptedActions | Injected comparison failure on the final river CHECK, then retry using real evaluator | Same RIVER decision/state/log on failure; retry commits the 24th action once and yields a result | failedComparisonPreservesTheFinalDecisionAndRetryCommitsOnlyOnce; passing |
| SS-B16 | submitAction; getAvailableActions | Big blind owes zero after five calls; CALL then CHECK | Zero-cost CALL rejects without closing decision or appending; CHECK advances to flop | zeroCostCallCannotCloseTheBigBlindsOption; passing |
| SS-B17 | revealCards; playBotTurns; getView | Six humans; viewer 0 revealed while actor 3 waits; zero bot actions | Switch to actor 3 and hide previous cards; old snapshot unchanged; no accepted event | botPumpHidesARevealedHandWhenSelectingAHumanWithoutPlayingAnAction; passing |
| SS-INT-false/true | start; submitAction; getView; getResult | Real setup with controlled shuffle; prescribed call/fold/check then check-through | Shared-flush tie or sole seat 5 straight; five live players;21 actions; pot 100/stacks 5900;32 deck cards; awaiting settlement | PlayedHandIntegrationTest.actionsAndStreetProgressionReachTheRealEvaluator; passing |

The accepted-action log is private and exposed as immutable snapshots. It is an
event source for pending statistics delivery, not completed-hand aggregation.
Legal-action queries use the pure engine on discarded snapshots; only successful
session submissions enter the log. Rejected actions hide the UI hand but do not
change the authoritative game state. Completed-hand/payout statistics remain
pending until settlement is implemented.

[Statistics integration](HandStatistics.md) adds 16 session cases and 12 adapter
cases: accepted actions reach pending PlayStats once after successful comparison;
queries/rejections do not record, and failed comparison/recording can be retried.

SessionBettingTest has 30 cases and PlayedHandIntegrationTest has 2. The fault
case injects one failing evaluator call solely to check transaction recovery;
the played winner/tie integration cases use the real evaluator throughout.
Mutation review added an explicit20-chip preflop view assertion and the
zero-action human-handoff privacy case. No completed-hand statistics are
claimed. The session has an actor whenever a hand has no result; it advances
closed rounds before publishing a state, so no extra actor-empty query guard
is needed.
