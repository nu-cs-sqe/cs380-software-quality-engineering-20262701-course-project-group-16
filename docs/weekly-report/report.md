# Weekly Reports

Reporting starts at Week 5. Add one report each week through the final week of
class, recording actual progress, remaining work, and links to supporting issues
or PRs when available.

## Week 5 (10/26/2026-11/01/2026)

### Goal

Build the card/deck and player/setup foundations, document BVA, and integrate
them toward a game ready for its first turn.

### Completed work

| Owner | Work completed | Evidence |
| --- | --- | --- |
| Estella | Cloned the course repository using HTTPS. | Successful clone in the local terminal. |
| Estella | Installed JDK 11 and JDK 25 and verified the local Gradle build. | `java_home -V` lists both JDKs; `bash ./gradlew clean build` reported `BUILD SUCCESSFUL`. |
| Estella | Recorded the proposed Texas Hold'em scope and contributor names. | [Project overview](../../README.md): six seats, 1-6 humans, bots, virtual chips, and shared-computer play. |
| Estella | Reviewed the rubric and simplified the README to the required project information. | [README](../../README.md) includes dependencies, build/test commands, documentation links, and exceptions. |
| Estella | Implemented player identity, six-seat setup, immutable hand state, action contracts, filtered views and poker statistics with tests and BVA. | [Shared contract](../design/shared-contract.md); [BVA index](../bva/README.md); source branch `feature/game-state-and-stats`. |
| Estella | Added CI to build and test pushes and PRs using JDK 25 and the JDK 11 toolchain. | [Workflow](../../.github/workflows/ci.yml). |
| Kevin | Implemented immutable cards with rank and suit, null validation, value equality and hashing. | [Card BVA](../bva/Card.md); GREEN commits `2219068`, `acbd186`, `a35669d`; 15 Card cases. |
| Kevin | Created a 52-card deck, added validated dealing and shuffling, and protected returned hands from modification. | [Deck BVA](../bva/Deck.md); GREEN commits `630deff`, `3a53ede`, `d2ad3b7`. |
| Kevin | Covered zero/full/near-full draws, partially used and empty decks, negative and excessive requests, extreme integers, and rejection without changing state. | BVA cases mapped to `DeckTest`; invalid-request RED commit `a1490c5` and GREEN commit `3a53ede`. |
| Kevin | Added repeatable shuffle tests and two test-only Random stub cases for the lowest and highest valid choices. Confirmed the stub tests detect a shuffle that ignores the supplied random source. | Checkpoint `f1c922e`; 48 Deck cases in total, including both stub cases. This extends existing behavior coverage. |
| Kevin | Verified two rounds of dealing for six hands: two cards per hand, 12 distinct dealt cards and 40 remaining. Defined the hand-evaluation interface for later work. | `9bf1c6e`; `DeckSetupTest`; [setup design](../design/game-setup.md); [planned evaluator cases](../bva/HandEvaluator.md). |
| Kevin | Documented Member A's contracts and BVA, updated project documentation, and pushed the cards-and-deck branch for integration. | [Member A PR #6](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/6), merged into `main`; component commits preserve the RED/GREEN history. |

**Branches:** Kevin: `feature/cards-and-deck`; Estella: `feature/game-state-and-stats`;
shared documentation: `docs/project-setup`. The November 2 initialization
follow-up is on local branch `feature/setup-dealing` (details in Week 6).

Estella combined the Week 5 work into `main` on November 2, preserving the TDD
history and reusing `domain.model.Card` throughout state and views. The duplicate
card enum was removed. Evidence: [Member A PR #6](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/6),
[Member B PR #8](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/8), and
[merge `881ad79`](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/commit/881ad796c1a3894d35da9667025a75bf6f85fa06).

### Setup checklist

Historical setup progress, reviewed November 3. Checked means implemented and
verified in the stated scope. Initialization/Swing were merged through PR #11.
Week 6 records subsequent evaluator, localization and quality work below.

- [x] **Member A components:** cards, deck, shuffling, dealing and BVA; 64 passing cases.
- [x] **Shared card model:** one Card model across deck/state/views, with integration tests.
- [x] **CI workflow:** automatic build/tests on pushes and PRs.
- [x] **Shared toolchain:** Gradle 9.7.1 runs locally on JDK 17 and in CI on JDK 25; compilation/tests use JDK 11.
- [ ] **Requirements:** complete the [game rules](../requirements/game-rules.md), currently a placeholder; reconcile 5/10 versus 10/20 blinds and the button policy.
- [x] **Setup UI:** English-only Swing setup preview, label resources and private-card handoffs merged through PR #11 from `feature/swing-setup`; native desktop walkthrough still pending.
- [x] **Initial model/setup design:** document responsibilities and APIs in the [design documents](../design/README.md).
- [ ] **Project board:** confirm assignees, status and PR links; record instructor-meeting evidence.
- [ ] **Review workflow:** verify required reviews/CI enforcement and failure detection; add the workflow run link.
- [x] **Kevin quality follow-up:** coverage/mutation reports, shared static checks and reviewed exceptions are now documented in Week 6 and quality.md; team-wide metrics and rubric acceptance remain separate.
- [x] **Application initialization:** GameInitializer returns a dealt PREFLOP state; 16 cases pass. Completed November 2 on `feature/setup-dealing`, then merged through PR #11.
- [ ] **First-turn readiness:** implement and verify usable first-turn actions; the setup interface is connected.

### Progress evaluation

**Member A's planned Week 5 components are complete; the team's full setup
feature remains incomplete.** The merged suite passed **554 tests**: 64 Member A,
488 Member B and two integration cases. Different scopes explain the counts;
they are not individual targets. The November 2 initializer adds 16 cases for
**570 passing tests**, with zero failures, errors or skips. Kevin verified both
runs locally with Gradle 9.7.1 on JDK 17 and the JDK 11 toolchain.

Setup/dealing tests verify two cards per player, 40 remaining, correct positions,
10/20 blinds, 6,000 conserved chips and private views. The initializer now does
this in application code. The later November 2 Swing follow-up adds a launchable
setup preview (601 tests after the English-only revision); betting actions remain absent. Finish the
rules and verify board/review settings. At the end of the original Week 5 component work, hand evaluation was an
interface and quality scores were unmeasured. Week 6 records their implementation
and measurements. Full betting, settlement and equity bots remain later work.

### Next week's priorities

Walk through the desktop screen and finish the rules and workflow follow-ups.
Continue hand evaluation and basic betting with BVA and TDD after confirming
shared interfaces.

## Week 6 (11/02/2026-11/08/2026)

### Goal

Finish setup integration, implement shared best-five evaluation and closed-state
winner/tie comparison, add English/Chinese resources and selection, and configure
quality checks with actual measurements. Estella owns betting/turn/street
progression; connect the new result API when her engine is available.

Tracked in [Week 6 issue #12](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/12).

**Branches:** `feature/setup-dealing` (initializer); `feature/swing-setup`
(package consolidation, session and interface, based on the initializer branch).
Kevin's evaluator checkpoint starts from merged main `9dbaa39` on
`feature/hand-evaluation`.

### Completed work

| Owner | Work completed | Evidence |
| --- | --- | --- |
| Kevin | Added `GameInitializer.start(humanCount)` to combine existing GameSetup and Deck operations. It deals two clockwise rounds and returns the first PREFLOP state. | [Setup design](../design/game-setup.md); [initializer BVA](../bva/GameInitializer.md). |
| Kevin | Added 16 TDD/BVA cases for constructor nulls, human-count boundaries, all six dealer positions, all-in blinds, repeated games and recovery after invalid input. | RED commit `2ae10c9`: 12 expected failures; GREEN commit `600ad38`: all 16 pass. |
| Kevin | Verified the merged project and new coordinator on this computer using JDK 17 for Gradle 9.7.1 and JDK 11 for compilation/tests. Updated README build instructions. | Clean build: 570 tests, zero failures, errors or skips. |
| Kevin | Moved all domain classes and matching tests into `domain.model`, retaining application/UI packages. | `d0109ec`; clean build: 570 tests. |
| Kevin | Added a setup session and immutable public view with explicit human-card reveals and safe handoffs/restarts. | RED `bc5accd`; GREEN `88a535b`; [20 session BVA cases](../bva/SetupSession.md), full suite 590 passing. |
| Kevin | Built the Swing setup preview with seats/chips/blinds/actor, human selection, reveal/hide, label resources and Gradle launch task. | RED `36a2975`; GREEN `58f968a`; 13 GUI behavior cases (locale fix: RED `217434e`, GREEN `2ac9fe3`); full clean build: 603 tests, zero failures/errors/skips; offscreen layout checks. |
| Kevin | Simplified the interface to English only, removed the language selector and extra translation bundle, and updated the tests and documentation. | English-only regression observed RED, then GREEN; build at that stage: 601 tests (11 GUI cases), zero failures/errors/skips. This checkpoint was merged through PR #11. |

### Kevin's evaluation, localization and quality checkpoints

All work below is on `feature/hand-evaluation`, based on merged main `9dbaa39`.
The branch retains real RED/GREEN commits. No commits were pushed during this
follow-up; a local commit does not establish a passing GitHub CI run.

| Completed work | Checkpoint evidence |
| --- | --- |
| Validate both seven-card holdings: nulls, sizes 0/6/8, duplicate values and rejection without modifying input. | RED `f5fd72e`: 16 expected failures; GREEN `631bb87`: 16 pass. |
| Implement one shared best-five-of-seven evaluator for all nine categories, kickers, wheels, best-five selection, shared boards and ties. | RED `935e12b`: 34 expected failures plus 3 existing ties; GREEN `8291f17`: all 53 evaluator cases pass. [Evaluator BVA](../bva/PokerHandEvaluator.md). |
| Compare ACTIVE/ALL_IN contenders, skip FOLDED/ELIMINATED, preserve ties in seat order and return immutable AWAITING_SETTLEMENT without paying chips. Reject unfinished/completed states; recognize uncontested wins without evaluation. | RED `29442e4`: 21 expected failures; GREEN `0d5172e`: 21 pass. [Resolver/result BVA](../bva/ShowdownResolver.md); [shared API](../design/showdown.md). |
| Connect real initialized cards/state to the evaluator/result API in known-winner and board-tie fixtures; preserve original PREFLOP state and chip totals. | `428b161`: two passing ShowdownIntegrationTest fixtures. These supply a closed-state boundary, not a played betting engine. |
| Add English/Chinese discovery, startup selection, UTF-8 resources, locale formatting and private-card hiding on language changes. New locale resources/configuration require no Java edits. | RED `132e255`: 40 expected failures plus 10 existing passing cases; GREEN `f6ad8e5`: 50 focused cases pass. [Localization design](../design/localization.md); [loader BVA](../bva/Localizations.md); [UI cases](../bva/GamePanelLocalization.md). |
| Cover missing-language and resource-I/O fault paths, including stream closure/cause preservation; remove a guard proven unreachable after Locale.Builder validation. | `7717b89`: focused cases expand to 52. This is coverage hardening of existing behavior, not an invented RED claim. |
| Configure Checkstyle/SpotBugs for main and test code; configure Kevin-only JaCoCo/PIT and CI report uploads; address actual findings. | `7e65692`, `5b4b092`, `c7dac83`; [quality configuration and measured findings](../quality.md). No broad ignored baseline or fabricated mutation score. |

The final review identified a translation-argument gap: a lower argument index
or an earlier repeated format could be lost. RED `c72b794` ran 68 focused cases
with 12 expected failures; GREEN `025baef` fixes actual placeholder/type matching,
quotes and nested choice arguments. A further compatible literal-brace fixture
brings focused coverage to 69 cases (52 catalog and 17 UI). The reviewer checked
the fix and reported no remaining actionable findings.

**Final local verification, November 3:** `clean build pitest --continue`
completed successfully, all 16 tasks executed, using JDK 17 for Gradle and JDK 11
for compilation/tests. **735 tests pass**, with zero failures/errors/skips;
Checkstyle and SpotBugs have zero unexcluded main/test findings. Kevin's nine
measured classes cover **319/319 lines, 184/184 branches and 153/153 JaCoCo
complexity counters** (also all 1,748 instructions and 61 methods). These are
scoped counters, not proof of independent basis-path execution or team-wide
coverage. PIT killed **178/186 mutants (95.70%)**, with eight reviewed survivors
and no uncovered/time-out/error mutants; [quality.md](../quality.md) records
their reasoning and the outstanding hash-contract acceptance question.

Offscreen Java 11 renders at 1100×740 were inspected in English and Chinese,
with private cards hidden and revealed. Text and controls fit; changing language
conceals the hand. Native window focus delivery and cross-platform rendering
still require the teammate's desktop walkthrough. Reports are generated under
`build/reports/`; no remote CI outcome is claimed before push.

### Integration and completion checklist

- [x] Initialize a dealt PREFLOP state using the real GameSetup and Deck; preserve positions, blinds, chip totals and private views.
- [x] Connect Swing setup and human-card handoffs to the initializer; merged through [PR #11](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/11).
- [x] Implement and test Kevin's evaluator and immutable winner/tie result API through the existing shared model.
- [x] Add real-component initialization-to-showdown boundary fixtures and BVA mappings.
- [x] Implement English/Chinese setup-flow localization, resource discovery/validation and privacy regressions.
- [x] Configure shared Checkstyle/SpotBugs, fix findings and retain test/static reports in CI.
- [x] Generate scoped coverage and mutation reports; review survivors and document actual counters and exclusions.
- [ ] Connect Estella's accepted/rejected actions and street advancement to the result API and Swing result screen; verify a genuinely played setup-to-result flow in both languages, including uncontested/all-in runout paths.
- [ ] Confirm instructor acceptance of mutation-survivor reasoning and the rubric's complexity-coverage interpretation; obtain partner-owned measurement results separately.
- [ ] Complete the native desktop walkthrough with the teammate, including font/layout and actual focus-loss hiding.
- [ ] Finish shared rules, board status/assignees and required-review/CI enforcement follow-ups.
- [ ] Push this branch, verify the actual remote workflow and obtain peer review before merging.

### Progress evaluation

Kevin's evaluator and closed-state showdown behavior are implemented and tested.
Localization covers the supported setup/private-card/error flow in both
languages. The quality tools run against real tests and bytecode; exact metrics,
static exceptions and the raw mutation score are in [quality.md](../quality.md).
High test counts are not individual targets and do not prove full-game readiness.

The initializer/session/deck/state/evaluator run together without mocked game
components. Fixed/seeded Random test seams control dealing; one fail-fast
evaluator stub proves uncontested results avoid comparison. Winner expectations
use the real evaluator. Integration fixtures explicitly provide the closed
SHOWDOWN state because Estella's betting/turn/street engine is not yet present.
The result remains AWAITING_SETTLEMENT: no pot payouts or side-pot allocation
is claimed. Translated result resources alone do not prove result-screen
integration. These dependencies remain open rather than checking all Week 6
work complete.

### Next priorities

After local checkpoints, Kevin will push and open/update the PR. Coordinate the
closed-state/result contract with Estella, integrate actions/streets/results,
then test the played flow and desktop handoffs in both languages. Review the
remaining rules and rubric questions together. Keep future settlement and
probability-based bots outside this checkpoint.

## Future weekly report template

Copy this template for each subsequent week and replace the placeholders.

### Week N (MM/DD/YYYY-MM/DD/YYYY)

**Goal:** State the week's intended outcome.

| Status | Owner | Task and acceptance criteria | Evidence / issue / PR |
| --- | --- | --- | --- |
| Planned / In progress / Done / Blocked | Name | Concrete task and completion condition | Link or local verification result |

**Progress evaluation:** Compare the plan with actual outcomes, record checks
and results, explain blockers, and identify work to carry forward.

**Next week's priorities:** List the next tasks and their owners.
