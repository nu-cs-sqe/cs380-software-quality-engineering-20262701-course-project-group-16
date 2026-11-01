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
| Kevin | Completed immutable Card values, null validation, equality, and hashing through RED/GREEN checkpoints. | [Card BVA](bva/Card.md); GREEN commits `2219068`, `acbd186`, `a35669d`. |
| Kevin | Implemented the full deck, validated stateful dealing, immutable returned hands, and controlled shuffling through RED/GREEN checkpoints. | [Deck BVA](bva/Deck.md); GREEN commits `630deff`, `3a53ede`, `d2ad3b7`. |
| Kevin | Verified two dealing rounds for six hands and defined the Week 6 evaluator interface. | `9bf1c6e`; [setup design](design/game-setup.md); [planned evaluator cases](bva/HandEvaluator.md). |

Work is committed locally on `feature/cards-and-deck` and `docs/project-setup`.
No PR links have been recorded here yet. Add the Draft PR links when the branches
are pushed and reviewed; this report does not claim the work is merged into main.

### Setup work remaining

Kevin is Member A; Estella is Member B. Track their tasks on the project board.
Unchecked items remain planned, partially complete, or in need of team verification.

- [ ] **Shared toolchain:** agree on the Gradle version and document the JDKs
  used to run Gradle and compile/test the project; verify both contributors can
  build the project.
- [ ] **Requirements:** complete the [game rules](requirements/game-rules.md),
  including the authoritative rule source, chosen variants, starting chips,
  blinds, match progression, and expected behavior for invalid actions.
- [ ] **UI and languages:** use Java Swing; confirm two languages and specify
  language selection, locale extension, and private-card handoffs on the shared
  computer.
- [ ] **Initial design:** document responsibilities and interfaces for game
  rules, bot logic, and the UI in the [design document](design/README.md).
- [ ] **Project board:** keep the existing Team 02 tasks current with actual
  assignees, acceptance criteria, status, and links to related work.
- [ ] **Review workflow:** agree on branch/PR conventions and a merge strategy
  that preserves TDD/BDD evidence; verify protection of `main`, required reviews,
  and required CI checks on GitHub.
- [ ] **CI:** configure builds and tests for PRs and `main`; report whether tests
  actually execute, and verify failure detection once tests are available.
- [ ] **Quality setup:** agree on Clean Code standards and a definition of done;
  prepare coverage and mutation-testing configuration, report locations, and
  justified exclusions using the course's required metrics.
- [ ] **First feature integration:** Member A's components and BVA are complete;
  integrate them with Member B's setup state, turn order, chips, blinds, and the
  shared interface before declaring game setup complete.

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
- Game settings and rules were committed on `docs/project-setup`; they still
  need integration with this branch. Swing is selected. CI, protection settings,
  language choices, and actual setup/UI integration still need team verification.
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
