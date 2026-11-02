# Weekly Reports

Reporting starts at Week 5. Add one report each week through the final week of
class, recording actual progress, remaining work, and links to supporting issues
or PRs when available.

## Week 5 (10/26/2026-11/01/2026)

### Goal

Prepare the development environment, requirements, initial design, and quality
workflow before implementing game logic.

### Completed work

| Owner | Work completed | Evidence |
| --- | --- | --- |
| Estella | Cloned the course repository using HTTPS. | Successful clone in the local terminal. |
| Estella | Installed JDK 11 and JDK 25 and verified the local Gradle build. | `java_home -V` lists both JDKs; `bash ./gradlew clean build` reported `BUILD SUCCESSFUL`. |
| Estella | Recorded the proposed Texas Hold'em scope and contributor names. | [Project overview](../README.md): six seats, 1-6 humans, bots, virtual chips, and shared-computer play. |
| Estella | Reviewed the rubric and simplified the README to the required project information. | [README](../README.md) includes dependencies, build/test commands, documentation links, and exceptions. |

The documentation changes are local. No PR links have been recorded for this
work. Kevin's environment and build verification still need to be confirmed.

### Setup work remaining

Kevin and Estella should agree on individual task owners and track the tasks on
the project board. Unchecked items are planned or need verification.

- [ ] **Shared toolchain:** agree on the Gradle version and document the JDKs
  used to run Gradle and compile/test the project; verify both contributors can
  build the project.
- [ ] **Requirements:** complete the [game rules](requirements/game-rules.md),
  including the authoritative rule source, chosen variants, starting chips,
  blinds, match progression, and expected behavior for invalid actions.
- [ ] **UI and languages:** choose the desktop UI framework and two languages;
  specify language selection, locale extension, and private-card handoffs on
  the shared computer.
- [ ] **Initial design:** document responsibilities and interfaces for game
  rules, bot logic, and the UI in the [design document](design/README.md).
- [ ] **Project board:** create detailed setup and feature tasks with owners,
  acceptance criteria, status, and links to related work.
- [ ] **Review workflow:** agree on branch/PR conventions and a merge strategy
  that preserves TDD/BDD evidence; verify protection of `main`, required reviews,
  and required CI checks on GitHub.
- [ ] **CI:** configure builds and tests for PRs and `main`; report whether tests
  actually execute, and verify failure detection once tests are available.
- [ ] **Quality setup:** agree on Clean Code standards and a definition of done;
  prepare coverage and mutation-testing configuration, report locations, and
  justified exclusions using the course's required metrics.
- [ ] **First feature preparation:** select a small feature, define its behavior
  and public-method contracts, and document its initial BVA cases using the
  [course format](bva/README.md) before implementing game logic.

### Progress evaluation

- Estella's local environment can build the starter project successfully.
- The game is not yet playable, and there are no implemented Java tests.
  A successful starter build does not establish tested game behavior.
- The wrapper currently specifies Gradle 9.7.1, while the README still lists
  Gradle 8.10. Resolve this difference as part of the shared-toolchain decision.
- Requirements and design documents still contain starter instructions. Local
  CI workflows are absent; GitHub board and protection settings remain unverified.
- Setup work remains before the team is ready to begin the first game feature.

### Next week's priorities

Complete the setup checklist, assign task owners, and add evidence links as work
is delivered. Review readiness together before starting the first feature with
BVA and the failing-test, implementation, and refactoring cycle.

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
