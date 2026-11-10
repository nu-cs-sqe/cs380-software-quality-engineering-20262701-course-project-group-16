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

Latest local verification: **November 6**, branch `feature/week6-finish`,
`build pitest` on Gradle 9.7.1 / JDK 17 with Java 11 compilation/tests. These
changes need push, remote CI and peer approval. Earlier main `ac70b3b` passed 970
tests and [GitHub CI](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/actions/runs/42232032730);
that result predates this checkpoint. [Quality history](quality.md) retains the
998-test pending-only checkpoint and all earlier measurements.

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

- [x] **Estella + Kevin:** Supported fold/check/call flow, streets, real results and bilingual Swing merged through PR #15/#16. CI passed for main `ac70b3b`; full betting and settlement remain below.
- [x] **Kevin:** Real pot/refund settlement, completed match statistics, fixed-seat continuation/elimination and bilingual statistics display.
- [ ] **Together:** Implement bets/raises and action/reopening rules, including postflop calls; add equity bots and saved profiles in later milestones.
- [x] **Kevin:** Split GamePanel construction into header/sidebar/private/action/table/footer helpers.
- [ ] **Together:** Keep later changes small and cohesive; BettingEngine.advanceStreet remains a possible focused refactor. Follow [method-design guidance](design/README.md#method-design).
- [ ] **Together:** Walk through native desktop focus-loss privacy and cross-platform fonts/layout.
- [x] **Kevin:** Complete [game rules](requirements/game-rules.md): agreed 10/20 blinds, random first button, fixed seats/no individual leaving, whole-game stop allowed; implemented match behavior and saved-profile limits are explicit.
- [ ] **Together:** Confirm board assignees/status/PR links and record meeting evidence.
- [ ] **Together:** Review rubric interpretation and mutation survivors with the instructor; obtain partner-owned quality measurements separately.
- [x] **Kevin:** Implementation branch merged through PR #13 (`d2a78f3`).
- [ ] **Together:** CI passed for merged main [run 42232032730](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/actions/runs/42232032730); required review/check enforcement and deliberate failure detection still need separate verification.

## Week 7 (11/09/2026–11/15/2026)

**Goal:** Complete the agreed backend; track adaptive bots as a separate backend
workstream. The full shared plan, preserved requirements, Kevin's K01–K06 tasks
and integration/quality gates are in [issue #20](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/20).
Quit-as-fold remains the agreed requirement; the older prohibition in the rules
still needs Kevin's K04 revision and implementation.

| Status | Owner | Task and acceptance criteria | Evidence / branch / PR |
| --- | --- | --- | --- |
| Separate committed branch; review/merge pending | Estella | E01/E02: local profiles, lifetime persistence and desktop account integration | `feature/player-profiles`, latest local commit `e0d6015`; detailed checkpoints in issue #20. Those sources are separate from this E03 branch. |
| Verified locally; awaiting commit/review/CI/merge | Estella | E03: legitimate-information Monte Carlo equity, multiway fractional ties, public side-pot eligibility, bounded/cancellable calculation | `feature/monte-carlo-equity`, based on main `c71e9d6`; 51 new cases, full suite 1,084 passing; [BVA](bva/MonteCarloEquityEstimator.md), [design](design/README.md#standalone-equity-e03), [quality](quality.md#e03-standalone-equity--november-10-2026). |
| Planned | Estella | E04: smoothed public-decision opponent learning and contextual uncertainty | Keep beliefs separate from exact PlayStats; exclude forced/rejected/departure-generated actions. |
| Planned | Estella | E05: varied legal decisions using equity, odds, position, stacks and beliefs | Consume Kevin's authoritative legal-action limits and public pot scenarios. |
| Planned | Estella | E06: off-EDT execution, cancellation and stale-decision rejection | Match/hand/state identity must be checked before engine submission. |
| Track in shared issue | Kevin | K01–K06: full betting/reopening, statistics context, quit-as-fold, legal-action contracts and applicable quality scope | Status not independently verified by this E03 task. |
| Pending | Together | I01–I07, M01 and Q01–Q10: real integration flows, bot evaluation, complete quality/process gates | E03 supplies equity-error, privacy and observational latency evidence only; reviews, remote CI and merge remain pending. |

**Progress evaluation:** E03 uses only own/public cards, reuses the existing
evaluator and samples compatible hypothetical deals without duplicates. It
returns numeric per-pot shares, eligible-pot equity and expected payout.
All five new executable components have zero missed scoped JaCoCo counters and
kill 108/108 mutants. Shared PIT kills 548/555; seven baseline survivors remain
visible. Checkstyle and SpotBugs pass without new exclusions. Local TDD observed
8 + 15 + 17 + 3 expected RED cases followed by GREEN; later hardening/integration
cases are recorded separately in BVA/quality. No agent commits/pushes or separate
scratch reports were created.

These results derive from main's 1,033 tests, not the profile branch's 1,223;
the two branches must be integrated through their PRs. No current bot actions
were changed: the calculator is ready for E04–E06, while public scenario creation
remains an engine/application contract. Scoped counters do not prove full-backend
coverage or independent basis paths. Task checkboxes stay open until accepted
review, CI and merge evidence is available.

**Next steps:** Review/commit E03, run remote CI and obtain Kevin's review.
Then implement E04 opponent learning, coordinate public pot/legal-action APIs,
and complete E05/E06 with real-game integration and repeated controlled evaluation.

## Future weekly report template

### Week N (MM/DD/YYYY–MM/DD/YYYY)

**Goal:** Planned outcome.

| Status | Owner | Task and acceptance criteria | Evidence / branch / PR |
| --- | --- | --- | --- |
| Planned / In progress / Done / Blocked | Name | Concrete task | Link or verified checkpoint |

**Progress evaluation:** Actual outcome, checks, blockers and differences from plan.

**Next steps:** Remaining tasks and owners.
