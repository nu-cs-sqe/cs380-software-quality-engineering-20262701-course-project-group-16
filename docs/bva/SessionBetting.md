# SetupSession betting/progression BVA

K01 full-wager application cases are in [FullBetting.md](FullBetting.md).
The original six rejection cases now use illegal amounts/action kinds, rather
than expecting all BET/RAISE requests to be unsupported.

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
| SS-B02 | getAvailableActions; getAcceptedActions; getResult; playBotTurns | No match | Empty query results; bot execution rejects with IllegalStateException | emptySessionHasNoActionsHistoryOrResult; passing |
| SS-B03-FOLD/CHECK/CALL | submitAction | No match; each action | IllegalStateException, no match created | cannotActWithoutAMatch; passing |
| SS-B04 | submitAction; getAcceptedActions; getView | Initial seat 3 calls20 | Deduct20 once; pot 50; next actor/viewer 4; cards hidden; one accepted observation; old snapshot unchanged | callUpdatesOneStateAndAppendsExactlyOneObservation; passing |
| SS-B05-unknown/outOfTurn/checkOwing/bet/raise | submitAction | Missing player, wrong actor, checking while owing, preflop BET or below-minimum RAISE to39 | Appropriate rejection, same hand/contributions/actor/history/result; conceal private cards | rejectionDoesNotChangeStateOrHistory; passing |
| SS-B06 | getAvailableActions; getAcceptedActions | Initial decision and big blind's final preflop option | FOLD/CALL/RAISE vs FOLD/CHECK/RAISE; immutable set; no committed action from queries | availableActionsUseEngineValidationWithoutCommittingActions; passing |
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
Legal-action queries read the current engine state without applying actions.
Only successful session submissions enter the log. Rejected actions hide the UI hand but do not
change the authoritative game state. Comparison leaves totals pending; explicit settleHand now pays pots and
aggregates completed statistics. See [completion cases](CompletedStatistics.md).

[Statistics integration](HandStatistics.md) adds 16 session cases and 12 adapter
cases: accepted actions reach pending PlayStats once after successful comparison;
queries/rejections do not record, and failed comparison/recording can be retried.

PlayedHandIntegrationTest has two cases. The fault
case injects one failing evaluator call solely to check transaction recovery;
the played winner/tie integration cases use the real evaluator throughout.
Session privacy cases cover a zero-action handoff and an ordinary turn wrapping
from seat 5 to seat 0 with the previous hand revealed. The next viewer's cards
stay hidden until that player reveals them. The session advances closed rounds
before publishing a new state.

## River Room seat action badges

| Boundary | Expected display |
| --- | --- |
| Hand starts with posted blinds and zero accepted decisions | Seats show their blind roles and round contributions. The current seat shows “To act”. Other active seats show “In play”. |
| A Flop bet is followed by another player's call | The bettor keeps “Bet” and the caller shows “Call”. If the bettor later calls a raise, their badge changes to “Call”. |
| The final preflop decision opens the Flop | The new street begins with an empty action map. The separate last-action strip can still describe the decision that closed preflop. |
| A call uses the player's remaining chips | The seat shows “All-in”. The last-action strip retains the call and its all-in qualifier. |

`DesktopBridgeIntegrationTest` exercises the public snapshot through the real Java session.
`desktop/test/table-state.test.mjs` checks badge precedence. The six-player showdown in
`desktop/e2e/room.test.mjs` checks the displayed actions, card colors, current street and
private controls in Electron.

## River Room showdown and replay

| Boundary | Expected outcome |
| --- | --- |
| Ordinary fold with no choice, deferred show, or immediate home-game show | Fold stays in the betting log. No cards appear publicly until the selected disclosure point. The folder receives zero from every pot. |
| `foldAndShow` before and after review starts | The unreviewed session rejects the request before accepting a fold. The reviewed session returns the logged fold and applies the configured disclosure timing. |
| Folded owner, a different seat, or a wrong password | The authenticated owner can inspect both cards. A rejected request and a private inspection add no public faces. |
| River checked through, bet once, or raised again | Show in clockwise order from left of the button after checks, or from the last aggressor after a wager. |
| Five players muck before a hand is tabled | The sixth seat can win unseen. Settlement waits for the preceding decisions. |
| One hand tabled, a stronger hand and the other opponents muck | The tabled hand wins all 100 chips on a fixed deal. Each mucked hand receives zero. The only fold in the betting record is the actual preflop fold. |
| Start review after the first accepted action, or leave during pending showdown decisions | The request is rejected. The current actor, betting record or public history remains intact, as applicable. |
| Voluntary show while payment is prepared, after saving but before publication, and after publication | Both unpublished states reject disclosure without changing history. After publication, the show adds one public event, preserves the winner and payout, and leaves exactly one award. |
| Deferred show followed by a muck before betting ends | The pending reveal is withdrawn. The replay records the muck at the decision and keeps those cards private through showdown. |
| Last claimant keeps their hand private until after a completed river showdown | The award contains no private hand category. When the winner later shows, the journal includes both cards and the category at that event. |
| All-in with a call or fold outstanding, then final decision | Live hands remain private while betting is pending. Expose them before the automatic board runout. |
| An all-in wins by folds, or a called river all-in already has five board cards | The uncontested winner stays hidden and receives the pot and uncalled refund. The contested river tables both hands and awards the pot without presenting a new runout. |
| A bot faces a tabled hand that beats, ties, or loses to the bot on a fixed board | The bot mucks the weaker hand and tables a tie or winner. The final pot pays 200 to one claimant or 100 each to the tied claimants. |
| A folded ace pair outranks both live kings, or a folded bot tries the human disclosure API | The live kings split the pot. The private API rejects the bot and leaves its cards hidden. |
| Optional bot disclosure enabled with a folded human and an unshown bot winner | The bot voluntarily shows. The human stays hidden and receives zero. |
| A bot folds under immediate and voluntary bot disclosure | The journal records the fold, the bot's revealed cards, then showdown in that order. |
| Review opens on an already-run-out all-in, or attempts to open after that hand has paid | The unpaid review publishes the public board and live hands. A paid hand rejects review even if it had zero accepted actions. |
| A paid winner voluntarily shows with exactly five known cards | The history records the category at the reveal. |
| Three-way board tie with one short stack | The main pot splits three ways and the side pot splits two ways. The journal retains the shares of each. |
| A showdown snapshot contains a side pot and its only eligible seat withdraws | Settlement rejects the empty layer and preserves the showdown state and the committed chips. |
| Two tied live hands above several different folded contributions | Award odd chips once per distinct pot. The 406-chip pot pays 203 each. |
| New deal, first call, later streets and showdown | The opening event captures the dealer and opening stacks. Blinds and the call carry their exact amounts. Each board prefix is recorded once and the opening snapshot remains stable. |
| Before and after a voluntary show, award, or next deal | Rewinding hides later cards and restores stacks and pot. The next hand retains the completed public history. |
| Showdown advances at the bridge while the old turn is still on screen | The stale handoff refreshes the turn and leaves the private-seat dialog closed. |
| SQLite refuses the automatic award, then accepts a retry | The hand remains unpaid and has no review or award. Retry pays the same hand exactly once and enables public review. |

`ReviewedHandTest`, `DesktopBridgeIntegrationTest` and the Electron suite use real
cooperating components. The renderer's `hand-history.test.mjs` checks public event
projection in isolation, including chip counts above JavaScript's safe integer limit.
`NextHandTest.fundedOpeningKeepsTheButtonAndContinuationRotatesIt` covers the
funded roster's opening and continuation positions with the same prior dealer.

## Continuous desktop table

A fixed deal checks the public deal event as each street opens. An ordinary
checked river tables every live seat automatically under the default policy.
The optional policy retains ordered show/muck. The royal-board all-in tie checks
the exact winning five for each main- and side-pot winner. A separate holding
requires a hole card to complete the straight.

The bridge tests a solo seat before expiry, at the 30-minute idle boundary,
after a rejected password and after successful reauthentication. When a different human becomes the expected actor, the bridge rejects private
reads from both seats until the named recipient accepts the handoff or the
owner signs in to switch seats. A private read and subsequent disclosure
within that seat session use the established identity. Public state, live
history and replay carry only disclosed cards.
