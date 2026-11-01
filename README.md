# Texas Hold'em

A Java Texas Hold'em course project for CS380.

## Contributors
- Kevin Wang
- Estella Xie

## Dependencies
- JDK 11
- JUnit 5.10
- Gradle 8.10 (included Gradle wrapper)

## Build and test

Configure your IDE and terminal to use JDK 11. Run these commands from the repository root. The included wrapper downloads the required Gradle version on its first run; a separate Gradle installation is not required.

Run the JUnit tests on macOS or Linux:

```bash
sh gradlew test
```

Build the project and run its checks, including tests:

```bash
sh gradlew build
```

On Windows, use `gradlew.bat test` and `gradlew.bat build` instead.

The HTML test report is generated at `build/reports/tests/test/index.html` after a test run.

## Project documentation

- [Project board](https://github.com/orgs/nu-cs-sqe/projects/128)
- [Game rules](docs/requirements/game-rules.md)
- [BVA requirements](docs/bva/README.md)
- [Weekly reports](docs/weekly-reports.md)

## Exceptions
### Code Coverage
N/A
### Mutation Testing
N/A
### Others
N/A
