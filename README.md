# Texas Hold'em - CS380 Group 16

A Java desktop no-limit Texas Hold'em game with six seats for 1–6 humans and bots
filling the remaining seats. Players share one computer and use virtual chips.
Seats and chip ownership stay fixed during a match. Humans can leave. The agreed
match target is one player holding all 6,000 chips; departure-related terminal
behavior still needs reconciliation with that requirement.

**Status:** The engine and application session support full bets/raises and
fold/check/call on every street, settlement and continued hands with carried
chips/statistics. Requests specify round targets; the engine calculates deductions
and individual reopening eligibility. Swing exposes the five actions in English
and Chinese, private-card handoffs, local password accounts and lifetime statistics.
Bots use a temporary check/call policy; adaptive bots remain on their separate branch.

Standalone equity estimation and opponent learning are available in `bot`;
varied bot decision integration follows in E05/E06. Profile work is separate.

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

On macOS, select the locally verified JDK 25 runtime with:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
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

## Run the desktop game

With the JDKs configured above, run `bash ./gradlew run` (Windows:
`.\gradlew.bat run`) on a computer with a desktop display. Choose English or
Chinese, then use **Profiles** to create a name/password account. Enter its
password under **Sign in**, choose **Sign in and add**, and repeat for 1–6 humans.
Choose **Start Game**; bots fill the remaining seats. Removing a profile from the
next-match roster does not change an ongoing match.

Use **Fold**, **Check** or **Call**, or enter a round total and choose **Bet** or
**Raise**. Engine-provided limits determine the allowed wagers. Pass the computer
to the selected human before choosing **Reveal cards**. Switching humans/tabs,
changing language, restarting or leaving the window hides cards and clears password
fields. Profile authentication identifies accounts; private-card access still relies
on shared-computer handoffs.

After a result, choose **Settle hand**, then **Next hand**. **Statistics** shows
current-match totals; **Profiles** shows the selected signed-in account's lifetime
totals. N/A means no qualifying opportunity. A new match resets stacks/match totals
and retains completed lifetime data. Closing discards unfinished play. Saved
accounts/statistics are local in `~/.cs380-holdem/profiles.properties`; they reopen
after password verification. Storage errors leave settlement retryable.

Select a human and choose **Leave match** to retire their seat. Current actors fold
immediately; other active departures queue a fold, and all-in eligibility lasts
through settlement. The queued-departure winner and 6,000-chip terminal-policy
follow-ups are still open; see [quality results](docs/quality.md).
New locales use resources/configuration; see [localization](docs/design/localization.md).
Tests run headlessly with temporary catalogs, without creating real profiles.

## Application initialization

`new application.GameInitializer().start(humanCount)` starts a new match for
1-6 humans with bots filling six seats. It initializes chips, dealer, blinds
and first actor, shuffles a fresh deck, deals two cards per seat, and returns
a PREFLOP GameState with 40 cards remaining. The Swing screen uses
`application.SetupSession` and its immutable `SetupView`, which expose public
player information and only the selected human's cards after explicit reveal.
All domain classes are under `domain.model`; coordinators stay in `application`
and Swing classes stay in `ui`.

## Local account backend

`ProfileService` provides `create(name, password)`, `list()`, `find(profileId)` and
`authenticate(profileId, password)`. `login` returns a verified account handle
for lifetime statistics and `SetupSession.startProfiles`. Passwords are caller-owned `char[]` values;
new registration requires 6–15 characters. Existing longer account passwords still verify. Only authentication verifies
access; list/find expose public selection metadata. IDs are generated separately
from match participants and seats.

Construct it with `new FileProfileRepository(dataDirectory)` and reuse the same
application-data path after restart, for example
`Path.of(System.getProperty("user.home"), ".cs380-holdem")`. Registration saves
identity and a salted PBKDF2 hash together in `profiles.properties`; plaintext
passwords are never saved. Read failures reject corrupt data without resetting it.
The application must serialize writes. Profile-bound matches save completed raw
counters and duplicate-prevention IDs before publishing settlement; failed saves
remain retryable. Lifetime data survives restarts while new matches reset stacks
and match totals. Unfinished matches are never saved. The desktop Profiles tab
uses these APIs; account/password work runs off Swing's event thread. The optional
`cs380.profileDirectory` JVM property selects another local data directory.
See [account design](docs/design/README.md#local-password-accounts).
## Standalone equity API

Create an `EquityRequest` from a filtered `GameView`, engine-supplied `EquityPot`
scenarios and 1–10,000 trials, then call `MonteCarloEquityEstimator.estimate`.
The result provides per-pot expected shares, eligible-pot equity and expected
payout. It handles multiway ties and side-pot eligibility without receiving the
real deck or opponents' cards. Uniform unknown hands are the initial model;
opponent learning is available separately in `bot.learning`, while varied action
selection and asynchronous session integration follow in E05–E06. See
[equity design](docs/design/README.md#standalone-equity-e03) and
[opponent learning](docs/design/README.md#opponent-learning-e04).

## Continuous integration

The [CI workflow](.github/workflows/ci.yml) runs the build, JUnit tests,
Checkstyle, SpotBugs, JaCoCo and PIT on pushes and PRs targeting `main`. It uses
JDK 25 for Gradle and JDK 11 for compilation/tests, and uploads quality reports.
Check the actual workflow result on your PR before merging; remote execution
and required status-check enforcement still need verification after push.

Run `bash ./gradlew build` for tests/static checks/coverage and
`bash ./gradlew pitest` for mutation testing. The team suite and shared static
checks run across the project; coverage/PIT use 44 documented parent components,
including betting/state, dealing, settlement, continuation, PlayStats, local account
storage/password services and the shared session. These are not full-game metrics.
checks run across the project; coverage/PIT use 46 documented parent components,
including betting/state, dealing, settlement, continuation, PlayStats and the
standalone bot components. These are not
full-game metrics. The build also checks named table-rule constants and
production formatting; see the quality scope below.
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

Coverage uses the documented 44-component scope, including betting/state,
shared session/view, statistics, settlement and local accounts. The rubric targets
100% cyclomatic coverage, with View/Main/Enum exemptions. JaCoCo line, branch
Coverage uses the documented 46-component scope, including betting, InitialDeal and the
shared session/view, statistics collectors and settlement. The rubric targets 100%
cyclomatic coverage, with View/Main/Enum exemptions. JaCoCo line, branch
and covered-complexity counters are reported separately; these do not prove
independent basis-path execution. The exact class scope and results are in
[quality.md](docs/quality.md). The complete team suite and static checks also
cover components outside that measurement scope.

### Mutation Testing

PIT uses the DEFAULTS operator group on the documented 46-component scope. The target
is to kill every non-equivalent mutant. Actual raw scores and each surviving
mutant's reasoning remain visible in [quality.md](docs/quality.md), including
the unresolved acceptance of a contract-preserving constant hash. Survivor reasoning and acceptance limits are documented alongside the raw score.

### Others

Static-analysis exceptions are exact reviewed class/member/pattern matches in
`config/spotbugs/exclude.xml`, explained in [quality.md](docs/quality.md).
