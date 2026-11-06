# Hand evaluation and showdown

## Shared API

Use the existing `HandEvaluator` interface; `PokerHandEvaluator` is its concrete
implementation. `compare(first, second)` accepts exactly seven non-null distinct
cards per list and returns positive/zero/negative for first stronger/tied/second
stronger. Shared board cards may occur in both lists. Inputs are unchanged.
It enumerates the 21 five-card subsets, compares category first and then kickers,
and treats A-2-3-4-5 as five-high. Suits and unused cards cannot break a tie.
A royal flush is the highest straight flush, not a separate category.

Ranking follows traditional high-hand [poker hand rankings](https://www.pokerstars.es/en/poker/games/rules/hand-rankings/).
The [evaluator BVA](../bva/PokerHandEvaluator.md) records invalid inputs, all nine
categories, kicker boundaries, wheel/straight-flush edges and best-five ties.

## Result without settlement

```java
ShowdownResolver resolver = new ShowdownResolver();
ShowdownResult result = resolver.resolve(closedState);
// result.getWinnerIds(); result.getHandId(); result.isUncontested();
// result.getStatus() == ShowdownResult.Status.AWAITING_SETTLEMENT
```

The resolver rejects SETUP, COMPLETION and unfinished decisions (pending actors
or raise rights). It includes ACTIVE and ALL_IN contenders and excludes FOLDED
and ELIMINATED players. No eligible player is invalid. A sole contender wins
uncontested at any dealt phase without evaluating cards. Multiple contenders
require SHOWDOWN, whose validated state has five community cards. Winners are
returned as immutable IDs in seat order, preserving all tied strongest players.

This result ranks eligible hands only. It does not transfer chips, calculate
side-pot eligibility, split pots, assign odd chips or move the hand to COMPLETION.
A later settlement component must compare each pot's own eligible contenders;
it cannot award every side pot from this single global winner list. No evaluator
is duplicated in the action engine or UI.

## Integration boundary and tests

`ShowdownIntegrationTest` uses real GameInitializer, GameSetup, Deck, Card,
GameState, PokerHandEvaluator and ShowdownResolver. Test-only bounded Random
stubs fix the dealer/shuffle so expected holdings are known. Two fixtures check
one six-high winner and a board-only royal-flush tie, conserved chips, unchanged
source PREFLOP state, and AWAITING_SETTLEMENT. The fixture explicitly constructs
a closed SHOWDOWN snapshot using the initialized cards. It supplies the future
engine boundary; it is not a played action-to-river test.

The 21 resolver cases use the real evaluator for contested results. A fail-fast
HandEvaluator stub is used only to prove an uncontested result never calls it.
No mocking framework or mock game/state components are used.

Estella's betting/street implementation is available on `feature/basic-betting`.
The application session connection supplies the closed state/result to Swing.
Setup-to-result play, all-in runout and result-screen integration remain open
until the connection is exercised together.

## Bilingual result component

`ui.ShowdownPanel` is a reusable Swing component, constructed/used on the event
thread. Its public handoff is:

```java
ShowdownPanel resultPanel = new ShowdownPanel();
resultPanel.showResult(view, result, selectedLanguage);
// view: GameView for the current hand; result: the existing ShowdownResult
// selectedLanguage: a configured tag such as "en" or "zh-CN"
resultPanel.clearResult(); // on a new hand or while no result should be shown
```

The component shows localized human/bot seat names for one winner, tied winners
or an uncontested winner, plus awaiting settlement. It reads only public
identity/type/seat information and retains no view/result or hole cards. A
mismatched hand, unknown winner or unsupported language is rejected before
replacing an earlier valid display. Call showResult again after language changes;
clearResult removes all old text and hides the panel.

Checkpoint: RED `2493bf3`, GREEN `750e7c4`; 18 real-resolver component cases,
753 passing project tests, clean Checkstyle/SpotBugs, and inspected Java 11
English/Chinese renders. [UI boundary analysis](../bva/GamePanelLocalization.md)
records the cases. No new dependencies, gameplay rules or payout logic were added.

The panel is not attached to GamePanel in this checkpoint. Estella can attach it
when her session exposes the matching GameView/ShowdownResult; retain the existing
private-card hiding/handoff callbacks. This prepares a result view without
changing her engine/session API or claiming the played-hand integration is done.
