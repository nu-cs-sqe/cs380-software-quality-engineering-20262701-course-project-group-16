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

Estella's action/street engine must supply the closed state and connect this
result to the coordinator and Swing display. Until that code is available,
setup-to-result play, all-in runout and result-screen integration remain open.
Translation resources include an awaiting-settlement result message, but that
alone does not constitute a playable bilingual result screen.
