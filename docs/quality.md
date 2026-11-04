# Quality checks and Kevin's measurement scope

The Gradle `check` lifecycle runs JUnit tests, Checkstyle on main/test sources,
SpotBugs on main/test bytecode and the scoped JaCoCo report. `build` includes
`check`. Mutation testing is a separate `pitest` task so ordinary feedback stays
fast; CI runs it after the build. All static findings fail their task: there is
no `ignoreFailures`, global suppression or baseline hiding existing warnings. Reviewed exceptions are exact bug-pattern/class/member matches in `config/spotbugs/exclude.xml`.
CI uploads test, coverage, mutation and static-analysis reports even after a
failed step.

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
ordering, line-length and complexity quotas are deferred until team agreement;
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
| `GameInitializer.deckRandom`, `GameSetup.dealerRandom` | `EI_EXPOSE_REP2` / 2 | Injected `Random` objects intentionally preserve caller-selected seeded/stub behavior. Copying would destroy the seam; these services are mutable and do not claim immutable/thread-safe ownership. |
| `BettingProgress.getPlayersToAct/getPlayersAllowedToRaise` | `EI_EXPOSE_REP` / 2 | Private helper validates then calls `Set.copyOf`; the returned sets are immutable. |
| `GameState.getCommunityCards/getPlayerStates/getRemainingDeck` | `EI_EXPOSE_REP` / 3 | Constructor helpers call `List.copyOf`; callers cannot mutate internal lists. |
| `GameView.getPlayers/getCommunityCards` | `EI_EXPOSE_REP` / 2 | Players use `Collectors.toUnmodifiableList`; community comes from the immutable state snapshot. |
| `PlayerView.getHoleCards` | `EI_EXPOSE_REP` / 1 | Constructor uses immutable `PlayerState.getHoleCards()` or `List.of()`. |
| `GamePanel.session/messages/humanViewers` | `SE_BAD_FIELD` / 3 | `JPanel` inherits serialization, but this live setup screen is never serialized; session/resources are intentional live runtime state. |

The immutable-collection contracts are also covered by existing state/view tests.
Revisit these matches if constructors, collection ownership or UI persistence
change. Shared Checkstyle violations are fixed in source instead of baselined.

## Scope and interpretation

JaCoCo and PIT both include Kevin's `Card`, `Deck`, `PokerHandEvaluator`,
`ShowdownResolver`, `ShowdownResult`, `GameInitializer`, `SetupSession` and
`SetupView` and `Localizations`, including their nested classes. `SetupView` remains in the scope
even though a display DTO could qualify for a view exemption: its private-card
filtering is useful behavior to measure. Tests exercise these components
through their public contracts; the complete JUnit suite also runs in the build.
PIT selects their focused domain/application tests plus deck-setup,
setup/showdown integration and localization-loader tests. PIT counts repeated
test executions across mutants separately from the JUnit suite count. It uses the standard `DEFAULTS` operator group, two
workers, HTML/XML output and deterministic report paths. A mutant may hang a
loop; timeout permits twice its baseline test duration plus two seconds.

Excluded from **Kevin's metric denominator**:

- Partner-owned domain state, betting and statistics components; their test
  execution is retained, but their coverage must not be attributed to Kevin.
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

## Measured results

Verified locally on November 3, 2026, on `feature/hand-evaluation`, after the
real showdown-integration and localization fault-path additions:

```sh
bash ./gradlew --no-daemon build pitest --continue
```

The measurement used an external init script to set the build directory to
`/private/tmp/kevin-quality-build` and an isolated Gradle project cache; no root
build directory was cleaned. HTML/XML reports were generated and inspected at
that location. The standard commands above regenerate them under `build/`.
The complete command exited 0 with `BUILD SUCCESSFUL` in 37 seconds.

- Full project JUnit suite: **718 tests**, zero failures, errors or skips. This
  is the team suite count, not an individual target or number of PIT test runs.
- Checkstyle: **0 main / 0 test violations** with the documented starter rules.
- SpotBugs: **0 main / 0 test unexcluded findings**, after the 13 exact reviewed
  exceptions listed above. This does not mean the unfiltered initial scan was empty.
- Kevin-only JaCoCo: **1,628/1,628 instructions**, **166/166 branches**,
  **292/292 lines**, **142/142 covered complexity**, **59/59 methods**, **9/9 classes**.
  Every reported counter is 100%; the complexity-counter result is not proof of
  independent basis-path execution or a substitute for the rubric interpretation.
- PIT: **170 generated; 162 killed; 8 survived**, raw mutation score **95.29%**.
  No uncovered, timed-out, non-viable, memory-error or run-error mutants. Its own
  line counter is **293/293**; PIT and JaCoCo count lines differently, so their
  denominators are reported separately. PIT selected 11 test classes and ran
  975 test executions across the mutants.

| Kevin component | JaCoCo lines | Branches | Covered complexity | PIT killed / generated |
| --- | --- | --- | --- | --- |
| Card | 11/11 | 6/6 | 8/8 | 7/8 |
| Deck | 18/18 | 8/8 | 9/9 | 8/8 |
| PokerHandEvaluator | 62/62 | 58/58 | 36/36 | 58/64 |
| ShowdownResolver | 36/36 | 20/20 | 14/14 | 16/16 |
| ShowdownResult | 9/9 | No branches | 5/5 | 5/5 |
| GameInitializer | 28/28 | 14/14 | 10/10 | 12/13 |
| SetupSession | 27/27 | 10/10 | 15/15 | 12/12 |
| SetupView | 19/19 | 2/2 | 9/9 | 8/8 |
| Localizations | 82/82 | 48/48 | 36/36 | 36/36 |

The first coverage run found untested localization rejection of the valid BCP47
`und` tag (whose `Locale` has no language) and the resource I/O failure path. Added
real fault-stream tests check rejection, preserved IOException cause and stream
closure. A Java 11 probe established that an empty language tag already throws
inside `Locale.Builder`; its unreachable later empty-string guard was removed.
The final report has no missed branches or lines. Existing source hygiene,
unused-import, test-exception and switch-format findings were corrected in
source; no failing baseline was waived to make the final command green.

## Mutation survivor review

The actual `DEFAULTS` run retained eight survivors; none is hidden by a PIT
exclusion. The review below reasons about the published component contracts.
It does not change PIT's raw mutation score or prove literal output identity
for every public method.

| Mutant location | Count | Assessment |
| --- | --- | --- |
| `PokerHandEvaluator.bestFive`, outer `a/b/c/d` loop boundaries | 4 | Changing `<` to `<=` adds an iteration whose next nested loop is empty (`b=4`, `c=5`, `d=6` or `e=7`). No five-card candidate or result changes. |
| `PokerHandEvaluator.rankFive`, equal-count comparator branch forced false | 1 | Ranks enter the list in descending order. The alternative count comparator returns zero for equal multiplicities; Java's stable `List.sort` preserves their descending input order, producing the same group order. |
| `PokerHandEvaluator.score`, five-position loop boundary | 1 | The sixth iteration appends a zero base-15 digit because kickers have at most five entries. Every private score becomes exactly 15 times its previous value; all scores stay below 103 million, so no integer overflow or comparison-order change occurs. |
| `GameInitializer.start`, hand-allocation loop boundary | 1 | The extra iteration appends an unused empty list. Valid player seats and the dealing loops still index the original six hands; returned game state and random consumption are unchanged. |
| `Card.hashCode`, integer return replaced by zero | 1 | Contract-preserving alternative, **not identical hash output**: a constant hash still satisfies equal objects having equal hashes. Different cards need not have distinct hashes. It can degrade hash-table performance; no hash-dispersion or performance requirement is currently specified. This case remains explicit for instructor/reviewer acceptance. |

The first seven are behaviorally equivalent under the current public contracts;
the eighth requires acceptance of contract equivalence rather than literal
hash-value equivalence. No artificial assertion of distinct hash codes was
added to inflate mutation kills. Consequently the raw score must remain below
100%, and the course's all-non-equivalent-mutants goal remains subject to this
review rather than an unqualified automated success claim.

References: [Java 11 stable list sorting](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html#sort(java.util.Comparator)),
[Java 11 hash-code contract](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Object.html#hashCode()).
