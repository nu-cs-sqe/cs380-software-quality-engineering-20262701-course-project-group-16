# Texas Hold'em - CS380 Group 16

A Java desktop no-limit Texas Hold'em game with six seats for 1–6 humans and bots
filling the remaining seats. Players share one computer and use virtual chips.
A match ends when one player holds all the chips.

**Status:** Cards, deck behavior, match setup, state and statistics are implemented.
The full game is not yet playable. Swing is the selected UI; the two startup
languages remain to be confirmed. Betting, equity bots and the interface follow.

## Contributors

- Kevin Wang (Member A)
- Estella Xie (Member B)

## Dependencies

- JDK 25 to run Gradle, plus JDK 11 for compilation and tests
- JUnit Jupiter 5.10.0 (managed by Gradle)
- Gradle 9.7.1 (included wrapper; no separate Gradle installation)

## Build and test

Install both JDKs and run from the repository root. The first build downloads
Gradle and dependencies. CI installs both JDKs automatically.

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

Test reports appear at `build/reports/tests/test/index.html`. Game launch
instructions will be added when a playable interface is available.

## Documentation

- [Game rules and requirements](docs/requirements/game-rules.md)
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
