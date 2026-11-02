# Weekly Reports

Reporting starts at Week 5. Add one report each week through the final week of
class, recording actual progress, remaining work, and links to supporting issues
or PRs when available.

## Week 5 (10/26/2026-11/01/2026)

### Goal

Initialize the game so it is ready for the first turn. Member A supplies cards,
a deck, shuffling and dealing; Member B supplies players and setup state. The
team still needs to connect these components to the application and interface.

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

The Week 5 component work is merged. Estella completed the combined merge into
`main` on November 2, 2026, preserving the branches' TDD commits. The integration
uses Member A's `domain.model.Card` throughout state and views and removes the
incompatible duplicate card enum. See [merge commit `881ad79`](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/commit/881ad796c1a3894d35da9667025a75bf6f85fa06)
and [Member B PR #8](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/8).
This merge status is a November 2 follow-up to the Week 5 report.

### Setup work remaining

Kevin is Member A; Estella is Member B. Track their tasks on the project board.
Checked items have supporting implementation or merge evidence. Unchecked items
are planned, partially complete, or still need team verification. Status was
updated on November 2; the application-initialization follow-up is currently
local to `feature/setup-dealing`, pending push and teammate review.

- [x] **Member A components:** complete cards, deck, shuffling, dealing and BVA;
  verify 64 component test cases.
- [x] **Shared card model:** combine the branches and use Member A's card values
  throughout state and views; add deck-to-state integration tests.
- [x] **CI workflow:** add automatic builds and tests for pushes and PRs.
- [x] **Shared toolchain:** Kevin verified Gradle 9.7.1 locally with JDK 17
  as its runtime and JDK 11 for compilation/tests on November 2. The merged
  suite passed 554 tests; the initializer branch passed 570. CI uses JDK 25
  as its Gradle runtime and JDK 11 for compilation/tests.
- [ ] **Requirements:** complete the [game rules](requirements/game-rules.md),
  including the authoritative rule source, chosen variants, starting chips,
  blinds, match progression and expected behavior for invalid actions. The
  merged setup currently defaults to 1,000 chips and 10/20 blinds; reconcile
  the earlier 5/10 rules draft with the current shared contract.
- [ ] **UI and languages:** build the Swing setup/first-turn interface, confirm
  two languages, and implement private-card handoffs on the shared computer.
- [x] **Initial model/setup design:** document the card/deck, setup/state and
  application-initializer responsibilities and APIs in the
  [design documents](design/README.md). Keep these current as betting, bots
  and interface components are implemented.
- [ ] **Project board:** update assignees, acceptance criteria, status and links
  to merged work.
- [ ] **Review workflow:** verify required reviews and passing CI checks on
  `main`; confirm CI failure detection and record the workflow run link.
- [ ] **Later project quality work:** configure coverage and mutation testing, document
  justified exclusions and evaluate the course's required metrics. The manual
  shuffle mutation check is not a full mutation-testing report.
- [x] **Application initialization:** connect GameSetup and Deck through
  GameInitializer and verify its dealt PREFLOP state with 16 new cases.
  Completed November 2 on `feature/setup-dealing`; not merged yet.
- [ ] **First-turn readiness:** connect the Swing interface and implement/verify
  usable actions for the first human or bot before declaring setup complete.

### Progress evaluation

**Week 5 result: Member A's planned components are complete, but the team's
full setup deliverable is not complete yet.** The initialization logic is ready
on the local integration branch; there is still no launchable game interface.

The test counts describe different component scopes, not a target each person
must match:

| Scope | Executed test cases |
| --- | ---: |
| Member A cards/deck and dealing check | 64 |
| Member B player/setup/state/action/view/statistics components | 488 |
| Original cross-component integration | 2 |
| Combined merged suite before the initializer | **554** |
| New application initializer | 16 |
| Current initializer branch total | **570** |

Estella's 488 cases span 15 test classes, including 129 statistics cases and 73
game-state cases. The original Member A scope has three test classes: Card (15),
Deck (48) and initial dealing (1). Different responsibilities need different
numbers of cases. The smaller count alone does not indicate missing work;
requirements and boundary/failure coverage matter. Coverage and mutation scores
have not been measured, so test counts do not establish those quality metrics.

- Kevin's Member A work has **64 verified passing component cases**: 15 Card,
  48 Deck and one six-player dealing check. The last local clean build before
  integration used Java 11 and Gradle 8.10, with zero failures, errors or skips.
  The recorded RED/GREEN checkpoints show tests failing before the related
  behavior was implemented.
- Estella's [integration merge record](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/commit/881ad796c1a3894d35da9667025a75bf6f85fa06)
  reports **554 passing tests** and successful GitHub CI: 488 Member B cases,
  64 Member A cases and two cross-component cases. On November 2, Kevin
  also verified this 554-test suite locally, then verified 570 cases after
  adding the application initializer. Both local runs used Gradle 9.7.1
  on JDK 17 with JDK 11 compilation/tests.
- The integration tests check 12 unique hole cards, 40 remaining cards, first
  actor seat 3, 10/20 blinds, 6,000 conserved chips, duplicate-card rejection
  by value and private human/bot views. They compose the components in test
  code; `GameSetup.create` itself still returns `SETUP` with no hole cards.
- The evaluator is an interface only. Hand evaluation, betting execution,
  pot settlement and equity estimation are not implemented yet.
- The full game is not yet playable. The setup goal is only partially met:
  the components and composition tests are ready. The November 2 follow-up
  adds the application coordinator on `feature/setup-dealing`; the first-turn
  interface and action execution remain unfinished.

### Week 5 completion review (November 2 follow-up)

This review compares the team's original Week 5 plan, the revised setup scope
and the provided course guidance against the repository. It does not treat
later hand evaluation, complete betting, pot settlement, equity bots or full
translation work as completed Week 5 requirements.

| Planned work | Status | Evidence / remaining action |
| --- | --- | --- |
| Member A: cards, deck behavior, evaluator interface, BVA and TDD | Complete | Card/Deck, interface-only HandEvaluator, BVA documents and 64 passing cases. |
| Member B: player/seat/action/state foundations, setup validation and bot assignment | Implemented | Shared model and setup classes, BVA and component tests; statistics extend beyond the original minimal foundation scope. |
| Build and initial CI | Implemented | Wrapper and CI workflow exist; local full-suite build verified. Required GitHub checks remain a separate verification item. |
| Shared card/deck/state initialization | Implemented locally | GameInitializer and 16 new cases pass; branch `feature/setup-dealing` still needs push, teammate review and merge. |
| Final rules and shared agreements | Incomplete | Game rules file is still a placeholder. Reconcile the older 5/10 and current 10/20 blind settings and document the agreed button policy. |
| Minimal launchable interface and localization structure | Incomplete | `ui/Main.java` is empty; no Swing setup/table screen or localization resources are implemented. Full translation remains later work. |
| Project board, PR protections and required instructor meeting | Needs external verification | Local files cannot establish current board assignments, enforced GitHub rules or meeting completion. Confirm these and record supporting links/evidence. |
| Weekly planning and evaluation | Updated | Contributions, test-count breakdown, merged work, remaining tasks and branch status are recorded here. |

The main outstanding Week 5 implementation task is the minimal launchable
setup/first-turn interface. Finish the rules and verify the board/repository
workflow alongside it. Keep overall setup unchecked until its acceptance
criteria are demonstrated; adding tests merely to match a teammate's count
will not complete those missing behaviors.

### Next week's priorities

Push and review the initialization checkpoint, connect it to the Swing
interface, finish the agreed game rules, and update the board and review/check
settings. Begin the Week 6 hand evaluator
and betting work with BVA and TDD after the shared interfaces are confirmed.

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
