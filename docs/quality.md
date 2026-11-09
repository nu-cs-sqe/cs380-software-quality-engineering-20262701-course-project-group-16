# Quality checks and measurement scope

The Gradle `check` lifecycle runs JUnit tests, Checkstyle on main/test sources,
SpotBugs on main/test bytecode and the scoped JaCoCo report. `build` includes
`check`. Mutation testing is a separate `pitest` task so ordinary feedback stays
fast; CI runs it after the build. All static findings fail their task: there is
no `ignoreFailures`, global suppression or baseline hiding existing warnings. Reviewed exceptions are exact bug-pattern/class/member matches in `config/spotbugs/exclude.xml`.
CI uploads test, coverage, mutation and static-analysis reports even after a
failed step.

## Local password accounts — November 9, 2026

E01 now implements registration, public identity selection, password verification
and local identity/credential persistence on `feature/player-profiles`. A new
service/repository instance reopens the same accounts with unchanged IDs. The
Swing account screens, authenticated match binding and E02 lifetime-statistics
storage remain pending. No unfinished match is serialized.

TDD observations: the original model ran 20 expected RED failures then 20 GREEN;
metadata service/storage ran 53 RED then 53 GREEN; the password-account suite ran
84 expected RED failures then the same 84 GREEN. Subsequent regression cases
check early rejection before derivation, registration without a valid password,
and five damaged/old credential catalogs. These are observed local runs, not
separate RED/GREEN Git commits. No commits/pushes or extra scratch reports were
created. New remote CI, partner review and merge remain pending.

Latest local verification uses `bash ./gradlew --no-daemon build pitest` with
JDK 25 for Gradle and the installed JDK 11 compilation/test toolchain.

| Check | Recorded result |
| --- | --- |
| Full JUnit suite | **1,144 passing**, zero failures/errors/skips; 111 cases above the 1,033-test baseline, including 20 profile-model and 91 account/storage/security cases. |
| Checkstyle / SpotBugs | Zero main/test violations or unexcluded findings; no new exclusions or suppressed failures. |
| New profile/account JaCoCo scope | Six executable classes: **165 lines, 64 branches, 75 complexity counters, 693 instructions and 43 methods**, zero missed counters. |
| Profile/account PIT | **86/86 mutants killed**, including invalid-input provider rejection and sensitive-buffer cleanup; none uncovered or surviving. |
| Shared measured scope | **23 parent components**, 24 reported classes including the existing PlayStats counter enum; **992 lines, 495 branches, 466 complexity counters, 5,076 instructions and 217 methods**, zero missed counters. |
| Shared PIT | **526/533 killed (98.69%)**, seven pre-existing survivors; zero uncovered, timeout or error mutants. |

PIT reports line coverage **992/993**. Its single uncovered marker is the closing
`finally` brace in Pbkdf2PasswordHasher.matches (line 74), representing a generated
exception-cleanup path. For valid immutable PasswordHash values, Base64 decoding
and MessageDigest.isEqual inside that try block have no ordinary exceptional
input; provider failures occur before entering it. JaCoCo filters compiler-
generated cleanup and reports all 39 hasher lines covered. This difference is
reported rather than relabeled as 100% PIT line coverage. Both success/failure
cleanup of the provider specification and successful derived-buffer cleanup are
directly asserted by isolated tests.

The seven existing survivors remain the six evaluator cases and Card.hashCode
constant return documented below; instructor acceptance of the hash-contract
interpretation remains pending. Interfaces ProfileRepository, PasswordHasher and
PasswordDeriver contain no executable default methods. Remaining betting/state
components are still outside the configured measurement scope. Covered counters
do not independently prove basis-path execution or whole-backend compliance.

The new component path review maps each constructor validation partition,
service lookup/authentication success and rejection, read/write/provider failure,
file absence/corruption/atomic-replacement failure, password-length/Unicode bounds
and cleanup behavior to the corresponding per-class BVA. File/crypto integration
tests use real components; service orchestration and sensitive-buffer lifecycle
tests inject controlled collaborators as required by the instructor's review.

## PlayerProfile model — November 9, 2026

This historical first E01 checkpoint added only immutable identity/display text.
The later account increment above implements the services and local persistence.
The existing match Player remains unchanged.

Local TDD ran the same 20 PlayerProfileTest cases before and after implementation:
RED **20 expected failures**, then GREEN **20 passing**, zero errors/skips.
These are local executions, not separate RED/GREEN Git commits. No commits or
pushes were made; new remote CI and peer review remain pending.

Final verification: `bash ./gradlew --no-daemon clean build pitest`, using JDK 25
for Gradle and the JDK 11 compilation/test toolchain, exited 0 in 53 seconds.

| Check | Recorded result |
| --- | --- |
| Full JUnit suite | **1,053 passing**, zero failures/errors/skips; 20 PlayerProfile cases added to the 1,033-test baseline. |
| Checkstyle / SpotBugs | Zero main/test violations or unexcluded findings, analysis errors or missing classes; no suppression added. |
| PlayerProfile JaCoCo | **9/9 lines, 4/4 branches, 6/6 complexity counters, 32/32 instructions, 4/4 methods**; zero missed counters. Tests cover null, blank and nonblank paths for each constructor argument. |
| PlayerProfile PIT | **5/5 mutants killed**; no survivors or uncovered mutants. |
| Shared measured scope | **18 parent components**, 19 reported classes including the existing PlayStats counter enum; 836 lines, 435 branches, 397 complexity counters, 4,415 instructions and 178 methods, all with zero missed counters. |
| Shared PIT | **445/452 killed (98.45%)**, seven survivors; no uncovered, timeout or error mutants. PIT line coverage 832/832; 23 selected test classes and 3,190 test executions across mutants. |

The five new mutants are all killed. The seven survivors remain four bestFive
loop boundaries, the equal-count rank comparator, the score-loop boundary and
Card.hashCode's constant return, as reviewed below. The hash-contract interpretation
still needs instructor acceptance. Scope is explicit: other betting/state
components remain unmeasured, and covered counters alone do not establish
independent basis paths or whole-backend coverage. The November 6 results below
retain their historical 17-component scope.

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
current configured scope has 23 parent components, including local account
identity/storage/password services, dealing, settlement, completed statistics,
continuation and the shared session/view.
Earlier nine-class results below are historical, not current full-game
coverage. PIT retains the renamed integration tests and the new session
and played-hand tests.

JaCoCo and PIT include `Card`, `PlayerProfile`, `PasswordHash`, `StoredProfile`,
`ProfileService`, `FileProfileRepository`, `Pbkdf2PasswordHasher`, `Deck`, `PokerHandEvaluator`,
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

Excluded from the **current 23-component metric denominator**:

- The remaining domain state and betting components; their test
  execution is retained, but their coverage is not established by this scope.
- `HandEvaluator`, `ProfileRepository`, `PasswordHasher` and `PasswordDeriver`,
  interfaces without executable default methods, and the
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
