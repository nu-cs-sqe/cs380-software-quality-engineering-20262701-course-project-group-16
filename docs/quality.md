# Quality checks and measurement scope

The Gradle `check` lifecycle runs JUnit tests, Checkstyle on main/test sources,
SpotBugs on main/test bytecode and the scoped JaCoCo report. `build` includes
`check`. Mutation testing is a separate `pitest` task so ordinary feedback stays
fast; CI runs it after the build. All static findings fail their task: there is
no `ignoreFailures`, global suppression or baseline hiding existing warnings. Reviewed exceptions are exact bug-pattern/class/member matches in `config/spotbugs/exclude.xml`.
CI uploads test, coverage, mutation and static-analysis reports even after a
failed step.

## E04 opponent learning — November 10, 2026

Verified locally on `feature/opponent-learning`, based on the committed/pushed
E03 checkpoint `2ce09a0`. Profile work remains on its separate branch. Gradle
9.7.1 runs on JDK 25 with Java 11 compilation/tests. Final `build pitest` exits 0.

| Check | Result |
| --- | --- |
| Full suite | **1,133 passing**, zero failures/errors/skips; **49 new E04 invocations** beyond E03's 1,084. |
| Checkstyle / SpotBugs | Zero main/test violations or unexcluded findings; zero analysis errors/missing classes. No new suppression. |
| Six new learning classes | **186 lines, 112 branches, 117 complexity counters, 920 instructions, 61 methods**, zero missed. |
| Newly measured StatsContext | **21 lines, 26 branches, 18 complexity counters, 86 instructions, 5 methods**, zero missed. Only accessor visibility changes; prior constructor behavior is retained. |
| Shared 29-parent scope | **1,222 lines, 679 branches, 633 complexity counters, 6,301 instructions, 292 methods, 30 reported classes**, zero missed. The extra class is the nested PlayStats counter enum. |
| New learning PIT | **138/138 killed**; StatsContext **20/20 killed**. No new survivors, uncovered, timeout or error mutants. |
| Shared PIT | **706/713 killed (99.02%)**; seven unchanged baseline survivors, zero uncovered/error/timeout mutants. PIT lines 1,218/1,218; 37 selected classes, 3,925 test executions across mutants. |

The new measured classes are DecisionContext, TendencyEstimate, PublicDecision,
ActionPrediction, OpponentBeliefs and OpponentModel; both new enums are exempt.
StatsContext is now public read-only metadata consumed outside domain.model and
included in both measurement tools with its existing tests. The shared scope
still excludes other betting/state components. Covered complexity counters do
not prove independent basis paths or whole-backend compliance. Baseline survivor
reasoning and the instructor-acceptance question below remain unresolved.

Local TDD stages observed 13 context/uncertainty RED cases, 9 event/prediction
RED cases, 13 aggregation/coordinator RED cases, 2 legal-opportunity consistency
RED cases and 3 context-free automatic-event RED cases, each followed by GREEN.
One all-in-call fixture was corrected after review to remove an impossible
three-bet opportunity. Eight real-component/evaluation invocations and one new
mutation regression were added after the implementation. They are not claimed
feature RED cases. No agent Git checkpoints, commits or pushes were created.

The first mutation run exposed two gaps: the position getter had only been
asserted true, and missing prediction context after learning could throw a
NullPointerException if its boundary guard were removed. Empty-state validation
alone masked the latter via the prior constructor. The new false-position
assertion and learned-state invalid-query case kill both. Static test findings
were fixed by consuming immutable results inside exception assertions and
removing unused final counter increments; no exclusions were added.

Controlled evaluation trains on 80 folds/10 calls/10 raises, then tests on a
separate 70/20/10 composition. Learned multiclass Brier error is **0.476199453**,
compared with **0.666666667** for the uniform, learning-disabled prior. Four seeded
permutations verify order invariance of this exchangeable count model; they are
not independent performance trials. The held-out actions never update the
model. Further evidence changes a raise belief from 11/103 to 91/203 while an
unseen street retains its zero-data prior. This is prediction/update evidence,
not poker win rate, calibrated real-player accuracy or proven playing strength.

The API excludes own choices, forced posts, rejected requests and departure
folds. Match-wide sequence checks prevent replay without retaining an unbounded
event ledger; failures leave both belief snapshots and the sequence unchanged.
Real engine integration verifies accepted CALL versus rejected CHECK and keeps
exact completed-hand PlayStats separate from immediate learning. No hidden
cards/deck or account credentials enter the model. See [design](design/README.md#opponent-learning-e04)
and [BVA](bva/OpponentModel.md).

E04 is locally verified, awaiting user commit, peer review, remote CI and merge.
E05 still supplies the varied legal policy; E06 wires committed events and
worker/state lifecycles. Sampling remains uniform until a validated hand-range
likelihood is supplied. I05/I06/M01 and whole-backend quality gates remain open.

## E03 standalone equity — November 10, 2026

Verified locally on `feature/monte-carlo-equity`, based on merged main `c71e9d6`.
The separate profile branch retains its committed E01/E02/UI work; it is not part
of this branch's 1,033-test baseline. No profile changes were removed. Gradle
9.7.1 runs on JDK 25; compilation/tests use the Java 11 toolchain.

| Check | Result |
| --- | --- |
| Full JUnit suite | **1,084 passing**, zero failures/errors/skips; 51 new E03 invocations. |
| Checkstyle / SpotBugs | Zero main/test violations or unexcluded findings; zero analysis errors/missing classes; no new suppression. |
| Five executable E03 classes | **188 lines, 110 branches, 107 complexity counters, 912 instructions, 52 methods**, zero missed. |
| Shared 22-parent scope | **1,015 lines, 541 branches, 498 complexity counters, 5,295 instructions, 226 methods, 23 reported classes**, zero missed. The nested PlayStats counter enum accounts for the extra class. |
| E03 PIT | **108/108 killed**, no survivors or uncovered/error/timeout mutants. |
| Shared PIT | **548/555 killed (98.74%)**, seven unchanged baseline survivors; no uncovered/error/timeout mutants. PIT line coverage 1,011/1,011; 28 selected classes, 3,518 test executions across mutants. |

Coverage/PIT now include EquityPot, EquityRequest, EquityEstimate,
UniformDealSampler and MonteCarloEquityEstimator. The two functional ports have
no executable defaults. Existing unmeasured betting/state components remain
outside the denominator. These counters do not establish independent basis
paths or whole-backend compliance; the seven baseline survivor interpretations
below still require review.

Local TDD observed eight pot-value RED cases, then fifteen request RED cases,
then seventeen sampler/estimator RED cases, and three filtered-view RED cases,
each followed by GREEN. The initial combined request attempt had a fixture
initialization failure while EquityPot was a stub; that attempt is not counted
as behavioral request RED evidence. Four later boundary/mutation regressions
and four real-equity integrations are additional checks, not new feature RED
claims. No Git TDD checkpoints, commits or pushes were created by the agent.

The first PIT run exposed four actionable gaps: invalid opponent identity masked
by invalid pot membership, unknown pot eligibility masked by missing hero
eligibility, and a losing hand masking an ineligible-pot error. Focused valid-
otherwise scenarios now kill all four mutants. No new equivalence claim or
exclusion was introduced. Isolated estimator tests use controlled sampler and
evaluator ports; separate integrations use the real evaluator and filtered views.

Exact river equity over all **990** possible opponent holdings is
**0.869191919**. With 2,048 trials, seeds 7/23/91 returned
0.870849609 / 0.864257813 / 0.869873047; absolute errors were
0.001657690 / 0.004934107 / 0.000681128 (all below the fixed 0.06 regression
tolerance). Observed river calculations took 28.371 / 20.811 / 20.639 ms in the
final JUnit run. A six-player preflop calculation with 1,000 trials took
115.856 ms. These are local observations, not latency or general statistical
accuracy guarantees. A shared royal flush verifies a six-way fractional tie;
changing actual hidden cards/deck order leaves seeded estimates unchanged.
Output stays in standard JUnit reports under build/.

E03 supplies a standalone synchronous calculator with between-trial cancellation.
Engine-owned pot-scenario construction, learned opponent ranges, varied legal
actions, off-EDT execution and stale-result rejection remain E04–E06/shared
integration work. CI, peer review and merge for this branch are pending; leave
E03 unchecked in issue #20 until those gates are met.

## Completed match statistics — November 6, 2026

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

SetupSession and SetupView include shared played-hand integration. The
current configured scope has 17 parent components, including dealing, settlement,
completed statistics, continuation and the shared session/view.
Earlier nine-class results below are historical, not current full-game
coverage. PIT retains the renamed integration tests and the new session
and played-hand tests.

JaCoCo and PIT include `Card`, `Deck`, `PokerHandEvaluator`,
`ShowdownResolver`, `ShowdownResult`, `GameInitializer`, `InitialDeal`,
`HandStatistics`, `MatchStatistics`, `NextHand`, `StatisticsView`, `PotSettlement`,
`Settlement`, `PlayStats`, `SetupSession`, `SetupView` and `Localizations`,
including their nested classes. `SetupView` remains in the scope
even though a display DTO could qualify for a view exemption: its private-card
filtering is useful behavior to measure. Tests exercise these components
through their public contracts; the complete JUnit suite also runs in the build.
PIT selects their focused domain/application tests plus deck-setup,
setup/showdown integration and localization-loader tests. PIT counts repeated
test executions across mutants separately from the JUnit suite count. It uses the standard `DEFAULTS` operator group, two
workers, HTML/XML output and deterministic report paths. A mutant may hang a
loop; timeout permits twice its baseline test duration plus two seconds.

Excluded from the **current 17-component metric denominator**:

- The remaining domain state and betting components; their test
  execution is retained, but their coverage is not established by this scope.
- `HandEvaluator`, an interface without executable default methods, and the
  `Rank`/`Suit` and nested `ShowdownResult.Status` enums, whose constants have no hand-written branching behavior.
- Swing `ui` classes and launch code, using the rubric's UI exemption; headless
  behavior tests still run and shared static analysis still checks those files.

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
