# Played Swing flow: BVA and integration acceptance cases

GamePanel is a view under the rubric exemption. These headless EDT cases still
verify the real session/engine/evaluator, both resource bundles, action enablement
and private-card handoffs. The initial10-case RED run failed all 10 before the controls were implemented.
All10 passed after GREEN. Four further assertions first failed for missing
folded/all-in labels, then passed after the visible status labels were added.

| ID | Method/flow | System state and input | Expected result | Test/status |
| --- | --- | --- | --- | --- |
| UI-PLAY00 | GamePanel constructor | No match | All action buttons disabled | noActionControlsAreEnabledBeforeStart; passing |
| UI-PLAY01-0/1 | Start, call, refresh | English/Chinese;2 humans; dealer 0 | Three bots call; human 0 acts at pot 90; CALL20 then human 1 CALL10; BB checks automatically; flop3, pot 120; hidden cards at handoff; labels selected locale | humanActionsDriveBotsAndTheFlopWithoutExposingCards; passing |
| UI-PLAY02-0/1 | Full supported hand, result, language switch, restart | Six humans; prescribed sequence then15 checks; shared flush |21 real actions, pot 100/stacks 5900, five-way tie awaiting settlement; folded seat visibly marked; controls disabled at result; translation updates without replay; successful restart clears result/log | controlledHandShowsRealTieAndAwaitingSettlement; passing |
| UI-PLAY03-0/1 | Fold and result | Five consecutive folds in both languages | BB uncontested; no board drawn; pot 30 unawarded; translated winner/result | foldsShowUncontestedResultWithoutPaying; passing |
| UI-PLAY04-0/1 | Call and runout | One human;20 starting chips,10/20 blinds | Bots and human contribute exactly five calls; automatic board runout; pot 120; all-in status visible; awaiting settlement in selected language | oneHumanAndBotsCanReachAnAllInComparison; passing |
| UI-PLAY05 | Viewer selection and controls | Six humans, current actor 3; viewer switches3→0→3 | Controls disabled for another viewer; re-enabled for actor; cards stay hidden | selectingAnotherViewerDisablesActionsUntilTheActorIsSelected; passing |

The existing GamePanelTest setup expectations will be updated from pot 30/first
bot to pot 90/current human because the played screen now executes bot turns
automatically. Its privacy, restart, invalid-input and locale assertions remain.
Offscreen checks are not a substitute for the team's native desktop walkthrough.

## Call-button visibility regression

| ID | Flow/input | Expected result | Status / test |
| --- | --- | --- | --- |
| UI-PLAY06-0/1 | Started two-human game; English/Chinese; 1,100×740 panel; 16-point action fonts | Fold/Check/Call buttons and labels fit inside their row; Call 20 remains visible and enabled | Passing: callButtonStaysVisibleInsideTheActionRow |

The existing FlowLayout can wrap the third button below the fixed-height row.
This regression tests actual component bounds and text fit after layout.

Local TDD: two visibility cases ran before the layout change; the English case
failed because Call wrapped below the row, while Chinese already fit. After
using one non-wrapping row with compact button margins and setting labels before
measuring its height, all 12 played-screen cases pass. No new commit was made.
