# Texas Hold'em - CS380 Group 16

A Java desktop no-limit Texas Hold'em project with six seats for 1–6 human
players, with bots filling the remaining seats. Players share one computer and
use virtual chips. A match ends when one player holds all the chips.

Planned features include:

- Complete betting rounds, blinds, all-ins, side pots, ties, and elimination.
- Bots that use simulated hand equity, pot size, calling cost, and legal actions.
- At least two languages selectable at startup, with support for adding locales.

**Status:** Initial development; the game is not yet playable. The UI framework
and supported languages are still to be selected.

## Contributors

- Kevin Wang
- Estella Xie

## Dependencies

- JDK 11
- JUnit Jupiter 5.10.0 (managed by Gradle)
- Gradle 8.10 (included Gradle Wrapper; no separate installation needed)

## Build and Test

Run from the repository root with JDK 11 installed. The first build requires
network access to download Gradle and dependencies.

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

There are currently no implemented tests or runnable game entry point. Launch
instructions will be added when the game is playable.

## Documentation

- [Game rules and requirements](docs/requirements/game-rules.md)
- [System design](docs/design/README.md)
- [Boundary value analysis (BVA)](docs/bva/README.md)
- [Weekly planning and progress](docs/weekly-reports.md)

These documents currently contain starter instructions and will be updated as
the project develops. The project-board link will be added once confirmed.

## Exceptions

### Code Coverage

Coverage is not yet configured or measured. The target is 100% cyclomatic
coverage, excluding View classes, the Main class, and Enum classes as allowed by
the course rubric. Document any additional exclusions with their justification.

### Mutation Testing

Mutation testing is not yet configured or measured. The target is to kill all
non-equivalent mutants. Document and justify any equivalent mutants identified.

### Others

No additional exceptions are documented.
