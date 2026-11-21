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

**Status at November 16:** PR #21 profiles, PR #22's instructor document,
PR #24's refactor/equity/learning foundations and PR #25's integration repair
have merged into main 69aae47. E05/E06 integration is committed/pushed as 4a9d9d3
and submitted in PR #26. At the user's request, Estella's I03 quit-eligibility
correction is verified locally on fix/quit-eligibility in the original
checkout. I07's confirmed policy preserves departed stacks and ends without a
winner when fewer than two funded playing seats remain and nobody owns the full
total; implementation is verified locally alongside I03. Kevin owns
betting/departure/contracts; Estella owns profiles/bots.
The checklist IDs below match issue #20. Older checkpoints retain their own
measurements and dates; local verification leaves review/remote-CI/merge gates open.

### I07 full-chip terminal policy — November 16 follow-up

The confirmed policy keeps departed stacks with their owners. A paid match has a
winner only when one player owns the full configured total, normally 6,000.
With zero/one funded playing seat and chips still split, play ends without a
match winner. Two funded playing seats can continue heads-up. All-in departure
can retain a complete payout/victory, and later retirement preserves a completed
champion. English/Chinese resources state a no-winner ending explicitly.

Four winner invocations were observed RED before the query correction, then
GREEN. The six UI boundaries observed four terminal-label failures and two
continuation passes before both resource values were updated. Real Next hand
checks verify heads-up dealer/SB/BB order, 48 remaining cards, privacy and 6,000
chips conserved. Configured 120-chip victory adds a supported-settings boundary.
The 98-case focused run passed.

The first full run passed 1,545 tests and killed 1,823/1,838 mutants, leaving one
new settlement-guard survivor alongside the 14 baseline cases. An explicit
before-start absence regression kills that mutation in the final report.
This is additional mutation hardening; no new feature RED or artificial Git TDD
history is claimed for that case.

Final build pitest --warning-mode all passes in **12m03s** with **1,546 tests**,
clean Checkstyle/SpotBugs, **2,893/2,894 scoped lines** and **1,824/1,838 mutants
killed**. The same 14 baseline survivors remain, with zero uncovered/timeout/error
mutants. Scope is unchanged at 66 parents / 68 reported classes. Exact counters,
first-run evidence and survivor identity comparison are in quality.md. No
source/test/configuration changes follow the final run.

I03 and I07 remain uncommitted together on fix/quit-eligibility in the original
checkout. User commit/push, Kevin's contract review, remote CI and merge are
pending. Local verification leaves the I07 checklist and whole issue open.

### I03 immediate quit-as-fold — November 16 follow-up

At the user's request, Estella implements the correction locally on
fix/quit-eligibility, based on committed/pushed integration 4a9d9d3 (PR #26),
using the original checkout. An active human folds immediately when leaving,
including out of turn. Another pending actor keeps their turn while betting
continues. Already-all-in players keep payout eligibility; folded players and
resolved hands acquire no extra fold. Cards, commitments, fixed seats, privacy
and exact completed statistics are retained.

The three session regressions first failed against the original queued-fold
source. Fourteen engine invocations then failed compilation against the missing
automatic-fold API. A new assertion initially compared Player object identities;
ownership assertions now compare IDs, seats, types, stacks and commitments.
After these test corrections and implementation, all 58 focused checks pass.
Two additional session boundaries and two bilingual UI cases provide further
hardening. Historical tests retain their business assertions with immediate-fold
timing. No synthetic test-first Git commits or extra reports were created.

The automatic event uses a separate no-context learning path, invalidates old bot
work and supports failed-recording retry without publication or duplicate folds.
Full build pitest --warning-mode all passes in **12m17s** with **1,534 tests**,
clean Checkstyle/SpotBugs, **2,892/2,893 scoped lines** and **1,822/1,836 mutants
killed**. Scope stays 66 parents / 68 reported classes. The same 14 baseline
survivors and unreachable enum-default miss remain. There are zero uncovered,
timeout or error mutants. Exact metrics and survivor identity comparison are in
quality.md; no source/test/configuration changes follow the final run.

Status is verified locally, awaiting user commit/push, Kevin's engine/contract
review, remote CI and merge. I03's issue checkbox remains open under those gates.
I07's departed-stack/6,000-chip terminal policy remains a separate follow-up.

### E05/E06 and profile integration repair — November 16

Estella's original feature/bot-integration checkout combines committed bot head
02a8e1a with merged main 69aae47, including PR #25. Ten merge conflicts prevented
the build. Resolution retains authenticated profiles/lifetime persistence and
the E05 policy/E06 worker, public learning and stale-result protections. Build
measurement and PIT test patterns retain the union of both branches.

The initial Gradle build failed on conflict markers. After resolving them, six
profile-screen tests exposed their synchronous-bot fixture assumption. The shared
deterministic UI scheduler now supports profile-bound presentation cases while
real worker tests remain separate. Three additional PM-C07/08 invocations cover
adaptive play, before/after-durable-save failures, retry without duplicate totals,
continued binding/learning and restart rejection of old bot turns. These are
passing merge regressions; no feature RED or synthetic Git history is claimed.

Final build pitest --warning-mode all passes in 12m23s with **1,513 tests**, clean
Checkstyle/SpotBugs, **2,894/2,895 scoped lines** and **1,822/1,836 mutants killed**.
Scope is 66 parents / 68 reported classes. The same 14 baseline survivors and
unreachable enum-default miss remain, with zero uncovered/timeout/error mutants.
Exact counters and identity comparison are in quality.md. Sources/tests/config
were unchanged after the final run; existing BVA/design/quality records are updated.

The merge resolution is staged for user review and commit. Combined remote CI,
peer review and merge acceptance remain pending. Native UI, broader evaluation
and the known departure/terminal-policy findings remain open. Checklist states
are unchanged by local verification.

### Main integration repair — November 16 (Week 7 follow-up)

The initial combined build failed on the duplicated localization-test selector
and a dangling Objects.requireNonNull call. After those compile fixes, the team
tests passed but Checkstyle rejected 19 remaining profile null guards. The repair
removes those guards, retains the instructor's strict rules and puts missing
saved-property validation at the storage boundary. Missing bound-hand deltas are
rejected before saving. Existing transaction, failure/retry and conservation
checks remain. Fifteen null-only invocations are retired; their BVA history stays
visible. Twenty-four new catalog-field cases plus retained corruption/missing-
delta cases exposed 13 RED failures during cleanup, then all 65 focused cases
passed with the boundary fixes. New helpers name schema, value, history and
publication responsibilities; no broad helper or metric exclusion was added.

Final build pitest passes in 7m20s: **1,396 tests**, clean main/test/seat
Checkstyle and SpotBugs, 2,265/2,266 scoped lines, and **1,400/1,414 mutants killed**.
All 14 survivors are the same baseline cases; there are no uncovered/timeout/error
mutants. Measurement retains 56 parent components / 57 reported classes; the one
unreachable GamePhase default and survivor acceptance remain visible in quality.md.
No commit, push, extra checkout or scratch report was created. E05/E06, native UI
work, the password-character policy and existing departure/terminal-policy findings
remain separate follow-ups. User commit/push, new remote CI and peer review remain
pending, so no completion checkbox is newly checked.

### PR #21 conflict resolution — November 15 (historical)

The original checkout is on feature/player-profiles. An uncommitted merge of
origin/main bda4954 into profile head e0d6015 resolves README, build configuration,
quality history, GamePanel and both language bundles. Profile authentication and
lifetime persistence are retained with full betting, legal wagers and departure
controls. PIT includes both profile persistence/security and main's betting tests;
measurement is the union of their 44 parent components, with no new exclusion.

UI-AC16-en/zh observed two expected RED failures for account-work control locking,
then passed after the new wager/departure controls joined the existing busy list.
PM-C06 verifies raised-hand statistics survive restart without duplicate counting.
Full verification results are recorded in quality.md. Commit/push, remote CI and
peer review remain pending. Adaptive bots remain on the separate pushed
feature/bot-integration branch. This merge does not resolve the known queued-
departure winner or below-6,000-chip terminal-policy findings, and does not claim
the older profile branch already meets the newer instructor null-flow convention.

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


## Week 7 (11/09/2026–11/15/2026)

**Goal:** Complete the agreed backend; track adaptive bots as a separate backend
workstream. The full shared plan, preserved requirements, Kevin's K01–K06 tasks
and integration/quality gates are in [issue #20](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/20).
Quit-as-fold remains the agreed requirement; the older prohibition in the rules
still needs Kevin's K04 revision and implementation.

| Status | Owner | Task and acceptance criteria | Evidence / branch / PR |
| --- | --- | --- | --- |
| Separate committed branch; review/merge pending | Estella | E01/E02: local profiles, lifetime persistence and desktop account integration | `feature/player-profiles`, latest local commit `e0d6015`; detailed checkpoints in issue #20. Profile sources remain on their separate branch. |
| Committed/pushed; review/CI/merge pending | Estella | E03: legitimate-information Monte Carlo equity, multiway fractional ties, public side-pot eligibility, bounded/cancellable calculation | `feature/monte-carlo-equity`, commit `2ce09a0`, based on main `c71e9d6`; 51 new cases, full suite 1,084 passing; [BVA](bva/MonteCarloEquityEstimator.md), [design](design/README.md#standalone-equity-e03), [quality](quality.md#e03-standalone-equity--november-10-2026). |
| Committed/pushed; local integration awaiting review/commit | Estella | E04: smoothed public-decision opponent learning and contextual uncertainty | `feature/opponent-learning`, commit `54de9ad`, based on E03 `2ce09a0`; 49 new cases, 1,133 total tests pass, 138/138 learning and 20/20 StatsContext mutants killed; [BVA](bva/OpponentModel.md), [quality](quality.md#e04-opponent-learning--november-10-2026). |
| Committed/pushed; review/CI/merge pending | Estella | E05: varied legal decisions using equity, odds, position, stacks and beliefs | `feature/bot-policy`, user commits `4e421c6` / `36f9a49`; 1,257 tests, 172/172 new-class mutants killed; detailed E05 checkpoint below. |
| Verified locally; commit/review/CI/merge pending | Estella | E06: live accepted learning, public call scenarios, off-EDT execution, cancellation and stale-decision rejection | `feature/bot-integration`, based on committed E05 `36f9a49`; detailed checkpoint below. |
| Merged PR #23; departure follow-up open | Kevin | K01–K06: full betting/reopening, statistics context, quit-as-fold, legal-action contracts and applicable quality scope | Merged to main `bda4954`; integrated locally with E03/E04. Full-betting/static checks pass; departure findings are recorded below. |
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

**E04 progress evaluation (November 10):** The new match-scoped model learns
conditional action probabilities and engine-flagged three-bet/c-bet tendencies.
It returns immutable snapshots with counts, prior-smoothed probabilities,
posterior uncertainty and evidence weight. Exact PlayStats and profile lifetime
totals are independent. Only committed public accepted events should be delivered;
own choices, forced posts and automatic departures cannot teach preferences.
Sequence replay, foreign-match input and failed-update retries are covered.
Automatic events need no fabricated legal-action context. Six new classes have
zero missed scoped counters and kill 138/138 mutants; newly measured StatsContext
kills 20/20. Shared PIT is 706/713 with seven unchanged baseline survivors.
All 1,133 tests and static checks pass without new exclusions.

Observed RED stages are recorded in BVA/quality; later integration/mutation
regressions are distinguished from feature RED. Controlled held-out multiclass
Brier error improves from 0.666666667 with learning disabled to 0.476199453 after
training. Four permutations test order invariance, not independent poker skill.
The model updates with later evidence and keeps unseen contexts at their prior.
No games won, learned hidden-card ranges or live Swing adaptation are claimed.
E03 sampling stays uniform; E05/E06 will consume beliefs and connect live events.
The agent created no E04 commit/push, extra checkout or scratch report. The user
subsequently committed/pushed E04 as `54de9ad`. E04 and shared
integration/evaluation gates stay unchecked until their acceptance gates are met.

**Next steps:** Review/commit E04 and finish the E03/E04 PR review and CI gates;
this branch depends on E03 `2ce09a0`. Coordinate Kevin's public legal-action,
position and pot-scenario contracts. Then implement E05 varied decisions and E06
safe application/worker integration, followed by repeated controlled games and
full-backend quality/process verification.

### Kevin's merged PR23 checkpoint (historical notes)

**Goal:** Complete betting rules and application contracts, measure the added
components, and document integration/localization plans on the project board.
Carry forward native desktop checks and review workflow verification.

### Planning and progress tracking

| Status | Owner | Task and acceptance criteria | Evidence / checkpoint |
| --- | --- | --- | --- |
| Done, engine/session | Kevin | K01: full BET/RAISE, postflop CALL and raised-preflop responses; round targets, engine-owned deductions, turn order and unchanged rejected snapshots. | `feature/week7-betting`, code `fda5bdf`, based on merged main `c71e9d6`; [BVA](bva/FullBetting.md); FullBettingTest and two real-session integration cases. |
| Done, engine/session | Kevin | K02: short all-in wagers, full/cumulative reopening and individual raise rights; retain BB option/short-blind behavior. | `feature/week7-betting`, code `978a826`; [K02 BVA](bva/ShortAllInBetting.md). Last-decision history stays immutable and resets on new streets/hands. |
| Done, engine/session | Kevin | K03: preserve exact aggression and opportunity context across short all-ins, reopened decisions and snapshots. | Code `a7c0f14`; [K03 cases](bva/BettingHistory.md#k03-exact-raise-counts-and-all-in-observations). |
| Done | Kevin | K04: quit-as-fold, all-in eligibility, fixed seats and later-hand participation. | Code `b9ae32b`. [Rules](requirements/game-rules.md) and [departure tests](bva/Departure.md). |
| Done | Kevin | K05: engine-owned actions, call cost, full wager range and short all-in target. | Code `b9ae32b`. Swing and check/call bots read the same [legal options](bva/LegalActions.md). |
| Done | Kevin | K06: measure core betting/state classes and address actionable findings. | Code `b9ae32b`. [Quality results](quality.md) record coverage and all surviving mutants. |
| Planned | Together | Record integration and i18n plans on the board; retain English/Chinese resources, privacy and regression tests. | Existing implementation/design is evidence, not a claim that board discussion is complete. |

**TDD and integration:** All 45 new boundary cases and both real-session tests
failed before implementation, then passed. Mutation-driven regressions follow
these checks. The integration uses controlled Random and real dealing, betting,
evaluation, payouts and statistics; no mock library. A raised hand reaches
pot 1,480, four tied winners receive 370 each, and the next hand keeps chips/totals.
Another hand verifies recorded three-bet and actual flop c-bet observations.

**K01 verification (historical):** Backend full wagers on every street; no bet/raise controls. The previous Week6
checkpoint was merged through main `c71e9d6`; its historical counts stay above.
Local `build pitest` passes **1,091 tests** and clean Checkstyle/SpotBugs on
Gradle 9.7.1/JDK 17 with Java 11 compilation/tests. The expanded 21-parent scope
has zero missed JaCoCo counters (1,034 lines/600 branches/509 complexity counters).
PIT kills **580/599 (96.83%)**, with 19 reviewed survivors and none uncovered;
[quality.md](quality.md) preserves exact scope, counters and survivor reasoning.
Push, new remote CI and peer review remain pending for this branch.

**K02 verification (Nov 11):** Local `build pitest` passes **1,127 tests**, with
clean Checkstyle/SpotBugs. JaCoCo has no missed counters in the 22-component
scope; PIT kills **621/635 (97.80%)**, with 14 documented survivors and none
uncovered. Of the original 20 K02 cases, 15 failed before implementation; all
passed afterward. Real-session tests verify cumulative reopening through
side-pot settlement and short all-ins through automatic board runout.
See [quality results](quality.md) for scope and survivor reasoning.

**K03 verification (Nov 12):** Local `build pitest` passes **1,138 tests**,
with clean Checkstyle/SpotBugs and no missed JaCoCo counters in the same
22-component scope. PIT kills **629/643 (97.82%)** with the same 14 reviewed
survivors. Legal replay verifies different raise labels at the same price;
settlement verifies short c-bets, capped calls and responses after a raise.
See [quality results](quality.md). Remote CI and partner review remain pending.

**K04–K06 verification (Nov 13):** Local `build pitest` passes **1,169 tests**,
with clean Checkstyle and SpotBugs. The 34-component JaCoCo scope covers
**1,531/1,532 lines** and **1,103/1,104 branches**. The sole missed
branch is an unreachable enum fallback. PIT reports **1,037/1,051 killed (98.67%)**
with 14 reviewed survivors and zero uncovered mutants. The
[quality results](quality.md) record the full counters and remaining hash-code
acceptance question.

**Next steps:** Push `feature/week7-betting`, request peer review, and verify CI.
Together, record integration and localization plans on the project board and
walk through the English/Chinese controls and card handoffs on a desktop.

### Instructor follow-up and integration — November 15

| Status | Owner | Task / outcome | Evidence |
| --- | --- | --- | --- |
| Verified locally; awaiting user commit/review/CI | Estella | Refactor BettingEngine.advanceStreet and each closed-round validation condition into focused helpers. | Integrated full-betting source on `refactor/instructor-null-formatting`; existing betting/runout tests pass. |
| Verified locally | Estella | Share named seat/default constants and enforce them automatically in the initial table-rule scope. | GameConfig.SEAT_COUNT; checkstyleSeatRules checks GameConfig, HandPositions, SeatOrder, Player and PotSettlement. Six original violations were observed before the fix. |
| Verified locally | Estella | Complete the non-null caller contract, explicit actor/history absence and membership-based lookups. | Zero manual null comparisons or requireNonNull guard calls in production; nullable actor constructors removed and callers migrated. AGENTS.md records future conventions. Optional aggression and immutable indexed seats; five business cases observed RED → GREEN, with no new programmer-null cases. |
| Verified locally | Estella | Remove the StatisticsPanel line-85 blank and enforce the formatting rule. | Negative Checkstyle caught that line and the same InitialDeal pattern; main/test/seat checks now pass. |
| Integrated locally, uncommitted | Together | Preserve Kevin's merged betting/state work and E03/E04 in one checkout. | Main `bda4954` + E04 `54de9ad`; conflict resolutions retain all scopes and test patterns. |
| Open follow-up | Together | Prevent an out-of-turn, non-all-in quitter from winning before its queued fold; reconcile departure termination with the 6,000-chip requirement. | Source review of PR #23; these functional issues are not claimed fixed by the instructor refactor. |

Final local `build pitest`: **1,194 tests pass**, clean Checkstyle/SpotBugs and
**1,223/1,237 mutants killed (98.87%)**, retaining Kevin's same 14 survivors and
zero uncovered/error/timeout mutants. The 46-parent scope covers 1,915/1,916
lines and 1,161/1,162 branches; the one enum-default path and survivor acceptance
remain explicit limitations. See [quality results](quality.md#instructor-cleanup-and-local-integration--november-15-2026).
This final pass retires 80 obsolete programmer-null invocations from the earlier
1,274-test checkpoint; meaningful game/lifecycle boundaries remain. A redundant
participant check found by mutation testing was consolidated; no new survivor or
measurement exclusion remains. All 11 remaining construction/composition-result null guards were removed in
the final follow-up; external resource validation remains. Two newly exposed
intentional collaborator-sharing SpotBugs findings have exact constructor/field
exceptions with rationale for peer review; no metric exclusion was added. This result excludes the
separate profile branch; E05/E06 remain upcoming.

The merge is resolved but intentionally uncommitted for the user to inspect.
There is no second checkout, scratch report or agent commit/push. Review/remote
CI gates remain pending. Next: user review/commit and CI; then implement E05 using
Kevin's LegalActions/Wager APIs, retaining E06 event-origin and worker contracts.

### E05 policy checkpoint — November 15

**Status:** Implemented and verified locally on feature/bot-policy; subsequently
committed/pushed by the user as `4e421c6` / `36f9a49`. Peer review, remote CI and
merge acceptance are still pending here. Base 93ab0fe is the user's committed
instructor-refactor integration; earlier checkpoint wording remains historical.

Completed E05's pure decision API: immutable filtered requests and distributions,
eligible-pot/net-call odds, public street-order position, stack-aware raise-to
sizing, three modest personalities and confidence-weighted opponent aggression.
New query counts eligible public decisions by street without changing exact
PlayStats. No private opponent cards or deck enter the policy.

Local feature RED/GREEN: missing APIs then 14 GREEN cases; missing contracts then
22 GREEN; 12 behavior failures against the unsupported policy then GREEN.
Fifteen later invocations strengthen integration, coverage and mutation cases.
No Git TDD checkpoint commits were fabricated; the rubric still requires review
of actual commit history and individual contributions.

Final build pitest passes: 1,257 tests, clean Checkstyle/SpotBugs, no new filter.
All four new executable classes have zero missed JaCoCo counters and kill 172/172
mutants; extended OpponentBeliefs kills 25/25. Shared scope is now 50 parents /
52 reported classes: 2,108/2,109 lines and 1,297/1,298 branches. Shared PIT is
1,402/1,416 (99.01%), with only the same 14 baseline survivors and no uncovered/
error/timeout mutants. [Full evidence](quality.md#e05-varied-legal-decision-policy--november-15-2026).

Controls verify legal actions/targets across streets and personalities, short/full
all-ins, true CALL classification, side-pot odds, new-call versus prior refunds,
position/stack effects, learned-versus-prior preferences, hidden-data invariance,
and seeded sampling frequencies. Real Monte Carlo/policy/engine checks and a
short-blind runout/settlement flow conserve chips. Twelve observed policy-only
construction calls took 0.039625–0.107167 ms; this excludes simulation and UI work
and is not a latency guarantee or playing-strength claim.

Updated required BVA and existing project records; no extra reports or checkout.
The E05 issue checkbox stays open until its review/CI/merge gates are complete.
At this E05 checkpoint, E06 was next and Swing used temporary check/call bots.
The E06 checkpoint below records the subsequent live integration. Profile integration and known departure/terminal
policy findings remain open.

### E06 live integration checkpoint — November 15

Branch `feature/bot-integration` starts from the user's E05 commits `4e421c6`
and `36f9a49`, in the original checkout. No agent commit/push, additional
checkout or scratch report was created. Profiles stay on their separate branch.

BotTable prepares accepted public-event learning transactionally, retaining
match-scoped beliefs between hands and excluding automatic departure folds.
PotLayout is shared by settlement and prospective-call scenarios. BotTurn carries
only immutable own/public information plus matching legal limits and state tokens.
The preview assumes no further betting; its refunds are conditional scenario
values and do not promise later betting outcomes.

BotComputer connects Monte Carlo equity to the varied legal policy. BotWorker
uses one background thread and one waiting job, cooperative cancellation and a
one-second budget. BotController applies results only on the owner thread after
match/revision/hand/actor checks and authoritative engine validation. GamePanel
uses translated thinking/retry controls and preserves human handoffs. Restart,
departure, game progression and close invalidate work; old callbacks cannot hide
newly revealed cards. Closing releases the executor; locale changes preserve play.

Local TDD observed missing APIs for contribution/position/transactional models,
then for session/worker/computer boundaries. Five controller behavior cases and
six Swing invocations were observed failing against unsupported implementations,
then passing. Later refund/context/lifecycle regressions address quality gaps;
they are not claimed as initial feature RED cycles. Git test-first evidence still
needs the user's honest commit history and review, not fabricated checkpoints.

Final `build pitest --warning-mode all` exits 0: **1,308 tests passing**, clean
Checkstyle and zero unexcluded SpotBugs findings. Six new executable parents
(seven reported classes) have zero missed counters and kill **136/136 mutants**.
Shared scope is 56 parents / 59 classes: 2,434/2,435 lines, 1,399/1,400 branches,
1,354/1,355 complexity counters and 649/649 methods. Shared PIT is **1,581/1,595
(99.12%)**, with the same 14 baseline survivors and zero uncovered/error/timeout
mutants. The existing enum-default miss remains; these are scoped measurements,
not independent basis-path or whole-backend proof. [Full evidence](quality.md#e06-live-adaptive-bots--november-15-2026).

Real integrations cover background/Swing dispatch, bounded queue replacement,
bot-to-human handoff, an adaptive played hand through payment/continuation, and
capped side-pot CALL settlement. Exact public learning contexts, automatic
fold exclusion and recorder-failure rollback/retry are verified. The final
three-bot-to-human flow took 216.011 ms locally; not a timing guarantee.
A precise live-Swing controller serialization exception is documented; no new
coverage/mutation exclusion was added.
Native display/focus testing, profile integration, peer review, remote CI and merge
remain pending. The issue E06 checkbox remains open until those code review/merge
gates are met. Existing queued-departure winner and below-6,000 terminal-policy
follow-ups still prevent claiming that the whole backend is complete.

Style follow-up: extracted named budget-validation and model-creation helpers;
checked 100-character lines, four-space indentation and absence of production
null guards. The real-thread integration now uses seeded randomness and bounded
explicit test-driver retries for supported timeouts; exact deadlines remain
covered with a controlled clock. The full 1,308-test build and PIT rerun pass.

### Readability review of current and earlier methods — November 15

Reviewed all 80 current production Java source files, including earlier shared
work. Refactored mixed responsibilities in GameState, PlayStats, StatsAction /
StatsContext, evaluation/showdown, setup/continuation and UI rendering. Helper
names identify real rules or transformations; cohesive parsing/betting algorithms
remain intact. Bot rule/sizing names are explicit, and UI tests share typed
component lookup. Thirty-nine statistics expectation rows use OptionalDouble.
The public APIs, game rules and 1,308 existing test invocations are retained.

Final `build pitest --warning-mode all` passes: clean Checkstyle and zero
unexcluded SpotBugs findings; PIT **1,645/1,659 (99.16%)**, same 14 baseline
survivors, zero uncovered/error/timeout mutants. The unchanged 56-parent /
59-class scope covers 2,544/2,545 lines, 1,403/1,404 branches, 1,414/1,415
complexity counters and 707/707 methods. The existing enum-default miss remains;
these are scoped counters, not independent basis-path proof. [Latest evidence](quality.md#readability-review-of-current-and-earlier-work--november-15-2026).

The profile branch e0d6015 was inspected read-only. It still needs internal
null-guard cleanup, storage-boundary normalization and profile-selector/roster
rendering separation. It was not merged or edited while E06 remains uncommitted;
that cleanup must be verified separately. No extra checkout/report, commit or
push was created. Existing BVA/design records and issue #20 retain review/CI/
merge gates; neither this review nor green local checks establish whole-backend
or final-grade completion.

## Week 8 (11/16/2026–11/22/2026)

**Goal:** Fix departure settlement and supply account-funded table APIs.

| Status | Owner | Work | Evidence / branch |
| --- | --- | --- | --- |
| Done locally | Kevin | Dead-pot settlement, including a zero-investment survivor. | PotLayoutTest / QuitIntegrationTest; `feature/week8-table-rules` |
| Done locally | Kevin | Open-ended tables, chosen 400+ buy-ins with no fixed cap, top-ups, replacement bots and retained statistics/learning. | AccountTableIntegrationTest; same branch |
| Done locally | Kevin | Immutable hand changes and atomic/idempotent storage contract; background save with owner-thread publication. | TableAccountStore / PreparedSettlement; same branch |
| Pending | Estella + Kevin | Connect SQLite, migration/recovery and desktop; verify the combined flow. | Shared account-table contract |

**Progress evaluation:** Departure, funding, privacy and save/retry regressions
were observed failing before fixes. Before the PR #30 merge, local `build pitest`
passed: **1,610 tests**, clean Checkstyle/SpotBugs and **2,049/2,072 PIT kills (98.89%)**.
[Scope and 23 reviewed survivors](quality.md). The account-table checkpoint is
committed in PR #30; combined SQLite/desktop integration and peer review remain pending.

**November 21 — PR #30 merge fix:** Restored background-settlement guards
and fixed line wrapping. Existing retry tests and a new owner-callback regression
pass. Local `build pitest`: **1,625 tests**, clean static checks,
**2,096/2,123 PIT kills (98.73%)**; [scope and 27 survivors](quality.md).
This follow-up awaits Kevin's commit/push and remote CI.

**Next steps:** Estella connects storage/UI; together test sign-in, paid hands,
top-up, departure and recovery in both languages. Kevin reviews, commits and pushes changes.
[Plan and live checklist: issue #28](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/28).

Estella is addressing W8-E01 on the merged baseline `9932bcd` (PRs #26 and
#27). Profile saves now run in the background; the Swing thread publishes the
payout and match statistics only after successful storage. During a save,
play and profile controls stay disabled and the table shows the saving
status in English or Chinese. A failure leaves the original hand available
for retry, including when a durable write was followed by a reported error.

The blocked-save regression first failed in both languages on the old
synchronous handler. Those cases now pass, along with the storage retry and
duplicate request boundaries in [session BVA](bva/SetupSession.md#week-8-background-settlement)
and [UI BVA](bva/ProfileScreen.md#w8-e01-background-settlement).

Local `build pitest` passed: 1,551 tests, clean Checkstyle and SpotBugs,
2,926/2,927 measured lines, and 1,837/1,851 mutants killed with the same
14 baseline survivors. See [quality evidence](quality.md#w8-e01-background-settlement--november-18-2026).
Peer review, remote CI, the native desktop walkthrough and merge remain pending.

### W8-E02 SQLite account migration

Estella added the SQLite adapter on top of E01 (`6d8aa08`) so desktop database
operations use the existing background account and settlement flows. The
legacy catalog is imported once when `profiles.db` is initialized. Account
identity, credentials and completed statistics are retained, and new and
migrated accounts each start with 2,000 virtual chips. Reopening an account
preserves its saved balance, including zero.

The initial [SQLite migration tests](bva/SqliteProfileRepository.md) failed to
compile before the adapter and balance API existed, then passed with the
implementation. Existing profile composition tests now also exercise SQLite
settlement and restart. The W8-E03 history and balance UI, W8-I01 shared
settlement contract and Kevin's W8-K01 side pot fix remain separate.

Local `build pitest` passed: 1,560 tests, clean Checkstyle and SpotBugs,
3,012/3,013 measured lines and 1,874/1,888 mutants killed after the style
review, with the same 14 baseline survivors.
See [quality evidence](quality.md#w8-e02-sqlite-account-migration--november-18-2026).
E02 is uncommitted; E01 is its pushed prerequisite and has not yet merged.
Peer review, remote CI and merge remain pending.

## Future weekly report template

### Week N (MM/DD/YYYY–MM/DD/YYYY)

**Goal:** Planned outcome.

| Status | Owner | Task and acceptance criteria | Evidence / branch / PR |
| --- | --- | --- | --- |
| Planned / In progress / Done / Blocked | Name | Concrete task | Link or verified checkpoint |

**Progress evaluation:** Actual outcome, checks, blockers and differences from plan.

**Next steps:** Remaining tasks and owners.
