# Quality checks and measurement scope

The Gradle `check` lifecycle runs JUnit tests, Checkstyle on main/test sources,
SpotBugs on main/test bytecode and the scoped JaCoCo report. `build` includes
`check`. Mutation testing is a separate `pitest` task so ordinary feedback stays
fast; CI runs it after the build. All static findings fail their task: there is
no `ignoreFailures`, global suppression or baseline hiding existing warnings. Reviewed exceptions are exact bug-pattern/class/member matches in `config/spotbugs/exclude.xml`.
CI uploads test, coverage, mutation and static-analysis reports even after a
failed step.

## Hybrid postflop proof and value betting

[Design](design/README.md#exact-showdown-outcomes-and-value-betting) and
[boundary tests](bva/BotPolicy.md#hybrid-postflop-proof-and-wager-strategy)
cover the exact and strategic components separately. The first focused
flop and turn private-royal tests failed because the legacy bot invoked
the simulation spy. After the proof and value-policy integration, both
cases returned a legal sized bet without simulation or a random draw.
The original locked-river replay still uses real betting and settlement.

### Controlled latency and policy comparison

The comparison used the original river-only proof and mixed strategy from
`73771f1` and the hybrid decision on identical public snapshots. Timings are
warm Java 25 measurements in milliseconds from 40 paired decisions after 10
warmups, with 512 equity trials where needed. Snapshot construction was
outside the timed interval. Fixed sample seeds started at 9,200. The payout
column is the immediate check/call benchmark before refunds. For exact
decisions it is the payout floor. The hybrid's modeled gain is measured from
the decision after deducting call risk and adding projected opponent chips.

| Scenario | Hybrid proof | Old → new MC runs | Old → new p50 / p95 ms | Seeded old → new action | Immediate payout | Old immediate net → new modeled net |
| --- | --- | ---: | ---: | --- | ---: | ---: |
| Private royal on flop | Guaranteed win | 40 → 0 | 6.298 / 8.171 → 0.108 / 0.147 | Check → Bet 30 | 60 | 60.00 → 82.98 |
| Private royal on turn | Guaranteed win | 40 → 0 | 5.674 / 7.162 → 0.054 / 0.086 | Check → Bet 30 | 60 | 60.00 → 81.88 |
| Broadway board facing 980 | Board-locked tie | 0 → 0 | 2.064 / 3.407 → 2.071 / 2.358 | Call → Call | 1,000 | 20.00 → 20.00 |
| Turn A♠ K♣ Q♦ J♥ with 2♣ 3♦ facing 20 | Uncertain | 40 → 40 | 5.682 / 6.117 → 5.705 / 6.133 | Fold → Fold | 11.00, if calling | −9.00 for Call in both policies, zero for Fold |

In the two royal scenarios, the original probabilities were 81.82% Check
and 18.18% Bet. The hybrid selected Bet 30 with probability 100%. The locked
river selects Call with probability 100% in both policies, and the chosen
uncertain-turn seed produced a 100% Fold distribution in both. The mixed
fallback's two nonzero action probabilities are independently exercised in
the uncertain-river regression.

The table below keeps the winning flop, 600-chip hero stack and 40-chip pot
fixed. Each profile supplies 20 observed actions facing a wager on the flop.
The response estimates are conditional on the hybrid's selected bet. The
immediate payout floor is 40 for every profile.

| Observed response | Hybrid bet | Modeled Fold / Call / Raise | Modeled net chips |
| --- | ---: | ---: | ---: |
| Fold | 20 | 97.69% / 2.29% / 0.03% | 40.47 |
| Call | 40 | 55.32% / 44.17% / 0.51% | 58.07 |
| Raise | 30 | 41.77% / 44.34% / 13.90% | 62.92 |

### Paired hands against the existing bot

The paired simulation used 384 identical seeded deals per opponent, with
deal seeds 38,000–38,383 and action seed `6101 + 43 * dealSeed + 83 * decision`.
Sampling used 24 trials for speed and the baseline opponent policy retained
its cautious, balanced or aggressive personality. Each hand started heads-up
on the private-royal flop, with 20 committed per player and a 600-chip hero
stack behind. Both sides used default opponent priors on each snapshot.
The comparison used actual engine actions and street advancement, then
`PotSettlement`. Profit is the hero's final stack minus its starting stack
and the 20 previously committed chips. All completed simulations conserved
chips. The intervals are normal approximations for the paired mean change.

| Opponent | Old → hybrid profit per hand | Paired change and 95% interval | Hero wagers | Old → hybrid mean wager chips |
| --- | ---: | ---: | ---: | ---: |
| Cautious | 75.326 → 164.323 | +88.997 [77.976, 100.019] | 223 → 986 | 92.67 → 59.27 |
| Balanced | 89.526 → 190.260 | +100.734 [88.150, 113.319] | 214 → 1,026 | 91.95 → 66.30 |
| Aggressive | 100.417 → 197.500 | +97.083 [85.825, 108.342] | 216 → 1,022 | 101.90 → 62.41 |

The results measure value extraction on this fixed winning flop. Broader
win rates, mixed stacks, opponents with history in the match and seat effects
need separate evidence. Earlier-street proofs can still fall back on states
outside the shortcuts when the 2,048-comparison budget is insufficient.
On the private king-high straight-flush turn from HB03, the production budget
returned uncertain at 3.538 ms median and 5.686 ms p95 for the proof alone.
The full 45,540-comparison proof took 74.315 ms median and 75.187 ms p95.
This was a separate warmed arm64 Java 25 sample with 40 evaluations per limit.

The measured Java class list adds `ExactOutcomeEvaluator` and
`KnownOutcomePolicy` with their nested classes. Existing measurement targets
remain in scope. [Gradle measurement list](../build.gradle.kts).

The original hybrid run passed 1,792 tests with no failures, errors or skips.
Checkstyle and SpotBugs reported zero findings. The JaCoCo report counts
4,310/4,311 lines and 2,128/2,138 branches over 96 selected Java classes.

| JaCoCo class | Lines | Branches |
| --- | ---: | ---: |
| `ExactOutcomeEvaluator`, including `RunoutSearch` | 70/70 | 50/50 |
| `KnownOutcomePolicy` body | 111/111 | 54/60 |
| `BotComputer` | 31/31 | 6/6 |

The new proof and decision boundaries have targeted assertions. These raw
counters describe bytecode execution and do not demonstrate every independent
path through the model.

`bash ./gradlew --no-daemon build pitest` passed. Its XML records
2,706/2,747 killed (98.51%), with 41 survivors and zero uncovered, timed-out
or error mutants. The locked-river baseline had 2,555/2,577 killed and 22
survivors. Those 22 remain. All 19 additional survivors are in the incomplete
board evaluator or value policy.

| PIT scope | Killed / generated |
| --- | ---: |
| `ExactOutcomeEvaluator` and `RunoutSearch` | 56/65 |
| `KnownOutcomePolicy` and `Plan` | 101/111 |
| `RiverOutcomeEvaluator` | 28/28 |
| `BotComputer` | 12/12 |
| `OpponentBeliefs` | 28/28 |

In the first mutation pass the value policy body killed 67/109. Numeric
projections and the bluff boundaries in HB12–14 raised it to 99/109. Remaining
value-policy mutants include in-position projections, equal-value wager
selection and pricing with chips already committed this round. The exact
evaluator's board-size `> 5` to `>= 5` mutant is equivalent at that helper
because `evaluate` sends five-card boards to the river evaluator first. Other
search survivors include the early-exit predicate, cancellation placement and
removal of the comparison accumulator. A separate Java probe of a turn with
four public aces and private K♣ 2♦ returned uncertain at the default budget
and guaranteed non-loss after all 45,540 comparisons. At that checkpoint the
persistent suite lacked a case that could catch changes to the private-ace
requirement or accumulator on a complete search that can tie.

### Instructor style follow-up — November 26, 2026

`ExactOutcomeEvaluator.evaluate` coordinates board dispatch, early proof and
bounded search. The search now owns both its comparison statistics and the
conversion of completed bounds to an outcome. `KnownOutcomePolicy` names
bluff return, check inducement and future-street contribution separately.
Check inducement derives from the zero target because the engine's wager
targets are always positive. Bluff evidence and participant thresholds use
named constants. The strategy formulas, comparison budget and settlement
contracts are preserved.

The new tests separate the private royal proof from the public board lock.
HB15 checks public ace quads with a private king using the real evaluator at
the production budget and at the complete-search boundary. It adds regression
evidence to the already established local test-first history. [Hybrid BVA and
test scope](bva/BotPolicy.md#hybrid-postflop-proof-and-wager-strategy).

`bash ./gradlew --no-daemon build pitest` passed with 1,794 tests and zero
failures, errors or skips. Checkstyle and SpotBugs reported zero findings.
JaCoCo measured 4,317/4,318 lines and 2,130/2,140 branches across the same 96
selected Java classes. The exact evaluator and its search covered 73/73 lines
and 52/52 branches. The value-policy body covered 115/115 lines and 54/60
branches.

PIT killed 2,716/2,754 mutants, with 38 survivors and zero uncovered mutants,
timeouts or errors. The exact evaluator and its search killed 63/69. The
value policy and its plan killed 104/114. PIT names HB15 as the test that
detects bypassing the private-ace check, dropping the comparison accumulator
and treating a tied comparison as a losing witness. The remaining exact
search and value-policy survivors are still visible in the report.

## Exact river bot decisions

The reported river was J♦ Q♠ A♠ 10♦ K♣. Every possible holding plays the
ace-high straight. The previous policy received a pot-weighted expected share
and treated it as ordinary uncertain equity. It subtracted break-even equity,
applied personality, position, opponent aggression and commitment terms, then
used a five-percentage-point mixing margin to produce Fold and Call weights.
Both bots could fold even with a positive immediate-settlement chip return.

The fixed-deal reconstruction gives Bot 1 and Bot 5 1,000 opening chips each
and learns from this hand starting at the default prior. Their live stacks,
earlier opponent observations and random draws are absent from the report.
Both reconstructed calls cost 980 with no new refund. The expected payout is
the estimator's fractional share of each eligible pot before odd-chip rounding.

| Bot in the original action order | Estimated share | Break-even share | Expected payout | Adjusted edge | Old Fold / Call | Exact Fold / Call |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Bot 5 | 48.544% | 47.573% | 1,000 | −0.807 pp | 58.069% / 41.931% | 0% / 100% |
| Bot 1, after Bot 5 folded and Bot 6 called | 33.059% | 32.237% | 1,005 | −2.844 pp | 78.443% / 21.557% | 0% / 100% |

The exact river proof evaluates all two-card combinations against the board,
then uses the acting bot's blockers when the board itself can be improved.
The evaluator distinguishes a universal board lock from a tie proved using
the acting player's blockers.
Opponent hands and the live deck do not enter the calculation. A win, tie or
non-loss favors checking if free and calling a legal wager otherwise. Under
the no-rake rules, each eligible tied tier contains at least one equal
contribution per claimant. A proved loss can recover only a sole-eligible
layer and any new call refund in the immediate-settlement preview. The ranking
proof is distinct from the expected share of eligible pots and the actual
return can vary with later betting, folds, and odd-chip rules. Uncertain
rivers and earlier streets retain Monte Carlo and stochastic policy behavior.

`BotRiverIntegrationTest` replays the original folds and checks the 3,040 and
6,050 pots, payouts of 1,014, 4,038 and 4,038, and 42,005 returned chips. With
the corrected bot calls and the fixture's stacks, the main pot becomes 5,000,
the side pot stays at 6,050 and the refund stays at 42,005. All six ending
balances equal their respective opening balances. Bot 3 folds preflop in
both runs and remains ineligible. `BotRiverDecisionsTest` checks capped calls,
free checks, two through six live seats and preservation of mixed decisions on
a beatable river. The exact calculation is included in the Java measurement
scope. [River decision boundaries](bva/BotPolicy.md#exact-river-decisions).

An independent suit-pattern audit compared all 1,024 Broadway boards with
the expected rank and suit boundaries. It found 604 locked and 420 beatable
boards.

`bash ./gradlew --no-daemon build pitest` passed with 1,756 tests and zero
failures, errors or skips. Checkstyle and SpotBugs reported zero findings.
The measured class list adds `RiverOutcomeEvaluator` and its nested enum for
both JaCoCo and PIT.

| JaCoCo scope | Lines | Branches | Complexity counters |
| --- | ---: | ---: | ---: |
| Selected Java classes (91) | 4,115/4,116 | 2,030/2,034 | 2,149/2,153 |
| `RiverOutcomeEvaluator` | 35/35 | 18/18 | 18/18 |
| `BotComputer` | 41/41 | 18/18 | 20/20 |

These counters describe executed bytecode. The BVA lists the separately
asserted ranking, validation, cancellation and call-decision boundaries.

PIT killed 2,555 of 2,577 mutants (99.15%), up from 2,508 of 2,530 in the
November 23 baseline. The same 22 documented survivors remain, with zero
uncovered mutants, timeouts or errors. `RiverOutcomeEvaluator` killed 28/28,
`BotComputer` 23/23, `BotPolicy` 62/62 and `PotSettlement` 48/48. The first
mutation pass found that the invalid board-size test also had an invalid total
card count. The added four-plus-three and six-plus-one cases isolate the
required five-card board boundary. The evaluator also identifies the public
board lock explicitly, so a mutant that skips the proof changes its result.

### BVA and style follow-up

The river decision test supplies expected actions and chip costs directly. It
checks 979, 980 and 981 behind a 980-chip wager, and adds a paid heads-up case
alongside the free check. The malformed-input and cancellation tests have
separate responsibilities. The fixed-deal replay names the two blind amounts
and isolates the seeded preflop decision in its own helper.

The [BVA and local TDD record](bva/BotPolicy.md#exact-river-decisions) maps
the inputs and assertions to their tests and records the observed fixed-deal
RED/GREEN sequence. The added tests are post-GREEN hardening. The coursework
criterion for GitHub commit evidence of that workflow remains open for this
local change.

The follow-up passed `bash ./gradlew --no-daemon build pitest` with 1,760
JUnit tests, including 30 across the three new river test classes. Checkstyle
and SpotBugs reported zero findings. JaCoCo retained the 91-class scope and
the counts above. PIT again killed 2,555/2,577, including 28/28 in the river
evaluator and 23/23 in the bot coordinator, with the same 22 documented
survivors and zero timeouts, uncovered mutants or errors.

## River Room submission audit — November 24, 2026

The desktop audit uses `7187187` and the uncommitted working tree, including
the new renderer modules. The funded desktop rules, localization design and
instructor feedback govern the acceptance checks below. The engine remains
responsible for seat and turn validation, legal wager targets, card visibility,
settlement and persistence.

| Changed feature | Requirement and visible behavior | Test evidence |
| --- | --- | --- |
| Lobby seating and funding | One to six ready local accounts choose distinct physical chairs. Bots fill the remaining chairs at the first deal. Invalid buy-ins cannot mark an account ready. | `lobby.test.mjs` seat and buy-in boundaries, and the native startup and solo-seat walkthroughs. [Lobby](../desktop/src/Lobby.jsx), [lobby model](../desktop/src/lobby-model.mjs). |
| Betting controls and table layout | Controls reflect the engine's legal actions. Raise-to is a total commitment, while the preview and accepted action charge only the additional chips. Presets and the slider use exact integer chip values. At a 900 × 650 native window size the private cards, amount, slider and action controls fit. | `table-state.test.mjs` includes exact integers beyond JavaScript's safe number range, legal presets and slider limits. The raised six-player and unequal all-in walkthroughs check the live controls and accepted wagers. [Betting controls](../desktop/src/BettingControls.jsx), [table state](../desktop/src/table-state.mjs). |
| Card faces, notices and street progress | Shared and private cards use the four suit inks. Seat badges show the latest applicable action and the table marks the current street. | The raised six-player walkthrough checks card inks, board and seat geometry, action badges and phase progress. [Table surface](../desktop/src/TableSurface.jsx). |
| Private handoff and request timing | Betting handoff and actions belong to the current shared seat. Switching private access outside the turn requires the owner's credentials. Blur conceals cards, a stale response cannot restore private cards, and solo play offers a local hide/show control. | The shared, solo and ordered-showdown walkthroughs, the handoff unit cases, and `DesktopBridgeIntegrationTest.onlyCurrentHumanMaySeeCardsOrActAndOldSnapshotsCannotReplay`. [Request hook](../desktop/src/use-table-requests.mjs), [owner hand](../desktop/src/OwnerHand.jsx), [Electron boundary](../desktop/electron/main.cjs). |
| Showdown, runout and payouts | Contested river play tables live hands by default. Optional muck withdraws a claim, and folded disclosure follows the selected timing. The paid table shows public winning cards, payouts, returns and exact stacks. | The raised six-player, ordered-showdown, closing-folder and unequal all-in walkthroughs. The latter checks five pots and 3,800 chips awarded. `ReviewedHandTest.royalBoardTiesTheMainAndSidePotsAfterAllLiveHandsAreExposed` covers ties. [Table surface](../desktop/src/TableSurface.jsx). |
| Save retry and read-only review | The full action-history UI becomes available only after a successful save and hides when the next hand begins. Retrying pays the same hand once. Results and replay preserve game state, and replay exposes each card at its recorded event. | The shared restart walkthrough injects SQLite failure and checks zero receipts before retry and one afterward. The raised and all-in walkthroughs check replay privacy and the side-pot case compares the state across results navigation. `hand-history.test.mjs` checks event-by-event chip conservation. [Review views](../desktop/src/HandReview.jsx), [replay projection](../desktop/src/hand-history.mjs). |
| Localization and recovery | English and Simplified Chinese can be selected in the lobby before the first deal. Validation and chip displays use the selected locale. The locale catalog and Java recovery bundles support another locale through configuration and resources. | The two native startup walkthroughs, bilingual recovery cases, `locales.test.mjs` and `LocalizationsTest.thirdLanguageIsDiscoveredAndNumbersUseItsLocaleFromResourcesOnly`. [Localization design](design/localization.md#river-room-desktop-resources). |

The table request hook tracks the failed operation. Both language catalogs now
provide the corresponding player-facing recovery instructions. The English
resource previously omitted that section, so a locale copied from English
inherited untranslated diagnostics. The resource-template regression failed
before the fix. The shared-showdown walkthrough also checks the localized
feedback after the engine rejects a stale handoff.

A funded player's blank opening buy-in was rejected by the roster form. The
English startup walkthrough reproduced the disabled Start button after the
empty submission. The form now applies the existing default and validates the
resolved amount. For a 2,000-chip balance the engine reserves exactly 1,000
chips at the selected chair. The Chinese walkthrough also checks an explicit
400-chip buy-in. [Roster BVA](bva/AccountTable.md#desktop-opening-buy-in--november-24).

The React wager preview and replay are presentation calculations. The wager
preview uses engine-provided targets and the replay reflects engine-produced
public events. Electron checks the requesting frame and window focus for
private operations. The Java bridge validates revision and owner before calling
the engine. The lobby repeats the 400-chip minimum and 1,000-chip default for
immediate form feedback. The engine still validates every reservation. The
remaining string dependency in `OwnerHand` recognizes an expired seat session
from the engine's English sign-in diagnostic. The longer-term contract question
is whether to give that response a stable error code.

The current renderer passed `npm test` with 32 tests and built as
`index-8X_tALPA.js` with SHA-256
`3b15c5c147d2ad505d6adb082a01fe436d6d103c505e26b0959466ede53ecdfe`.
The staged Java JAR matches `build/libs` with SHA-256
`a920d33a973ca261a5fbfbc605135ef4003e3fa5fe786ecc88200d20671e97b2`.
All 12 native Electron walkthroughs passed in one run against that renderer
and prepared engine, using temporary accounts. Earlier batches were interrupted
around macOS idle sleep. The clean run cleared the host's `ELECTRON_RUN_AS_NODE`
flag and held the display and system awake only for the test command. The native
shell loaded the current build directly. Signed-package evidence below is from
earlier runs.

This pass changed renderer code and desktop tests. The full Java build and PIT
results below are from the prior audit. A Node unit-coverage sample reports
100% lines and 95.98% branches across the four loaded `.mjs` feature modules.
That sample covers `hand-history`, `lobby-model`, `locales` and `table-state`.
JSX, the request hook and Electron's process boundary need separate coverage
evidence. Native scenarios exercise those paths behaviorally.

Coverage of the viewport checks and the fund, wager and history boundaries
listed here is explicit. A complete frontend BVA and independent path map
remain open. The submitted CI artifact available for comparison is the earlier
main run with passing Java and React jobs. The current audit diff is local,
and the repository workflow currently runs desktop unit tests and the React
build. The Electron walkthroughs are local evidence. [Earlier main CI run](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/actions/runs/45112401965).

River Room settles individual Hold'em hands and lets players end the table
between paid hands. Confirm with the instructor whether those hand results
satisfy the rubric's win-condition criterion and how its whole-code coverage
requirement applies to the JavaScript renderer and Electron adapter.

## Mutation, basis paths and course evidence — November 23, 2026

`bash ./gradlew --no-daemon build pitest` passed with 1,730 JUnit tests and
zero failures, errors or skips. Checkstyle and SpotBugs reported zero findings
in their configured scopes.
The JaCoCo counters are from the full JUnit run. The class selection is the
same as the preceding checkpoint. The bounded seat-enumeration call adds one
measured line, and the preflop winner regression restores coverage of the
empty hand-category branch.

| JaCoCo scope | Lines | Branches | Complexity counters |
| --- | ---: | ---: | ---: |
| Selected Java classes (89) | 4,054/4,055 | 1,996/2,000 | 2,115/2,119 |
| `GameSetup` | 45/45 | 16/16 | 19/19 |
| `ReviewedHand` | 182/182 | 118/118 | 111/111 |
| `SetupSession` | 403/403 | 158/158 | 191/191 |

### PIT results and finding review

| XML result | Before | After |
| --- | ---: | ---: |
| Killed | 2,498 | 2,508 |
| Survived | 30 | 22 |
| Timed out | 2 | 0 |
| Uncovered | 0 | 0 |
| Total | 2,530 | 2,530 |

The final XML confirms all eight behavior-changing survivors were killed,
along with both previous timeouts. `GameSetup` killed 25/25 mutants,
`ReviewedHand` 136/136 and `SetupSession` 229/229. There are no unresolved
timeouts. The review below accounts for all 32 starting findings. The
measurement configuration stays in place. The only production change in this
pass bounds count-based seat selection to the physical table. The desktop
supplies explicit seats, so this pass relies on the earlier renderer and
real-engine walkthroughs for desktop evidence.

| Previous finding | Count | Disposition and deterministic evidence |
| --- | ---: | --- |
| `GameSetup.firstSeats` and the upper guard in `requireHumanCount` | 2 timeouts | Either mutation let the existing `Integer.MAX_VALUE` test reach seat enumeration bounded by the requested count. Enumeration now uses the six physical seats. PIT names `GameSetupTest.GU_06` at seven humans as the killing test for both. `firstSeatsAcceptsExactlyOneThroughSixHumans` directly rejects 0 and 7 and checks the exact seat sets at 1 and 6. The extreme-input and no-random-draw tests remain. |
| `GameSetup.create` with selected seats | 1 | Removing count validation allows an empty human-seat set. `selectedSeatOverloadRejectsAnAllBotTableBeforeDrawingTheDealer` checks the rejection and that the dealer source is untouched. The other selected-seat tests cover the valid endpoints and indices just outside the table. |
| `PokerHandEvaluator.winningCards` | 1 | Equivalent. PIT index 31 changes the size test from `> 5` to `>= 5`. An extra removal leaves four cards, where `bestFive` has no candidate and returns zero. The saved score for a valid five-card hand is positive, so the loop restores each removed card at the same index. The other boundary on line 51, which can skip index zero, is killed by `theFirstCardCanBeTheOnlyCardOutsideTheWinningFive`. |
| `BettingEngine.flopContext` | 1 | Equivalent. PIT index 61 changes `cBet > 0` to `cBet >= 0`. At zero the remaining conditions would require a zero current target and a negative contribution, which `PlayerState` rejects. The separate boundary on line 262 is killed by `PostflopActionsTest.anAlreadyMatchedContributionCarriesNoFacedContinuationBetContext`, which asserts zero chips added, an unchanged pot and no faced-bet context at the matched contribution. |
| `BettingAmounts.chipsAdded` | 1 | Removing the zero-call guard changes what this calculation accepts before accepted-action validation. `callAmountRejectsAMatchedPlayerBeforeConstructingAnAcceptedAction` checks rejection at zero. Existing call and capped-call cases cover positive amounts. |
| `AccountTable.open` and its balance predicate | 2 | A later buy-in check also rejects the underfunded account. `AccountTableIntegrationTest.minimumBuyInRejectsAnUnderfundedSelectedAccount` checks the selected-account failure reason and preserves the original hand. The funded-table cases cover valid selection. |
| `AccountTable.addChips` | 1 | A later funding check also rejects a released account. `releasedAccountCannotReserveChipsAfterLeavingThePaidTable` checks the departed-account failure reason, released reservation and persisted balance. The funded-table cases cover accepted top-ups. |
| `TableFunds` constructor | 1 | A reservation greater than the balance also fails the subsequent range check. `FundingCapacityTest.reservedChipsAboveTheBalanceAreRejectedBeforeTheFundingLimit` checks the invariant's own diagnostic. The existing capacity cases cover consistent funded values. |
| `TableRoster.opening` and `requireSafeTotal` | 2 | Either mutation admits a six-seat total above `Long.MAX_VALUE`. `openingRosterAcceptsTheExactChipLimitButRejectsOverflow` checks the exact limit and one chip over it, including all five bot stacks. |
| `GameState.hasShortBlindTarget` | 1 | Equivalent in `validateBettingAmounts`. It calls the predicate only if the current target differs from the observed maximum. The predicate also requires the target to equal the big blind, so allowing `maximum == bigBlind` cannot make a previously false result true. `BettingStateBoundaryTest` covers the short all-in preflop blind and invalid street or status. |
| `BettingEngine.nextPendingActor` | 1 | Equivalent. The extra full-circle step revisits the initiating seat, which `BettingRound.after` removes from the pending set on both action and departure. Postflop rotation and `QuitIntegrationTest.dep08NonCurrentQuitFoldsImmediatelyAndPreservesTheCurrentDecision` cover the relevant actor transitions. |
| `BettingEngine.resolveAction` | 1 | Equivalent. The mutation adds the zero-chip case to a comparison against the active actor's strictly positive stack, so it cannot classify a zero-chip action as all-in. Postflop Check and Fold cases and `ShortAllInIntegrationTest.shortAllInsAndCappedCallsRunOutWithoutExtraActiveDecisions` cover the no-charge and all-in outcomes. |
| `BettingRound.closeMatchedLoneActor` | 1 | Equivalent. Including the lone actor's nonnegative contribution in the opponents' maximum leaves the closure test unchanged. Its contribution is at least the expanded maximum exactly when it is at least the opponents' maximum. Existing short-all-in cases cover a lone actor owing chips and the matched automatic runout. |
| `PokerHandEvaluator.bestFive` nested loop bounds | 8 | Equivalent for the supported five, six and seven cards. Each extra outer index `i >= n - k` starts the next index at `i + 1 >= n - (k - 1)`. That unchanged inner bound prevents an additional candidate or card lookup. The category and winning-five tests cover the supported lengths. |
| `PokerHandEvaluator.ranksByMultiplicity` comparator | 1 | Equivalent. The input order is descending rank and the sort is stable. Comparing equal multiplicities as equal preserves the existing descending tie order used for kickers and groups. |
| `SqliteProfileRepository.transaction` void callback | 1 | Equivalent. The callback's boolean return is discarded. Commit or rollback depends on work completing or throwing, independently of the sentinel value. The SQLite settlement, retry and rollback integration cases assert those effects. |
| `SqliteChipLedger.recordHand` duplicate cleanup return | 1 | Equivalent. The mutated bytecode return already carries `true` from the successful-insert arm. If closing the statement throws, that return is unreachable. The insert and retry cases cover the other results. |
| `SqliteRecoveryLedger.wasClaimed` duplicate cleanup returns | 2 | Equivalent. The absent-row return already carries `false` and the owned-claim return already carries `true`. A cleanup exception bypasses the return. The recovery ledger integration checks absent, owned and foreign claims. |
| `FundingCapacity.maximumAddition` | 1 | Equivalent for funded domain inputs. At equality with the cap, the remaining capacity is zero and available balance is nonnegative, so both branches return zero. `FundingCapacityTest` covers exact capacity and the over-cap path. |
| `GameConfig.validateSettings` | 1 | Equivalent. A zero starting stack passes the mutated first guard, then fails either blind ordering or the requirement that the positive big blind fit in the starting stack. The funded minimum and invalid blind cases exercise those guards. |
| `Card.hashCode` constant zero | 1 | Contract-only survivor. `CardTest.equalCardsHaveSameHashAndDeduplicateInASet` establishes the equality and set behavior Java requires. Card sets in `GameState`, `PokerHandEvaluator` and `EquityRequest` use insertion for duplicate detection. Dealing and sampling use ordered lists. A constant hash preserves those game outcomes and changes numeric outputs and collision distribution. Acceptance as equivalent for grading remains open. |

Twenty-one survivors have an equivalence argument under the stated invariants.
Eight behavior-changing survivors have targeted tests. The hash survivor is
tracked separately. The tests assert game effects, storage outcomes or existing
failure reasons. Separate numeric hash values are outside the equality contract.

### Independent path evidence and measurement boundaries

Gradle uses the same [explicit Java class list](../build.gradle.kts) for JaCoCo
and PIT, including nested classes except the excluded `ShowdownResult.Status`
enum. JaCoCo uses the full JUnit run. PIT uses the configured test package list,
which omits `desktop.*Test` and the Swing tests. The starting JaCoCo report
measured 89 classes with 4,053/4,054 lines, 1,996/2,000 branches and 2,115/2,119
complexity counters. Raw counters include the branches described below.

The basis counts below use source decisions and merge points. Exceptions are
listed separately because the JaCoCo source decision counters omit exception
edges. Counts here establish an independent basis for the named methods only.

| Method | Feasible source basis and test mapping | Raw JaCoCo |
| --- | --- | --- |
| `GameState.hasBoardSizeForPhase` | 13/13. Each of the four shared phase groups for board sizes 0, 3, 4 and 5 has an accept and reject path. `GameStateTest` covers size zero in `GS_01` and `GS_44`, valid flop through showdown in `GS_45` to `GS_48`, and the corresponding rejections in `GS_49` to `GS_57`. The completion chain has five independent paths. `aPaidHandPreservesTheBoardSizeAtWhichPlayEnded` accepts 0, 3, 4 and 5, and `GS_58` rejects size 2. | 7/8 lines, 21/22 branches, 13/14 complexity |
| `SqliteChipLedger.recordHand` | 2/2. A single inserted row returns success. The duplicate or ignored insert follows receipt verification. `SqliteHandSettlementTest.settlesMultipleProfilesOnceAndPreservesSessionHistoryAcrossReopen` covers success and the identical retry, and `refusesToPayIfTheDatabaseDoesNotRecordTheHand` and `SqliteTableIntegrationTest.rejectsRetriesWithOneChangedFieldAndAcceptsReorderedAccounts` cover missing and conflicting receipts. The cleanup tests exercise write plus close failure and successful write followed by close failure. | 11/11 lines, 3/4 branches, 2/3 complexity |
| `SqliteRecoveryLedger.wasClaimed` | 3/3. `SqliteHandSettlementTest.recoveryLedgerDistinguishesNewOwnAndForeignClaims` checks a new award, a retry for its owner and rejection for another account, plus exact balances and history. The cleanup tests cover query failure before row acquisition and a row-read failure with both close errors suppressed in order. | 10/10 lines, 6/8 branches, 3/5 complexity |
| `GameSetup.requireHumanCount` | 3/3. The first comparison rejects 0, the second rejects 7, and both pass at 1 and 6. `firstSeatsAcceptsExactlyOneThroughSixHumans` asserts every outcome directly. `GU_04` and `GU_07` extend the range of invalid inputs. | 3/3 lines, 4/4 branches, 3/3 complexity |
| `GameSetup.firstSeats` and explicit seat validation | The straight-line success path returns exact seat sets at 1 and 6, and the count guard supplies both rejection paths. The selected-seat predicate has three short-circuit paths. `GU_08` and `GU_11` accept legal endpoints and `rejectsSelectedSeatsImmediatelyOutsideTheTableBeforeChoosingADealer` rejects -1 and 6. The explicit empty-set test checks count validation before seat traversal. | See full `GameSetup` counters above. |
| `ReviewedHand.category` | 2/2. `preflopWinnerCanShowWithoutPublishingAHandCategory` publishes both cards with no category, no selected winning five and the same settlement. `winningCategoryCanBePublishedWithExactlyFiveKnownCards` exercises the threshold with a three-card flop and two pocket cards. The river and royal-board tests cover seven-card comparison. | 3/3 lines, 2/2 branches, 2/2 complexity |

Additional basis evidence for the targeted domain decisions is recorded here.

| Method | Independent paths and test witnesses |
| --- | --- |
| `GameConfig.validateSettings` | 6/6. `GC_04`, `GC_07`, `GC_10`, `GC_11` and `GC_05` each fail the first, second, third, fourth and fifth condition respectively. `GC_02` passes all five at the minimum legal stakes. |
| `TableFunds` four-argument constructor | 6/6. `tableFundingBoundariesRejectNegativeOrUnreservedAmounts` rejects negative balance and negative table chips. `reservedChipsAboveTheBalanceAreRejectedBeforeTheFundingLimit` reaches the third condition. `suppliedFundingLimitsCannotExceedAvailableFunds` checks negative limit, excessive limit and a valid funded value. |
| `TableRoster.requireSafeTotal` | 2/2. `openingRosterAcceptsTheExactChipLimitButRejectsOverflow` qualifies a safely representable total and rejects its first unrepresentable successor. |
| `PokerHandEvaluator.bestFive` | Three feasible loop traces under the evaluator's size contract. Five, six and seven cards yield outer loop counts 1, 2 and 3 and second-loop counts 1, 3 and 6. Those pairs with the common entry edge give three independent traversal-count vectors. `categoryRequiresFiveToSevenDistinctCards` exercises all three sizes. The exact-winning-five tests assert selected cards as well as ranks. |

Disassembly of the Java 11 classes with `javap -p -c -l` ties the single
missed branch at `recordHand` line 63 and the two at `wasClaimed` lines 45
and 46 to generated resource null checks. The normal cleanup copies test at
offsets 76 and 91 in `recordHand`, and offsets 40, 51, 92 and 103 in
`wasClaimed`. JDBC's successful statement and
result-set acquisitions return objects. An acquisition failure throws before
its corresponding resource can enter the body. `SqliteLedgerCleanupTest`
exercises the feasible ordered closes and the exception suppression behavior
without returning invalid JDBC resources. In `hasBoardSizeForPhase`, the
compiled switch map assigns all seven declared `GamePhase` constants to
five covered targets. The default at bytecode offset 132 requires an unmapped
phase, so no domain input can reach line 193. These bytecode paths remain in the raw
reports. The separate JDBC fault cases establish the tested exception routes.
They leave other exception combinations outside this basis claim.

The rubric exempts View, Main and enums from cyclomatic coverage. The current
list also omits executable logic relevant to the course scope. These are the
known gaps from comparing the Java source inventory with the Gradle list.

| Outside the Java reports | Missing evidence |
| --- | --- |
| `DesktopBridge.respond` and `dispatch`, `DesktopRequest` readers such as `chips` and `integer` | Request routing, malformed input and boundary failure paths have no measured basis. |
| `DesktopAccounts.active`, `require` and session lifecycle, `DesktopRecovery` challenge and claim | Session expiration, authorization and recovery adapter decisions have no measured basis. |
| `DesktopTable` seating, current revision checks, private reads, action dispatch and history gating | Tests through the real bridge exercise these flows. Their adapter decisions have no measured basis. |
| `ProfileRepository.loadChipHistory` | The default method is executable and outside the class list even though other profile storage implementations are measured. |
| `DesktopSnapshot.legal`, `result` and `latestActionsOnStreet`, the `GameView` and `PlayerView` constructors, and responsibility-bearing Swing panel code | Review the View exemption for decisions that enforce public projection, card privacy or workflow. Keep display-only code under the stated exemption. |
| React and Electron | Existing unit and walkthrough results are separate from Java counters. An interpretation of the rubric that includes the desktop JavaScript needs a separate measure. |

The mutation cases above trace behavior and invariants at focused decision
points. A method-by-method independent basis for the remaining measured and
in-scope methods remains open.

### Repository process and rubric evidence

The evidence below reflects the local checkout and read-only GitHub records
checked November 23. The course PDF remains the authority for grade criteria.

| Rubric area | Status | Evidence and remaining gap |
| --- | --- | --- |
| Playability, cards and winning conditions | Partially supported | The real-engine walkthroughs cover complete hands and payouts. River Room uses a full deck, resolves each Hold'em hand, splits pots and lets players end a paid table. Instructor confirmation of the rubric criterion for per-hand results remains open. [Game rules](requirements/game-rules.md). |
| Boundary value analysis and matching tests | Partially supported | The existing BVA files map boundary inputs and expected outcomes to tests. This pass documents the directly targeted seat, funding, hand review and storage boundaries. A full cross-check of every completed component's definition of done and assertions remains open. [Game setup BVA](bva/GameSetup.md). |
| Automated testing and static analysis | Verified for the configured checks | Gradle runs JUnit, Checkstyle and SpotBugs. CI runs the Java build and PIT, plus desktop tests and a React build. [Main run 45112401965](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/actions/runs/45112401965). |
| Mutation and 100% cyclomatic coverage | Partially supported | The scoped reports and the basis analysis above establish individual results. Executable adapters are outside the scope and a repository-wide basis has yet to be established. The hash equivalence decision for grading is open. |
| Protected main, required PR approval and checks | Partially supported | The branch is flagged protected. The visible effective rules protect against deletion and non-fast-forward updates. The branch endpoint shows classic protection disabled and no required check contexts. PR #6 has recorded approval. PRs #23 and #33 merged with no submitted reviews. The connector cannot read administrator-only branch protection details. Required PR approval and status checks are not supported by the effective rules observed. [Main rules](https://api.github.com/repos/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/rules/branches/main), [PR #6](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/6), [PR #23](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/23), [PR #33](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/33). |
| CI history and early operation | Partially supported | Initial repository history starts October 29. The workflow entered version control November 1 in `de3345e`. The visible early feature and main runs are November 2 UTC. Scoped analysis was added in `7e65692`, and the desktop job in `7187187`. Both jobs passed on PR #33 and on the sampled recent main run. These records establish early and ongoing CI with staged expansion. [Early feature run](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/actions/runs/41374638265), [Early main run](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/actions/runs/41396991828), [PR #33 run](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/actions/runs/45080844408). |
| Project board operation and steady use | Unverified | The linked organization project and its items were inaccessible with the available GitHub access. Column usage, item movement and update cadence remain unverified. Issues #20 and #28 independently show detailed owned checklists and acceptance steps. [Project #128](https://github.com/orgs/nu-cs-sqe/projects/128), [Week 7 issue](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/20), [Week 8 issue](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/28). |
| Weekly plans, progress and integration records | Verified for the recorded weeks | Weeks 5 through 8 record plans, ownership, progress, verification and follow-ups. The sampled GitHub records show staged integration work. [Weekly reports](weekly-reports.md), [Week 7 issue](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/20), [Week 8 issue](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/28). |
| Published TDD or BDD | Partially supported | Commit `7d89634` adds the deck-size expectation while its stub returns zero. `3d10c55` implements the expected 52. `a1490c5` adds invalid-input tests before `3a53ede` implements validation. The session trace and the Week 7 profile reports document observed local RED and GREEN runs. Some were published together, so the history only establishes test-first order for the sampled split increments. [Session trace](tdd/week6-session.md), [Profile chronology](weekly-reports.md#e01-profile-identity-increment--november-9), [Deck RED](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/commit/7d89634), [Deck GREEN](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/commit/3d10c55). |
| Team conventions and Clean Code | Partially supported | The configured checks and targeted instructor review provide evidence. Team-wide responsibilities and readability still need manual evidence. [Instructor review](instructor-code-review.md). |
| A-level integration of at least two major features | Verified | Real-engine and packaged-app cases exercise showdown with side-pot settlement and private seat handoff with public replay. The historical runs below preserve each test scope. [Electron scenarios](../desktop/e2e/room.test.mjs), [Bridge integration](../src/test/java/desktop/DesktopBridgeIntegrationTest.java). |
| A-level startup language choice and extension | Verified | The desktop startup walkthroughs exercise English and Simplified Chinese. The catalog test loads an additional language through resources and registration data. [Catalog tests](../desktop/test/locales.test.mjs), [Localization design](design/localization.md#river-room-desktop-resources). |
| Team attendance and individual peer evaluation | Unverified | No instructor attendance or peer evaluation records were available in the repository audit. |

## River Room request timing and showdown review

The real-engine shared-showdown walkthrough reproduced a stale handoff. The
bridge had already advanced to the next claimant, but the renderer opened the
private-seat dialog after the old request was rejected. The renderer now
refreshes the turn and opens that dialog only when the handoff succeeds.
Private request generations and the last observed turn control response
acceptance. Automatic settlement waits for the runout and tries once per hand.
The retry walkthrough uses a SQLite trigger to reject the first award, then
verifies that manual retry publishes one award for the same hand.

The Java review found duplicate account-session checks on a betting request
and repeated save guards on operations that already reject a pending settlement.
The adapter now checks the acting seat and private ownership once each. A save
always has a prepared token until publication, so a prior pending-token guard
already rejects an in-flight save. Publication keeps its dedicated save guard.
The last unshown claimant now exits the automatic disclosure loop immediately.
Dequeuing a bot and recording its choice are explicit operations. Removing
either the former deque clear or the former combined bot choice call could keep
the loop on the same seat indefinitely. Those timeouts were control-flow changes.

| Paths exercised | Evidence |
| --- | --- |
| Current, concealed, or stale private turn | Renderer handoff and replay walkthroughs, handoff gate tests, and `DesktopBridgeIntegrationTest.onlyCurrentHumanMaySeeCardsOrActAndOldSnapshotsCannotReplay` |
| Failed, saved-but-unpublished, or paid hand | SQLite retry walkthrough and `ProfiledMatchIntegrationTest.savedAsyncHandWaitsForOwnerBeforePublishing` |
| Checked river with a losing, tied, or winning bot | `ReviewedHandTest.botTablesAWinnerOrTieAndMucksALosingHand` checks the public event, visibility, winner and payout. A separate deal leaves a stronger folded hand private and still ties the two live hands. |
| Last unshown claimant, uncontested all-in, or contested all-in | `ReviewedHandTest` checks hidden winners, refund and award, river without a runout, and the fixed royal-board main and side pots |
| New board street and late voluntary show | `ReviewedHandTest` checks the deal event at street change and the hand category when exactly five cards become public |

These cases provide evidence for distinct branches and lifecycle paths. The
settlement predicate's unit test varies one decision at a time across an
unfinished hand, a paid hand, a previously attempted award and a newly eligible
award. The private read tests exercise the dealt-human guard's three
short-circuit paths with a bot, an undealt seat and a dealt human.
A complete independent basis across the measured methods remains open.
JaCoCo reports computed complexity counters
without exception edges. The Gradle class and test selections are unchanged.
Desktop bridge, Java view classes outside the selected list, and the React
renderer have integration evidence outside the measured Java denominator.

All 1,712 JUnit tests passed with no failures, errors or skips. The desktop
suite passed 30 unit tests. The renderer built successfully, and all ten
walkthroughs passed in headless Chrome against the Java engine. The five
affected walkthroughs passed again after the final queue refactor.

| JaCoCo scope | Lines | Branches | Complexity counters |
| --- | ---: | ---: | ---: |
| Selected Java classes (89) | 4,053/4,054 | 1,996/2,000 | 2,115/2,119 |
| `ReviewedHand` | 182/182 | 118/118 | 111/111 |
| `SetupSession` | 403/403 | 158/158 | 191/191 |

The final disclosure cases exercise an attempt to change a forfeited showdown
muck, a repeated private muck and a forced bot show during an ordinary
contested showdown. The selected-seat boundary test covers both invalid
endpoints before dealer selection. The sole uncovered line in the measured
scope is the `GameState` phase switch default, which no declared phase reaches.

PIT killed all 136 `ReviewedHand` mutants and all 229 `SetupSession` mutants.
The earlier run killed 105 of 136 and 221 of 231, respectively. The immediate
bot-fold case distinguishes a reveal at the fold from a reveal at the end of
the hand, which left the same final visibility but changed the public journal.

`bash ./gradlew --no-daemon build pitest` passed. Checkstyle and SpotBugs
reported no findings in their configured scopes. The XML report records
2,498 killed mutants out of 2,530, with 30 survivors, two `GameSetup` timeouts
and zero uncovered mutants. The console's 2,500 detected count includes those
two timeouts. The remaining survivors and timeouts require individual review.

Real-component integration covers showdown and side-pot settlement, as well as
private seat handoff and replay. Desktop startup exercises English and
Simplified Chinese before dealing. A catalog unit test adds a third locale
through resource data and verifies its label and message formatting.
[Locale registration](design/localization.md#river-room-desktop-resources).

## Continuous table, chosen seats and private seat sessions — November 23, 2026

The signed Apple Silicon build passed all ten Electron walkthroughs using
temporary accounts. They cover the completed table, read-only results navigation,
the default river reveal, optional ordered mucking, immediate and deferred
folded shows, private handoffs, chosen solo seating, both languages, recovery
and persistence across restart. One unequal-stack hand awards 3,800 chips
across five pots. The results screen and live journal agree on every pot.
The fixed royal-board Java deal covers ties in both the main and side pots.

The bridge regressions first reproduced ignored physical seat choices, required
passwords on ordinary private reads and a river waiting for manual decisions.
A subsequent regression showed that the outgoing seat could still read its
cards after the next human's turn began. The bridge now clears that private
authority on handoff. Tests distinguish a current session, a switch to a
different account and the 30-minute idle boundary. [Cases](bva/SessionBetting.md).

All 1,697 Java tests and 25 desktop unit tests passed. Checkstyle and SpotBugs
reported zero findings in their configured scopes. JaCoCo covered 4,047/4,054
measured lines, 1,974/2,000 branches and 2,092/2,117 complexity counters.
The desktop adapter and React renderer are outside the scoped Java coverage
list. The bridge and packaged-app tests exercise their integration.

The full Gradle build and PIT run passed. PIT recorded 2,456 killed mutants
out of 2,532, with 59 surviving, 14 uncovered and three timed out. HandHistory
killed all 32 mutants, including the two previously surviving street boundaries.
ReviewedHand killed 105 of 136. The remaining 17 survived, 13 lacked coverage
and one timed out. Uncovered mutations concern optional bot decisions, the
private deferred-show getter and the selected-seat startup entry point. The
other two timeouts occurred in GameSetup. Surviving mutations and timeouts
remain open for individual review.

## River Room showdown and replay — November 23, 2026

The Java, Electron and renderer tests exercise ordinary showdown order, the last
unshown claimant, mandatory all-in exposure, optional folded cards, ties and
public replay. A regression with unequal folded contributions reproduced a
202/204 award to tied hands. Settlement now pays 203/203 from that 406-chip pot.
A second regression reproduced a closing fold's immediate show arriving after
the automatic runout. The fold and its chosen disclosure now enter the journal
before the remaining board. [Boundary cases](bva/SessionBetting.md).

The full JUnit run passed 1,677 tests with zero failures, errors or skips.
Production and test Checkstyle and SpotBugs reports had zero findings in their
configured scopes. JaCoCo covered 3,780 of 3,785 measured lines. The existing
measurement list excludes the two new review classes and the desktop adapter.
PIT killed 2,297 of 2,341 mutations. Forty survived and four had no coverage
in PIT's selected tests. Two uncovered mutations are in `SetupSession.foldAndShow`
and two are in the `SetupView` blind getters. Desktop bridge integration tests
exercise those paths in the full JUnit run. The new survivors still need review.
The desktop unit suite passed 21 tests and the Vite production build succeeded.

The packaged Apple Silicon app passed signature verification. Seven native
scenarios passed before the closing-fold correction. In the final eight-case
run, one passed and seven timed out at the private-card handoff while macOS was
locked. With the final renderer and bundled Java engine, isolated headless
checks passed for an immediate show in English and a deferred show in Chinese,
including rewinding the public timeline. A final unlocked native run is pending.

### Instructor-style follow-up

The review separates recording a fold, tracking the river aggressor, exposing
all-in hands and resolving showdown choices. The history owns disclosure event
fields. The desktop adapter keeps password verification at the request boundary.
An empty showdown queue returns an empty Optional, and a seat must be a dealt
human before its visibility or cards are read. Hand ranking names the five-card
hand size and the complete seven-card holding size.

The added real-session case checks that `foldAndShow` rejects an unreviewed hand
and returns the accepted fold in a reviewed one. All 1,678 Java tests passed.
Checkstyle and SpotBugs reported zero findings. JaCoCo covered 3,783 of 3,787
measured lines. The style rules and measurement scope are unchanged.
The full build and PIT run succeeded. PIT killed 2,295 of 2,342 mutations,
including both previously uncovered `foldAndShow` mutations. This run
reported 45 survivors, up from 40, and two uncovered `SetupView` blind getters.
The survivors still require review.

### Rubric audit of the instructor-style follow-up

The rubric's B criteria call for killing every non-equivalent mutant and
100% cyclomatic coverage outside the stated View, Main and enum exemptions.
The A criteria additionally call for Clean Code and integration testing on at
least two main features. The scoped report covers 3,783/3,787 lines (99.89%),
1,842/1,852 branches (99.46%) and 1,957/1,967 complexity counters (99.49%).
JaCoCo complexity counters do not establish independent path coverage. The
measured-class list omits the new `ReviewedHand` disclosure rules and
`HandHistory` event logic. Existing integration tests exercise those components.
Whole-backend coverage remains unmeasured.

Compared with the preceding showdown run, both uncovered `foldAndShow`
mutations were killed and five additional survivors appeared. The two
`SetupView` blind-getter mutants have no coverage in PIT's test selection.
`DesktopBridgeIntegrationTest` asserts the actual 10/20 values in the full
JUnit suite. A direct Java application test for configured blind values
would exercise the getters within the existing PIT selection.

| Additional survivor | Review |
| --- | --- |
| `SetupSession.withdrawnClaims` always returns an empty set | Behavior-changing. Settlement can pay a stronger hand that mucked. The existing payout fixture shuffles the deck without a fixed seed. If the first seat to show already has the strongest hand, its payout assertion can pass under the mutant. |
| `updateShowdownClaims` skips checking that settlement is absent | Behavior-changing. After a hand with a muck has paid, a lawful late show could trigger comparison on the completed hand and throw after recording the disclosure. |
| `updateShowdownClaims` skips checking that a result exists | Invariant to verify. The session sets a comparison result before processing terminal disclosure. Before comparison, there are no withdrawn claims. |
| `updateShowdownClaims` skips checking that claims were withdrawn | An extra comparison produces the same result with the normal evaluator. An injected evaluator can observe an extra call. Keep this survivor pending an equivalence decision. |
| `NextHand.reseat` takes the rotating branch for the opening deal | Behavior-changing under the configured dealer contract. Cover both opening and continuation of a funded table. The existing dealer assertions in `NextHandTest` exercise the legacy `start` entry point. |

A fixed-deck check used dealer seed 17 and shuffle seed 42. One hand folded
during betting, a stronger hand mucked at showdown, and the weaker tabled
hand received all 100 chips. A folded player's disclosure was rejected while
payment was prepared and accepted after publication with the result intact.
A separate domain snapshot confirmed that removing the sole claimant for a
side pot produces the explicit settlement error. These are repeatable test
inputs for the payout and lifecycle gaps.

Four JaCoCo lines were missed. Three are the errors for starting review at the
wrong time, leaving during pending showdown decisions, and settling a pot
without a claimant. The fourth is the exhaustive `GamePhase` switch default.
The eight evaluator loop mutations change iteration bounds without changing
the sequence of five-card combinations for 5, 6 or 7 cards. They have a
concrete equivalence argument. The raw mutation count retains them.

Separate real-component integration tests exercise showdown and replay across
the desktop bridge, and settlement with retry through SQLite. These provide
evidence for two feature areas. The final unlocked packaged walkthrough, the
remaining survivor review and broader project assessment are still open.

### Deterministic settlement regressions and direct review measurement

The five-way fixture fixes the dealer and shuffle seeds and checks that at
least one mucked hand objectively outranks the tabled winner. It asserts who
receives every share of the pot and confirms the betting record contains the
actual fold. The disclosure lifecycle test checks three points: payment
prepared, saved but unpublished, and published. Only the published state
accepts the late show, which appends a public event and preserves the award.
Other regressions cover a side pot left without a claimant, funded dealer
positions, the last unshown winner, river showdown order, a canceled deferred
show, and the public journal's opening stacks, blinds and street progression.
These harden existing behavior. [Boundary cases](bva/SessionBetting.md).

The existing Gradle measured-class list now explicitly includes
`application.ReviewedHand` and `application.HandHistory` for both JaCoCo and
PIT. The rest of the class scope and PIT's test selection are unchanged.

`build pitest --warning-mode all` passed. All 1,686 Java tests passed with
zero failures, errors or skips. Checkstyle and SpotBugs reported zero findings.

| Scope | JaCoCo lines | Branches | Complexity counters | PIT killed |
| --- | --- | --- | --- | --- |
| HandHistory | 62/62 | 18/18 | 24/24 | 30/32 |
| ReviewedHand | 165/166 | 98/106 | 94/102 | 99/126 |
| Expanded scope | 4,013/4,015 | 1,962/1,976 | 2,079/2,093 | 2,434/2,500 |

PIT killed all five additional survivors from the previous audit and both
previously uncovered blind getters. Within the previous class scope, 2,305 of
2,342 mutants were killed and 37 survived. The expanded XML reports 55 survivors,
10 uncovered mutants and one timeout. PIT's console counts the timeout as a
detection, so its headline displays 2,435. The table uses XML's KILLED status.

The uncovered mutants are in ordinary bot showdown decisions. The timed-out
mutant removes the queue clear for the last unshown claimant, leaving the
mutated loop without progress. Two journal survivors can delay recording the
flop or turn until the next street. The final board sequence still matches,
so the next regression belongs at the moment each street opens.

## Funded-table continuation in River Room — November 22, 2026

The Java session requires a funded human among the next hand's funded seats.
The new account-table cases first failed when bots could continue after the
last human left or ran out of table chips. The cases passed after the roster
check, including resuming once the busted human reserved an eligible top-up.
The Electron bridge rejects the blocked deal and retains the saved balance
and history after restart. [Account table BVA](bva/AccountTable.md).

| Check | Local result |
| --- | --- |
| Gradle build and PIT | Passed with Gradle on JDK 25 and Java 11 compilation and tests. |
| JUnit and static checks | 1,665 tests passed with zero failures, errors or skips. Checkstyle and SpotBugs reported zero findings in their configured scopes. |
| JaCoCo | 3,722/3,723 measured lines, 1,804/1,808 branches, and 1,923/1,927 complexity counters. |
| PIT | 2,253/2,287 killed. All continuation mutants were killed. The same 32 survivors remain, with two uncovered mutants and zero timeout or error mutants. |
| React and packaged Mac checks | 18 unit tests and all three Electron tests passed. The React production build succeeded. |

The uncovered mutations are in the River Room branch's `SetupView` small-
and big-blind getters. JUnit exercises both through
`DesktopBridgeIntegrationTest`. The existing PIT test selection omits
the `desktop` package. Measurement and exclusions are unchanged.
The new React CI job is configured and awaits a GitHub run.

## Desktop funded-table controls — November 21, 2026

The funded desktop now takes a buy-in choice per signed-in account and shows
saved balance, reserved chips, available funds and session gain/loss above
the chip history. Existing top-up and departure actions share the funded
table's save lock with the buy-in and recovery controls.

Five focused Swing invocations first failed on missing buy-in or funding
controls. After the implementation, the bilingual cases passed. A separate
two-locale regression exposed an enabled recovery control during a paused
settlement. The save-lock change made those cases pass. The
[boundary cases](bva/SqliteProfileRepository.md) include malformed buy-ins,
balance and minimum limits, private-card handoffs and the lost commit reply.

Local `build pitest --warning-mode all` passed with Gradle on JDK 25 and
compilation and tests on Java 11.

| Check | Local result |
| --- | --- |
| JUnit | 1,655 passed, zero failures, errors or skips. |
| Checkstyle and SpotBugs | Zero style findings and unexcluded bugs. |
| JaCoCo | 3,715/3,716 measured lines, 1,800/1,804 branches and 1,918/1,922 complexity counters. |
| PIT | 2,250/2,282 killed. The same 32 survivors remain, with zero uncovered, timed-out or errored mutants. |

The measured scope and exclusions are unchanged. The native Mac walkthrough
and remote verification of this update remain pending.

## W8-E04 recovery verification — November 21, 2026

The first recovery test failed to compile before the API existed. The first
executable recovery test passed; two persistence fixtures needed to remove the
new claim table before emulating versions 1 and 2. The bilingual Swing test
preserves answers across a language change and observes the saved reward.
The [BVA](bva/RecoveryChallenge.md) maps questions to existing rule tests.
The first full local build passed 1,650 tests, and PIT killed 2,245/2,281
mutants. Targeted follow-up tests checked the answer key independently,
sampling distinct questions across challenges, timestamp order for mixed
reward/hand history and rejection of a malformed hand event.

Final local `build pitest --warning-mode all` passed in 27m33s with Gradle
on JDK 25 and compilation and tests on Java 11.

| Check | Final local result |
| --- | --- |
| JUnit | 1,652 passed; zero failures, errors or skips. |
| Checkstyle and SpotBugs | Zero style findings and unexcluded bugs. |
| JaCoCo | 3,712/3,713 lines, 1,800/1,804 branches, 1,917/1,921 complexity counters, and 86 reported classes. |
| PIT | 2,249/2,281 killed; 32 survivors; zero uncovered, timeout or error mutants. |

The four follow-up gaps were caught by the final suite. Of the 32 remaining
survivors, 28 also appeared in the E03 checkpoint. Three additional survivors
replace literal return values with the same values or change a transaction
callback result that the caller discards. These are retained in the reported
score. The fourth removes a pending-settlement guard in
`SetupSession.applyRecoveredBalance`: the unpaid-hand check still rejects
the balance change, but does not itself conceal cards. The additional
concealment effect remains a test gap. The existing GameState enum default
is the sole missed JaCoCo line. No rules or exclusions were relaxed.
Native walkthrough, peer review, and remote CI are pending.

## W8-E03 live SQLite settlement — November 21, 2026

The storage and history checkpoint `e626661` was applied to merged PR #30
(`4edda97`) without committing. The first integration test did not compile:
the SQLite adapter did not yet implement Kevin's `TableAccountStore`. The
adapter now consumes `HandCompletion`, checks expected balances inside the
transaction, and stores a canonical receipt to distinguish an identical
request from a conflicting reply. Schema upgrades retain existing accounts
and history. Tests use temporary databases and the real game session.

The English and Chinese Swing cases use the funded table and a store that
reports failure after committing. A retry pays once and shows one history
row. The tests also check that balance loading and saving stay off Swing,
top-ups preserve saved balances, and the last human's departure prevents a
bot-only hand from the UI. Explicit initial buy-in selection and recovery
are separate work.

The first combined build passed 1,640 tests and killed 2,170/2,213 mutants.
The follow-up tests vary one saved field at a time to distinguish true
receipt comparison from generic rejection. They also exercise six versus
seven accounts, invalid stacks, and a database that declines to record a
hand marker. These are mutation follow-ups after implementation.

The final local `build pitest --warning-mode all` succeeded on JDK 25
with Java 11 compilation and tests. The run resumed after an interruption;
PIT reported 21m30s for its own timed stages.

| Check | Final local result |
| --- | --- |
| JUnit | 1,645 passed; zero failures, errors or skips. |
| Checkstyle and SpotBugs | No style findings or unexcluded bugs; no analysis errors. |
| JaCoCo | 3,584/3,585 lines, 1,752/1,754 branches, 1,866/1,868 complexity counters, and 82 reported classes. |
| PIT | 2,180/2,208 killed, 28 survivors, zero uncovered, timeout or error mutants. |

The SQLite integration's remaining survivor replaces the literal `return true`
in `SqliteChipLedger.recordHand` with a true return. That branch remains
true in the original and mutant; the distinct false return on an identical
retry is tested. The other 27 survivors are in the existing table/game
components; this run does not establish their equivalence. JaCoCo still misses
the GameState enum default and reports two partially covered branches.
No quality rules or exclusions were relaxed. Native desktop verification,
peer review and remote CI for the integration remain pending.

## PR #30 settlement merge fix — November 21, 2026

Restored the save-in-progress checks in `prepareSettlement` and
`publishSettlement` and wrapped the overlong callback line. The two existing
async retry cases failed on merged commit `b1e7765`. The new
`savedAsyncHandWaitsForOwnerBeforePublishing` regression also failed before the
fix; all three pass afterward. Queued executors control ordering against a real
temporary profile catalog.

Local `build pitest` passed with JDK 17 for Gradle and JDK 11 compilation/tests:
**1,625 tests**, zero failures/errors/skips, clean Checkstyle and SpotBugs.
Shared JaCoCo records 3,416/3,417 lines, 1,700/1,702 branches,
1,796/1,798 complexity counters, 16,722/16,727 instructions and 941/941 methods.
Remaining misses are the GameState enum-default path and the account-table
branch of the asynchronous settlement fast-path decision; combined account-table
UI/storage integration is not established by these results.

PIT killed **2,096/2,123 mutants (98.73%)**, with 27 survivors and zero
uncovered/timeout/error mutants; 102 selected test classes and 16,255 executions.
Twenty-three survivors match the preceding checkpoint. Three additional
start/startProfiles/quit save-guard calls overlap the earlier pending-token guard.
The remaining `requireNoSettlementSave` card-concealment survivor is a test gap:
the queued-publication regression does not assert concealment after a fresh
reveal. It is retained in the score and is not claimed equivalent. No exclusions
or quality rules were relaxed. This follow-up is local on
`feature/week8-table-rules`; user commit/push, remote CI and peer review remain pending.

## Chosen 400+ buy-ins — November 19, 2026

The account-table API accepts chosen amounts of at least 400, with no fixed upper
cap beyond affordability and safe numeric totals. The existing default overload
uses min(1,000, balance). Funding restores at least 400 between paid hands;
smaller post-loss stacks and larger winnings remain playable. Recovery flags
cover busted balances below 400. SQLite/recovery rewards and desktop controls
remain integration work; legacy desktop entry points retain their match rules.

Final local `build pitest` exits 0 with JDK 17 for Gradle and JDK 11 compilation/
tests: **1,610 tests pass**, zero failures/errors/skips; Checkstyle and SpotBugs
have zero unexcluded main/test findings. Only documentation changed after that run.

| Scope | Verified result |
| --- | --- |
| Shared JaCoCo: 73 parent components / 76 executable classes | 3,305/3,306 lines; 1,679/1,680 branches; 1,760/1,761 complexity counters; 16,276/16,281 instructions; 915/915 methods. |
| BuyInLimits / FundingCapacity | 29/29 lines, 22/22 branches, 17/17 complexity counters and 6/6 methods; PIT 30/31 killed. |
| Shared PIT | **2,049/2,072 killed (98.89%)**; 23 survivors; zero uncovered/timeout/error mutants. 101 selected test classes; 15,757 executions across mutants. |

Fourteen survivors retain the documented baseline. The other nine are reviewed
under the current public session flow, without excluding them from the score:

| Survivor(s) | Remaining protection / interpretation |
| --- | --- |
| SetupSession pending guards in submitAction/nextHand (2) | Terminal-hand or unpaid-hand checks still reject before mutation; both paths conceal cards. |
| AccountTable departed funding guard (1) | A paid departed account has zero funding capacity and cannot satisfy the positive minimum. |
| AccountTable opening minimum predicate/branch (2) | Default and explicit buy-in validation independently enforce the minimum and affordability. |
| TableFunds constructor range branch (1) | Other reservation/available-funding range checks still reject invalid values. |
| TableRoster early total guard call/branch (2) | AccountTable's mandatory capacity check independently rejects an unrepresentable roster before publication. |
| FundingCapacity maximumAddition boundary (1) | At total == ceiling, both branches return zero. |

Overlapping guards can produce different diagnostic text. This review does not
claim independent unit-path equivalence for every internal helper. No rule,
suppression, target-test pattern or coverage exclusion was weakened. The lone
JaCoCo miss remains the existing GameState enum-default path; these are scoped
counters, not whole-backend or independent basis-path proof.

Minimum/recovery cases were observed RED before implementation; chosen-amount
cases first lacked the API and then passed. Seven review regressions were
observed RED then GREEN: payable wallet/replacement headroom, offered funding
limits, pending-save concealment, blank account IDs, released-balance refresh and
safe continuation. Numeric bounds reserve signed-64-bit room for winnings and
replacement seats; they are not a fixed game maximum. A paid table near that
technical boundary can close instead of starting an unsafe hand.

Later mutation hardening checks the selected human's actual stack and rejects
an unsafe refreshed balance. The latter was observed failing with its capacity
check removed, then passing after restoration. No synthetic test-first commits
are claimed. Tests use real game components with a fake account store and stub
hasher; actual SQLite atomicity, migration, daily recovery and Swing wiring are
not verified here. Uncommitted branch: `feature/week8-table-rules`; user commit/
push, peer review, remote CI and merge remain pending.

## Week 8 account table APIs — November 18, 2026

Local `build pitest` exits 0 using JDK 17 for Gradle and JDK 11 for compilation/
tests. **1,568 tests pass**, zero failures/errors/skips; Checkstyle and SpotBugs
have zero unexcluded main/test findings. Changes remain uncommitted on
`feature/week8-table-rules`; remote CI and peer review are pending.

| Scope | Verified result |
| --- | --- |
| Shared JaCoCo: 71 parent components / 74 executable classes | 3,249/3,250 lines; 1,639/1,640 branches; 1,723/1,724 complexity counters; 15,957/15,962 instructions; 898/898 methods. |
| Five new parents, including AccountChange: six executable classes | 193/193 lines, 76/76 branches, 94/94 complexity counters and 56/56 methods; PIT **99/99 killed**. |
| Shared PIT | **1,993/2,012 killed (99.06%)**; 19 survivors; zero uncovered/timeout/error mutants. 98 selected test classes; 15,118 executions across mutants. |

Fourteen survivors are the existing documented baseline. Five new survivors
remove `SetupSession.requireNoPendingSettlement` calls in `submitAction`,
`nextHand`, `addTableChips`, `refreshTableBalances` and `endTable`. A pending
payment already has a terminal unpaid hand: action/continuation/paid-hand guards
still reject these operations before mutation if the extra check is removed.
Diagnostic text may differ. The explicit lifecycle checks remain; these mutants
remain counted as survivors. No rule, suppression or exclusion was weakened.

Initial departure/API regressions were observed failing before their fixes.
Later mutation checks added missing boundaries for zero-balance account selection,
failed-start privacy, retained top-ups, safe replacement funding, token identity
and turn revisions. They are additional hardening, not claimed as initial TDD
cycles or fabricated test-first commits. Read-only review also checked the
prepared-save lifecycle and unpaid departure reservations.

The integration tests use real betting/dealing/settlement with a fake account
store and stub password hasher. They verify the application contract, not SQLite
transactions, migration, recovery limits or the new desktop flow. Those remain
Estella's adapter/integration work. Existing legacy entry points retain their
match rules. The lone JaCoCo miss is the existing GameState enum-default path;
coverage counters do not establish independent basis-path or whole-backend proof.
Only documentation changed after the final verification run.

## W8-E03 account settlement storage and history — November 19, 2026

The first integration tests failed to compile before the storage handoff and
ledger API existed. The initial executable tests and the full build passed
after implementation and formatting corrections. They cover schema upgrade,
atomic balance/history/statistics updates, replay and rollback. The bilingual
Profiles screen cases use saved hand records from the storage API; live play
still needs the W8-I01 contract agreement and W8-K04 inputs.

The first full run passed 1,574 tests and killed 1,928/1,944 mutants. Two new
survivors revealed missing assertions: a missing account must report the
domain-specific storage error, and a write that would leave orphaned history
must fail and roll back. The additional assertions were added after this run.

Final `build pitest --warning-mode all` passed in 14m29s (Gradle on JDK 25,
tests on Java 11).

| Check | Final local result |
| --- | --- |
| JUnit | 1,575 passing; zero failures, errors or skips. |
| Checkstyle and SpotBugs | Zero style findings and unexcluded bugs. |
| JaCoCo | 3,130/3,131 lines; 1,555/1,556 branches; 1,658/1,659 complexity; 76 reported classes. |
| PIT | 1,930/1,944 killed; no uncovered, timeout or error mutants. |

The 14 remaining mutants match the baseline by class, method, descriptor,
operator and description. The existing GameState enum default remains the
sole missed JaCoCo line. New backend types are measured; prior selectors and
exclusions are unchanged. The run verifies the storage and history increment;
the W8-I01 agreement, live settlement integration, native walkthrough and
remote review remain pending.

## W8-E02 SQLite account migration — November 18, 2026

Based on W8-E01 commit `6d8aa08`. The new migration cases first failed at
compilation because the SQLite adapter and balance API did not exist. They
then passed against the real driver and temporary catalogs. The checks cover
v2 and v3 imports, existing passwords, preservation of completion IDs,
reopening a zero balance, invalid storage and atomic rollback. The two
language desktop tests use SQLite through the existing profile flow.
See [SQLite BVA](bva/SqliteProfileRepository.md).

The first full build passed 1,559 tests and PIT killed 1,871/1,886 mutants.
One new mutant skipped the unsupported-schema check. The first test used
an existing table, which still caused initialization to fail when that
check was removed. The new boundary starts with an empty database bearing
an unsupported version, so it can distinguish version rejection from a
table conflict. This was mutation hardening after implementation.

Before the style review, `build pitest --warning-mode all` passed in 13m52s with Gradle on
JDK 25 and tests on Java 11. SQLite is included in both measurement tools;
no prior selectors, mutators, rules or exclusions were removed.

| Check | Final local result |
| --- | --- |
| JUnit | 1,560 passed; zero failures, errors or skips. |
| Checkstyle and SpotBugs | Zero style findings and unexcluded bugs. |
| Shared scope | 67 parent components / 70 reported classes. |
| JaCoCo | 3,008/3,009 lines, 1,529/1,530 branches, 1,605/1,606 complexity, 14,595/14,600 instructions, 835/835 methods and 70/70 classes. |
| PIT | 1,872/1,886 killed. The same 14 survivors match by class, method, descriptor, operator and description; zero uncovered, timeout or error mutants. |

The new schema-version case killed the additional survivor. The only missed
JaCoCo line remains the existing GameState enum default; the baseline survivor
equivalence and independent basis-path review remain separate. Native desktop
review, remote CI and merge are pending.

The style review separated the version check from the legacy import, named the
uninitialized schema version, and wrapped the rollback test's long line.
The full build and PIT then passed in 13m38s: 1,560 tests, clean Checkstyle
and SpotBugs, 3,012/3,013 measured lines, and 1,874/1,888 mutants killed.
The same 14 baseline survivors remain, with none in the SQLite adapter.
The measurement scope is unchanged.

## W8-E01 background settlement — November 18, 2026

Based on merged main `9932bcd`. The English and Chinese blocked-save
regressions first failed because Swing could not process a second event while
the save was paused. Both passed after moving the storage operation off the
event thread. Queued-executor integration cases verify publication on the
owner, duplicate/restart/departure rejection, worker submission failure and
retry before or after a durable write. The profile screen tests also verify
localized status, disabled controls and retry. See the
[session BVA](bva/SetupSession.md#week-8-background-settlement) and
[profile screen BVA](bva/ProfileScreen.md#w8-e01-background-settlement).

Full `build pitest --warning-mode all` passed in 12m53s (Gradle JDK 25,
Java 11 compilation/tests). No measurement selectors or exclusions changed.

| Check | Final local result |
| --- | --- |
| JUnit | 1,551 passed; zero failures, errors or skips. |
| Checkstyle / SpotBugs | Zero style findings or unexcluded bugs; no analysis errors or missing classes. |
| Shared scope | 66 parent components / 69 reported classes; the additional class is the nested pending settlement. |
| JaCoCo | 2,926/2,927 lines; 1,515/1,516 branches; 1,578/1,579 complexity; 14,241/14,246 instructions; 815/815 methods; 69/69 classes. |
| PIT | 1,837/1,851 killed; the same 14 baseline survivors; no uncovered, timed-out or error mutants. |

The sole missed line is the existing GameState enum default. Survivor
class, method, descriptor, operator and description match the prior baseline.
Survivor equivalence and basis-path review remain separate. The native
desktop walkthrough, peer review, remote CI and merge remain pending.

## I07 fixed-stack continuation checkpoint — November 16, 2026

This historical run covered paid ownership, retained stacks after departure,
and heads-up continuation with stable seats, blinds and chip totals. Four
fixed-stack result-query invocations first failed against the previous source,
then passed after the query changed. Six bilingual UI boundaries first produced
four terminal-label failures and two continuation passes, then all six passed
after resource updates. A 120-chip fixture exercised an injected configuration.
The 98-case focused result, continuation and localization run passed.

The first full build/PIT run passed 1,545 tests in 12m57s but exposed one new
non-equivalent getMatchWinner settlement-guard survivor: PIT killed 1,823/1,838,
with 15 survivors. A before-start Optional.empty regression was added to preserve
legitimate absence. The final PIT report names that test as killing the guard
mutation. This is mutation hardening after implementation, preserving the actual
RED/GREEN history. No synthetic test-first Git commits are claimed.

Final build pitest --warning-mode all exits 0 in 12m03s with JDK 25 for Gradle
and Java 11 compilation/tests. No source/test/configuration changes follow that
run. Existing BVA/design/rules/weekly records are updated; the uncommitted I03
work is preserved on fix/quit-eligibility in the original checkout.

| Check | Final local result |
| --- | --- |
| JUnit | **1,546 passing**, zero failures/errors/skips; 12 new invocations above the 1,534 I03 checkpoint. |
| Checkstyle | Zero main/test/seat-rule findings; production/new test lines remain within 100 characters. |
| SpotBugs | Zero unexcluded main/test findings, analysis errors or missing classes. |
| Shared scope | **66 parent components / 68 reported classes**, unchanged. |
| JaCoCo | **2,893/2,894 lines**, 1,509/1,510 branches, 1,567/1,568 complexity counters, 14,126/14,131 instructions, 807/807 methods and 68/68 classes. |
| PIT | **1,824/1,838 killed**, the same 14 documented baseline survivors; zero uncovered/timeout/error mutants; 97 selected test classes and 13,459 executions across mutants. PIT lines 2,898/2,902. |

Survivor class/method/signature/operator/description identities match the I03
baseline. All mutations in the new winner query are killed. The existing
GameState.hasBoardSizeForPhase enum-default miss and baseline equivalence/basis-
path interpretation remain review limits. No measured class, test pattern,
operator, rule, suppression or exclusion was added/removed for these results.

I03/I07 changes are uncommitted on the same local branch. User commit/push,
Kevin's contract review, remote CI and merge acceptance remain pending. Issue
checkboxes remain open under those gates; the scoped result establishes no
whole-backend or individual-grade conclusion.

## I03 immediate quit-as-fold correction — November 16, 2026

At the user's request, fix/quit-eligibility starts at the verified integration
commit 4a9d9d3 in the original checkout. E05/E06 integration was committed/pushed
and submitted in PR #26. This local I03 correction is separate uncommitted work.

An active human's quit now records an immediate automatic FOLD, including an
out-of-turn departure. BettingEngine.foldDepartingPlayer preserves another pending
actor, fixed seats, cards, stacks and commitments while removing the quitter's
eligibility and decision/raise rights. Ordinary apply retains its turn validation.
The session prepares the resulting hand/model successors, records the accepted
fold, then publishes it. Recording failure leaves eligibility, log, learning and
revision retryable. All-in departures retain their current-hand payout eligibility.
Already-folded players and resolved hands receive no duplicate/retroactive fold.

BotTable.afterDeparture uses the automatic PublicDecision overload without legal
choices or position. Automatic folds never train strategic preferences. Captured
bot results become stale on successful departure; fresh pot/equity scenarios
exclude the folded quitter. The compatibility playDepartureTurns API retains its
private-handoff refresh, with an empty action list after immediate departure.
Obsolete queued-fold branches were removed from the passive helper and session.

Three DEP-08/09/10 assertions failed against the original source before production
changes. Fourteen DB invocations then failed compilation against the missing
engine operation. Their first executable run exposed an incorrect identity-based
Player-list assertion in the new test; it was corrected to compare ownership
fields. The new UI case also required its List import. These test-development
corrections are recorded explicitly, not claimed as additional behavior REDs.
The complete focused rerun passed 58 invocations. Four additional boundary/UI
invocations supplement the initial 17 engine/session invocations, for 21 new cases.
Existing all-in, privacy, invalid/repeated departure and next-hand cases remain.
No synthetic Git TDD commits or scratch reports were created.

Final build pitest --warning-mode all exits 0 in 12m17s with JDK 25 running
Gradle and Java 11 compilation/tests. No source/test/configuration changes follow
that run. Checkstyle rules, SpotBugs filters, measured classes, mutators and test
patterns are unchanged.

| Check | Local result |
| --- | --- |
| JUnit | **1,534 passing**, zero failures/errors/skips; 21 new invocations above the 1,513 integration baseline. |
| Checkstyle | Zero main/test/seat-rule findings; production and new test lines remain within 100 characters. |
| SpotBugs | Zero unexcluded main/test findings, analysis errors or missing classes. |
| Shared scope | **66 parent components / 68 reported classes**, unchanged. |
| JaCoCo | **2,892/2,893 lines**, 1,509/1,510 branches, 1,566/1,567 complexity counters, 14,119/14,124 instructions, 806/806 methods and 68/68 classes. |
| PIT | **1,822/1,836 killed**, the same 14 documented baseline survivors; zero uncovered/timeout/error mutants; 97 selected test classes and 13,412 executions across mutants. PIT lines 2,897/2,901. |

Survivor class/method/signature/operator/description identities match the previous
integration report. GameState.hasBoardSizeForPhase's unreachable enum-default line
remains the sole JaCoCo miss. Survivor equivalence and independent basis-path
interpretation retain their existing review limits. Reduced branch/line counters
reflect removal of the obsolete queue paths; measurement scope is unchanged.

I03 is verified locally and awaits user commit/push, Kevin's contract review,
remote CI and merge acceptance. The rules/design/BVA now describe immediate
active departure and preserve the historical queued-fold checkpoint. I07's
fixed-stack continuation review remained open at this checkpoint. No issue
checkbox or whole-backend completion is claimed by this local fix.

## E05/E06 and profile integration — November 16, 2026

The original checkout on feature/bot-integration combines bot head 02a8e1a with
main 69aae47, which includes merged PR #25. The user initiated the merge. Its
ten unresolved files included Gradle and Java conflict markers; the initial
build failed with ten Kotlin-script compilation errors at build.gradle.kts:203.
The resolved source retains both profile persistence and adaptive bot behavior.

SetupSession initializes bot tables/revisions for both start shapes and saves
completed profile deltas before publishing paid state/statistics. GamePanel keeps
profile selection/names, private handoffs, busy controls, bounded background bots,
explicit bot retry and close cleanup. PlayStats keeps the named action/c-bet
helpers with the persisted Statistic counters. StatisticsPanel keeps profile
lifetime rows, escaped names and separate column-width configuration.

After conflict resolution, six profile UI invocations failed because they assumed
synchronous bot completion. The existing deterministic UiBotFixtures scheduler
now also supports profile-bound presentation tests. Their assertions remain.
PM-C07-before/after and PM-C08 add three passing integration regressions using
real seeded Monte Carlo/policy/engine decisions and temporary profile catalogs.
They cover save failures before/after durable replacement, idempotent retry,
continued bindings/learning, reload and stale-turn rejection after restart.
These are merge regressions added after resolution, not new feature RED cycles.
The focused profile/UI rerun passed all 34 invocations. Historical TDD is retained.

Final build pitest --warning-mode all exits 0 in 12m23s with JDK 25 running
Gradle and Java 11 compilation/tests. No source/test/configuration changes follow
that run. The shared scope is the union of both branches; no class, test pattern,
mutator or rule was dropped and no new static-analysis exclusion was introduced.

| Check | Combined local result |
| --- | --- |
| JUnit | **1,513 passing**, zero failures/errors/skips, including three new regression invocations. |
| Checkstyle | Zero main/test/seat-rule findings. |
| SpotBugs | Zero unexcluded main/test findings, analysis errors or missing classes. |
| Shared scope | **66 parent components / 68 reported classes**, including profiles, persistence/security, policy and live bot coordination. |
| JaCoCo | **2,894/2,895 lines**, 1,521/1,522 branches, 1,566/1,567 complexity counters, 14,107/14,112 instructions, 800/800 methods and 68/68 classes. |
| PIT | **1,822/1,836 killed**, the same 14 documented baseline survivors, zero uncovered/timeout/error mutants; 96 selected test classes and 13,406 executions across mutants. |

Survivor class/method/signature/operator/description identities match the previous
main/profile report, accounting for the already-documented rankFive to
ranksByMultiplicity helper rename. GameState.hasBoardSizeForPhase's unreachable
enum-default line remains the sole JaCoCo miss. Survivor equivalence/acceptance
and independent basis-path interpretation remain review gates.

Conflict resolutions are staged for user review and commit. Combined remote CI,
peer review, native desktop walkthrough and broader bot evaluation remain pending.
Departure eligibility and fixed-stack continuation remained open at this checkpoint.
Original instructor feedback and all dated measurement history are preserved.

## Main/profile integration repair — November 16, 2026

Branch fix/main-integration starts from merged main c05a306, in the original
checkout. PR #21, #22 and #24 are present; E05/E06 still belong to the separate
feature/bot-integration branch. The initial build pitest failed while compiling
build.gradle.kts: a duplicated localization selector lacked a comma. After fixing
that and the dangling Objects call, the team tests passed but Checkstyle reported
19 production null-flow violations across ten profile-related classes.

The repair retains the union of all configured class/test scopes. It removes
internal null guards under the instructor's non-null reference/element contract.
Required catalog strings are normalized using Optional.ofNullable only at Java
Properties.getProperty, before model construction. Missing bound-player deltas
are validated by key presence before any profile save. Blank/range, foreign and
unknown identities, duplicates, statistics consistency, overflow, malformed
encoding, corrupt catalogs and storage failures remain business rejections.

Focused storage/lifetime verification first exposed 13 behavioral failures after
removing the guards: missing fields and hand deltas had reached models as null.
The boundary fixes pass all 65 focused invocations, including 24 new ST-C06 cases
that each remove a required catalog field and verify the damaged file is retained.
This is an observed local regression RED/GREEN sequence, not fabricated Git TDD
commits. Fifteen null-only invocations are retired; null-only assertions in mixed
cases are also removed. Required business assertions remain. BVA preserves the
retired IDs and historical measurements; it does not reuse them for new cases.

CompletedStatistics names separate counter-shape, nonnegative-counter and ID-text
validations. StatisticsProperties separates counter decoding from completion-ID
decoding. ProfileService separates bound-delta validation, local record preparation
and atomic publication. These helpers name distinct responsibilities. New source
and test lines remain within 100 characters. Checkstyle's null-flow, formatting
and scoped magic-number rules are unchanged; no new SpotBugs filter, suppression
or measurement exclusion was added. The README now states the actual 56-parent
measurement scope rather than the earlier 44-parent profile checkpoint.

Final build pitest exits 0 in 7m20s on the final source (JDK 25 for Gradle,
Java 11 compilation/tests). No source/test/configuration changes followed that run.

| Check | Final local result |
| --- | --- |
| JUnit | **1,396 passing**, zero failures/errors/skips: 24 catalog-field invocations added and 15 null-only invocations retired from the merged baseline. |
| Checkstyle | Zero main/test/seat-rule findings; rules unchanged. |
| SpotBugs | Zero unexcluded main/test findings, analysis errors or missing classes; filters unchanged. |
| Scope | **56 parent components / 57 reported classes**, retaining all prior targets and test patterns. |
| JaCoCo | **2,265/2,266 lines**, 1,279/1,280 branches, 1,236/1,237 complexity counters, 11,288/11,293 instructions, 591/591 methods and 57/57 classes. |
| PIT | **1,400/1,414 killed**, 14 identical baseline survivors, zero uncovered/timeout/error mutants; 81 selected test classes, 10,552 executions across mutants. |

The only JaCoCo miss is the existing unreachable GamePhase default in
GameState.validateCards. All mutations in the repaired profile/storage components
are killed; the survivors belong to unchanged baseline engine/evaluator/hash
cases described below. Their equivalence/course acceptance remains a review gate.
These scoped counters do not establish independent basis-path or full-backend
completion. Historical measurements retain their original scopes and dates.

Commit/push, new remote CI and peer review remain pending. This repair does not
claim E05/E06 live-bot integration, the queued-departure or fixed-stack
continuation fixes, password-character policy changes or full-backend completion.

## PR #21 profile verification — November 15, 2026 (historical)

This run combined `feature/player-profiles` (`e0d6015`) with main (`bda4954`).
That checkpoint measured profiles and betting together.

Six conflicting files combine the two implementations: README, build.gradle.kts,
this quality history, GamePanel and the English/Chinese resources. Automatically
merged application code was also reviewed. Authentication, atomic lifetime saves,
failed-save retry and profile bindings are retained alongside full betting,
legal wager limits, departures and carried match statistics.

The merged scope is 44 parent components / 45 reported classes. PIT selects
persistence/security tests as well as main's domain/application/integration tests;
no profile test set or main measurement component was discarded. No new
suppression, exception or metric exclusion was added for the merge.

| Recorded check | Result |
| --- | --- |
| Full JUnit | **1,362 passing**, zero failures/errors/skips. |
| Checkstyle | Zero main/test findings. |
| SpotBugs | Zero unexcluded findings, analysis errors or missing classes. |
| JaCoCo | **1,888/1,889 lines**, 1,269/1,270 branches, 1,093/1,094 complexity counters, 9,566/9,571 instructions, 453/453 methods, 45/45 classes. |
| PIT | **1,233/1,247 killed**; the same 14 documented main-branch baseline survivors; zero uncovered, timeout or error mutants. |

The one JaCoCo miss remains GameState.validateCards's unreachable GamePhase
default. Survivor identities are the same evaluator/hash, betting-boundary,
GameConfig and GameState cases already reasoned about below. Their course/peer
acceptance is not inferred from the successful Gradle exit.

UI-AC16-en/zh first produced two expected RED failures because Leave match was
still enabled during gated background profile work. After adding quit, wager and
wagerTarget to the existing busy-control list, both pass. No new null guards or
null-input cases were introduced. PM-C06 is a passing merge integration regression:
a profile's three-bet and flop c-bet produce exact completed rates, a 720-chip pot
settles with 6,000 chips conserved, restart reloads identical lifetime statistics,
and the next hand retains bindings without another completion increment.

Final formatted sources pass build pitest in 7m44s, using JDK 25 for Gradle
and Java 11 compilation/tests. PIT selects 66 test classes and records 10,869
executions across mutants; that count is separate from the 1,362 JUnit cases.
No production, test or configuration changes followed that final run.

At that checkpoint, native UI checks, bot/profile integration, and the
queued-departure and fixed-stack continuation findings were still open.

## Password length revision — November 10, 2026

New-account registration enforces 6–15 characters; sign-in verification accepts
6–128 to retain existing account access. The English/Chinese hint uses plain
wording with two lines. Credentials, the hashing algorithm/work factor and the
catalog schema are unchanged. No existing user account is rewritten.

Revised password tests ran against the prior policy: **15 cases, 7 expected
failures and 8 passes**, then all 15 GREEN after implementation. Boundaries are
5/6/7/14/15/16 for registration, with independent existing 28/127/128-character
credential vectors for compatibility and the existing 129-character rejection.
Fixtures creating new accounts use a valid 14-character sample.

Final `build pitest` exited 0 in 4m19s: **1,223 tests passing**, zero failures/
errors/skips; Checkstyle/SpotBugs clean with the existing documented exceptions.
Pbkdf2PasswordHasher kills **24/24 mutants** and covers all 39 lines, 18 branches,
18 complexity counters, 154 instructions and 9 methods. Shared scope remains
27 components: 1,184 lines, 597 branches, 562 complexity counters, 6,027 instructions
and 262 methods, all with zero missed counters. Shared PIT is **636/643 killed**,
seven unchanged baseline survivors, no uncovered/timeout/error mutants; 35 selected
test classes and 3,780 executions. Its synthetic-cleanup line marker remains
1,189/1,190. Earlier results below are historical checkpoints.

These are local results; no separate Git TDD commits, commit/push, new remote CI
or peer/merge completion is claimed. Standard Gradle outputs only; no extra
scratch report, second checkout or real user profile data was created.

## Desktop profile flow — November 10, 2026

The production Main now opens a profile-enabled GamePanel. Registration, password
sign-in, an ordered 1–6-human next-match roster, saved lifetime statistics and
profile names are connected to the existing backend. Account work uses SwingWorker;
UI callbacks stay on the event thread and a busy guard serializes profile writes
against match mutation. Only authenticated profiles reveal lifetime rows.

The original 20 UI cases ran RED with 20 expected UnsupportedOperationException
failures against a compiling constructor stub. First implementation passed 19/20;
the remaining test exposed saved-profile selection being reset on refresh. After
fixing selection, all original cases passed. Three later regressions cover markup-
like names, password cleanup after provider failure and same-name account choices.
These are local observations, not separate RED/GREEN Git commits.

| Check | Result |
| --- | --- |
| Full JUnit suite | **1,219 passing**, zero failures/errors/skips; 23 new profile-screen cases above E02's 1,196. |
| Profile integration | Real registration/login → actions → settlement → lifetime display → restart/login → fresh match; temporary file catalogs and real hashing in the end-to-end case. |
| UI failure/privacy checks | Wrong passwords, duplicate/full rosters, invalid registration, corrupt storage, retryable settlement, nonblocking gated hashing, cleared password arrays, hidden card handoffs, markup-like names and both-language layouts. |
| Static analysis | Zero main/test Checkstyle violations and unexcluded SpotBugs findings; zero analysis errors/missing classes. One exact SE_BAD_FIELD exception is added for ProfilePanel.model, consistent with the existing live GamePanel exceptions. No broad suppression or disabled check. |
| JaCoCo | Same 27-component backend scope: 1,184 lines, 597 branches, 562 complexity counters, 6,025 instructions and 262 methods; zero missed counters. Swing/Main retain the documented view exemption; their tests/static checks still run. |
| PIT | **636/643 killed (98.91%)**, seven unchanged baseline survivors, zero uncovered/timeout/error mutants; 35 selected test classes and 3,748 executions across mutants. PIT line coverage 1,189/1,190 retains the documented synthetic-cleanup marker. |

Final `bash ./gradlew --no-daemon build pitest` exited 0 in 4m15s with the above
results, using JDK 25 for Gradle and JDK 11 compilation/tests. No subsequent
production/test/configuration changes were made; only documentation was updated.

The explicit serialization exception is reviewed against the code boundary:
ProfilePanel.model contains services, authenticated handles and view callbacks;
Main never serializes/restores Swing screens. FileProfileRepository writes only
explicit identity, credential and completed-statistics properties. Persisting the
UI graph or marking its live fields transient is not a supported restore design.
The filter matches only ui.ProfilePanel / model / SE_BAD_FIELD. Peer review of this
exception and the full changes remains pending.

Headless layout checks pass at 1,100×740 in English/Chinese. A native desktop
preview used an empty temporary profile path, but the computer-use native bridge
failed to initialize, so no screenshot/native-focus walkthrough could be verified.
The preview process was closed; no accounts were created there. A native visual
check on the user's Mac remains pending. All automated file tests use JUnit TempDir;
no real user profile data, scratch report, second checkout, commit or push was made.

The old baseline survivor reasoning and broader backend measurement gaps remain
visible below. Local UI completion does not mark review, remote CI or merge gates
Done and does not establish whole-project A compliance.

## Completed lifetime statistics — November 9, 2026

E02 is verified locally on `feature/player-profiles`, building on the user's E01
commit `e1a96f4`. Password-verified handles bind human profiles to stable match
players. Completed raw counters/history survive restart, while new matches reset
stacks and match totals. Settlement publishes paid chips and match totals only
after saving all bound human lifetime deltas atomically. Both failure before save
and a reported failure after a durable save are retryable without double counting.
Existing V2 accounts load unchanged and upgrade to V3 on a successful save.

TDD observed **28 expected RED failures → 28 GREEN** for snapshots/restoration,
then **11 expected RED failures → 11 GREEN** for service/storage/application
integration. Thirteen additional boundary, independent-validation, privacy and
failure-window regressions improve the final tests; they are not claimed as
additional feature RED cycles. These are local executions, not separate Git
RED/GREEN commits. No E02 commit, push, new remote CI or peer approval is claimed.

| Check | Recorded result |
| --- | --- |
| Latest full JUnit suite | **1,196 passing**, zero failures/errors/skips; 52 cases above E01's 1,144. |
| Checkstyle / SpotBugs | Zero main/test violations or unexcluded findings, analysis errors or missing classes; no new suppression. |
| Four new executable components | CompletedStatistics, AuthenticatedProfile, ProfileMatch and StatisticsProperties: **124 lines, 72 branches, 61 complexity counters, 688 instructions and 25 methods**, zero missed counters. Statistic is an enum with stable storage keys. |
| New-component PIT | **72/72 killed**. Across all ten new/modified E02 components, **331/331 killed**; none surviving or uncovered. |
| Shared JaCoCo scope | **27 parent components / 27 reported classes**; **1,184 lines, 597 branches, 562 complexity counters, 6,025 instructions and 262 methods**, zero missed counters. The former private PlayStats counter enum is replaced by the separate exempt Statistic enum. |
| Shared PIT | **636/643 killed (98.91%)**, seven pre-existing survivors, zero uncovered/timeout/error mutants; 35 selected test classes, 3,794 executions across mutants. |

`build pitest` passed on the final production code with 1,195 tests. The additional
PM-C05 durable-save/error/retry regression was then added without changing
production code; the final `build` passed all 1,196 tests in 48 seconds. Runtime:
Gradle 9.7.1 on JDK 25, JDK 11 compilation/tests. PIT was not repeated after that
pure test addition. PIT line coverage is **1,189/1,190**; its one uncovered marker
remains the existing generated finally-cleanup brace in Pbkdf2PasswordHasher,
as explained in the earlier checkpoint below. JaCoCo reports zero missed counters.

Mutation testing drove explicit assertions for rejected-start card concealment,
six versus seven distinct accounts, negative counters independent of other
validation, and duplicate nonparticipant history. Redundant size bounds and
non-contractual ID sorting were removed without weakening catalog validation.
Malformed large counts still fail promptly on the first missing/duplicate record
without allocating by the declared count. The seven unchanged baseline survivors
remain the six evaluator cases and Card.hashCode constant return reviewed below.
The hash-contract interpretation still needs instructor acceptance.

BVA maps constructors, raw snapshots, restoration, authentication ownership,
missing/corrupt storage, V2/V3 upgrade, multiple-account atomicity and retries.
Real integration flows include setup → actions → showdown → save → restart/new
match and setup → finished hand → failed save → retry/payment. Isolated tests
inject repositories/hashers and exercise the codec/model directly. No saved
unfinished match, real user data, extra scratch report or second checkout was
created. The Swing profile/login/lifetime screens remain pending.

The remaining betting/state classes are still outside this measurement scope;
these results do not establish whole-backend coverage or independent basis-path
execution. Coverage counters and BVA/path review are reported separately. E02/I04
stay unchecked pending commit, remote CI, peer review and merge.

## Local password accounts — November 9, 2026

This historical E01 checkpoint implements registration, public identity selection, password verification
and local identity/credential persistence on `feature/player-profiles`. A new
service/repository instance reopens the same accounts with unchanged IDs. At this checkpoint the Swing account screens, authenticated match binding and E02
storage were pending. The later E02 increment adds binding and completed lifetime
persistence; the Swing account screens remain pending. No unfinished match is serialized.

TDD observations: the original model ran 20 expected RED failures then 20 GREEN;
metadata service/storage ran 53 RED then 53 GREEN; the password-account suite ran
84 expected RED failures then the same 84 GREEN. Subsequent regression cases
check early rejection before derivation, registration without a valid password,
and five damaged/old credential catalogs. These are observed local runs, not
separate RED/GREEN Git commits. No commits/pushes or extra scratch reports were
created. New remote CI, partner review and merge remain pending.

Latest local verification uses `bash ./gradlew --no-daemon build pitest` with
JDK 25 for Gradle and the installed JDK 11 compilation/test toolchain.

| Check | Recorded result |
| --- | --- |
| Full JUnit suite | **1,144 passing**, zero failures/errors/skips; 111 cases above the 1,033-test baseline, including 20 profile-model and 91 account/storage/security cases. |
| Checkstyle / SpotBugs | Zero main/test violations or unexcluded findings; no new exclusions or suppressed failures. |
| New profile/account JaCoCo scope | Six executable classes: **165 lines, 64 branches, 75 complexity counters, 693 instructions and 43 methods**, zero missed counters. |
| Profile/account PIT | **86/86 mutants killed**, including invalid-input provider rejection and sensitive-buffer cleanup; none uncovered or surviving. |
| Shared measured scope | **23 parent components**, 24 reported classes including the existing PlayStats counter enum; **992 lines, 495 branches, 466 complexity counters, 5,076 instructions and 217 methods**, zero missed counters. |
| Shared PIT | **526/533 killed (98.69%)**, seven pre-existing survivors; zero uncovered, timeout or error mutants. |

PIT reports line coverage **992/993**. Its single uncovered marker is the closing
`finally` brace in Pbkdf2PasswordHasher.matches (line 74), representing a generated
exception-cleanup path. For valid immutable PasswordHash values, Base64 decoding
and MessageDigest.isEqual inside that try block have no ordinary exceptional
input; provider failures occur before entering it. JaCoCo filters compiler-
generated cleanup and reports all 39 hasher lines covered. This difference is
reported rather than relabeled as 100% PIT line coverage. Both success/failure
cleanup of the provider specification and successful derived-buffer cleanup are
directly asserted by isolated tests.

The seven existing survivors remain the six evaluator cases and Card.hashCode
constant return documented below; instructor acceptance of the hash-contract
interpretation remains pending. Interfaces ProfileRepository, PasswordHasher and
PasswordDeriver contain no executable default methods. Remaining betting/state
components are still outside the configured measurement scope. Covered counters
do not independently prove basis-path execution or whole-backend compliance.

The new component path review maps each constructor validation partition,
service lookup/authentication success and rejection, read/write/provider failure,
file absence/corruption/atomic-replacement failure, password-length/Unicode bounds
and cleanup behavior to the corresponding per-class BVA. File/crypto integration
tests use real components; service orchestration and sensitive-buffer lifecycle
tests inject controlled collaborators as required by the instructor's review.

## PlayerProfile model — November 9, 2026

This historical first E01 checkpoint added only immutable identity/display text.
The later account increment above implements the services and local persistence.
The existing match Player remains unchanged.

Local TDD ran the same 20 PlayerProfileTest cases before and after implementation:
RED **20 expected failures**, then GREEN **20 passing**, zero errors/skips.
These are local executions, not separate RED/GREEN Git commits. No commits or
pushes were made; new remote CI and peer review remain pending.

Final verification: `bash ./gradlew --no-daemon clean build pitest`, using JDK 25
for Gradle and the JDK 11 compilation/test toolchain, exited 0 in 53 seconds.

| Check | Recorded result |
| --- | --- |
| Full JUnit suite | **1,053 passing**, zero failures/errors/skips; 20 PlayerProfile cases added to the 1,033-test baseline. |
| Checkstyle / SpotBugs | Zero main/test violations or unexcluded findings, analysis errors or missing classes; no suppression added. |
| PlayerProfile JaCoCo | **9/9 lines, 4/4 branches, 6/6 complexity counters, 32/32 instructions, 4/4 methods**; zero missed counters. Tests cover null, blank and nonblank paths for each constructor argument. |
| PlayerProfile PIT | **5/5 mutants killed**; no survivors or uncovered mutants. |
| Shared measured scope | **18 parent components**, 19 reported classes including the existing PlayStats counter enum; 836 lines, 435 branches, 397 complexity counters, 4,415 instructions and 178 methods, all with zero missed counters. |
| Shared PIT | **445/452 killed (98.45%)**, seven survivors; no uncovered, timeout or error mutants. PIT line coverage 832/832; 23 selected test classes and 3,190 test executions across mutants. |

The five new mutants are all killed. The seven survivors remain four bestFive
loop boundaries, the equal-count rank comparator, the score-loop boundary and
Card.hashCode's constant return, as reviewed below. The hash-contract interpretation
still needs instructor acceptance. Scope is explicit: other betting/state
components remain unmeasured, and covered counters alone do not establish
independent basis paths or whole-backend coverage. The November 6 results below
retain their historical 17-component scope.

## Readability review of current and earlier work — November 15, 2026

Reviewed the active branch across its 80 production Java source files, including
previous setup, betting/state, statistics, evaluation, application, bot,
localization and UI work. The changes preserve rules and public APIs. Required
BVA notes explain the responsibilities; the [design record](design/README.md#readability-review-across-current-and-earlier-work--november-15)
distinguishes cohesive algorithms from mixed-purpose methods.

The final `build pitest --warning-mode all` exits 0 on Gradle 9.7.1 / JDK 25
with Java 11 compilation/tests. This is the latest current-branch measurement;
the E06 section below retains its earlier checkpoint counters.

| Check | Result |
| --- | --- |
| JUnit | **1,308 passing**, zero failures/errors/skips; existing cases retained. |
| Static checks | Zero Checkstyle main/test/seat-rule errors; zero unexcluded SpotBugs main/test findings or errors. No new filter, suppression or metric exclusion. |
| Shared scope | Same 56 parent components / 59 reported classes: **2,544/2,545 lines, 1,403/1,404 branches, 1,414/1,415 complexity counters, 12,522/12,527 instructions and 707/707 methods**. |
| PIT | **1,645/1,659 killed (99.16%)**, same 14 baseline survivors; zero uncovered/error/timeout mutants. Final XML records 12,860 executions; 83 selected test classes. |
| Existing miss/survivors | The sole JaCoCo miss remains the unreachable GamePhase default, now in GameState.hasBoardSizeForPhase. The equal-count comparator survivor moved from rankFive to ranksByMultiplicity; its stable-sort reasoning is unchanged. Other survivors retain the same methods/rationale. |

State checks now identify card/phase, betting/actor and history invariants.
Statistics separate action validation, checked aggregate preparation and final
publication, keeping overflow/retry atomicity explicit. Evaluation separates rank
facts while preserving category order and the increasing-index combination
algorithm. UI rendering separates seats, human selection and action parsing;
metric formatting uses names instead of unexplained indices. Initial setup,
continuation, accepted-event replay and bot sizing use descriptive steps/units.
No blanket method-length limit was introduced; longer cohesive parsing and
betting algorithms remain together with descriptive state names.

Private helpers are exercised through the existing public-API cases. This was
refactoring from a GREEN suite, not a newly claimed feature RED cycle or synthetic
Git TDD history. Five duplicated UI-test searches now share an explicit typed
query; 39 statistics expectation rows use OptionalDouble rather than nullable
rate values. No original BVA cases were removed. Increased method/mutation counts
reflect extracted boundaries, not proof of stronger playing ability or full
basis-path coverage. Survivor acceptance remains a separate review requirement.

The saved feature/player-profiles branch was reviewed read-only without switching,
merging or creating a checkout. It predates the instructor cleanup: internal
null guards, missing catalog-field normalization and mixed profile selector/
roster rendering remain follow-ups. Those absent profile sources were not edited
or covered by this branch's build. Current E06 work stays uncommitted for user
review; the profile branch requires its own cleanup/integration afterward.
No extra report, commit or push was created. Peer review, remote CI, merge,
native UI checks and known departure/terminal-policy work remain pending.

## E06 live adaptive bots — November 15, 2026

Verified locally on `feature/bot-integration`, based on the user's committed/
pushed E05 `4e421c6` / `36f9a49`. The original checkout was used throughout;
no agent commit/push, extra checkout or scratch report was created. The profile
branch remains separate. The final command was `build pitest --warning-mode all`,
using Gradle 9.7.1 on JDK 25 with Java 11 compilation/tests, and exited 0.

| Check | Final local result |
| --- | --- |
| Full suite | **1,308 passing**, zero failures/errors/skips; 51 additional invocations over committed E05's 1,257. |
| Static checks | Zero Checkstyle main/test/seat-rule findings and zero unexcluded SpotBugs main/test findings or errors. Production/new-test lines remain within 100 characters; no production null guards were introduced. |
| Six new executable parents / seven reported classes | **269/269 lines, 68/68 branches, 113/113 complexity counters, 1,171/1,171 instructions, 79/79 methods**; zero missed. Includes PotLayout.Layer. |
| Shared measurement | **56 parent components / 59 reported classes**: 2,434/2,435 lines, 1,399/1,400 branches, 1,354/1,355 complexity counters, 12,258/12,263 instructions, 649/649 methods. The unchanged GameState enum-default path remains the sole miss. |
| New-class PIT | **136/136 killed**: BotTurn 8/8, BotTable 25/25, BotComputer 4/4, BotWorker 41/41, BotController 39/39, PotLayout including Layer 19/19. |
| Shared PIT | **1,581/1,595 killed (99.12%)**, the same 14 documented baseline survivors; zero uncovered/error/timeout mutants. PIT lines 2,430/2,433; 83 selected test classes and 11,797 executions across mutants. |
| Observed live latency | Three background bot turns reaching the human took **216.011 ms** in the final ordinary suite. This includes scheduling and 512-trial estimates per turn on this machine; it is observational, not a realtime or playing-strength guarantee. |

The extended session, position and transactional model paths have no added miss
or surviving mutant. No class, operator or test pattern was excluded to obtain
these results. BotFailure is a data-only enum; BotSession/BotScheduler are ports
without executable bodies. Shared counters do not independently prove every
basis path or full-backend grading compliance. Baseline survivor acceptance still
needs the documented review/instructor assessment.

New behavior was developed through observed API RED/GREEN stages, then five
controller behavior failures and six Swing invocations failing against unsupported
implementations before GREEN. Later tests add failure/retry, refund, context,
coverage and mutation regressions; those are not claimed as initial feature RED
cycles. No synthetic Git TDD history was created. Required per-class BVA and the
existing weekly/design records distinguish isolated port tests from integrations.

Real-component integrations verify background computation and Swing callback
ownership; replacement of a queued job; three adaptive turns reaching a private
human handoff; a full adaptive hand through accepted actions, learning, payout,
completed statistics and continuation; and a capped all-in CALL with ineligible
side pots and an uncalled refund. Controlled clocks exercise exact deadline,
clock-wrap, cancellation, close and late-callback boundaries. Injected recorder
failure preserves chips, log, learning and the retryable captured revision.
Learning tests inspect exact pre-action legal/street/position contexts, including
free-check previews and automatic departure exclusion.

The first mutation run exposed 30 additional survivors. Meaningful cancellation,
notification and learned-context assertions close the gaps; redundant checks and
status resets were simplified. Final raw survivors are only the pre-existing 14.
SpotBugs concurrency/test findings were corrected in source. One exact new
GamePanel.bots SE_BAD_FIELD match follows the existing documented live-Swing
serialization policy: the panel/controller is never serialized, and window close
releases its executor. It is visible in the exception table for peer review;
there is no new coverage or mutation exclusion.

Production now uses filtered live decisions rather than the legacy passive
helper. Calculations are cooperatively bounded/cancellable, and old match/hand/
revision/actor output is rejected before touching cards, chips, statistics or
learning. Public pot previews share settlement's contribution algorithm and
assume no further betting; conditional scenario refunds are not promised across
future actions. Equity still samples uniform opponent hands; the policy adapts
public tendencies and varies actions, without claiming GTO or measured win rate.

E06 is verified locally, awaiting user commit/push, peer review, remote CI and
merge. Native display/focus verification, profile-branch integration, broader
controlled playing evaluation and known departure/terminal-policy follow-ups
remain. E06 does not establish whole-backend completion or an A by itself.

Style verification follow-up: worker budget validation and model creation were
extracted to requireBudget and createModels, keeping constructors/coordinators
focused. The rerun exposed a timing-sensitive real-thread assertion; BG03 now
uses controlled randomness and its test driver explicitly retries only timeout
outcomes, at most twice. Production still waits for user retry, and deterministic
clock tests retain exact deadline assertions. The final full build/PIT rerun
passes with the updated counters above. No new static exception was added.

## E05 varied legal decision policy — November 15, 2026

Verified locally on feature/bot-policy, based on the user's committed instructor
integration 93ab0fe. Earlier instructor/refactor sections below are dated
checkpoints; their uncommitted wording describes that earlier verification.
At that checkpoint E05 was uncommitted and E06 was not implemented. The user
subsequently committed/pushed E05 as 4e421c6 / 36f9a49; see the newer E06 section
above for the current local integration. Profile work remains separate.

BotPolicy returns reproducible, normalized probabilities over legal choices.
It uses public call equity, eligible-pot odds with new-call refunds, street
position, stacks, modest personalities and confidence-weighted learned aggression.
Raise-to sizing consumes LegalActions.Wager full/short-all-in limits. It has no
live state, private opponent cards, mutable RNG, UI scheduling or accepted-action
side effects. See the [design contract](design/README.md#varied-decision-policy-e05).

| Check | Final local result |
| --- | --- |
| Full suite | 1,257 passing, zero failures/errors/skips; 63 additional invocations over the committed 1,194 baseline. |
| Static checks | Zero Checkstyle main/test/seat-rule findings; zero unexcluded SpotBugs main/test findings, errors or missing classes. No new suppression or filter. |
| Four new executable policy classes | 180/180 lines, 126/126 branches, 115/115 complexity counters, 1,020/1,020 instructions, 52/52 methods; zero missed. |
| Extended OpponentBeliefs | 47/47 lines, 20/20 branches, 25/25 complexity counters, 265/265 instructions, 15/15 methods; zero missed. |
| Shared measurement | 50 parent components / 52 reported classes: 2,108/2,109 lines, 1,297/1,298 branches, 1,207/1,208 complexity counters, 10,796/10,801 instructions, 553/553 methods. |
| E05 mutations | CallValue 23/23, PolicyRequest 51/51, ActionDistribution 36/36, BotPolicy 62/62; 172/172 in new classes. Extended OpponentBeliefs kills 25/25. |
| Shared PIT | 1,402/1,416 killed (99.01%); the same 14 documented baseline survivors, zero uncovered/timeout/error mutants. PIT lines 2,104/2,107; 74 selected test classes, 10,767 executions across mutants. |
| Policy construction latency | Twelve observed calls across four streets and three personalities: 0.039625–0.107167 ms locally. Excludes equity simulation and application scheduling; observational, not a realtime bound. |
| Sampling evaluation | 10,000 fixed-seed draws: reproducible sequences and empirical frequencies within .02 of published probabilities. This is action variation/calibration evidence, not playing-strength or win-rate proof. |

The new classes and opponent-query extension introduce no coverage miss or
surviving mutant. The shared missed enum-default path and 14 existing survivor
interpretations still require the documented acceptance; scoped counters do not
independently prove all basis paths or full-backend grading compliance.
BotPersonality is a data/configuration enum using the course's enum exemption;
all three configurations are exercised through the policy tests.

The initial feature cycles observed missing APIs before 14 and 22 GREEN cases,
then 12 behavior failures against the unsupported policy followed by GREEN.
Fifteen later invocations add integration, coverage and mutation hardening.
These later tests are not claimed as original feature RED cycles. No synthetic
Git TDD commits, extra checkout or scratch reports were created. Genuine local
RED/GREEN evidence does not substitute for the rubric's commit-history review;
the user retains responsibility for reviewing and committing the work.

The first mutation run found six policy preference/calibration gaps, a redundant
normalization check and an equivalent strict/non-strict seat comparison.
Regressions address the meaningful preference boundaries. Normalization now
rejects nonrepresentable probabilities once; position directly selects the last
active seat. The rerun kills every new mutant without an operator/class exclusion.
SpotBugs findings were fixed in source: immutable probabilities are copied at
field ownership, and the legality integration uses deterministic quantiles.
Seeded variation remains in its separate evaluation. No new static filter was added.

Real-component checks combine Monte Carlo/evaluator output, policy decisions and
engine acceptance on all four streets; hidden cards/deck changes preserve seeded
results. A second flow applies a refundable short-blind CALL, runs out the board
and settles while conserving 6,000 chips. Target/stack and all-in CALL tests use
the actual engine. These are separate from claims of live application integration.

Existing README, per-class BVA, design, weekly report and issue #20 are synchronized.
Peer review, user commit/push, remote CI and merge remain pending. E06 must supply
authoritative prospective-call scenarios/refunds, committed public events and
immutable worker inputs with match/hand/revision checks. Live bots still use the
temporary check/call session policy; departure/terminal-policy findings and profile
integration remain separate.

## Instructor cleanup and local integration — November 15, 2026 (historical)

Local branch `refactor/instructor-null-formatting` combines E03/E04 (`54de9ad`)
with main `bda4954`, which merged Kevin's PR #23 (`41c1455`). The merge is resolved
and deliberately uncommitted. Profile PR #21 remains separate from this code.
The source of the Week 7 feedback is [instructor PR #22](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/22).

The four requested cleanup areas are implemented locally: focused street and
validation helpers; shared named table/default constants; a consistent non-null
caller contract with explicit actor/aggressor absence; and automatic formatting
checks. The earlier internal-only cleanup was incomplete; this final pass removes
the repeated manual null guards across production (zero manual null comparisons),
removes nullable String actor constructors and migrates existing callers/fixtures.
BettingHistory uses Optional for absence; automatic PublicDecision events use a
separate overload. The localization loader normalizes a missing JDK resource once;
external resource validation remains. The final follow-up removes all 11 remaining requireNonNull calls as well,
including construction and composition-result guards. There are no production
manual null comparisons or requireNonNull/isNull/nonNull guard calls.
Checkstyle enforces the 100-character limit. Code review checks whether each
method has a focused responsibility.
See the [API contract](design/README.md#instructor-follow-up-november-15).

| Check | Verified result |
| --- | --- |
| Full suite | **1,194 passing**, zero failures/errors/skips. Includes Kevin's betting suite, E03/E04 and five explicit-absence business cases; 80 obsolete programmer-null invocations retired from the earlier 1,274 checkpoint (six in this final follow-up). |
| Checkstyle | Zero main, test and seat-rule findings. The new seat-rule task is part of check/build. |
| SpotBugs | Zero main/test unexcluded findings, analysis errors or missing classes. Two exact constructor/collaborator exceptions are documented below and remain visible for peer review. |
| Shared measurement | **46 parent components / 48 reported classes**, including newly measured GameSetup and both standalone bot packages. |
| JaCoCo | **1,915/1,916 lines; 1,161/1,162 branches; 1,084/1,085 complexity counters; 9,703/9,708 instructions; 498/498 methods; 48/48 classes.** |
| PIT | **1,223/1,237 killed (98.87%)**; the same 14 documented survivors as Kevin's PR, zero uncovered/timeout/error mutants. PIT lines 1,911/1,914; 68 selected test classes, 9,989 executions across mutants. |

The sole missed JaCoCo path remains GameState's enum-default fallback;
its rationale and acceptance limits are retained below. The seven original
survivors and seven betting/state survivors remain visible with their existing
reasoning. The cleanup introduces no new survivor. This is not an unqualified
100% cyclomatic/path or all-mutants grading claim; instructor/peer acceptance of
the remaining interpretations is still required. GameSetup kills 15/15 mutants;
EquityRequest 45/45 and OpponentModel 24/24.
No operator, target or exclusion was removed to improve the score.

The retained five OptionalGameStateTest cases were observed RED before the
factory implementation, then GREEN. They cover actor presence, explicit absence,
provided history, an unknown actor and pending-decision consistency. New TDD/BVA
cases follow the user's business-boundary focus. Current business boundaries
remain covered; retired reference-input cases are
removed from current BVA tables without renumbering. Historical RED/GREEN evidence
and dated counts remain intact; no synthetic test-first Git commits were created.

A verification run exposed one additional EquityRequest survivor: the hero-ID
check overlapped other scenario validation. Participant validation was consolidated
into one addParticipant helper for hero and opponents. The final rerun kills that
mutant and retains only the 14 existing survivors. No mutation operator, measured
class or test-target pattern was dropped. Reduced counters reflect removed guards,
not measurement exclusions.

Negative Checkstyle verification caught the exact extra blank at StatisticsPanel
line 85 plus InitialDeal's matching final blank before either was fixed. The
new production check also caught the existing internal absence propagation
patterns. A final temporary requireNonNull guard in Card was rejected by the
strengthened rule, then the original source was restored before final verification. MagicNumber first reported six actual violations in HandPositions and
GameConfig, then passed after named constants were introduced. Its initial scope
is explicitly GameConfig, HandPositions, SeatOrder, Player and PotSettlement;
ordinary production style continues to apply to every main Java source. This is
not a global magic-number compliance claim. The task uses supported Gradle task
registration and introduces no Gradle deprecation warning.

Gradle reports are under `build/`. This PIT run included the domain, application,
integration, localization and standalone bot suites.

Two functional PR #23 review follow-ups were separate from this refactor.
A queued, non-all-in quitter could remain eligible for an uncontested hand win
before its turn. Retirement and continuation also needed an explicit policy
and regression coverage. E05/E06 still needed varied decisions and safe live
application wiring.

## E04 opponent learning — November 10, 2026

Verified locally on `feature/opponent-learning`, based on the committed/pushed
E03 checkpoint `2ce09a0`. Profile work remains on its separate branch. Gradle
9.7.1 runs on JDK 25 with Java 11 compilation/tests. Final `build pitest` exits 0.

| Check | Result |
| --- | --- |
| Full suite | **1,133 passing**, zero failures/errors/skips; **49 new E04 invocations** beyond E03's 1,084. |
| Checkstyle / SpotBugs | Zero main/test violations or unexcluded findings; zero analysis errors/missing classes. No new suppression. |
| Six new learning classes | **186 lines, 112 branches, 117 complexity counters, 920 instructions, 61 methods**, zero missed. |
| Newly measured StatsContext | **21 lines, 26 branches, 18 complexity counters, 86 instructions, 5 methods**, zero missed. Only accessor visibility changes; prior constructor behavior is retained. |
| Shared 29-parent scope | **1,222 lines, 679 branches, 633 complexity counters, 6,301 instructions, 292 methods, 30 reported classes**, zero missed. The extra class is the nested PlayStats counter enum. |
| New learning PIT | **138/138 killed**; StatsContext **20/20 killed**. No new survivors, uncovered, timeout or error mutants. |
| Shared PIT | **706/713 killed (99.02%)**; seven unchanged baseline survivors, zero uncovered/error/timeout mutants. PIT lines 1,218/1,218; 37 selected classes, 3,925 test executions across mutants. |

The new measured classes are DecisionContext, TendencyEstimate, PublicDecision,
ActionPrediction, OpponentBeliefs and OpponentModel; both new enums are exempt.
StatsContext is now public read-only metadata consumed outside domain.model and
included in both measurement tools with its existing tests. The shared scope
still excludes other betting/state components. Covered complexity counters do
not prove independent basis paths or whole-backend compliance. Baseline survivor
reasoning and the instructor-acceptance question below remain unresolved.

Local TDD stages observed 13 context/uncertainty RED cases, 9 event/prediction
RED cases, 13 aggregation/coordinator RED cases, 2 legal-opportunity consistency
RED cases and 3 context-free automatic-event RED cases, each followed by GREEN.
One all-in-call fixture was corrected after review to remove an impossible
three-bet opportunity. Eight real-component/evaluation invocations and one new
mutation regression were added after the implementation. They are not claimed
feature RED cases. No agent Git checkpoints, commits or pushes were created.

The first mutation run exposed two gaps: the position getter had only been
asserted true, and missing prediction context after learning could throw a
NullPointerException if its boundary guard were removed. Empty-state validation
alone masked the latter via the prior constructor. The new false-position
assertion and learned-state invalid-query case kill both. Static test findings
were fixed by consuming immutable results inside exception assertions and
removing unused final counter increments; no exclusions were added.

Controlled evaluation trains on 80 folds/10 calls/10 raises, then tests on a
separate 70/20/10 composition. Learned multiclass Brier error is **0.476199453**,
compared with **0.666666667** for the uniform, learning-disabled prior. Four seeded
permutations verify order invariance of this exchangeable count model; they are
not independent performance trials. The held-out actions never update the
model. Further evidence changes a raise belief from 11/103 to 91/203 while an
unseen street retains its zero-data prior. This is prediction/update evidence,
not poker win rate, calibrated real-player accuracy or proven playing strength.

The API excludes own choices, forced posts, rejected requests and departure
folds. Match-wide sequence checks prevent replay without retaining an unbounded
event ledger; failures leave both belief snapshots and the sequence unchanged.
Real engine integration verifies accepted CALL versus rejected CHECK and keeps
exact completed-hand PlayStats separate from immediate learning. No hidden
cards/deck or account credentials enter the model. See [design](design/README.md#opponent-learning-e04)
and [BVA](bva/OpponentModel.md).

E04 is locally verified, awaiting user commit, peer review, remote CI and merge.
E05 still supplies the varied legal policy; E06 wires committed events and
worker/state lifecycles. Sampling remains uniform until a validated hand-range
likelihood is supplied. I05/I06/M01 and whole-backend quality gates remain open.

## E03 standalone equity — November 10, 2026

Verified locally on `feature/monte-carlo-equity`, based on merged main `c71e9d6`.
The separate profile branch retains its committed E01/E02/UI work; it is not part
of this branch's 1,033-test baseline. No profile changes were removed. Gradle
9.7.1 runs on JDK 25; compilation/tests use the Java 11 toolchain.

| Check | Result |
| --- | --- |
| Full JUnit suite | **1,084 passing**, zero failures/errors/skips; 51 new E03 invocations. |
| Checkstyle / SpotBugs | Zero main/test violations or unexcluded findings; zero analysis errors/missing classes; no new suppression. |
| Five executable E03 classes | **188 lines, 110 branches, 107 complexity counters, 912 instructions, 52 methods**, zero missed. |
| Shared 22-parent scope | **1,015 lines, 541 branches, 498 complexity counters, 5,295 instructions, 226 methods, 23 reported classes**, zero missed. The nested PlayStats counter enum accounts for the extra class. |
| E03 PIT | **108/108 killed**, no survivors or uncovered/error/timeout mutants. |
| Shared PIT | **548/555 killed (98.74%)**, seven unchanged baseline survivors; no uncovered/error/timeout mutants. PIT line coverage 1,011/1,011; 28 selected classes, 3,518 test executions across mutants. |

Coverage/PIT now include EquityPot, EquityRequest, EquityEstimate,
UniformDealSampler and MonteCarloEquityEstimator. The two functional ports have
no executable defaults. Existing unmeasured betting/state components remain
outside the denominator. These counters do not establish independent basis
paths or whole-backend compliance; the seven baseline survivor interpretations
below still require review.

Local TDD observed eight pot-value RED cases, then fifteen request RED cases,
then seventeen sampler/estimator RED cases, and three filtered-view RED cases,
each followed by GREEN. The initial combined request attempt had a fixture
initialization failure while EquityPot was a stub; that attempt is not counted
as behavioral request RED evidence. Four later boundary/mutation regressions
and four real-equity integrations are additional checks, not new feature RED
claims. No Git TDD checkpoints, commits or pushes were created by the agent.

The first PIT run exposed four actionable gaps: invalid opponent identity masked
by invalid pot membership, unknown pot eligibility masked by missing hero
eligibility, and a losing hand masking an ineligible-pot error. Focused valid-
otherwise scenarios now kill all four mutants. No new equivalence claim or
exclusion was introduced. Isolated estimator tests use controlled sampler and
evaluator ports; separate integrations use the real evaluator and filtered views.

Exact river equity over all **990** possible opponent holdings is
**0.869191919**. With 2,048 trials, seeds 7/23/91 returned
0.870849609 / 0.864257813 / 0.869873047; absolute errors were
0.001657690 / 0.004934107 / 0.000681128 (all below the fixed 0.06 regression
tolerance). Observed river calculations took 28.371 / 20.811 / 20.639 ms in the
final JUnit run. A six-player preflop calculation with 1,000 trials took
115.856 ms. These are local observations, not latency or general statistical
accuracy guarantees. A shared royal flush verifies a six-way fractional tie;
changing actual hidden cards/deck order leaves seeded estimates unchanged.
Output stays in standard JUnit reports under build/.

E03 supplies a standalone synchronous calculator with between-trial cancellation.
Engine-owned pot-scenario construction, learned opponent ranges, varied legal
actions, off-EDT execution and stale-result rejection remain E04–E06/shared
integration work. CI, peer review and merge for this branch are pending; leave
E03 unchecked in issue #20 until those gates are met.

## Week 7 departure, legal actions and state measurement (November 13, 2026)

Branch `feature/week7-betting`, code `b9ae32b`. Local `build pitest` passed
with Gradle 9.7.1 on JDK 17 and Java 11 compilation/tests.

| Check | Result |
| --- | --- |
| Team suite | **1,169 passing**, zero failures, errors or skips. |
| Checkstyle / SpotBugs | Zero main/test findings and no new exclusions. |
| JaCoCo, 34 configured parent components | **1,531/1,532 lines, 1,103/1,104 branches, 922/923 complexity counters, 7,922/7,927 instructions, 365/365 methods and 36/36 classes.** |
| PIT | **1,037/1,051 killed (98.67%)**, 14 reviewed survivors, zero uncovered, timeout or error mutants. |

The new scope adds core state, betting input/output and statistics context
classes. The sole missed JaCoCo branch is the unknown-phase fallback in
`GameState.validateCards`. Every `GamePhase` constant has a case, and
construction rejects a missing phase before validation.

Boundary and real-session tests cover departure during and outside a player's
turn, all-in eligibility through payout, exclusion from later hands, and
private-card handoffs. They also check exact-stack calls, full and short wager
targets, and the inclusive raise-count limit for constructed snapshots.
Headless Swing tests exercise the controls in both languages.

| Remaining survivor | Count | Reason |
| --- | --- | --- |
| Evaluator and hash-code baseline | 7 | Six evaluator mutations preserve ranking. The constant hash preserves Java's contract but changes hash values. Course acceptance remains pending. See [mutation survivor review](#mutation-survivor-review). |
| `BettingEngine` | 3 | A zero c-bet cannot have an outstanding call in the matching context. The acting seat has left the pending set before the search wraps. A zero chip delta cannot exhaust an active player's positive stack. |
| `BettingRound.closeMatchedLoneActor` | 1 | Including the actor in the maximum keeps the same comparison against other contributions. |
| `BettingAmounts.chipsAdded` | 1 | Accepted-action validation still rejects a zero-chip CALL before any state is published. |
| `GameState.hasShortBlindTarget` | 1 | The caller checks that the posted maximum differs from the target, which this helper requires to equal the big blind. |
| `GameConfig.validateSettings` | 1 | A zero starting stack still violates the required positive big blind and its stack limit. |

## Betting statistics context — November 12, 2026

Branch `feature/week7-betting`, code `a7c0f14`. Local `build pitest` passed
with Gradle 9.7.1 on JDK 17 and Java 11 compilation/tests.

| Check | Result |
| --- | --- |
| Team suite | **1,138 passing**, zero failures/errors/skips; 11 cases added. |
| Checkstyle / SpotBugs | Zero main/test findings; no new exclusions. |
| 22 configured parent components | **1,080 lines, 646 branches, 550 complexity counters, 5,735 instructions, 223 methods and 23 reported classes**, zero missed counters. |
| PIT | **629/643 killed (97.82%)**, 14 survivors; zero uncovered, timeout or error mutants. Selected-test line coverage 1,076/1,078; 36 test classes and 6,499 executions across mutants. |

The scope is unchanged. GameState's new count bound is exercised by the full
suite; GameState remains outside the measured scope pending K06. The 14
survivors match the prior checkpoint, including the c-bet boundary now in
`BettingEngine.flopContext`. No new survivor or exclusion was introduced.

Four inconsistent-count cases failed before constructor validation. Review
identified the overflow risk from an impossible count; two more tests failed
before the snapshot bound was added. The legal replay cases reach the same
wager with different raise counts, including short all-ins. Settlement tests
confirm that the all-in caller remains CALL and that direct c-bet responses
alone receive a fold-to-c-bet denominator. See [K03 cases](bva/BettingHistory.md#k03-exact-raise-counts-and-all-in-observations).

## Short all-ins and reopening — November 11, 2026

Branch `feature/week7-betting`, code `978a826`, after K01 report commit
`cc17bf4`. Local `build pitest` passed with Gradle 9.7.1 on JDK 17 and Java 11
compilation/tests. Remote checks and partner approval remain pending.

| Check | Result |
| --- | --- |
| Team suite | **1,127 passing**, zero failures/errors/skips; 36 cases added. |
| Checkstyle / SpotBugs | Zero main/test findings; no new exclusions. The immutable map copy is explicit in its constructor. |
| 22 configured parent components | **1,078 lines, 638 branches, 544 complexity counters, 5,700 instructions, 221 methods and 23 reported classes**, zero missed counters. |
| PIT | **621/635 killed (97.80%)**, 14 survivors; zero uncovered, timeout or error mutants. Selected-test line coverage 1,074/1,076; 36 test classes and 6,493 executions across mutants. |

The scope adds BettingProgress. GameState's new decision-history validation is
covered by the full suite; GameState and the other state classes remain outside
the measurement scope pending K06. Scoped JaCoCo counters do not prove
whole-game coverage or independent basis paths.

The [K02 cases](bva/ShortAllInBetting.md) first showed 13 of 18 engine cases
failing and both application cases failing, then all passing. The first full
run and code review caught an opening-minimum regression with a valid unopened
snapshot carrying a stale prior increment. A short-opening regression also
failed before the correction. The final run covers short/full openings,
individual reopening, exact-stack calls, side pots and automatic board runout.

Seven survivors are the unchanged evaluator/hash baseline described below.
The other seven preserve the engine contract for the same reasons as at K01:
three BettingEngine boundary changes (zero c-bet, zero chip delta and revisiting
the acting seat), three BettingAmounts guards (zero-cost CALL and two routes
past unopened preflop BET validation), and one BettingRound predicate (including
the lone actor in the opposing maximum). The redundant guards can affect
exception text but still reject the same requests before publishing state.
The target-at-current-wager guard now matters for exact-stack matches; a
regression kills both mutations there. No survivors were excluded from the score.

## Full betting checkpoint — November 9, 2026 (historical)

Branch `feature/week7-betting`, code `fda5bdf`, based on merged main `c71e9d6`. Local
`build pitest` passed with Gradle 9.7.1 on JDK 17 and Java 11 compilation/tests.
Push, new remote CI and partner approval remain pending.

| Check | Result |
| --- | --- |
| Team suite | **1,091 passing**, zero failures/errors/skips; 58 added cases. |
| Checkstyle / SpotBugs | Zero main/test findings, analysis errors or missing classes; existing exact exceptions retained. |
| 21 configured parent components | **1,034 lines, 600 branches, 509 complexity counters, 5,468 instructions, 205 methods and 22 reported classes**, zero missed counters. |
| PIT | **580/599 killed (96.83%)**, 19 survivors; zero uncovered, timeout or error mutants. Selected-test line coverage 1030/1032; 32 test classes and 6,097 executions across mutants. |

The added components are BettingEngine, BettingAmounts, BettingRound and
BettingHistory. This extends the earlier 17-parent scope; the remaining state
classes are still outside it. Swing remains exempt from scoped metrics.
JaCoCo uses the full JUnit suite; PIT uses selected tests, so its line count
can differ. These counters do not prove whole-game or independent basis-path
coverage. No mutation operator, target class or exclusion was removed to improve
these results; K06 still includes the remaining betting/state measurement work.

TDD: 45 boundary cases and two real played-hand integration cases failed before
implementation, then passed. Eleven later regression cases address reported
coverage/mutation gaps: affordable full-raise opportunity boundaries, opening
minimum/stack limits, rejection before dealing, matched/aggressor c-bet context,
short-blind targets and closing independent raise rights. The actual pipeline
uses real setup/dealing, betting, evaluation, payouts and completed statistics,
with controlled Random; it adds no mock library. Read-only code review found no
actionable K01 issues.

The raw 19 survivors remain visible. Seven are the unchanged evaluator/hash
baseline reviewed below; the twelve additional betting survivors are reviewed
separately below. K02 short all-in/reopening rules and K05 UI/limits are not
claimed complete by these measurements.

### Betting survivor review

Read-only review of source, PIT reports and bytecode found these twelve survivors
equivalent under the public engine contract (exception type, acceptance and
immutable state). Guard-removal mutants can change error messages or exception
origin; those are not promised by that contract. Keep all mutants in the raw
score and reassess these conclusions when K02 changes validation/reopening.

| Location / mutant | Count | Reason |
| --- | --- | --- |
| BettingEngine.contextFor:178, c-bet positive boundary | 1 | With cBet=0, the remaining conditions require currentBet=0 and a negative round contribution, which valid states reject. |
| BettingEngine.nextPendingActor:211, loop boundary | 1 | The omitted final iteration revisits the actor, whose ID was already removed from pending decisions. |
| BettingEngine.resolveAction:152, positive chip delta boundary | 1 | An ACTIVE actor has a positive stack; zero chips cannot equal that stack or make the actor all-in. |
| BettingAmounts.canMakeFullWager:51, maximum/current boundary | 1 | Equality leaves zero increment, below the positive full minimum. |
| BettingAmounts.chipsAdded:18, remove zero-cost CALL guard | 1 | AcceptedAction constructs StatsAction, which independently rejects zero-chip CALL before publishing state. |
| BettingAmounts.wagerDelta:35, target/current boundary and removed ordering guard | 2 | Targets at/below currentBet have a nonpositive increment and still fail the later positive full minimum; subtracting nonnegative bounded amounts cannot overflow. |
| BettingAmounts.wagerDelta:29/30, remove opening/PREFLOP checks | 2 | Bytecode indices 32/38 skip only the unopened-preflop phase rejection; the BET/opening-kind check remains. Their only new path is preflop BET, independently rejected by StatsAction.validateStreet. |
| BettingRound.after:25, participant-count boundary | 1 | With exactly one participant, lone-actor cleanup already clears pending, or no active pending player remains. The pending-empty condition still closes both sets. |
| BettingRound.after:27, remove pending.clear | 1 | Pending is already empty whenever this block runs; independent raise rights still clear. |
| BettingRound.closeMatchedLoneActor:48, include actor in opposing maximum | 1 | Testing actor >= max(opponents, actor) gives the same result as actor >= max(opponents), including the empty-opponent zero default. |

The other seven survivors retain the earlier six evaluator and one hash-contract
rationales below. No additional exclusion or artificial hash-value assertion was
added to force a perfect mutation score.

## Completed match statistics — November 6, 2026 (historical)

Branch `feature/week6-finish`; local `build pitest` uses Gradle 9.7.1 / JDK 17
with Java 11 compilation/tests. Checkpoints: `1a99b7d` pays pots and refunds;
`78e5271` retains completed totals and continues fixed-seat hands; `e1c2992` fixes short-blind decisions and completion checks. These local
changes still need push, remote CI and partner review.

| Check | Latest result |
| --- | --- |
| Team tests | **1,033 passing**, zero failures/errors/skips. |
| Checkstyle / SpotBugs | Zero main/test unexcluded findings, analysis errors or missing classes; no new suppression. |
| Configured 17 parent components | **827 lines, 431 branches, 391 complexity counters, 4,383 instructions, 174 methods and 18 reported classes**, zero missed counters. Reported classes include the generated PlayStats counter enum. |
| PIT | **440/447 killed (98.43%)**; 7 survivors, 0 uncovered. Full counts/statuses remain in the report. |

The measured scope now includes PotSettlement/Settlement, MatchStatistics,
NextHand, StatisticsView and PlayStats. Existing partner state/betting classes
remain outside this denominator, including the short-blind GameState/BettingEngine
follow-up; their full tests and shared static checks still run. UI stays exempt
from scoped metrics. Covered complexity counters do not prove independent basis
paths or full-game coverage.

TDD observed 9 failing payout cases, 4 completed-statistics/copy cases, 4 continuation
cases and 2 bilingual UI cases before their implementations. Three short-blind /
last-active regressions failed before the engine fix; two embedded-header layout
cases failed before the presentation fix. Later mutation-driven tests cover
stronger late contenders, separated-seat odd chips, every nonparticipant outcome,
reverse c-bet contexts, rejected/successful continuation privacy and raise rights;
they are regression coverage, not additional claimed feature RED cycles.

The final read-only code review found three betting/continuation issues; the
follow-up confirmed all three resolved. English/Chinese table and statistics
layouts were rendered at 1,100×740; native OS focus and cross-platform fonts still
need manual walkthrough. The actual pipeline is accepted ledger → real payout →
transactional completed totals → immutable bilingual table → next stable-seat hand.
Stopping the game discards in-memory statistics; saved profiles remain future work.

PIT line coverage is 823/823; 22 selected test classes ran 3,105 executions across
mutants. There are no uncovered, timeout or error mutants. The seven unchanged
survivors are the six evaluator cases and hash-contract case reviewed below.
All new settlement, continuation, completed-statistics and copy mutants are
killed. The configured 17 parent components produce 18 reported classes because
of the nested counter enum; this is not an 18-component full-game claim.

## Pending-statistics checkpoint — November 6, 2026

Branch `feature/week6-finish`, based on merged main `ac70b3b` plus the progress
notes in `4baef67`; statistics code is `634b2c6`. Local `build pitest` exited 0 in 58 seconds on Gradle 9.7.1 /
JDK 17, with Java 11 compilation/tests. Remote CI for this checkpoint is pending
push; the earlier main CI result below does not verify these new changes.

| Check | Result |
| --- | --- |
| Full team JUnit suite | **998 tests**, zero failures/errors/skips; includes 12 new adapter and 16 session-statistics cases. |
| Checkstyle / SpotBugs | Zero main/test findings after the existing reviewed exceptions; zero analysis errors/missing classes. No new exclusions. |
| Shared eleven-class JaCoCo scope | **2,353 instructions, 226 branches, 471 lines, 217 complexity counters, 104 methods, 11 classes**; zero missed counters. |
| PIT | **242/249 killed (97.19%)**, seven unchanged survivors; no uncovered/time-out/error mutants. Line coverage 472/472; 16 selected test classes, 1,523 executions across mutants. |
| New adapter / session | HandStatistics: 19 lines, 8 branches and 8 complexity counters covered; PIT 7/7 killed. SetupSession: 107 lines, 42 branches and 47 complexity counters covered; PIT 46/46 killed. |

The scope now includes the pending-statistics adapter. PlayStats itself, the
betting/state engine and Swing remain outside this metric denominator; their
tests and shared static checks still run. Counters do not prove independent
basis-path execution. The seven baseline survivors remain the six evaluator
cases and the unresolved hash-contract case reviewed below.

TDD first ran 28 new cases against the missing adapter/session connection:
25 failed and three existing zero-rate cases passed. All 28 passed after
implementation. Integration tests use real initialization, betting, adapter and
PlayStats, capturing observation deliveries and injecting failures before
mutation. No mock library, completed-hand totals or payout-dependent results
were added. See [BVA](bva/HandStatistics.md). Native focus/font walkthrough,
peer approval and new remote checks remain separate pending verification.

## Merged baseline — November 6, 2026

Verified main `ac70b3b` with `build pitest`: Gradle 9.7.1 on JDK 17 and
Java 11 compilation/tests. The local run exited 0 in 1 minute 19 seconds;
log: `/private/tmp/current-progress-check.log`. Its matching
[GitHub CI run](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/actions/runs/42232032730)
also completed successfully.

| Check | Result |
| --- | --- |
| Full team JUnit suite | 970 tests; zero failures, errors or skips. |
| Checkstyle / SpotBugs | Zero main/test unexcluded findings; zero analysis errors or missing classes. |
| Shared ten-class JaCoCo scope | 2,216 instructions, 216 branches, 438 lines, 204 complexity counters, 96 methods and ten classes; zero missed counters. |
| PIT | 237 generated, 230 killed, seven survived (97.05%); no uncovered, time-out or error mutants. PIT line coverage 439/439; 14 selected test classes and 1,436 executions across mutants. |

These scoped metrics include InitialDeal and the shared application session/view,
not the whole betting/state/statistics engine or Swing. They do not prove
independent basis-path execution. The seven survivors are the six evaluator
cases and hash-code contract case documented below. Earlier branch measurements
are preserved as history. The pending-statistics checkpoint above is newer.

## Reproduce

Gradle 9.7.1 requires a runtime JDK of at least 17; this computer uses JDK 17,
and CI installs JDK 25 plus the JDK 11 compilation/test toolchain. The Java
sources and tests still compile/run with Java 11. SpotBugs explicitly uses the
Gradle runtime JDK to analyze that Java 11 bytecode. Checkstyle 10.26.1 can run
on the project's Java 11 toolchain.

```sh
bash ./gradlew --no-daemon build
bash ./gradlew --no-daemon pitest
```

On CI the commands also pass
`-Porg.gradle.java.installations.fromEnv=JAVA_HOME_11_X64,JAVA_HOME` so both
installed JDKs are discoverable. Reports are under:

| Tool | HTML | Machine-readable |
| --- | --- | --- |
| JUnit | `build/reports/tests/test/index.html` | `build/test-results/test/TEST-*.xml` |
| Checkstyle | `build/reports/checkstyle/{main,test}.html` | sibling `.xml` files |
| SpotBugs | `build/reports/spotbugs/{main,test}.html` | sibling `.xml` files |
| JaCoCo | `build/reports/jacoco/test/html/index.html` | `build/reports/jacoco/test/jacocoTestReport.xml` |
| PIT | `build/reports/pitest/index.html` | `build/reports/pitest/mutations.xml`, `linecoverage.xml` |

## Pinned tools and rules

| Component | Version | Compatibility basis |
| --- | --- | --- |
| JaCoCo | 0.8.15 | Gradle's documented version; supports modern runtime instrumentation. |
| PIT Gradle plugin | 1.19.0 | Released with Gradle 9 support and JDK 17 minimum. |
| PIT engine | 1.22.1 | Default engine of plugin 1.19.0, explicitly pinned. |
| PIT JUnit 5 plugin | 1.2.3 | Requires PIT 1.19.4 or later; discovers this project's Jupiter tests. |
| Checkstyle | 10.26.1 | Java 11 compatible 10.x line, deliberately avoids newer JDK-only versions. |
| SpotBugs Gradle plugin | 6.5.13 | Current stable release checked against the project wrapper. |
| SpotBugs engine | 4.10.2 | Explicit engine pin; runs separately from the Java 11 test process. |

Checkstyle's [Sun starter](https://checkstyle.org/sun_style.html) and
[Google starter](https://checkstyle.org/google_style.html) informed
[`config/checkstyle/checkstyle.xml`](../config/checkstyle/checkstyle.xml).
The initial rules check naming, imports, braces, indentation, whitespace,
modifier order, fall-through, equals/hashCode pairing and source-file hygiene.
Indentation is four spaces, matching this repository; compact accessor/constructor methods remain allowed by the brace-placement rule. Mandatory Javadoc, import
ordering and complexity quotas are deferred until team agreement;
production Java has a 100-character line limit;
this is an explicitly adapted starter, not a claim of full Sun/Google compliance.
Tests use a separate `checkstyle-test.xml` retaining file/import hygiene,
type names, modifier checks and error-prone construct checks. Existing BVA IDs
such as `BP_01`, wildcard assertion imports and compact assertion tables are
accepted; method-name, import-star, indentation, brace-placement and whitespace
rules apply to production sources. This avoids a large unrelated reformat of
partner tests while retaining failing checks for actionable test mistakes.
SpotBugs uses maximum effort and includes low-confidence findings for review.

Primary references:
[Gradle JaCoCo](https://docs.gradle.org/9.7.1/userguide/jacoco_plugin.html),
[Gradle Checkstyle](https://docs.gradle.org/9.7.1/userguide/checkstyle_plugin.html),
[PIT plugin releases](https://github.com/szpak/gradle-pitest-plugin/blob/master/CHANGELOG.md),
[PIT JUnit 5 compatibility](https://github.com/pitest/pitest-junit5-plugin),
[SpotBugs plugin](https://github.com/spotbugs/spotbugs-gradle-plugin),
[SpotBugs plugin release](https://plugins.gradle.org/plugin/com.github.spotbugs/6.5.13).

## Reviewed static-analysis exceptions

The first real main-bytecode run reported 13 SpotBugs findings. Removing the
last null guards exposed two additional intentional-sharing findings on November
15: Deck.random and GamePanel.session. The old requireNonNull calls obscured
these aliases from the analyzer; they were not a real ownership fix. Each exception
below matches only its exact class, method/field and bug pattern; other findings
in these classes still fail. These are not metric exclusions.

| Exact location | Pattern / count | Review and reason |
| --- | --- | --- |
| `InitialDeal.random`, `GameSetup.dealerRandom` | `EI_EXPOSE_REP2` / 2 | Injected `Random` objects intentionally preserve caller-selected seeded/stub behavior. Copying would destroy the seam; these services are mutable and do not claim immutable/thread-safe ownership. |
| `Deck.<init>:random` | `EI_EXPOSE_REP2` / 1, November 15 | Same intentional injected Random contract as InitialDeal/GameSetup. Seeded/stub shuffle behavior is tested; copying changes the selected sequence. The filter requires this exact constructor, field and bug type. |
| `GamePanel.<init>:session` | `EI_EXPOSE_REP2` / 1, November 15 | Mutable application collaborator deliberately shared between actions and rendering on Swing's event thread. It is not immutable snapshot data; copying would disconnect UI actions from the session. Existing UI/session integrations exercise the sharing. The filter requires this exact constructor, field and bug type. |
| `BettingProgress.getPlayersToAct/getPlayersAllowedToRaise` | `EI_EXPOSE_REP` / 2 | Private helper validates then calls `Set.copyOf`; the returned sets are immutable. |
| `GameState.getCommunityCards/getPlayerStates/getRemainingDeck` | `EI_EXPOSE_REP` / 3 | Constructor helpers call `List.copyOf`; callers cannot mutate internal lists. |
| `GameView.getPlayers/getCommunityCards` | `EI_EXPOSE_REP` / 2 | Players use `Collectors.toUnmodifiableList`; community comes from the immutable state snapshot. |
| `PlayerView.getHoleCards` | `EI_EXPOSE_REP` / 1 | Constructor uses immutable `PlayerState.getHoleCards()` or `List.of()`. |
| `ProfilePanel.model` | `SE_BAD_FIELD` / 1 | Live Swing view model contains services, authenticated handles and callbacks. Main never serializes screens; FileProfileRepository writes explicit profile/statistics properties. Making it transient would not create a valid restorable UI. Added November 10; peer review remains pending. |
| `GamePanel.session/messages/humanViewers/bots` | `SE_BAD_FIELD` / 4 | `JPanel` inherits serialization, but this live setup screen is never serialized; session/resources/controller are intentional live runtime state. The exact bots field exception was added for E06; closing the window releases its executor. This application does not serialize Swing or save unfinished matches. |

The immutable-collection contracts are also covered by existing state/view tests.
Revisit these matches if constructors, collection ownership or UI persistence
change. Shared Checkstyle violations are fixed in source instead of baselined.

## Scope and interpretation

The current scope has 66 parent components, including betting, state, dealing,
settlement, statistics, profile persistence, password hashing, localization,
equity, opponent learning, policy and live bot coordination. The exact class
list is in [the Gradle build script](../build.gradle.kts). SetupView remains
measured for its private-card filtering. Earlier runs retain their dated scopes.

PIT selects domain, application, integration, persistence, security, localization,
bot, learning and policy tests. The complete JUnit suite also runs headless Swing
tests. PIT uses DEFAULTS mutators, two workers, HTML/XML reports and a timeout of
twice the baseline duration plus two seconds. Mutant executions differ from the
ordinary JUnit case count.

Record combined results after running build pitest on the resolved source.

Excluded from the current metric denominator:

- Read-only `GameView` and `PlayerView` projections. The full team suite still
  tests them.
- Interfaces without executable default methods, including `HandEvaluator`,
  `ProfileRepository`, `PasswordHasher` and `PasswordDeriver`, and enums without
  handwritten branch behavior. The Gradle configuration excludes
  `ShowdownResult.Status` explicitly.
- Swing and launch code under the rubric's UI exemption; headless tests and
  shared static analysis still check those files.

The rubric's 100% cyclomatic-complexity coverage goal is distinct from 100% line,
instruction or branch coverage. [JaCoCo counters](https://www.jacoco.org/jacoco/trunk/doc/counters.html)
report covered/missed complexity derived from branch coverage and do not prove
independent basis-path execution; exception edges do not count as branches.
Report every counter separately and review any missed paths. The mutation goal
is to kill every non-equivalent mutant. PIT's raw killed/total percentage does
not automatically remove equivalent mutants or justify survivors/timeouts.
There is no invented score threshold or forced green metric gate: actual
findings and rubric gaps must remain visible until assessed.

## Setup and result-component baseline (November 6)

Verified locally on **November 6, 2026**, on `feature/showdown-display`, after
merging main `e8c66ff` with the bilingual result-display component. This includes
the isolated tests and setup, null-handling and localization refactoring:

```sh
bash ./gradlew --no-daemon build pitest
```

Gradle ran on JDK 17; source compilation and JUnit execution used the JDK 11
toolchain. The command exited 0 in 55 seconds (15 actionable tasks,
14 executed and one up-to-date). Reports use the standard paths above;
local log: `/private/tmp/showdown-main-merge.log`.

- Full team suite: **764 tests**, zero failures, errors or skips. This includes
  18 bilingual result-display and 11 isolated initialization cases; the original
  16 initializer and 20 session cases remain real-component integration tests.
  The result panel is included in the team suite and static checks, while Swing
  remains outside Kevin's coverage/mutation denominator.
- Checkstyle: **0 main / 0 test violations**, including the new 100-character
  production line limit. This is an adapted style, not full Google/Sun compliance.
- SpotBugs: **0 main / 0 test unexcluded findings**, zero analysis errors and
  zero missing classes. The reviewed exact filters above remain explicit.
- Kevin-only JaCoCo, now **ten classes** including extracted InitialDeal:
  **1,856/1,856 instructions**, **182/182 branches**, **353/353 lines**,
  **167/167 complexity counters**, **76/76 methods**, **10/10 classes**.
  These are 100% counters, not proof of independent basis-path execution.
- PIT: **197 generated; 190 killed; seven survived**; raw score **96.45%**.
  Zero uncovered, timed-out, non-viable, memory-error or run-error mutants.
  PIT line coverage: **354/354** (its line counting differs from JaCoCo).
  It selected 12 test classes and ran 1,085 test executions across mutants.

| Kevin component | JaCoCo lines | Branches | Covered complexity | PIT killed / generated |
| --- | --- | --- | --- | --- |
| Card | 11/11 | 6/6 | 8/8 | 7/8 |
| Deck | 18/18 | 8/8 | 9/9 | 8/8 |
| PokerHandEvaluator | 62/62 | 58/58 | 36/36 | 58/64 |
| ShowdownResolver | 36/36 | 20/20 | 14/14 | 16/16 |
| ShowdownResult | 9/9 | No branches | 5/5 | 5/5 |
| GameInitializer | 14/14 | 6/6 | 7/7 | 4/4 |
| InitialDeal | 31/31 | 8/8 | 10/10 | 14/14 |
| SetupSession | 28/28 | 6/6 | 15/15 | 11/11 |
| SetupView | 19/19 | 2/2 | 9/9 | 8/8 |
| Localizations | 125/125 | 68/68 | 54/54 | 59/59 |

Earlier branch checkpoints: the November 5 result-display build passed 753 tests;
the November 6 refactoring build passed 746. The combined suite above retains both.

Previous measurement (November 3): 735 tests; nine scoped classes; 319 lines,
184 branches and 153 complexity counters covered; PIT 178/186 killed with eight
survivors. Refactoring changes the denominators, so those historical values are
not current measurements. InitialDeal stays measured after extraction. All
197 component-baseline mutants are accounted for below; none was excluded to improve a score.

Existing localization fault-path fixtures cover missing/invalid resources,
IOException cause and stream closure, quoted braces/apostrophes, repeated or
reordered arguments, type compatibility and nested choice outputs. They still
pass after parser extraction. Native desktop focus and cross-platform layout
checks remain a separate manual team task.

## Mutation survivor review

The 197-mutant component-baseline `DEFAULTS` run retained seven survivors; none is hidden
by a PIT exclusion. The review below reasons about the published component contracts.
It does not change PIT's raw mutation score or prove literal output identity
for every public method.

| Mutant location | Count | Assessment |
| --- | --- | --- |
| `PokerHandEvaluator.bestFive`, outer `a/b/c/d` loop boundaries | 4 | Changing `<` to `<=` adds an iteration whose next nested loop is empty (`b=4`, `c=5`, `d=6` or `e=7`). No five-card candidate or result changes. |
| `PokerHandEvaluator.rankFive`, equal-count comparator branch forced false | 1 | Ranks enter the list in descending order. The alternative count comparator returns zero for equal multiplicities; Java's stable `List.sort` preserves their descending input order, producing the same group order. |
| `PokerHandEvaluator.score`, five-position loop boundary | 1 | The sixth iteration appends a zero base-15 digit because kickers have at most five entries. Every private score becomes exactly 15 times its previous value; all scores stay below 103 million, so no integer overflow or comparison-order change occurs. |
| `Card.hashCode`, integer return replaced by zero | 1 | Contract-preserving alternative, **not identical hash output**: a constant hash still satisfies equal objects having equal hashes. Different cards need not have distinct hashes. It can degrade hash-table performance; no hash-dispersion or performance requirement is currently specified. Acceptance of contract equivalence for this case remains pending. |

The six evaluator survivors are behaviorally equivalent under the current public
contracts; the hash-code survivor requires acceptance of contract equivalence
rather than literal hash-value equivalence. No artificial assertion of distinct hash codes was
added to inflate mutation kills. Consequently the raw score must remain below
100%, and the course's all-non-equivalent-mutants goal remains subject to this
review rather than an unqualified automated success claim.

References: [Java 11 stable list sorting](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html#sort(java.util.Comparator)),
[Java 11 hash-code contract](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Object.html#hashCode()).


## Earlier shared session follow-up — November 5, 2026

After integrating main with Estella's betting branch, local `clean build --continue`
and `pitest` pass on JDK 25 with Java 11 compilation/tests. The full suite is
**939 tests**, zero failures/errors/skips. Checkstyle reports0 main / 0 test
violations; SpotBugs reports0 main / 0 test unexcluded findings,0 analysis errors
and 0 missing classes. No exclusions or failing baselines were added.

The same nine-class measurement scope now includes shared session follow-ups;
it must not be attributed wholly to Kevin or treated as full-game coverage.
Current JaCoCo counts: **2,093 instructions,218 branches,392 lines,189 covered
complexity counters,80 methods,9 classes**, all with 0 missed. SetupSession
covers83/83 lines,44/44 branches and 41/41 complexity counters; SetupView covers
36/36 lines,2/2 branches and 19/19 complexity counters. These counters do not
establish independent basis paths or coverage of betting/state/statistics
classes outside the documented scope.

Current PIT: **227 generated,219 killed,8 survived**, raw **96.48%**. There are
no uncovered, timed-out, non-viable or run-error mutants. The selected 13 test
classes run 1,329 test executions across mutants; this is distinct from the 939
JUnit cases. PIT line coverage is393/393; its line denominator differs from
JaCoCo. The session's43/43 mutants and view's18/18 mutants are killed. The same
8 baseline survivors remain in the earlier review table, including the unresolved
hash-contract acceptance question; none is hidden or newly declared equivalent.

The first follow-up mutation run found two actionable gaps: checking the view's
20-chip preflop betting level, and concealing a revealed hand when playBotTurns
selects the human actor without executing a bot action. Both now have direct
behavior assertions. A separate actor-empty guard was redundant: session state
is published only after advancing closed rounds or producing a result; it was
removed rather than manufacturing invalid states to cover it. The explicit
fault/retry test verifies failed comparison does not publish the candidate or
append a duplicate action.

See the [local TDD record](tdd/week6-session.md) for RED/GREEN results and scope.
Headless English/Chinese played flows and offscreen startup/flop/result layouts
were checked. Native focus delivery and cross-platform fonts still require the
team's walkthrough. New remote CI and peer-review evidence remain pending push.


### Call-button layout regression — November 5

The later UI-only fix keeps the third action button visible when larger fonts
would otherwise wrap it below a fixed-height row. English/Chinese bounds and
label-fit tests extend the full suite to **941 passing tests**, with no
failures/errors/skips. Checkstyle and SpotBugs remain clean, without added
exclusions. The earlier mutation measurements are retained as historical
results; mutation testing was not rerun for this Swing-only layout change.

## Add tests and simplify initialization

InitialDeal is included in Kevin's coverage/mutation scope; moving work out of
GameInitializer did not remove it from the denominator. PIT includes both the
isolated coordinator tests and the renamed initialization/session integration
tests. All initialization/localization mutants are killed in this run. The old
extra-hand-allocation mutant is now killed because dealing traverses the actual
hand collection; an extra allocated seat changes the deal and fails integration
checks. This was a structural refactor, not a new user-visible requirement.


## Earlier PR15 reconciliation with main e8c66ff (November 6)

Full build: 952 tests, zero failures/errors/skips; Checkstyle 0 main / 0 test,
SpotBugs 0 main / 0 test unexcluded findings, zero analysis errors/missing classes.
The 100-character production rule is retained. No new suppression was added.

The shared ten-class scope includes InitialDeal and the shared session/view;
it is not a whole-game measurement. JaCoCo covers 2,216/2,216 instructions,
216/216 branches, 438/438 lines, 204/204 complexity counters, 96/96 methods and
10/10 classes. These counters do not prove independent basis paths. PIT
produces 237 mutants: 230 killed and 7 survivors (97.05%), with no uncovered,
timeout or error mutants. Its line coverage is 439/439. Fourteen selected test
classes ran 1,379 test executions in this run. The seven baseline survivors
are the six evaluator mutants plus the contract-preserving hash-code mutant
already documented above; the previous eight-survivor figure belongs to the
November 5 scope before the setup refactoring killed the allocation mutant.

A separate combined-source build includes the 18 ShowdownPanel cases from PR16:
970 tests and static checks passed at that earlier compatibility checkpoint.
The merged-history and CI verification are now recorded at the top of this file. The two result displays retain separate
resource keys because one formats a message plus settlement status and the
other formats winner names alone. Null-flow refactors, privacy/error states,
accepted-action logging and the visible Call button remain covered.
