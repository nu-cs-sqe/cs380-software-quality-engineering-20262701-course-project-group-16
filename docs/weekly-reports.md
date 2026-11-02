# Weekly Reports

Reporting starts at Week 5. Add one report each week through the final week of
class, recording actual progress, remaining work, and links to supporting issues
or PRs when available.

## Week 5 (10/26/2026-11/01/2026)

### Goal

Initialize the game so it is ready for the first turn. Complete Member A's card/deck components and prepare the interfaces needed for Member B and shared setup integration.

### Completed work

| Owner | Work completed | Evidence |
| --- | --- | --- |
| Estella | Cloned the course repository using HTTPS. | Successful clone in the local terminal. |
| Estella | Installed JDK 11 and JDK 25 and verified the local Gradle build. | `java_home -V` lists both JDKs; `bash ./gradlew clean build` reported `BUILD SUCCESSFUL`. |
| Estella | Recorded the proposed Texas Hold'em scope and contributor names. | [Project overview](../README.md): six seats, 1-6 humans, bots, virtual chips, and shared-computer play. |
| Estella | Reviewed the rubric and simplified the README to the required project information. | [README](../README.md) includes dependencies, build/test commands, documentation links, and exceptions. |
| Estella | Implemented player identity, six-seat setup, immutable hand state, action contracts, filtered views and poker statistics with tests and BVA. | [Shared contract](design/shared-contract.md); [BVA index](bva/README.md); source branch `feature/game-state-and-stats`. |
| Estella | Added CI to build and test pushes and PRs using JDK 25 and the JDK 11 toolchain. | [Workflow](../.github/workflows/ci.yml). |
| Kevin | Completed immutable Card values, null validation, equality, and hashing through RED/GREEN checkpoints. | [Card BVA](bva/Card.md); GREEN commits `2219068`, `acbd186`, `a35669d`. |
| Kevin | Implemented the full deck, validated stateful dealing, immutable returned hands, and controlled shuffling through RED/GREEN checkpoints. | [Deck BVA](bva/Deck.md); GREEN commits `630deff`, `3a53ede`, `d2ad3b7`. |
| Kevin | Verified two dealing rounds for six hands and defined the Week 6 evaluator interface. | `9bf1c6e`; [setup design](design/game-setup.md); [planned evaluator cases](bva/HandEvaluator.md). |

Both contributors' work is pushed. The integration branch combines
`docs/project-setup`, `feature/cards-and-deck`, and `feature/game-state-and-stats`
without squashing their TDD commits. Member A's original [PR #6](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/pull/6)
provides its review context; the combined PR records final verification.

### Setup work remaining

Kevin is Member A; Estella is Member B. Track their tasks on the project board.
Unchecked items remain planned, partially complete, or in need of team verification.

- [ ] **Shared toolchain:** agree on the Gradle version and document the JDKs
  used to run Gradle and compile/test the project. The combined build uses JDK
  25, a JDK 11 toolchain and Gradle 9.7.1; Kevin still needs to verify this
  updated setup locally.
- [ ] **Requirements:** complete the [game rules](requirements/game-rules.md),
  including the authoritative rule source, chosen variants, starting chips,
  blinds, match progression, and expected behavior for invalid actions.
- [ ] **UI and languages:** use Java Swing; confirm two languages and specify
  language selection, locale extension, and private-card handoffs on the shared
  computer.
- [ ] **Initial design:** document responsibilities and interfaces for game
  rules, bot logic, and the UI in the [design document](design/README.md).
- [ ] **Project board:** keep the project tasks current with actual
  assignees, acceptance criteria, status, and links to related work.
- [ ] **Review workflow:** agree on branch/PR conventions and a merge strategy
  that preserves TDD/BDD evidence; verify protection of `main`, required reviews,
  and required CI checks on GitHub.
- [ ] **CI:** the build/test workflow is implemented. Record the combined PR's
  GitHub result and verify required status checks and failure detection.
- [ ] **Quality setup:** agree on Clean Code standards and a definition of done;
  prepare coverage and mutation-testing configuration, report locations, and
  justified exclusions using the course's required metrics.
- [ ] **First feature integration:** use Member A's Card/Rank/Suit model throughout
  state and views, verify deck-to-state composition, then connect the setup
  coordinator and Swing interface before declaring the full setup story complete.

### Progress evaluation

- Estella verified her starter build locally. Kevin verified Java 11 and Gradle
  8.10 for the implemented components.
- Member A's Week 5 code has 64 passing cases: 15 Card, 48 Deck, and one
  six-player initial-deal check. The recorded RED/GREEN commits show tests
  failing for missing behavior before the implementations passed.
- Boundary cases include fresh, partially used, one-card, and empty decks;
  zero/full/near-full draws; negative and extreme integer requests; rejection
  without changing remaining card order; returned-hand ownership; and repeatable
  shuffles that preserve only the remaining cards.
- An independent read-only review found no Week 5 code/test/BVA issues.
  The earlier 62-case independent review preceded the added random-stub tests.
  `sh gradlew --no-daemon clean build` passed with Java 11: all 64 cases ran,
  with zero failures, errors, or skipped cases.
- Added two test-only random-stub cases for the lowest and highest valid
  choices in a two-card shuffle. They check actual card order; a temporary
  ignored-source mutation was detected and restored. Production code is unchanged.
- The evaluator is an interface for Week 6 only. Evaluation, pot settlement,
  and equity estimation have not been implemented.
- The combined contract uses 1,000 starting chips, fixed 10/20 blinds, a random
  initial dealer, stable seats and no antes/blind increases. The shared Card
  value class is Member A's `domain.model.Card`; the duplicate enum is removed.
- Swing is selected. The CI workflow is present; required review/status-check
  settings, language choices, complete game rules and setup/UI integration
  remain to be completed or verified.
- The full game is not yet playable. Do not check off overall game setup until
  players, chips, blinds, first actor, and interface readiness are integrated.

### Next week's priorities

Review and combine the branches through PRs, add their links to this report,
and integrate the completed deck with Estella's setup classes. Continue Week 6
hand evaluation and betting using BVA and TDD after the shared interfaces are
confirmed.

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
