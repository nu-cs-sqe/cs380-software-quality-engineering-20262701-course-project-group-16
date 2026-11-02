# Texas Hold'em - CS380 Group 16

A Java desktop no-limit Texas Hold'em game with six seats for 1–6 humans and bots
filling the remaining seats. Players share one computer and use virtual chips.
A match ends when one player holds all the chips.

**Status:** Cards, deck behavior, table initialization, immutable game state and
statistics are implemented. The application
initializer now combines table setup and dealing into a first PREFLOP state.
The Swing interface still needs to call it and display the initialized game. The full game is not yet
playable. Java Swing is selected; the two startup languages remain to be confirmed.
Betting execution, hand evaluation, equity bots and the interface remain future work.

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

Test reports appear at `build/reports/tests/test/index.html`. Game launch
instructions will be added when a playable interface is available.

## Application initialization

`new application.GameInitializer().start(humanCount)` starts a new match for
1-6 humans with bots filling six seats. It initializes chips, dealer, blinds
and first actor, shuffles a fresh deck, deals two cards per seat, and returns
a PREFLOP GameState with 40 cards remaining. Interface code should request
`state.viewFor(playerId)` so opponents' cards and the remaining deck stay private.
This is an application API; a playable Swing entry point is not implemented yet.

## Continuous integration

The [CI workflow](.github/workflows/ci.yml) runs a clean build and tests on pushes
and pull requests targeting `main`. It uses JDK 25 to run Gradle and JDK 11 for
compilation and tests, and uploads test reports. Check the workflow result on
your PR before merging. Required status-check enforcement still needs verification.

## Documentation

- [Game rules and requirements](docs/requirements/game-rules.md) (still needs to be completed)
- [System design and shared contract](docs/design/README.md)
- [Card/deck and setup integration](docs/design/game-setup.md)
- [Boundary value analysis](docs/bva/README.md)
- [Weekly planning and progress](docs/weekly-reports.md)
- [Project board](https://github.com/orgs/nu-cs-sqe/projects/128)

## Exceptions

### Code Coverage

Coverage is not configured or measured yet. The target is 100% cyclomatic
coverage, excluding View, Main and Enum classes as permitted by the rubric.
Document any additional exclusions and their justification.

### Mutation Testing

Mutation testing is not configured or measured yet. The target is to kill all
non-equivalent mutants and document justified equivalent mutants.

### Others

No additional exceptions are documented.
