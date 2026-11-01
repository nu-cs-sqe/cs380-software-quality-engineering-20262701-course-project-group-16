# Player statistics

These are the project's statistics conventions. `PlayStats` aggregates the
completed hands of one player/profile. Profile storage, the profile screen,
and the betting-engine adapter are separate work.

## Required results

| API | Definition |
| --- | --- |
| `getHandsPlayed()` | Completed hands in which the player was dealt in |
| `getVpipPercent()` | `100 * voluntaryPreflopHands / handsPlayed` |
| `getPfrPercent()` | `100 * preflopRaiseHands / handsPlayed` |
| `getVpipPfrGapPercentagePoints()` | VPIP minus PFR, in percentage points |
| `getThreeBetPercent()` | `100 * threeBetHands / threeBetOpportunityHands` |
| `getAggressionFactor()` | Postflop `(bets + raises) / calls`; a ratio |
| `getAggressionFrequencyPercent()` | Postflop `100 * (bets + raises) / (bets + raises + calls + folds)` |
| `getFlopCBetPercent()` | `100 * flopCBetHands / flopCBetOpportunityHands` |
| `getFoldToFlopCBetPercent()` | `100 * foldedToFlopCBetHands / facedFlopCBetOpportunityHands` |
| `getWtsdPercent()` | `100 * showdownHands / sawFlopHands` |
| `getWonAtShowdownPercent()` | W$SD: `100 * showdownPayoutHands / showdownHands` |

Every rate returns `OptionalDouble`. A zero denominator returns empty so the UI
can display N/A. AF also returns empty whenever calls are zero, including when
there were bets or raises. This is the project's API/display policy.

An existing denominator with no qualifying success returns numeric zero.
Results contain no display strings, rounding, NaN, or Infinity. Round in the UI.

## Counting rules

### Participation and preflop

- Count only completed hands where `dealtIn=true`. Elimination/nonparticipation
  hands do not change any counter. Pending hands never change lifetime totals.
- VPIP requires an accepted preflop CALL or RAISE adding positive chips. Forced
  small blinds, big blinds, and antes alone do not qualify. Checking the big
  blind without adding chips does not qualify.
- Completing the small blind or calling a raise from a blind qualifies. Calling
  then raising, repeated calls, and repeated raises count once per hand.
- Any accepted preflop raise qualifies for PFR, including opening raises and
  re-raises. Therefore PFR cannot exceed VPIP. A later fold does not undo either.
- Three-bet opportunities are eligible decision hands, including declined calls
  or folds. Multiple eligible observations within one hand form one denominator
  hand. The engine classifies opening raises, three-bets, four-bets, and later
  raises explicitly; bet size does not determine that classification.
- A four-bet is a preflop raise but not a three-bet. If an earlier decision in
  that hand declined an eligible three-bet opportunity, that opportunity remains.

### Postflop and c-bets

- AF and AFq count individual accepted FLOP, TURN, and RIVER actions. Checks
  contribute to neither formula. AFq includes folds; AF does not.
- A flop c-bet is an opening flop bet by the final preflop aggressor. An
  opportunity exists only if that player can legally make the bet. Checking or
  folding at an eligible opening decision declines the opportunity.
- An all-in player before the flop has no c-bet opportunity. Facing another
  player's opening flop bet is not an opportunity to open with a c-bet.
- Fold-to-c-bet opportunities require an actual flop c-bet and a legal response
  decision. An ordinary flop fold is not sufficient. Calls/raises are declined
  fold opportunities. Each numerator and opportunity denominator counts hands.
- These eligibility facts come from the engine. `PlayStats` does not reconstruct
  the betting tree, legal raise sizes, or the identity of opponents/aggressors.

### Showdown

- Seeing the flop means remaining in the hand when the flop is dealt, including
  players already all-in. Folding preflop excludes flop participation.
- Showdown participation requires remaining through showdown. A win because all
  opponents fold is not showdown. Showdown requires seeing the flop.
- Any positive payout from a contested showdown pot qualifies for W$SD. Split
  pots and side pots count; multiple qualifying payouts still count once per
  hand. An uncalled-bet refund alone does not qualify.
- Positive contested payout qualifies even if smaller than the player's
  investment, producing a negative net result. This statistic is not net profit.

## Engine input contract

Use one instance per player/profile and serialize calls on one engine event
thread. The class deliberately does not expose mutable counters or collections.

1. `beginHand(handId, dealtIn)` starts pending collection. The ID is nonblank
   and globally unique across matches for that instance, for example a generated
   UUID or a collision-free match/hand identifier. A second active hand or a
   completed ID cannot be started.
2. `recordAction(StatsAction)` supplies immutable observations once per engine
   action. Rejected observations are ignored; all observations must still be
   structurally valid. Only accepted actions enter the pending hand.
3. `completeHand(handId, sawFlop, reachedShowdown, contestedShowdownPayout,
   uncalledBetRefund)` is called only after the engine has actually completed the
   hand. Payout and refund amounts are separate nonnegative values. Completion
   atomically applies all counters and marks the ID complete.

`StatsAction` uses `ResolvedActionType`: FORCED_POST, CHECK, CALL, BET, RAISE,
or FOLD. The engine resolves all-ins before creating an observation:

- All-in calls are CALL; all-in raises are RAISE; postflop all-in opening bets
  are BET; stack-exhausting forced blinds remain FORCED_POST.
- `chipsAdded` means newly committed chips, not a raise-to total. CALL, BET,
  RAISE, and FORCED_POST require positive additions; CHECK and FOLD require zero.
- A preflop opening raise is RAISE with OPENING_RAISE context, not BET.

`StatsContext` describes one decision. Its raise type is NONE, OPENING_RAISE,
THREE_BET, FOUR_BET, or LATER_RAISE. A preflop RAISE requires a non-NONE type;
other actions cannot carry a preflop raise type.

- `threeBetOpportunity` is an eligible preflop CALL/FOLD/THREE_BET decision. A
  THREE_BET requires this flag. Opening/four/later raises cannot set it for that
  decision; earlier opportunities in the hand are still retained.
- `flopCBetOpportunity` certifies final-preflop-aggressor status and the legal
  ability to open flop betting. It accompanies BET, CHECK, or FOLD on the flop.
- `facedFlopCBetOpportunity` certifies facing an actual c-bet with a decision
  available. It accompanies CALL, RAISE, or FOLD on the flop.
- Preflop and flop opportunity context cannot be combined. A player cannot
  both open and face an actual flop c-bet in the same hand.

Accepted actions after a fold, actions by a nonparticipant, backward street
transitions, and incompatible contexts are rejected before changing pending
data. Completion rejects inconsistent participation/showdown/payout facts.
Invalid input preserves lifetime totals and the active hand for correction.
Completed IDs cannot be aggregated twice, including previously nonparticipating
hands. Counter additions are checked for overflow before any totals change.

Application shutdown discards pending hands. A future profile store must persist
completed counters and deduplication identity state, not serialize pending hands
or save an unfinished match. Such persistence is not implemented in this step.

## Verification

```bash
bash ./gradlew test --tests 'domain.*Stats*Test'
bash ./gradlew clean build
```

The BVA documents for PlayStats, StatsAction, and StatsContext map unique case
IDs to their JUnit tests. Enums are exempt from separate BVA documents under the
course template. TDD execution logs are kept separately; no commits are made
automatically.
