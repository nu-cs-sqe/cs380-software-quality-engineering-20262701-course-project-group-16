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
| Estella | Recorded the proposed Texas Hold'em scope and contributor names. | [Project overview](../README.md): six seats, 1-6 humans, bots, virtual chips, and shared-computer play. |
| Estella | Reviewed the rubric and simplified the README to the required project information. | [README](../README.md) includes dependencies, build/test commands, documentation links, and exceptions. |
| Estella | Implemented player identity, six-seat setup, immutable hand state, action contracts, filtered views and poker statistics with tests and BVA. | [Shared contract](design/shared-contract.md); [BVA index](bva/README.md); source branch `feature/game-state-and-stats`. |
| Estella | Added CI to build and test pushes and PRs using JDK 25 and the JDK 11 toolchain. | [Workflow](../.github/workflows/ci.yml). |
| Kevin | Implemented immutable cards with rank and suit, null validation, value equality and hashing. | [Card BVA](bva/Card.md); GREEN commits `2219068`, `acbd186`, `a35669d`; 15 Card cases. |
| Kevin | Created a 52-card deck, added validated dealing and shuffling, and protected returned hands from modification. | [Deck BVA](bva/Deck.md); GREEN commits `630deff`, `3a53ede`, `d2ad3b7`. |
| Kevin | Covered zero/full/near-full draws, partially used and empty decks, negative and excessive requests, extreme integers, and rejection without changing state. | BVA cases mapped to `DeckTest`; invalid-request RED commit `a1490c5` and GREEN commit `3a53ede`. |
| Kevin | Added repeatable shuffle tests and two test-only Random stub cases for the lowest and highest valid choices. Confirmed the stub tests detect a shuffle that ignores the supplied random source. | Checkpoint `f1c922e`; 48 Deck cases in total, including both stub cases. This extends existing behavior coverage. |
| Kevin | Verified two rounds of dealing for six hands: two cards per hand, 12 distinct dealt cards and 40 remaining. Defined the hand-evaluation interface for later work. | `9bf1c6e`; `DeckSetupTest`; [setup design](design/game-setup.md); [planned evaluator cases](bva/HandEvaluator.md). |
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

Status updated November 2. Checked means implemented/verified, not necessarily
merged; the GameInitializer follow-up still needs push and teammate review.

- [x] **Member A components:** cards, deck, shuffling, dealing and BVA; 64 passing cases.
- [x] **Shared card model:** one Card model across deck/state/views, with integration tests.
- [x] **CI workflow:** automatic build/tests on pushes and PRs.
- [x] **Shared toolchain:** Gradle 9.7.1 runs locally on JDK 17 and in CI on JDK 25; compilation/tests use JDK 11.
- [ ] **Requirements:** complete the [game rules](requirements/game-rules.md), currently a placeholder; reconcile 5/10 versus 10/20 blinds and the button policy.
- [ ] **UI and languages:** add the Swing setup screen and localization structure, confirm languages, and implement private-card handoffs.
- [x] **Initial model/setup design:** document responsibilities and APIs in the [design documents](design/README.md).
- [ ] **Project board:** confirm assignees, status and PR links; record instructor-meeting evidence.
- [ ] **Review workflow:** verify required reviews/CI enforcement and failure detection; add the workflow run link.
- [ ] **Later project quality work:** configure coverage/mutation testing, measure required metrics and justify exclusions; the manual shuffle check is not a full mutation report.
- [x] **Application initialization:** GameInitializer returns a dealt PREFLOP state; 16 cases pass. Completed November 2 on `feature/setup-dealing`, not merged yet.
- [ ] **First-turn readiness:** connect the interface and verify usable first-turn actions.

### Progress evaluation

**Member A's planned Week 5 components are complete; the team's full setup
feature remains incomplete.** The merged suite passed **554 tests**: 64 Member A,
488 Member B and two integration cases. Different scopes explain the counts;
they are not individual targets. The November 2 initializer adds 16 cases for
**570 passing tests**, with zero failures, errors or skips. Kevin verified both
runs locally with Gradle 9.7.1 on JDK 17 and the JDK 11 toolchain.

Setup/dealing tests verify two cards per player, 40 remaining, correct positions,
10/20 blinds, 6,000 conserved chips and private views. The initializer now does
this in application code, but there is no launchable Swing game yet. Finish the
rules/interface and verify board/review settings. Hand evaluation is still an
interface; full betting, settlement, equity bots and complete translations are
later milestones. Coverage and mutation scores have not been measured.

### Next week's priorities

Push/review the initializer, connect the Swing setup screen, and finish the
rules and workflow follow-ups. Begin hand evaluation and basic betting with
BVA and TDD after confirming shared interfaces.

## Week 6 (11/02/2026-11/08/2026)

### Goal

Connect the tested deck-to-state composition to an application method, then
prepare to call it from the Swing interface.

**Branch:** `feature/setup-dealing`

### Completed work

| Owner | Work completed | Evidence |
| --- | --- | --- |
| Kevin | Added `GameInitializer.start(humanCount)` to combine existing GameSetup and Deck operations. It deals two clockwise rounds and returns the first PREFLOP state. | [Setup design](design/game-setup.md); [initializer BVA](bva/GameInitializer.md). |
| Kevin | Added 16 TDD/BVA cases for constructor nulls, human-count boundaries, all six dealer positions, all-in blinds, repeated games and recovery after invalid input. | RED commit `2ae10c9`: 12 expected failures; GREEN commit `600ad38`: all 16 pass. |
| Kevin | Verified the merged project and new coordinator on this computer using JDK 17 for Gradle 9.7.1 and JDK 11 for compilation/tests. Updated README build instructions. | Clean build: 570 tests, zero failures, errors or skips. |

### Integration checklist

- [x] Connect GameSetup and Deck through GameInitializer to produce a dealt PREFLOP state.
- [x] Preserve dealer, blinds, first actor, chip totals and private player views.
- [x] Document BVA and complete the RED/GREEN cycle for 16 coordinator cases.
- [x] Verify the full clean build: 570 tests pass.
- [ ] Push `feature/setup-dealing`, open a PR and complete teammate review before merging.
- [ ] Connect the Swing setup screen and private-card handoffs to the initializer.
- [ ] Implement and verify usable first-turn actions before marking the full setup feature complete.

### Progress evaluation

- The coordinator reuses Member A's real deck and Member B's real setup/state
  classes. GameSetup's existing SETUP contract and tests remain unchanged.
- Tests verify two cards per player, 40 remaining, correct clockwise order,
  conserved chips, preserved blind/actor/status data and private player views.
- This checkpoint is committed locally on `feature/setup-dealing`; it has not
  been pushed, reviewed by the teammate or merged into main yet.
- The Swing interface and action execution are not implemented. The first-turn
  interface requirement remains open even though the initialized state is ready.

### Next priorities

Review this checkpoint, push its branch and link the PR here. Connect the Swing
setup screen to GameInitializer, add privacy handoffs between humans, and verify
first-turn readiness through the interface before marking setup complete.

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
