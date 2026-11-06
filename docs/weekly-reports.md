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
| Done | Kevin | Update README, BVA/design contracts and weekly report with checkpoint evidence and open dependencies. | `8a322f9`, report cleanup `42a02d5`; [BVA index](bva/README.md). |

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

### Instructor review follow-up (November 6)

Branch: `refactor/instructor-feedback`. Each review item has its own local commit.

| Item | Status | Work and verification |
| --- | --- | --- |
| 1: Method size (`34effa7`) | Done | Extract clockwise dealing, hand allocation, player copying/creation and configuration validation into named helpers. Existing setup/dealing and BVA tests preserve behavior. |
| 2: Test isolation (`7b61f41`) | Done | Inject setup/deal operations; retain original real-component cases under integration test names and add 11 isolated coordinator cases. Include extracted InitialDeal in quality scope. |
| 3: Null flow | Done | Use Optional for session/actor/statistics-hand absence and empty lists for hidden cards; remove nullable UI rendering arguments. Preserve public validation and legacy actor input compatibility. Existing lifecycle/privacy/state tests cover these paths. |

### Verification and progress evaluation

Last implementation verification: **November 3**, local `clean build pitest --continue`,
16 tasks executed successfully. Gradle 9.7.1 used JDK 17 locally; compilation/tests
used JDK 11. CI is configured with JDK 25 plus the JDK 11 toolchain.

| Check | Recorded result |
| --- | --- |
| Team tests | **735 passing**, zero failures/errors/skips. |
| Checkstyle / SpotBugs | Zero unexcluded main/test findings; reviewed exceptions documented. |
| Kevin-only JaCoCo: nine classes | **100%**: 319/319 lines, 184/184 branches, 153/153 complexity counters, 1,748/1,748 instructions, 61/61 methods. |
| PIT | **178/186 killed (95.70%)**; eight reviewed survivors; zero uncovered/time-out/error mutants. |
| UI / review | English/Chinese offscreen layouts and card concealment checked; code review found no remaining actionable findings after the argument-validation fix. |

Kevin's comparison and supported bilingual setup flow are complete. Integration
uses real game components, controlled Random stubs and one fail-fast evaluator
stub for the uncontested path; no game components are mocked. The showdown
fixtures supply the engine boundary, so they do not establish played-hand
progression or result-screen readiness. Coverage is Kevin-scoped; JaCoCo
complexity counters do not prove independent basis-path execution. Mutation
survivor reasoning, including the hash-contract acceptance question, remains in
[quality.md](quality.md).

### Remaining work and next steps

- [ ] **Estella + Kevin:** Connect betting/turn/street progression to showdown and Swing results; test the played flow in both languages, including uncontested/all-in runout paths. Payouts/side pots and equity bots remain later work.
- [ ] **Together:** Walk through native desktop focus-loss privacy and cross-platform fonts/layout.
- [ ] **Together:** Complete [game rules](requirements/game-rules.md), reconcile proposed 5/10 versus implemented 10/20 blinds/button policy, confirm board assignees/status/PR links and record instructor-meeting evidence.
- [ ] **Together:** Review rubric interpretation and mutation survivors with the instructor; obtain partner-owned quality measurements separately.
- [x] **Kevin:** Implementation branch merged through PR #13 (`d2a78f3`).
- [ ] **Together:** Confirm remote CI results and required review/check enforcement, including failure detection; local results alone do not establish these settings.

## Future weekly report template

### Week N (MM/DD/YYYY–MM/DD/YYYY)

**Goal:** Planned outcome.

| Status | Owner | Task and acceptance criteria | Evidence / branch / PR |
| --- | --- | --- | --- |
| Planned / In progress / Done / Blocked | Name | Concrete task | Link or verified checkpoint |

**Progress evaluation:** Actual outcome, checks, blockers and differences from plan.

**Next steps:** Remaining tasks and owners.
