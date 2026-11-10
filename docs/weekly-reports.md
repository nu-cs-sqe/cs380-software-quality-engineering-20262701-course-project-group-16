# Weekly Reports

One section per week from Week 5 through the last week of class. Each records
the goal, owner, task status, evidence, progress evaluation and next steps.
**Done** means completed in the stated scope. This is the single weekly report;
checkpoint commits, verification and remaining work are recorded below.

## Week 5 (10/26/2026–11/01/2026)

**Goal:** Build card/deck and player/state foundations with BVA/TDD, then integrate
setup toward readiness for the first turn.

### Planning and progress tracking

| Status | Owner | Work | Evidence |
| --- | --- | --- | --- |
| Done | Estella | HTTPS clone, JDK 11/25 setup and local build; project scope, README/rubric review and build/test CI. | [README](../README.md); [CI workflow](../.github/workflows/ci.yml). |
| Done | Estella | Player identity, six-seat setup, immutable state, action contracts, private views and statistics, with tests/BVA. | [Shared contract](design/shared-contract.md); [BVA](bva/README.md); branch `feature/game-state-and-stats`. |
| Done | Kevin | Immutable cards, null validation, value equality/hash codes; 15 cases. | [Card BVA](bva/Card.md); GREEN `2219068`, `acbd186`, `a35669d`. |
| Done | Kevin | Full deck, validated/immutable dealing and controlled shuffling; 48 cases covering fresh/used/empty decks, boundary/extreme inputs and unchanged state after rejection. | [Deck BVA](bva/Deck.md); RED `a1490c5`; GREEN `630deff`, `3a53ede`, `d2ad3b7`; Two bounded Random-stub checks `f1c922e` also detect a shuffle that ignores its injected source. |
| Done | Kevin | Six two-card hands, 12 unique dealt cards, 40 remaining; shared evaluator interface and design. | `9bf1c6e`; DeckSetupTest; [setup design](design/game-setup.md). |
| Done, Nov 2 follow-up | Together | Merge both component branches using one shared Card model; preserve TDD history. | [PR #6](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/6), [PR #8](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/8); merge `881ad79`. |

**Branches:** Kevin `feature/cards-and-deck`; Estella `feature/game-state-and-stats`;
shared documentation `docs/project-setup`.

**Progress evaluation:** Member A's Week 5 components are complete. The merged
suite passed 554 cases: 64 Member A, 488 Member B and two integration cases.
The team had not completed first-turn readiness. Application initialization,
Swing and later evaluation/quality work are recorded in Week 6.

**Carry forward:** Finish initialization/Swing integration, game rules, project-board
and review-workflow checks; continue hand evaluation and basic betting with BVA/TDD.

## Week 6 (11/02/2026–11/08/2026)

**Goal:** Finish setup integration; implement best-five evaluation and winner/tie
comparison; add English/Chinese support; configure and measure quality checks.
[Plan: issue #12](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/12).

### Planning and progress tracking

| Status | Owner | Work | Evidence / checkpoint |
| --- | --- | --- | --- |
| Done | Kevin | GameInitializer composes setup/deck into PREFLOP: chips, blinds, positions, two cards per seat and 40 remaining. Consolidate domain classes/tests into `domain.model`. | RED `2ae10c9` → GREEN `600ad38` (16 cases); package move `d0109ec`; [initializer BVA](bva/GameInitializer.md). |
| Done | Kevin | SetupSession/SetupView and launchable Swing setup with explicit card reveals, safe handoffs/restarts and hidden cards on focus loss. | RED `bc5accd` → GREEN `88a535b`; RED `36a2975` → GREEN `58f968a`; [PR #11](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/11). Locale fix RED `217434e` → GREEN `2ac9fe3`; the later English-only revision was merged in PR #11 before Week 6 bilingual support. |
| Done | Kevin | Validate seven-card holdings; compare all nine categories, kickers, wheels, best-five selection and ties without modifying inputs. | RED `f5fd72e` → GREEN `631bb87`; RED `935e12b` → GREEN `8291f17`; 53 cases; [evaluator BVA](bva/PokerHandEvaluator.md). |
| Done | Kevin | Resolve eligible ACTIVE/ALL_IN winners/ties; reject unfinished states; recognize uncontested wins. Return immutable AWAITING_SETTLEMENT without payouts. | RED `29442e4` → GREEN `0d5172e`; 21 cases; [resolver/result contract](design/showdown.md). |
| Done | Kevin | Connect real initialized cards/state to known-winner and board-tie showdown fixtures, preserving chips and the original state. | `428b161`; two integration cases; fixtures supply a closed SHOWDOWN state. |
| Done | Kevin | English/Chinese discovery, selection, UTF-8 messages, locale formatting and privacy on language changes; new locales need resources/configuration only. | RED `132e255` → GREEN `f6ad8e5`; hardening `7717b89`; argument-validation RED `c72b794` → GREEN `025baef`; 69 focused cases; [localization](design/localization.md). |
| Done | Kevin / shared quality | Configure Checkstyle/SpotBugs, scoped JaCoCo/PIT and CI report uploads; fix findings and review survivors. | `7e65692`, `c7dac83`; measurement records `5b4b092`, `377a143`; [quality results](quality.md). |
| Done, merged through PR #15 | Estella | Connect BettingEngine actions/turns/streets to the real showdown resolver through one SetupSession; handle uncontested/all-in runout and atomic failure/retry. | [Session BVA](bva/SessionBetting.md); [played-hand flow](design/played-hand.md). |
| Done, merged through PR #15 | Estella | Add Swing legal actions, automatic temporary check/call bots, public board/status, private handoffs and English/Chinese pending results. | [Played-screen cases](bva/PlayedGamePanel.md); two real played winner/tie integration cases. |
| Done, merged through PR #15 | Estella / shared quality | Preserve setup/privacy checks, update mutation test selection and review findings; initial local checkpoint passed 939 tests with clean static checks; current merged verification is recorded below. | [Local TDD record](tdd/week6-session.md); [current shared-scope measurements](quality.md). |
| Done | Kevin | Update README, BVA/design contracts and weekly report with checkpoint evidence and open dependencies. | `8a322f9`, report cleanup `42a02d5`; [BVA index](bva/README.md). |
| Done, component only | Kevin | Reusable English/Chinese showdown display: winner/ties/uncontested, awaiting settlement, stale-input guards and clearing. Reads public names only; the played-hand screen uses its own translated inline result. | Branch `feature/showdown-display`; RED `2493bf3` → GREEN `750e7c4`; 18 cases; [result contract](design/showdown.md). |
| Done | Kevin | Feed accepted human/bot actions once into pending PlayStats; preserve rejection/retry and failed-start state. Complete fixed-seat match/stop rules. | Branch `feature/week6-finish`, code `634b2c6`; 12 adapter + 16 integration cases; [BVA](bva/HandStatistics.md), [game rules](requirements/game-rules.md). |
| Done | Kevin | Settle main/side pots with split ties and refunds; complete player statistics once, carry totals/chips/IDs into later hands and show bilingual read-only statistics. Fix short-blind and last-active decisions; split UI construction. | `feature/week6-finish`; payouts `1a99b7d`, cumulative tracking `78e5271`, decision fixes `e1c2992`; [completion BVA](bva/CompletedStatistics.md), [UI BVA](bva/StatisticsPanel.md). |

**Branches:** Initializer `feature/setup-dealing`; session/Swing `feature/swing-setup`
(merged through PR #11); Kevin branch `feature/hand-evaluation`, based on
merged main `9dbaa39` and now merged through
[PR #13](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/13)
(commit `d2a78f3`).

**TDD history:** Initializer RED had 12 expected failures before all 16 cases
passed. Session RED had 19 expected failures and one existing pass before all
20 passed. Ranking RED had 34 expected failures and three existing ties; all
53 validation/ranking cases passed after GREEN. Localization RED had 40
expected failures plus ten existing passes. Resource-failure hardening expanded
the focused suite to 52; placeholder RED then had 12 expected failures among
68 cases, and GREEN plus a literal-brace fixture reached 69 (52 catalog/17 UI).
The resource hardening tested existing behavior and is not claimed as a new
RED cycle. The final fix was reviewed with no remaining actionable findings.

**Intermediate team suites:** 554 after component integration → 570 after the
initializer/package move → 590 after the session → 603 after original Swing →
601 after the English-only revision → 617 after evaluator validation → 735 at
the final November 3 check. These are historical scopes, not individual targets.
Setup checks preserve the 10/20 blinds, 6,000-chip total, correct positions,
two-card hands, 40-card remainder and private views.

### Add tests and simplify setup (November 6)

Branch: `refactor/instructor-feedback`. Each task has a separate checkpoint commit.

| Task | Status | Work and verification |
| --- | --- | --- |
| Simplify setup methods (`34effa7`) | Done | Extract clockwise dealing, hand allocation, player copying/creation and configuration validation into named helpers. Existing setup/dealing and BVA tests preserve behavior. |
| Add isolated tests (`7b61f41`) | Done | Inject setup/deal operations; retain original real-component cases under integration test names and add 11 isolated coordinator cases. Include extracted InitialDeal in quality scope. |
| Fix null handling (`00819a8`) | Done | Use Optional for session/actor/statistics-hand absence and empty lists for hidden cards; remove nullable UI rendering arguments. Preserve public validation and legacy actor input compatibility. Existing lifecycle/privacy/state tests cover these paths. |
| Simplify localization (`55bd93d`) | Done | Split element scanning, argument recording and nested-choice handling; extract translation validation. Enforce 100-character production Java lines and wrap existing long lines without behavior changes. |

### Verification and progress evaluation

Historical checkpoints: the November 5 result-display build passed 753 tests;
the November 6 setup/test refactoring build passed 746 tests. These are separate
branch measurements. Both the 18 result-display cases and 11 isolated coordinator
cases are retained in the combined branch.

Latest merged baseline: [PR #19](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/19)
merged on November 8 into main `c71e9d6`. Its recorded local verification is the
November 6 `feature/week6-finish` `build pitest` run on Gradle 9.7.1 / JDK 17
with Java 11 compilation/tests. Its [CI run](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/actions/runs/42324270929)
passed build, Checkstyle, SpotBugs and PIT. No new local run is claimed by this
planning update. Formal review/enforcement evidence remains separate.
Earlier main `ac70b3b` passed 970 tests and its own CI; [quality history](quality.md)
retains the 998-test pending-only checkpoint and earlier measurements.

| Check | Latest local result |
| --- | --- |
| Team suite | **1,033 passing**, zero failures/errors/skips. |
| Checkstyle / SpotBugs | Zero main/test findings after existing exact exceptions; 100-character production limit retained. |
| 17-component scope | **827 lines, 431 branches, 391 complexity counters, 4,383 instructions, 174 methods and 18 reported classes**, zero missed counters; these are scoped counters, not whole-game/basis-path proof. |
| PIT | **440/447 killed (98.43%)**, 7 survivors; 0 uncovered. See [survivor review](quality.md). |
| UI | English/Chinese completed totals, payment/continuation and privacy pass; offscreen layouts checked. Native focus/font walkthrough remains pending. |

Current pipeline: play supported actions → compare → **Settle hand** → update
completed totals once → **Next hand** with the same IDs/chips/statistics. Paid
hands conserve chips and eliminate zero stacks; one remaining stack finishes
the match. Start Game resets the match; stopping closes it. There is no individual
leaving/replacement or saved profile. Bets/raises, postflop calls and equity bots
remain future work.

TDD: 9 payout, 4 completion/copy, 4 continuation and 2 bilingual UI cases failed
before implementation. Three short-blind/last-active cases and two embedded
header cases also failed before their fixes. Later regression tests address
mutation gaps. The earlier 28-case pending tracker checkpoint remains recorded
in its BVA and quality history. Code review follow-up found no remaining
issues in the three corrected betting paths.

The refactoring preserves behavior. The 11 new coordinator cases were run
first against an unimplemented injection boundary and all failed as expected,
then passed after implementation. Existing BVA/integration tests protect the
method, null-flow and parser refactors; these are not claimed as new feature
RED cycles. Original setup/dealing integration cases and their historical
checkpoint commits remain recorded above. PR status and remote checks are tracked
on GitHub.

Earlier merged baseline: the supported played-hand flow was merged through
[PR #15](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/15)
into main `ac70b3b`. SetupSession connects legal FOLD/CHECK/CALL decisions,
closed-round advancement, temporary CHECK/CALL bots and real winner/tie or
uncontested comparison to the bilingual Swing screen. Results await settlement;
no pot payout or completed-hand aggregation is claimed. The reusable
ShowdownPanel from PR #16 is also retained; GamePanel uses separate inline result
messages. Real integration and UI cases cover played winners/ties, all-in runout,
rejection/atomic retry and privacy. Unit coordinator cases use controlled
setup/deal operations. Native focus/font walkthrough remains pending.

Full betting is still incomplete: BET/RAISE, raised preflop rounds and postflop
CALL are explicitly unsupported. Payouts/side pots, next-hand rotation and elimination/match completion are now
implemented by the checkpoint above. Equity bots and saved profiles remain later work. Scoped quality
counters do not establish full-game or independent basis-path coverage.

### Estella's local integration verification — November 5

Historical checkpoint; superseded by the merged-main verification above.

The pending integration of main at 11a4e00 with feature/basic-betting now runs the
supported played flow through Swing to a real winner/tie result awaiting
settlement. **939 tests pass**, zero failures/errors/skips; Checkstyle and
SpotBugs have0 main/test unexcluded findings. The new 42 cases extend the 897-case
merged baseline. TDD ran RED before session/UI implementations; the record is
local and is not claimed as separate RED/GREEN Git commits.

The controlled call/fold/check hand ends with 21 accepted actions, five live
participants, pot 100, stacks 5,900 and 32 remaining cards. Tests also verify
uncontested endings, all-in runout, rejected-action preservation, atomic
comparison retry, human privacy and both languages. Automatic bots only check
or call; no opponent cards/equity are used. Pending actions are logged once;
no payout, settled completion or completed-hand statistics are fabricated.

Nine-class shared-scope JaCoCo counters are all covered; PIT kills219/227
mutants (96.48%), retaining the eight documented baseline survivors. Session
43/43 and view 18/18 mutants are killed. These are not whole-game quality
measurements or proof of independent basis paths. See [quality.md](quality.md)
and [TDD evidence](tdd/week6-session.md). Offscreen English/Chinese layouts were
inspected; native focus/teammate walkthrough and new remote CI remain pending.

### Call-button layout follow-up — November 5

A larger button font could wrap Call below the action row and clip it. The
screen now keeps Fold/Check/Call in one row, with compact margins and labels
set before measuring row height. Two bilingual visibility cases were added:
English failed before the fix; both pass afterward. The full build now passes
**941 tests**, with zero Checkstyle/SpotBugs main/test findings. This UI-only
follow-up does not extend the mutation measurement scope. See
[UI-PLAY06](bva/PlayedGamePanel.md#call-button-visibility-regression).

### Merge setup/test changes into the played-hand flow (November 6)

Historical reconciliation checkpoint; the final merge is recorded above.

The local merge targets main e8c66ff while retaining the played-hand flow and
Call-button visibility fix. Keep the injected initializer/dealer seam, renamed
unit/integration tests, Optional session/actor state and smaller rendering helpers.
Production Java still follows the 100-character Checkstyle rule; no checks or
SpotBugs exclusions were weakened. The UI uses a non-null explicit error enum.

Local verification: **952 tests pass**, zero failures/errors/skips, with clean
Checkstyle and SpotBugs reports. The ten-class shared scope has zero missed
JaCoCo counters. PIT kills 230/237 mutants (97.05%), retaining the seven documented
baseline survivors. These are scoped counters, not full-game/basis-path proof.
A separate combined-source check including PR16's 18 result-panel tests passes
**970 tests** and static checks. The inline result keys are namespaced separately
from ShowdownPanel's single-argument messages to preserve both consumers.

Those figures describe the earlier local reconciliation checkpoint. The resolved
branches have since merged through PR #16 (`f6a5bf4`) and PR #15 (`ac70b3b`).
Setup/test results (746), earlier played-flow results (941), reconciliation (952)
and combined-source results (970) describe different checkpoints.

### Remaining work and next steps

- [x] **Estella + Kevin:** Supported fold/check/call flow, streets, real results and bilingual Swing merged through PR #15/#16. PR #19 subsequently adds settlement, completed totals and continuation; full betting carries into Week 7.
- [x] **Kevin:** Real pot/refund settlement, completed match statistics, fixed-seat continuation/elimination and bilingual statistics display.
- [ ] **Together:** Carry full betting, reopening, postflop calls, profiles and adaptive bots into [Week 7 issue #20](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/20): Kevin K01–K06; Estella E01–E06.
- [x] **Kevin:** Split GamePanel construction into header/sidebar/private/action/table/footer helpers.
- [ ] **Together:** Keep later changes small and cohesive; BettingEngine.advanceStreet remains a possible focused refactor. Follow [method-design guidance](design/README.md#method-design).
- [ ] **Together:** Walk through native desktop focus-loss privacy and cross-platform fonts/layout.
- [x] **Kevin:** Complete [game rules](requirements/game-rules.md) for the Week 6 scope: 10/20 blinds, random first button, fixed seats and whole-game stop. Week 7 K04 must replace the current individual-leaving prohibition with the requested quit-as-fold lifecycle; profiles remain pending E01/E02.
- [ ] **Together:** Confirm board assignees/status/PR links and record meeting evidence.
- [ ] **Together:** Review rubric interpretation and mutation survivors with the instructor; obtain partner-owned quality measurements separately.
- [x] **Kevin:** Implementation branch merged through PR #13 (`d2a78f3`).
- [ ] **Together:** [PR #19 CI](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/actions/runs/42324270929) passed for the current implementation baseline; required review/check enforcement and deliberate failure detection still need separate verification under Week 7 Q09.

## Week 7 (11/09/2026–11/15/2026)

[Plan and live checklist: issue #20](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/20).

**Status:** In progress: E01/E02 backends verified locally November 9; desktop
profile integration verified locally November 10, against main `c71e9d6` plus
account commit `e1a96f4`. Kevin is Member A
(betting/departure/contracts); Estella is Member B (profiles/adaptive bots).
The checklist IDs below match issue #20. Update both locations as work is
verified; record local/in-review work accurately without checking it as merged Done.

### Goal

Complete the agreed backend, with adaptive bot AI tracked as a separate backend workstream. Frontend expansion follows the resulting application APIs; preserve existing Swing privacy and locale behavior.

### Verified baseline

- [x] B01 — PR #19 merged into main at `c71e9d6`: setup, evaluation, basic actions, settlement, main/side pots, ties/refunds, completed statistics and match continuation already exist.
- [x] B02 — PR #19 records 1,033 passing tests. Its [CI run](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/actions/runs/42324270929) passed build, Checkstyle, SpotBugs and PIT. This is baseline evidence, not a new Week 7 run.
- Current measurement covers 17 parent components; remaining betting/state classes are outside its denominator. Baseline PIT: 440/447 killed, seven documented survivors. These are not whole-backend or independent basis-path claims.

Baseline: [PR #19](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/19). Documentation: [weekly report](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/blob/main/docs/weekly-reports.md), [quality scope](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/blob/main/docs/quality.md), [instructor review](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/blob/main/docs/instructor-code-review.md).

### Requirements to preserve

- Six stable seats; 1–6 human players; bots fill remaining seats at setup.
- Shared-computer private-card handoffs.
- Virtual chips; 1,000 starting chips per player; fixed 10/20 blinds.
- Complete no-limit Hold'em: all-ins, side pots, ties and elimination.
- Continue matches until one player holds all 6,000 chips.
- Quit-as-fold. The existing prohibition on individual leaving must be revised; define current/non-current actor, already-all-in and later-hand behavior before implementation.
- Persistent password-protected local human accounts and completed statistics.
- No saved unfinished matches.
- English/Chinese support; new locales through resources/configuration without editing existing Java code.
- Bots use legitimate information, varied decisions and learned opponent tendencies. Keep equity, range and strategy diagnostics internal.

### Kevin — Member A (@kevinwang328)

Owner of betting, departure behavior and application contracts.

- [ ] K01 — Complete betting on every street: BET, RAISE, postflop CALL and responses to raised preflop rounds. Preserve raise-to semantics; requests supply round targets and the engine calculates deductions.
- [ ] K02 — Complete full/short all-ins and reopening: full raises, short all-in raises, cumulative reopening, pending decisions and individual raise eligibility. Preserve the big blind's option and short-blind behavior.
- [ ] K03 — Produce authoritative statistics context: opening raises, three-bets and later raises separately from reopening eligibility; final preflop aggressor and actual flop c-bet history. All-in calls remain CALL.
- [ ] K04 — Implement quit-as-fold and revise the conflicting game rule. Specify and test turn order, already-all-in players and later hands; preserve seats, chip ownership and participation statistics. Departure cannot block progression.
- [ ] K05 — Expose legal-action information: available actions, actual calling cost, minimum full bet/raise targets, maximum targets and permitted short all-ins. Humans and bots consume the same engine-owned limits.
- [ ] K06 — Include betting/state components and relevant tests in coverage/mutation measurement; resolve uncovered paths and non-equivalent survivors.

### Estella — Member B (@estellaxie)

Owner of profiles and the adaptive-bot task.

#### Profiles and completed statistics

- [ ] E01 — Provide backend operations to register, reopen and authenticate password-protected local human profiles. Keep profile IDs separate from match participant IDs and seats; never save plaintext passwords.
- [ ] E02 — Persist completed lifetime statistics across restarts/new matches, distinct from match totals. New matches reset stacks; lifetime totals remain. Save deduplication identity and test failed writes, invalid data and retries without duplicate aggregation. Do not save pending matches.

**E01 progress (November 9):** Verified locally on `feature/player-profiles`:
registration with UUID profile IDs, public identity listing/lookup, password
authentication and atomic local identity/credential storage. Reload tests reopen
the same IDs and verify passwords. The full suite passes 1,144 tests;
Checkstyle/SpotBugs are clean. The six profile/account classes have zero missed
JaCoCo counters and kill 86/86 mutants; shared PIT is 526/533 with seven baseline
survivors. BVA records the observed local RED/GREEN cycles and later regressions.
This is the E01 checkpoint; the user's account implementation commit is
`e1a96f4`. E02 below adds authenticated match binding and completed lifetime
persistence. Login/profile screens and verified review/CI/merge gates remain
pending; E01 stays unchecked until those gates are met.

**E02 progress (November 9):** Verified locally on `feature/player-profiles`:
authenticated human/profile bindings, completed raw counters and duplicate IDs,
V2 → V3 catalog upgrade, separate match/lifetime totals, atomic save-before-payment,
restart/new-match behavior and retries before/after durable save. Final build:
1,196 tests passing; Checkstyle/SpotBugs clean. Four new executable components
kill 72/72 mutants; all ten new/modified E02 components kill 331/331. Shared PIT
is 636/643 with seven unchanged baseline survivors; scoped coverage has zero
missed counters. Local TDD observed 28 RED → GREEN and 11 RED → GREEN cases;
no separate Git checkpoints or E02 commit/push are claimed. Login/profile screens,
remote CI, peer review and merge remain pending; E02/I04 remain unchecked.

**Desktop profile integration (November 10):** Production startup now uses local
password accounts. English/Chinese registration, sign-in, 1–6-human selection,
profile names, saved lifetime rows and retryable storage errors are connected to
the E01/E02 APIs. Password work runs off the event thread and captured char arrays
are cleared after success/failure. Existing private-card handoffs remain in place;
roster removal affects only the next match. Local suite: 1,219 passing, including
23 new profile-screen cases. The original 20 cases were observed RED before UI
implementation; three later regressions are additional hardening. One exact live-
Swing-model serialization exception is documented. Native visual/focus checking
remains pending because the computer-use bridge was unavailable; automated layouts
pass in both languages. Final build/PIT passed: Checkstyle/SpotBugs clean;
PIT 636/643, seven baseline survivors unchanged. No commit/push, new remote CI or
peer approval is claimed. E01/E02/I04 and review/merge gates remain unchecked.

**Password length follow-up (November 10):** New accounts now require 6–15
characters; English/Chinese hints use plain wording and two lines. Sign-in retains
6–128-character verification for existing credentials, with no file migration or
hashing change. Revised BVA ran 15 password cases against the prior policy:
7 expected failures and 8 existing passes, then all 15 passed after implementation.
The current full test suite passes 1,223 tests; static checks are clean. Final
coverage/mutation evidence is recorded in quality.md. No commit/push or review/
merge completion is claimed; task checklist states remain unchanged.

#### Adaptive bot AI — separate backend task

Implement independently testable equity estimation, opponent modeling, range sampling and decision-policy responsibilities; reuse the existing evaluator and action contracts.

- [ ] E03 — Implement Monte Carlo equity estimation: sample compatible opponent hands and board cards without duplicates; include multiway outcomes, fractional tie shares and side-pot eligibility. Use only the bot's cards and legitimate public information.
- [ ] E04 — Implement opponent learning from eligible public decisions, opportunity counts, confidence and context using a small smoothed model. Keep exact PlayStats reporting separate from beliefs. Exclude forced blinds, rejected actions and quit-generated automatic folds from strategic-preference learning.
- [ ] E05 — Implement varied decisions using equity, pot odds, position, stacks, legal limits and opponent estimates. Use documented probability distributions over reasonable legal actions and modest personality differences. Describe this as an adaptive probability-based policy; do not claim proven GTO play.
- [ ] E06 — Integrate bounded, cancellable calculations without blocking Swing's event thread. Reject stale decisions from an old hand/state/match. Engine validation remains authoritative.

### Together — complete backend integration

- [ ] I01 — A raised hand progresses through streets, settles correctly, updates statistics once and continues.
- [ ] I02 — Short all-ins produce correct reopening decisions, pots, refunds and elimination.
- [ ] I03 — Quit-as-fold preserves the documented lifecycle and chip totals.
- [ ] I04 — Completed profile statistics survive restart without duplicate counting.
- [ ] I05 — Bots act legally using filtered information; changing inaccessible cards/deck data cannot alter their observable inputs or seeded decisions.
- [ ] I06 — Controlled opponents demonstrate measurable learning and variation.
- [ ] I07 — Heads-up transition and final match completion conserve all 6,000 chips.

At least two substantial integration flows must run real components; distinguish these from isolated unit tests.

### Bot evaluation

- [ ] M01 — Measure equity error against manageable exact cases, action legality, action-prediction quality, learning enabled/disabled comparisons, action distributions and decision latency. Use controlled seeds/opponents and uncertainty across repeated games; do not require a bot to win an individual test.

### Rubric and instructor-review gates

The course rubric is cumulative from D through A. Backend completion alone does not establish final frontend/application completion or an individual A.

- [ ] Q01 — BVA matches the implemented methods, system states, boundary inputs and assertions. Maintain the required per-class documentation convention and add/update analysis for new/extracted non-view, non-enum classes.
- [ ] Q02 — Preserve genuine test-first RED → GREEN → refactor Git evidence for applicable non-View/non-Main code. Keep historical evidence accurate.
- [ ] Q03 — All existing and new tests pass. Preserve privacy, short-blind, conservation and failure/retry regressions.
- [ ] Q04 — Coverage includes all applicable backend code with only rubric-permitted exemptions. Meet the 100% cyclomatic-coverage target; report line/branch/complexity separately and review paths rather than treating JaCoCo counters alone as independent basis-path proof.
- [ ] Q05 — Kill every non-equivalent mutant. Review and document every survivor, report raw results and justify equivalence; resolve the outstanding baseline interpretation with the instructor.
- [ ] Q06 — Apply Clean Code: focused named responsibilities, controlled/injectable collaborators for isolated tests, explicit internal absence and boundary validation. Review engine, session, continuation, storage and AI code; retain the 100-character production line limit.
- [ ] Q07 — Checkstyle, SpotBugs and CI pass; review exact existing exclusions and record actual scope/results. Green CI alone does not establish complete coverage or mutation compliance.
- [ ] Q08 — Preserve English/Chinese startup selection and resource-based extension, including new outcomes/errors and existing UI regression tests. Verify full new frontend flows when those interfaces are implemented.
- [ ] Q09 — Both teammates review each other's changes. Verify main requires PR approval and required CI checks; coordinate with a repository administrator where necessary.
- [ ] Q10 — Keep game rules, detailed project-board tasks and the existing weekly report synchronized with actual status, evidence and limitations. Preserve early CI/process history and record instructor-meeting participation and individual contributions; complete peer evaluation when due.

### Definition of done and status updates

Each task keeps its ID in this issue and the Week 7 weekly-report section.

- Leave an item unchecked while Planned, In progress, Verified locally/awaiting commit, or In review.
- Check it only after its acceptance criteria and relevant BVA/tests/quality checks are verified, evidence is linked, and code changes have completed peer review and merge.
- For each completed item, update both this issue and the existing weekly report with the same ID, implementation PR/commit, relevant test/CI evidence and any remaining limitation.
- Keep this issue open until all implementation, integration, evaluation and quality items are complete. Do not mark current scoped baseline measurements as full-backend evidence.

The backend is finished when every agreed feature works through application APIs; tests pass; applicable coverage/mutation gaps are resolved; static checks/CI pass; peer review and enforcement are verified; and rules, board and weekly report match the implementation. Frontend expansion remains a separate application milestone.


### E01 profile identity increment — November 9

Historical model-only checkpoint: 20 expected RED failures, then 20 passing
PlayerProfile cases; full suite 1,053 passing, clean static checks, zero missed
profile counters and 5/5 profile mutants killed. The later account increment below
supersedes the original pending-service/storage status. These are local test runs,
not separate RED/GREEN Git commits.

Evidence: [PlayerProfile model](../src/main/java/domain/model/PlayerProfile.java),
[unit tests](../src/test/java/domain/model/PlayerProfileTest.java),
[case mapping and RED/GREEN evidence](bva/PlayerProfile.md),
[measured scope and mutation review](quality.md#playerprofile-model--november-9-2026).
No Week 7 task is marked Done by this local increment.

### E01 local password accounts — November 9

User clarification: profiles live on this device and require passwords when
reopened. The backend now registers accounts, lists saved public identities and
authenticates the selected stable profile ID after restart. Registration publishes
identity plus salted PBKDF2 credential atomically; failed writes, corrupt data and
provider failures do not create a successful partial account. No passwordless
registration API or saved unfinished match was added.

Observed TDD: service/storage 53 RED → 53 GREEN; password-account tests 84 RED →
84 GREEN. Seven later provider/input/corrupt-catalog regressions are not claimed
as feature RED cycles. Final suite: 1,144 passing; static checks clean; six new
profile/account classes fully covered by reported JaCoCo counters and 86/86 mutants
killed. Shared scope is 23 parent components, not the complete backend. Its seven
baseline survivors and PIT's generated-cleanup line marker remain visible in
[quality results](quality.md#local-password-accounts--november-9-2026).

Evidence: [service](../src/main/java/application/ProfileService.java),
[file adapter](../src/main/java/persistence/FileProfileRepository.java),
[real reload/authentication tests](../src/test/java/integration/ProfilePersistenceIntegrationTest.java),
[account BVA](bva/ProfileService.md) and [storage BVA](bva/FileProfileRepository.md).
All file tests use JUnit temporary directories; no real profile data was created.
At this E01 checkpoint, the login screen, match binding and E02 persistence were
pending. The E02 increment below adds the backend binding/persistence. Login and
lifetime-statistics screens remain pending; the original account commit is
`e1a96f4`. No E02 commit/push or new remote CI/review/merge is claimed.

### E02 completed lifetime statistics — November 9

Implemented immutable raw completed snapshots and restoration, issuer-bound login
handles, stable human-only profile bindings and one atomic V3 catalog save for all
human completion deltas. V2 password accounts load with empty totals and upgrade
only on successful save. Match totals and stacks reset for new matches; lifetime
counters and completed IDs persist. Bots and unfinished matches are not saved.

TDD: 28 expected RED → 28 GREEN snapshot cases, then 11 expected RED → 11 GREEN
service/storage/application cases. Thirteen later validation/privacy/failure-window
regressions are additional coverage, not claimed feature RED cycles. Final suite:
1,196 passing; Checkstyle/SpotBugs clean. `build pitest` passed on the same final
production code with 636/643 mutants killed; seven baseline survivors remain.
The final extra PM-C05 test then passed with the full build; production was unchanged.
Four new executable components kill 72/72 mutants; all ten added/modified E02
components kill 331/331. Scope is 27 components, not the entire backend, with
zero missed JaCoCo counters. See [quality evidence](quality.md#completed-lifetime-statistics--november-9-2026).

Real integration covers a played showdown saved/reloaded into a fresh match,
failed-write retry before payment, and durable-save/error/retry without a second
write or increment. The latter verifies completion IDs survive actual disk reload.
Evidence: [lifetime BVA](bva/LifetimeStatistics.md),
[snapshot model](../src/main/java/domain/model/CompletedStatistics.java),
[profile binding](../src/main/java/application/ProfileMatch.java) and
[integration tests](../src/test/java/application/ProfiledMatchIntegrationTest.java).
No real user data or extra scratch reports were created. The existing Swing
Start Game remains unprofiled until login/profile screens consume these APIs.
E02/I04 are verified locally, awaiting the user's commit, remote CI, peer review
and merge. All 30 top-level Week 7 items remain unchecked under the agreed gates.

### Desktop profile integration — November 10

Completed the next frontend increment: Main opens Profiles, where users create
and password-sign-in to local accounts, select the ordered human roster, start
profile-bound matches and inspect lifetime statistics. Existing Table and
Statistics flows use profile names and preserve the shared-computer handoff.
Register/sign-in/list/read operations use background workers; game mutation is
disabled during those operations. Wrong passwords and storage/provider failures
have localized feedback. A failed settlement can be retried without paying or
counting twice. Ongoing matches keep their bindings when the next-match roster changes.

Evidence: [profile-screen BVA](bva/ProfileScreen.md),
[view](../src/main/java/ui/ProfilePanel.java),
[23 component/integration cases](../src/test/java/ui/ProfileGamePanelTest.java),
[quality results and exception review](quality.md#desktop-profile-flow--november-10-2026).
Full local `build pitest` passed: 1,219 tests; Checkstyle/SpotBugs clean;
636/643 mutants killed with seven unchanged baseline survivors. UI/Main keep the
documented coverage/mutation exemption; all static checks still run. No real account data or extra scratch
reports were created. The attempted native preview could not be inspected because
the desktop bridge was unavailable and was closed. Native font/focus walkthrough,
commit, remote CI, peer review and merge remain pending.

### Progress evaluation

Planning was recorded on November 8, 2026. On November 9, E01's profile model/account
backend and E02 completed-lifetime-statistics backend were verified locally as
recorded above. B01/B02 remain
the verified merged baseline; all 30 top-level Week 7 items remain unchecked
because their complete acceptance/review/merge criteria have not been met.
The 1,033-test baseline belongs to PR #19; the latest local suite passes 1,219 after the November 10 desktop increment.
Adaptive AI remains a distinct backend workstream with its own acceptance criteria.

### Next steps

Agree on the K05 action/amount contract and K04 departure lifecycle together.
Kevin implements K01–K06; Estella reviews/commits E01–E02 and then proceeds with
AI E03–E06 as a separate workstream using the agreed K05 action contract. Review each other's work and link commits/PRs, BVA/tests,
CI results, measurements and limitations under the corresponding task IDs.
Keep issue #20 and this existing report synchronized; close the issue only after
all acceptance gates pass.


## Future weekly report template

### Week N (MM/DD/YYYY–MM/DD/YYYY)

**Goal:** Planned outcome.

| Status | Owner | Task and acceptance criteria | Evidence / branch / PR |
| --- | --- | --- | --- |
| Planned / In progress / Done / Blocked | Name | Concrete task | Link or verified checkpoint |

**Progress evaluation:** Actual outcome, checks, blockers and differences from plan.

**Next steps:** Remaining tasks and owners.
