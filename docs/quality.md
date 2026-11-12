# Quality checks and measurement scope

The Gradle `check` lifecycle runs JUnit tests, Checkstyle on main/test sources,
SpotBugs on main/test bytecode and the scoped JaCoCo report. `build` includes
`check`. Mutation testing is a separate `pitest` task so ordinary feedback stays
fast; CI runs it after the build. All static findings fail their task: there is
no `ignoreFailures`, global suppression or baseline hiding existing warnings. Reviewed exceptions are exact bug-pattern/class/member matches in `config/spotbugs/exclude.xml`.
CI uploads test, coverage, mutation and static-analysis reports even after a
failed step.

## Week 7 departure, legal actions and state measurement (November 13, 2026)

Branch `feature/week7-betting`, code `b9ae32b`. Local `build pitest` passed
with Gradle 9.7.1 on JDK 17 and Java 11 compilation/tests.

| Check | Result |
| --- | --- |
| Team suite | **1,169 passing**, zero failures, errors or skips. |
| Checkstyle / SpotBugs | Zero main/test findings and no new exclusions. |
| JaCoCo, 34 configured parent components | **1,531/1,532 lines, 1,103/1,104 branches, 922/923 complexity counters, 7,922/7,927 instructions, 365/365 methods and 36/36 classes.** |
| PIT | **1,037/1,051 killed (98.67%)**, 14 reviewed survivors, zero uncovered, timeout or error mutants. |

The new scope adds core state, betting input/output and statistics context
classes. The sole missed JaCoCo branch is the unknown-phase fallback in
`GameState.validateCards`. Every `GamePhase` constant has a case, and
construction rejects a missing phase before validation.

Boundary and real-session tests cover departure during and outside a player's
turn, all-in eligibility through payout, exclusion from later hands, and
private-card handoffs. They also check exact-stack calls, full and short wager
targets, and the inclusive raise-count limit for constructed snapshots.
Headless Swing tests exercise the controls in both languages.

| Remaining survivor | Count | Reason |
| --- | --- | --- |
| Evaluator and hash-code baseline | 7 | Six evaluator mutations preserve ranking. The constant hash preserves Java's contract but changes hash values. Course acceptance remains pending. See [mutation survivor review](#mutation-survivor-review). |
| `BettingEngine` | 3 | A zero c-bet cannot have an outstanding call in the matching context. The acting seat has left the pending set before the search wraps. A zero chip delta cannot exhaust an active player's positive stack. |
| `BettingRound.closeMatchedLoneActor` | 1 | Including the actor in the maximum keeps the same comparison against other contributions. |
| `BettingAmounts.chipsAdded` | 1 | Accepted-action validation still rejects a zero-chip CALL before any state is published. |
| `GameState.hasShortBlindTarget` | 1 | The caller checks that the posted maximum differs from the target, which this helper requires to equal the big blind. |
| `GameConfig.validateSettings` | 1 | A zero starting stack still violates the required positive big blind and its stack limit. |

## Betting statistics context — November 12, 2026

Branch `feature/week7-betting`, code `a7c0f14`. Local `build pitest` passed
with Gradle 9.7.1 on JDK 17 and Java 11 compilation/tests.

| Check | Result |
| --- | --- |
| Team suite | **1,138 passing**, zero failures/errors/skips; 11 cases added. |
| Checkstyle / SpotBugs | Zero main/test findings; no new exclusions. |
| 22 configured parent components | **1,080 lines, 646 branches, 550 complexity counters, 5,735 instructions, 223 methods and 23 reported classes**, zero missed counters. |
| PIT | **629/643 killed (97.82%)**, 14 survivors; zero uncovered, timeout or error mutants. Selected-test line coverage 1,076/1,078; 36 test classes and 6,499 executions across mutants. |

The scope is unchanged. GameState's new count bound is exercised by the full
suite; GameState remains outside the measured scope pending K06. The 14
survivors match the prior checkpoint, including the c-bet boundary now in
`BettingEngine.flopContext`. No new survivor or exclusion was introduced.

Four inconsistent-count cases failed before constructor validation. Review
identified the overflow risk from an impossible count; two more tests failed
before the snapshot bound was added. The legal replay cases reach the same
wager with different raise counts, including short all-ins. Settlement tests
confirm that the all-in caller remains CALL and that direct c-bet responses
alone receive a fold-to-c-bet denominator. See [K03 cases](bva/BettingHistory.md#k03-exact-raise-counts-and-all-in-observations).

## Short all-ins and reopening — November 11, 2026

Branch `feature/week7-betting`, code `978a826`, after K01 report commit
`cc17bf4`. Local `build pitest` passed with Gradle 9.7.1 on JDK 17 and Java 11
compilation/tests. Remote checks and partner approval remain pending.

| Check | Result |
| --- | --- |
| Team suite | **1,127 passing**, zero failures/errors/skips; 36 cases added. |
| Checkstyle / SpotBugs | Zero main/test findings; no new exclusions. The immutable map copy is explicit in its constructor. |
| 22 configured parent components | **1,078 lines, 638 branches, 544 complexity counters, 5,700 instructions, 221 methods and 23 reported classes**, zero missed counters. |
| PIT | **621/635 killed (97.80%)**, 14 survivors; zero uncovered, timeout or error mutants. Selected-test line coverage 1,074/1,076; 36 test classes and 6,493 executions across mutants. |

The scope adds BettingProgress. GameState's new decision-history validation is
covered by the full suite; GameState and the other state classes remain outside
the measurement scope pending K06. Scoped JaCoCo counters do not prove
whole-game coverage or independent basis paths.

The [K02 cases](bva/ShortAllInBetting.md) first showed 13 of 18 engine cases
failing and both application cases failing, then all passing. The first full
run and code review caught an opening-minimum regression with a valid unopened
snapshot carrying a stale prior increment. A short-opening regression also
failed before the correction. The final run covers short/full openings,
individual reopening, exact-stack calls, side pots and automatic board runout.

Seven survivors are the unchanged evaluator/hash baseline described below.
The other seven preserve the engine contract for the same reasons as at K01:
three BettingEngine boundary changes (zero c-bet, zero chip delta and revisiting
the acting seat), three BettingAmounts guards (zero-cost CALL and two routes
past unopened preflop BET validation), and one BettingRound predicate (including
the lone actor in the opposing maximum). The redundant guards can affect
exception text but still reject the same requests before publishing state.
The target-at-current-wager guard now matters for exact-stack matches; a
regression kills both mutations there. No survivors were excluded from the score.

## Full betting checkpoint — November 9, 2026 (historical)

Branch `feature/week7-betting`, code `fda5bdf`, based on merged main `c71e9d6`. Local
`build pitest` passed with Gradle 9.7.1 on JDK 17 and Java 11 compilation/tests.
Push, new remote CI and partner approval remain pending.

| Check | Result |
| --- | --- |
| Team suite | **1,091 passing**, zero failures/errors/skips; 58 added cases. |
| Checkstyle / SpotBugs | Zero main/test findings, analysis errors or missing classes; existing exact exceptions retained. |
| 21 configured parent components | **1,034 lines, 600 branches, 509 complexity counters, 5,468 instructions, 205 methods and 22 reported classes**, zero missed counters. |
| PIT | **580/599 killed (96.83%)**, 19 survivors; zero uncovered, timeout or error mutants. Selected-test line coverage 1030/1032; 32 test classes and 6,097 executions across mutants. |

The added components are BettingEngine, BettingAmounts, BettingRound and
BettingHistory. This extends the earlier 17-parent scope; the remaining state
classes are still outside it. Swing remains exempt from scoped metrics.
JaCoCo uses the full JUnit suite; PIT uses selected tests, so its line count
can differ. These counters do not prove whole-game or independent basis-path
coverage. No mutation operator, target class or exclusion was removed to improve
these results; K06 still includes the remaining betting/state measurement work.

TDD: 45 boundary cases and two real played-hand integration cases failed before
implementation, then passed. Eleven later regression cases address reported
coverage/mutation gaps: affordable full-raise opportunity boundaries, opening
minimum/stack limits, rejection before dealing, matched/aggressor c-bet context,
short-blind targets and closing independent raise rights. The actual pipeline
uses real setup/dealing, betting, evaluation, payouts and completed statistics,
with controlled Random; it adds no mock library. Read-only code review found no
actionable K01 issues.

The raw 19 survivors remain visible. Seven are the unchanged evaluator/hash
baseline reviewed below; the twelve additional betting survivors are reviewed
separately below. K02 short all-in/reopening rules and K05 UI/limits are not
claimed complete by these measurements.

### Betting survivor review

Read-only review of source, PIT reports and bytecode found these twelve survivors
equivalent under the public engine contract (exception type, acceptance and
immutable state). Guard-removal mutants can change error messages or exception
origin; those are not promised by that contract. Keep all mutants in the raw
score and reassess these conclusions when K02 changes validation/reopening.

| Location / mutant | Count | Reason |
| --- | --- | --- |
| BettingEngine.contextFor:178, c-bet positive boundary | 1 | With cBet=0, the remaining conditions require currentBet=0 and a negative round contribution, which valid states reject. |
| BettingEngine.nextPendingActor:211, loop boundary | 1 | The omitted final iteration revisits the actor, whose ID was already removed from pending decisions. |
| BettingEngine.resolveAction:152, positive chip delta boundary | 1 | An ACTIVE actor has a positive stack; zero chips cannot equal that stack or make the actor all-in. |
| BettingAmounts.canMakeFullWager:51, maximum/current boundary | 1 | Equality leaves zero increment, below the positive full minimum. |
| BettingAmounts.chipsAdded:18, remove zero-cost CALL guard | 1 | AcceptedAction constructs StatsAction, which independently rejects zero-chip CALL before publishing state. |
| BettingAmounts.wagerDelta:35, target/current boundary and removed ordering guard | 2 | Targets at/below currentBet have a nonpositive increment and still fail the later positive full minimum; subtracting nonnegative bounded amounts cannot overflow. |
| BettingAmounts.wagerDelta:29/30, remove opening/PREFLOP checks | 2 | Bytecode indices 32/38 skip only the unopened-preflop phase rejection; the BET/opening-kind check remains. Their only new path is preflop BET, independently rejected by StatsAction.validateStreet. |
| BettingRound.after:25, participant-count boundary | 1 | With exactly one participant, lone-actor cleanup already clears pending, or no active pending player remains. The pending-empty condition still closes both sets. |
| BettingRound.after:27, remove pending.clear | 1 | Pending is already empty whenever this block runs; independent raise rights still clear. |
| BettingRound.closeMatchedLoneActor:48, include actor in opposing maximum | 1 | Testing actor >= max(opponents, actor) gives the same result as actor >= max(opponents), including the empty-opponent zero default. |

The other seven survivors retain the earlier six evaluator and one hash-contract
rationales below. No additional exclusion or artificial hash-value assertion was
added to force a perfect mutation score.

## Completed match statistics — November 6, 2026 (historical)

Branch `feature/week6-finish`; local `build pitest` uses Gradle 9.7.1 / JDK 17
with Java 11 compilation/tests. Checkpoints: `1a99b7d` pays pots and refunds;
`78e5271` retains completed totals and continues fixed-seat hands; `e1c2992` fixes short-blind decisions and completion checks. These local
changes still need push, remote CI and partner review.

| Check | Latest result |
| --- | --- |
| Team tests | **1,033 passing**, zero failures/errors/skips. |
| Checkstyle / SpotBugs | Zero main/test unexcluded findings, analysis errors or missing classes; no new suppression. |
| Configured 17 parent components | **827 lines, 431 branches, 391 complexity counters, 4,383 instructions, 174 methods and 18 reported classes**, zero missed counters. Reported classes include the generated PlayStats counter enum. |
| PIT | **440/447 killed (98.43%)**; 7 survivors, 0 uncovered. Full counts/statuses remain in the report. |

The measured scope now includes PotSettlement/Settlement, MatchStatistics,
NextHand, StatisticsView and PlayStats. Existing partner state/betting classes
remain outside this denominator, including the short-blind GameState/BettingEngine
follow-up; their full tests and shared static checks still run. UI stays exempt
from scoped metrics. Covered complexity counters do not prove independent basis
paths or full-game coverage.

TDD observed 9 failing payout cases, 4 completed-statistics/copy cases, 4 continuation
cases and 2 bilingual UI cases before their implementations. Three short-blind /
last-active regressions failed before the engine fix; two embedded-header layout
cases failed before the presentation fix. Later mutation-driven tests cover
stronger late contenders, separated-seat odd chips, every nonparticipant outcome,
reverse c-bet contexts, rejected/successful continuation privacy and raise rights;
they are regression coverage, not additional claimed feature RED cycles.

The final read-only code review found three betting/continuation issues; the
follow-up confirmed all three resolved. English/Chinese table and statistics
layouts were rendered at 1,100×740; native OS focus and cross-platform fonts still
need manual walkthrough. The actual pipeline is accepted ledger → real payout →
transactional completed totals → immutable bilingual table → next stable-seat hand.
Stopping the game discards in-memory statistics; saved profiles remain future work.

PIT line coverage is 823/823; 22 selected test classes ran 3,105 executions across
mutants. There are no uncovered, timeout or error mutants. The seven unchanged
survivors are the six evaluator cases and hash-contract case reviewed below.
All new settlement, continuation, completed-statistics and copy mutants are
killed. The configured 17 parent components produce 18 reported classes because
of the nested counter enum; this is not an 18-component full-game claim.

## Pending-statistics checkpoint — November 6, 2026

Branch `feature/week6-finish`, based on merged main `ac70b3b` plus the progress
notes in `4baef67`; statistics code is `634b2c6`. Local `build pitest` exited 0 in 58 seconds on Gradle 9.7.1 /
JDK 17, with Java 11 compilation/tests. Remote CI for this checkpoint is pending
push; the earlier main CI result below does not verify these new changes.

| Check | Result |
| --- | --- |
| Full team JUnit suite | **998 tests**, zero failures/errors/skips; includes 12 new adapter and 16 session-statistics cases. |
| Checkstyle / SpotBugs | Zero main/test findings after the existing reviewed exceptions; zero analysis errors/missing classes. No new exclusions. |
| Shared eleven-class JaCoCo scope | **2,353 instructions, 226 branches, 471 lines, 217 complexity counters, 104 methods, 11 classes**; zero missed counters. |
| PIT | **242/249 killed (97.19%)**, seven unchanged survivors; no uncovered/time-out/error mutants. Line coverage 472/472; 16 selected test classes, 1,523 executions across mutants. |
| New adapter / session | HandStatistics: 19 lines, 8 branches and 8 complexity counters covered; PIT 7/7 killed. SetupSession: 107 lines, 42 branches and 47 complexity counters covered; PIT 46/46 killed. |

The scope now includes the pending-statistics adapter. PlayStats itself, the
betting/state engine and Swing remain outside this metric denominator; their
tests and shared static checks still run. Counters do not prove independent
basis-path execution. The seven baseline survivors remain the six evaluator
cases and the unresolved hash-contract case reviewed below.

TDD first ran 28 new cases against the missing adapter/session connection:
25 failed and three existing zero-rate cases passed. All 28 passed after
implementation. Integration tests use real initialization, betting, adapter and
PlayStats, capturing observation deliveries and injecting failures before
mutation. No mock library, completed-hand totals or payout-dependent results
were added. See [BVA](bva/HandStatistics.md). Native focus/font walkthrough,
peer approval and new remote checks remain separate pending verification.

## Merged baseline — November 6, 2026

Verified main `ac70b3b` with `build pitest`: Gradle 9.7.1 on JDK 17 and
Java 11 compilation/tests. The local run exited 0 in 1 minute 19 seconds;
log: `/private/tmp/current-progress-check.log`. Its matching
[GitHub CI run](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/actions/runs/42232032730)
also completed successfully.

| Check | Result |
| --- | --- |
| Full team JUnit suite | 970 tests; zero failures, errors or skips. |
| Checkstyle / SpotBugs | Zero main/test unexcluded findings; zero analysis errors or missing classes. |
| Shared ten-class JaCoCo scope | 2,216 instructions, 216 branches, 438 lines, 204 complexity counters, 96 methods and ten classes; zero missed counters. |
| PIT | 237 generated, 230 killed, seven survived (97.05%); no uncovered, time-out or error mutants. PIT line coverage 439/439; 14 selected test classes and 1,436 executions across mutants. |

These scoped metrics include InitialDeal and the shared application session/view,
not the whole betting/state/statistics engine or Swing. They do not prove
independent basis-path execution. The seven survivors are the six evaluator
cases and hash-code contract case documented below. Earlier branch measurements
are preserved as history. The pending-statistics checkpoint above is newer.

## Reproduce

Gradle 9.7.1 requires a runtime JDK of at least 17; this computer uses JDK 17,
and CI installs JDK 25 plus the JDK 11 compilation/test toolchain. The Java
sources and tests still compile/run with Java 11. SpotBugs explicitly uses the
Gradle runtime JDK to analyze that Java 11 bytecode. Checkstyle 10.26.1 can run
on the project's Java 11 toolchain.

```sh
bash ./gradlew --no-daemon build
bash ./gradlew --no-daemon pitest
```

On CI the commands also pass
`-Porg.gradle.java.installations.fromEnv=JAVA_HOME_11_X64,JAVA_HOME` so both
installed JDKs are discoverable. Reports are under:

| Tool | HTML | Machine-readable |
| --- | --- | --- |
| JUnit | `build/reports/tests/test/index.html` | `build/test-results/test/TEST-*.xml` |
| Checkstyle | `build/reports/checkstyle/{main,test}.html` | sibling `.xml` files |
| SpotBugs | `build/reports/spotbugs/{main,test}.html` | sibling `.xml` files |
| JaCoCo | `build/reports/jacoco/test/html/index.html` | `build/reports/jacoco/test/jacocoTestReport.xml` |
| PIT | `build/reports/pitest/index.html` | `build/reports/pitest/mutations.xml`, `linecoverage.xml` |

## Pinned tools and rules

| Component | Version | Compatibility basis |
| --- | --- | --- |
| JaCoCo | 0.8.15 | Gradle's documented version; supports modern runtime instrumentation. |
| PIT Gradle plugin | 1.19.0 | Released with Gradle 9 support and JDK 17 minimum. |
| PIT engine | 1.22.1 | Default engine of plugin 1.19.0, explicitly pinned. |
| PIT JUnit 5 plugin | 1.2.3 | Requires PIT 1.19.4 or later; discovers this project's Jupiter tests. |
| Checkstyle | 10.26.1 | Java 11 compatible 10.x line, deliberately avoids newer JDK-only versions. |
| SpotBugs Gradle plugin | 6.5.13 | Current stable release checked against the project wrapper. |
| SpotBugs engine | 4.10.2 | Explicit engine pin; runs separately from the Java 11 test process. |

Checkstyle's [Sun starter](https://checkstyle.org/sun_style.html) and
[Google starter](https://checkstyle.org/google_style.html) informed
[`config/checkstyle/checkstyle.xml`](../config/checkstyle/checkstyle.xml).
The initial rules check naming, imports, braces, indentation, whitespace,
modifier order, fall-through, equals/hashCode pairing and source-file hygiene.
Indentation is four spaces, matching this repository; compact accessor/constructor methods remain allowed by the brace-placement rule. Mandatory Javadoc, import
ordering and complexity quotas are deferred until team agreement;
production Java has a 100-character line limit;
this is an explicitly adapted starter, not a claim of full Sun/Google compliance.
Tests use a separate `checkstyle-test.xml` retaining file/import hygiene,
type names, modifier checks and error-prone construct checks. Existing BVA IDs
such as `BP_01`, wildcard assertion imports and compact assertion tables are
accepted; method-name, import-star, indentation, brace-placement and whitespace
rules apply to production sources. This avoids a large unrelated reformat of
partner tests while retaining failing checks for actionable test mistakes.
SpotBugs uses maximum effort and includes low-confidence findings for review.

Primary references:
[Gradle JaCoCo](https://docs.gradle.org/9.7.1/userguide/jacoco_plugin.html),
[Gradle Checkstyle](https://docs.gradle.org/9.7.1/userguide/checkstyle_plugin.html),
[PIT plugin releases](https://github.com/szpak/gradle-pitest-plugin/blob/master/CHANGELOG.md),
[PIT JUnit 5 compatibility](https://github.com/pitest/pitest-junit5-plugin),
[SpotBugs plugin](https://github.com/spotbugs/spotbugs-gradle-plugin),
[SpotBugs plugin release](https://plugins.gradle.org/plugin/com.github.spotbugs/6.5.13).

## Reviewed static-analysis exceptions

The first real main-bytecode run reported 13 SpotBugs findings. Each exception
below matches only its exact class, method/field and bug pattern; other findings
in these classes still fail. These are not metric exclusions.

| Exact location | Pattern / count | Review and reason |
| --- | --- | --- |
| `InitialDeal.random`, `GameSetup.dealerRandom` | `EI_EXPOSE_REP2` / 2 | Injected `Random` objects intentionally preserve caller-selected seeded/stub behavior. Copying would destroy the seam; these services are mutable and do not claim immutable/thread-safe ownership. |
| `BettingProgress.getPlayersToAct/getPlayersAllowedToRaise` | `EI_EXPOSE_REP` / 2 | Private helper validates then calls `Set.copyOf`; the returned sets are immutable. |
| `GameState.getCommunityCards/getPlayerStates/getRemainingDeck` | `EI_EXPOSE_REP` / 3 | Constructor helpers call `List.copyOf`; callers cannot mutate internal lists. |
| `GameView.getPlayers/getCommunityCards` | `EI_EXPOSE_REP` / 2 | Players use `Collectors.toUnmodifiableList`; community comes from the immutable state snapshot. |
| `PlayerView.getHoleCards` | `EI_EXPOSE_REP` / 1 | Constructor uses immutable `PlayerState.getHoleCards()` or `List.of()`. |
| `GamePanel.session/messages/humanViewers` | `SE_BAD_FIELD` / 3 | `JPanel` inherits serialization, but this live setup screen is never serialized; session/resources are intentional live runtime state. |

The immutable-collection contracts are also covered by existing state/view tests.
Revisit these matches if constructors, collection ownership or UI persistence
change. Shared Checkstyle violations are fixed in source instead of baselined.

## Scope and interpretation

The current scope has 34 parent components, including betting, state, dealing,
settlement, completed statistics, continuation and the shared session/view.
Earlier measurements below are historical and retain their original scopes.

JaCoCo and PIT include `Card`, `Deck`, `PokerHandEvaluator`, `ShowdownResolver`,
`ShowdownResult`, `GameInitializer`, `InitialDeal`, `HandStatistics`,
`MatchStatistics`, `NextHand`, `StatisticsView`, `PotSettlement`, `Settlement`,
`PlayStats`, `BettingEngine`, `BettingAmounts`, `BettingRound`, `BettingHistory`,
`BettingProgress`, `SetupSession`, `SetupView`, `Localizations`, `GameConfig`,
`GameState`, `Player`, `PlayerState`, `HandPositions`, `SeatOrder`,
`ActionRequest`, `AcceptedAction`, `BettingResult`, `LegalActions`,
`StatsContext` and `StatsAction`, including nested classes. `SetupView` remains
in scope because it implements the session's private-card filtering.

PIT selects the domain, application, integration and localization-loader
suites. The complete JUnit suite also runs, including headless Swing tests.
PIT uses its standard DEFAULTS group, two workers and HTML/XML reports. Its
executions across mutants are separate from the JUnit case count. Timeout
permits twice a test's baseline duration plus two seconds.

Excluded from the current metric denominator:

- Shared foundation `GameSetup` and the read-only `GameView`/`PlayerView`
  projections; the full team suite still tests these.
- `HandEvaluator` and enums without hand-written branch behavior, including
  `Rank`, `Suit` and `ShowdownResult.Status`.
- Swing and launch code under the rubric's UI exemption; headless tests and
  shared static analysis still check those files.

The rubric's 100% cyclomatic-complexity coverage goal is distinct from 100% line,
instruction or branch coverage. [JaCoCo counters](https://www.jacoco.org/jacoco/trunk/doc/counters.html)
report covered/missed complexity derived from branch coverage and do not prove
independent basis-path execution; exception edges do not count as branches.
Report every counter separately and review any missed paths. The mutation goal
is to kill every non-equivalent mutant. PIT's raw killed/total percentage does
not automatically remove equivalent mutants or justify survivors/timeouts.
There is no invented score threshold or forced green metric gate: actual
findings and rubric gaps must remain visible until assessed.

## Setup and result-component baseline (November 6)

Verified locally on **November 6, 2026**, on `feature/showdown-display`, after
merging main `e8c66ff` with the bilingual result-display component. This includes
the isolated tests and setup, null-handling and localization refactoring:

```sh
bash ./gradlew --no-daemon build pitest
```

Gradle ran on JDK 17; source compilation and JUnit execution used the JDK 11
toolchain. The command exited 0 in 55 seconds (15 actionable tasks,
14 executed and one up-to-date). Reports use the standard paths above;
local log: `/private/tmp/showdown-main-merge.log`.

- Full team suite: **764 tests**, zero failures, errors or skips. This includes
  18 bilingual result-display and 11 isolated initialization cases; the original
  16 initializer and 20 session cases remain real-component integration tests.
  The result panel is included in the team suite and static checks, while Swing
  remains outside Kevin's coverage/mutation denominator.
- Checkstyle: **0 main / 0 test violations**, including the new 100-character
  production line limit. This is an adapted style, not full Google/Sun compliance.
- SpotBugs: **0 main / 0 test unexcluded findings**, zero analysis errors and
  zero missing classes. The reviewed exact filters above remain explicit.
- Kevin-only JaCoCo, now **ten classes** including extracted InitialDeal:
  **1,856/1,856 instructions**, **182/182 branches**, **353/353 lines**,
  **167/167 complexity counters**, **76/76 methods**, **10/10 classes**.
  These are 100% counters, not proof of independent basis-path execution.
- PIT: **197 generated; 190 killed; seven survived**; raw score **96.45%**.
  Zero uncovered, timed-out, non-viable, memory-error or run-error mutants.
  PIT line coverage: **354/354** (its line counting differs from JaCoCo).
  It selected 12 test classes and ran 1,085 test executions across mutants.

| Kevin component | JaCoCo lines | Branches | Covered complexity | PIT killed / generated |
| --- | --- | --- | --- | --- |
| Card | 11/11 | 6/6 | 8/8 | 7/8 |
| Deck | 18/18 | 8/8 | 9/9 | 8/8 |
| PokerHandEvaluator | 62/62 | 58/58 | 36/36 | 58/64 |
| ShowdownResolver | 36/36 | 20/20 | 14/14 | 16/16 |
| ShowdownResult | 9/9 | No branches | 5/5 | 5/5 |
| GameInitializer | 14/14 | 6/6 | 7/7 | 4/4 |
| InitialDeal | 31/31 | 8/8 | 10/10 | 14/14 |
| SetupSession | 28/28 | 6/6 | 15/15 | 11/11 |
| SetupView | 19/19 | 2/2 | 9/9 | 8/8 |
| Localizations | 125/125 | 68/68 | 54/54 | 59/59 |

Earlier branch checkpoints: the November 5 result-display build passed 753 tests;
the November 6 refactoring build passed 746. The combined suite above retains both.

Previous measurement (November 3): 735 tests; nine scoped classes; 319 lines,
184 branches and 153 complexity counters covered; PIT 178/186 killed with eight
survivors. Refactoring changes the denominators, so those historical values are
not current measurements. InitialDeal stays measured after extraction. All
197 component-baseline mutants are accounted for below; none was excluded to improve a score.

Existing localization fault-path fixtures cover missing/invalid resources,
IOException cause and stream closure, quoted braces/apostrophes, repeated or
reordered arguments, type compatibility and nested choice outputs. They still
pass after parser extraction. Native desktop focus and cross-platform layout
checks remain a separate manual team task.

## Mutation survivor review

The 197-mutant component-baseline `DEFAULTS` run retained seven survivors; none is hidden
by a PIT exclusion. The review below reasons about the published component contracts.
It does not change PIT's raw mutation score or prove literal output identity
for every public method.

| Mutant location | Count | Assessment |
| --- | --- | --- |
| `PokerHandEvaluator.bestFive`, outer `a/b/c/d` loop boundaries | 4 | Changing `<` to `<=` adds an iteration whose next nested loop is empty (`b=4`, `c=5`, `d=6` or `e=7`). No five-card candidate or result changes. |
| `PokerHandEvaluator.rankFive`, equal-count comparator branch forced false | 1 | Ranks enter the list in descending order. The alternative count comparator returns zero for equal multiplicities; Java's stable `List.sort` preserves their descending input order, producing the same group order. |
| `PokerHandEvaluator.score`, five-position loop boundary | 1 | The sixth iteration appends a zero base-15 digit because kickers have at most five entries. Every private score becomes exactly 15 times its previous value; all scores stay below 103 million, so no integer overflow or comparison-order change occurs. |
| `Card.hashCode`, integer return replaced by zero | 1 | Contract-preserving alternative, **not identical hash output**: a constant hash still satisfies equal objects having equal hashes. Different cards need not have distinct hashes. It can degrade hash-table performance; no hash-dispersion or performance requirement is currently specified. Acceptance of contract equivalence for this case remains pending. |

The six evaluator survivors are behaviorally equivalent under the current public
contracts; the hash-code survivor requires acceptance of contract equivalence
rather than literal hash-value equivalence. No artificial assertion of distinct hash codes was
added to inflate mutation kills. Consequently the raw score must remain below
100%, and the course's all-non-equivalent-mutants goal remains subject to this
review rather than an unqualified automated success claim.

References: [Java 11 stable list sorting](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html#sort(java.util.Comparator)),
[Java 11 hash-code contract](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Object.html#hashCode()).


## Earlier shared session follow-up — November 5, 2026

After integrating main with Estella's betting branch, local `clean build --continue`
and `pitest` pass on JDK 25 with Java 11 compilation/tests. The full suite is
**939 tests**, zero failures/errors/skips. Checkstyle reports0 main / 0 test
violations; SpotBugs reports0 main / 0 test unexcluded findings,0 analysis errors
and 0 missing classes. No exclusions or failing baselines were added.

The same nine-class measurement scope now includes shared session follow-ups;
it must not be attributed wholly to Kevin or treated as full-game coverage.
Current JaCoCo counts: **2,093 instructions,218 branches,392 lines,189 covered
complexity counters,80 methods,9 classes**, all with 0 missed. SetupSession
covers83/83 lines,44/44 branches and 41/41 complexity counters; SetupView covers
36/36 lines,2/2 branches and 19/19 complexity counters. These counters do not
establish independent basis paths or coverage of betting/state/statistics
classes outside the documented scope.

Current PIT: **227 generated,219 killed,8 survived**, raw **96.48%**. There are
no uncovered, timed-out, non-viable or run-error mutants. The selected 13 test
classes run 1,329 test executions across mutants; this is distinct from the 939
JUnit cases. PIT line coverage is393/393; its line denominator differs from
JaCoCo. The session's43/43 mutants and view's18/18 mutants are killed. The same
8 baseline survivors remain in the earlier review table, including the unresolved
hash-contract acceptance question; none is hidden or newly declared equivalent.

The first follow-up mutation run found two actionable gaps: checking the view's
20-chip preflop betting level, and concealing a revealed hand when playBotTurns
selects the human actor without executing a bot action. Both now have direct
behavior assertions. A separate actor-empty guard was redundant: session state
is published only after advancing closed rounds or producing a result; it was
removed rather than manufacturing invalid states to cover it. The explicit
fault/retry test verifies failed comparison does not publish the candidate or
append a duplicate action.

See the [local TDD record](tdd/week6-session.md) for RED/GREEN results and scope.
Headless English/Chinese played flows and offscreen startup/flop/result layouts
were checked. Native focus delivery and cross-platform fonts still require the
team's walkthrough. New remote CI and peer-review evidence remain pending push.


### Call-button layout regression — November 5

The later UI-only fix keeps the third action button visible when larger fonts
would otherwise wrap it below a fixed-height row. English/Chinese bounds and
label-fit tests extend the full suite to **941 passing tests**, with no
failures/errors/skips. Checkstyle and SpotBugs remain clean, without added
exclusions. The earlier mutation measurements are retained as historical
results; mutation testing was not rerun for this Swing-only layout change.

## Add tests and simplify initialization

InitialDeal is included in Kevin's coverage/mutation scope; moving work out of
GameInitializer did not remove it from the denominator. PIT includes both the
isolated coordinator tests and the renamed initialization/session integration
tests. All initialization/localization mutants are killed in this run. The old
extra-hand-allocation mutant is now killed because dealing traverses the actual
hand collection; an extra allocated seat changes the deal and fails integration
checks. This was a structural refactor, not a new user-visible requirement.


## Earlier PR15 reconciliation with main e8c66ff (November 6)

Full build: 952 tests, zero failures/errors/skips; Checkstyle 0 main / 0 test,
SpotBugs 0 main / 0 test unexcluded findings, zero analysis errors/missing classes.
The 100-character production rule is retained. No new suppression was added.

The shared ten-class scope includes InitialDeal and the shared session/view;
it is not a whole-game measurement. JaCoCo covers 2,216/2,216 instructions,
216/216 branches, 438/438 lines, 204/204 complexity counters, 96/96 methods and
10/10 classes. These counters do not prove independent basis paths. PIT
produces 237 mutants: 230 killed and 7 survivors (97.05%), with no uncovered,
timeout or error mutants. Its line coverage is 439/439. Fourteen selected test
classes ran 1,379 test executions in this run. The seven baseline survivors
are the six evaluator mutants plus the contract-preserving hash-code mutant
already documented above; the previous eight-survivor figure belongs to the
November 5 scope before the setup refactoring killed the allocation mutant.

A separate combined-source build includes the 18 ShowdownPanel cases from PR16:
970 tests and static checks passed at that earlier compatibility checkpoint.
The merged-history and CI verification are now recorded at the top of this file. The two result displays retain separate
resource keys because one formats a message plus settlement status and the
other formats winner names alone. Null-flow refactors, privacy/error states,
accepted-action logging and the visible Call button remain covered.
