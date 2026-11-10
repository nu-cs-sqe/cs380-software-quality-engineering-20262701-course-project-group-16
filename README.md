# Texas Hold'em - CS380 Group 16

A Java desktop no-limit Texas Hold'em game with six seats for 1–6 humans and bots
filling the remaining seats. Players share one computer and use virtual chips.
Seats stay fixed during a match; players can stop the whole game.
The intended full match ends when one player holds all the chips.

**Status:** The supported fold/check/call flow now reaches real results, pays pots
and carries chips and player statistics into the next hand. Swing supports
English and Chinese, explicit private-card handoffs and a read-only statistics
tab. Bots use a temporary check/call policy. Bets/raises, postflop calls, equity
bot decisions and saved profiles remain unfinished on this branch. Standalone
Monte Carlo equity calculation is available in `bot`; action-policy wiring is pending.

## Contributors

- Kevin Wang (Member A)
- Estella Xie (Member B)

## Dependencies

- JDK 17 (verified locally) or JDK 25 (used by CI) to run Gradle, plus JDK 11 for compilation and tests
- JUnit Jupiter 5.10.0 (managed by Gradle)
- Gradle 9.7.1 (included wrapper; no separate Gradle installation)

## Build and test

Install a Gradle runtime JDK and JDK 11. Set `JAVA_HOME` to JDK 17 or 25 before running Gradle from the
repository root. Gradle uses the installed JDK 11 toolchain to compile and run
tests; setting `JAVA_HOME` to JDK 11 does not select the required Gradle runtime.
The first build downloads Gradle and dependencies. CI installs both JDKs automatically.

On macOS, select the locally verified JDK 17 runtime with:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
```

On Linux or Windows, set `JAVA_HOME` to your JDK 17 or 25 installation directory.
Use JDK 25 if you want the same Gradle runtime as CI.

macOS / Linux:

```bash
bash ./gradlew clean build
bash ./gradlew test
```

Windows PowerShell:

```powershell
.\gradlew.bat clean build
.\gradlew.bat test
```

If Gradle cannot find an installed JDK 11, set `JAVA_HOME_11` to that JDK
installation directory and add
`-Porg.gradle.java.installations.fromEnv=JAVA_HOME_11` to the Gradle command.

Test reports appear at `build/reports/tests/test/index.html`.

## Run the supported hand demo

With the JDKs configured above, run `bash ./gradlew run` (Windows:
`.\gradlew.bat run`) on a computer with a desktop display. Enter 1–6 human
players, select English or Chinese, and choose **Start Game**; bots fill the remaining seats. The highlighted
seat is the current human actor; bots act automatically. Use the enabled
**Fold**, **Check** or **Call** control, then pass the computer to the next human.
The board and winner/tie result update as rounds finish. Select a human in
**Private hand** and pass the computer
to that person before choosing **Reveal cards**. Switching humans, restarting
or leaving the window hides the cards. Language changes also hide cards while
preserving the match. English is the explicit default, independent of the
computer's language settings. Add future languages through resources/configuration;
see [localization](docs/design/localization.md).

This is shared-computer privacy by handoff, not authentication: anyone using the
computer can select another human. Actions are enabled only for the current
human viewer. After a result, choose **Settle hand** to pay pots and update
statistics, then **Next hand** to keep playing with the same players and stacks.
The **Statistics** tab shows completed-hand totals; N/A means no qualifying
opportunity. Switching tabs conceals cards. A match finishes when one player
holds all chips. **Start Game** starts a separate match and resets totals.
Statistics are kept in memory for this match; closing the game discards them.
Tests run headlessly, without opening a desktop window.

## Application initialization

`new application.GameInitializer().start(humanCount)` starts a new match for
1-6 humans with bots filling six seats. It initializes chips, dealer, blinds
and first actor, shuffles a fresh deck, deals two cards per seat, and returns
a PREFLOP GameState with 40 cards remaining. The Swing screen uses
`application.SetupSession` and its immutable `SetupView`, which expose public
player information and only the selected human's cards after explicit reveal.
All domain classes are under `domain.model`; coordinators stay in `application`
and Swing classes stay in `ui`.

## Standalone equity API

Create an `EquityRequest` from a filtered `GameView`, engine-supplied `EquityPot`
scenarios and 1–10,000 trials, then call `MonteCarloEquityEstimator.estimate`.
The result provides per-pot expected shares, eligible-pot equity and expected
payout. It handles multiway ties and side-pot eligibility without receiving the
real deck or opponents' cards. Uniform unknown hands are the initial model;
learning, varied action selection and asynchronous session integration follow
in E04–E06. See [equity design](docs/design/README.md#standalone-equity-e03).

## Continuous integration

The [CI workflow](.github/workflows/ci.yml) runs the build, JUnit tests,
Checkstyle, SpotBugs, JaCoCo and PIT on pushes and PRs targeting `main`. It uses
JDK 25 for Gradle and JDK 11 for compilation/tests, and uploads quality reports.
Check the actual workflow result on your PR before merging; remote execution
and required status-check enforcement still need verification after push.

Run `bash ./gradlew build` for tests/static checks/coverage and
`bash ./gradlew pitest` for mutation testing. The team suite and shared static
checks run across the project; coverage/PIT use twenty-two documented parent components,
including dealing, settlement, continuation, PlayStats, the shared session and equity. These are not
full-game metrics.
[Quality results](docs/quality.md)
list exact scope, report paths, adapted Checkstyle rules and reviewed SpotBugs
exceptions. There is no invented metric threshold that hides findings.

## Documentation

- [Game rules and requirements](docs/requirements/game-rules.md)
- [System design and shared contract](docs/design/README.md)
- [Card/deck and setup integration](docs/design/game-setup.md)
- [Boundary value analysis](docs/bva/README.md)
- [Hand evaluation and showdown boundary](docs/design/showdown.md)
- [Played-hand application flow](docs/design/played-hand.md)
- [Localization](docs/design/localization.md)
- [Quality checks and measured results](docs/quality.md)
- [Weekly planning and progress](docs/weekly-reports.md)
- [Project board](https://github.com/orgs/nu-cs-sqe/projects/128)

## Exceptions

### Code Coverage

Coverage uses the documented 22-component scope, including InitialDeal and the
shared session/view, statistics collectors and settlement. The rubric targets 100%
cyclomatic coverage, with View/Main/Enum exemptions. JaCoCo line, branch
and covered-complexity counters are reported separately; these do not prove
independent basis-path execution. The exact class scope and results are in
[quality.md](docs/quality.md); the remaining betting/state components
remain outside this measurement scope,
while their tests and shared static checks still run.

### Mutation Testing

PIT uses the DEFAULTS operator group on the documented 22-component scope. The target
is to kill every non-equivalent mutant. Actual raw scores and each surviving
mutant's reasoning remain visible in [quality.md](docs/quality.md), including
the unresolved acceptance of a contract-preserving constant hash. Survivor reasoning and acceptance limits are documented alongside the raw score.

### Others

Static-analysis exceptions are exact reviewed class/member/pattern matches in
`config/spotbugs/exclude.xml`, explained in [quality.md](docs/quality.md).
