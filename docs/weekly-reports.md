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
are planned, partially complete, or still need team verification.

- [x] **Member A components:** complete cards, deck, shuffling, dealing and BVA;
  verify 64 component test cases.
- [x] **Shared card model:** combine the branches and use Member A's card values
  throughout state and views; add deck-to-state integration tests.
- [x] **CI workflow:** add automatic builds and tests for pushes and PRs.
- [ ] **Shared toolchain:** verify the updated setup on Kevin's computer. The
  merged project uses JDK 25 to run Gradle 9.7.1 and JDK 11 to compile/run tests.
- [ ] **Requirements:** complete the [game rules](requirements/game-rules.md),
  including the authoritative rule source, chosen variants, starting chips,
  blinds, match progression and expected behavior for invalid actions. The
  merged setup currently defaults to 1,000 chips and 10/20 blinds; reconcile
  the earlier 5/10 rules draft with the current shared contract.
- [ ] **UI and languages:** build the Swing setup/first-turn interface, confirm
  two languages, and implement private-card handoffs on the shared computer.
- [ ] **Initial design:** keep the [design document](design/README.md) current
  as the application coordinator, betting engine, bot logic and UI are added.
- [ ] **Project board:** update assignees, acceptance criteria, status and links
  to merged work.
- [ ] **Review workflow:** verify required reviews and passing CI checks on
  `main`; confirm CI failure detection and record the workflow run link.
- [ ] **Quality setup:** configure coverage and mutation testing, document
  justified exclusions and evaluate the course's required metrics. The manual
  shuffle mutation check is not a full mutation-testing report.
- [ ] **First-turn readiness:** move the tested deck-to-state composition into
  the application setup coordinator, connect the interface, and verify that
  the first human or bot can start its turn.

### Progress evaluation

- Kevin's Member A work has **64 verified passing component cases**: 15 Card,
  48 Deck and one six-player dealing check. The last local clean build before
  integration used Java 11 and Gradle 8.10, with zero failures, errors or skips.
  The recorded RED/GREEN checkpoints show tests failing before the related
  behavior was implemented.
- Estella's [integration merge record](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/commit/881ad796c1a3894d35da9667025a75bf6f85fa06)
  reports **554 passing tests** and successful GitHub CI: 488 Member B cases,
  64 Member A cases and two cross-component cases. Kevin has not yet rerun
  this combined suite with the updated local toolchain; 554 is the reported
  integration result, not a new local verification claim.
- The integration tests check 12 unique hole cards, 40 remaining cards, first
  actor seat 3, 10/20 blinds, 6,000 conserved chips, duplicate-card rejection
  by value and private human/bot views. They compose the components in test
  code; `GameSetup.create` itself still returns `SETUP` with no hole cards.
- The evaluator is an interface only. Hand evaluation, betting execution,
  pot settlement and equity estimation are not implemented yet.
- The full game is not yet playable. The setup goal is only partially met:
  the components and composition tests are ready, while the application
  coordinator and first-turn interface remain unfinished.

### Next week's priorities

Verify the merged build locally with the updated toolchain. Connect the setup
coordinator to dealing and the Swing interface, finish the agreed game rules,
and update the board and review/check settings. Begin the Week 6 hand evaluator
and betting work with BVA and TDD after the shared interfaces are confirmed.

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
