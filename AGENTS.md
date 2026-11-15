# Project instructions

## Instructor conventions for all future changes

Read `docs/instructor-code-review.md` before changing production code. Follow the
Week 7 feedback linked from `docs/quality.md` until it is included in that file.
Preserve the instructor's original feedback.

- Required application-owned references and collection elements are non-null
  caller contracts. Do not add null guards, `Objects.requireNonNull`, isNull /
  nonNull checks, null-returning helpers, or nullable internal state. Do not hide
  the same checks behind renamed helpers.
- Represent legitimate absence with Optional / OptionalDouble / OptionalLong or
  empty collections. Use separate overloads for distinct event shapes. Normalize
  nullable results from an external Java API once at the adapter boundary with
  Optional; retain meaningful validation of missing/malformed resources.
- Keep business validation: blank/unknown IDs, range and overflow checks,
  duplicates, illegal actions, lifecycle consistency, privacy and conservation.
  New TDD/BVA cases must cover these behaviors and explicit absence, rather than
  programmer-null inputs. Preserve Java Object.equals contract tests.
- Give each method one named responsibility and one level of abstraction. Keep
  coordinators to a short sequence of named steps. Extract independent loops,
  validation conditions, dealing and state transitions into meaningful private
  helpers; do not merely move a large block to another large method.
- Keep constructors short. Supply collaborators when isolated orchestration tests
  need them. Distinguish isolated unit tests from tests using real components.
- Limit production Java lines to 100 characters, with four-space indentation.
  Keep new test code within 100 characters too (legacy test formatting has a
  separate Checkstyle configuration). This repository adapts the Google/Sun starter;
  do not claim full Google/Oracle style compliance. Method design requires review;
  the project does not impose an instructor-mandated numeric method-length cap.
- Name domain constants instead of scattering magic numbers. Preserve the
  declared MagicNumber scope and production Checkstyle formatting/null-flow
  checks. Do not add suppressions or exclusions to evade a finding. Any justified
  false-positive exception must be exact, documented and visible for review.

## Workflow

- Use genuine RED -> GREEN -> refactor for new behavior. Keep BVA aligned with
  implemented methods and meaningful boundaries; retain historical evidence.
- Review generated code. Run appropriate tests and required checks; source changes
  must pass `bash ./gradlew --no-daemon build pitest` before claiming final quality
  verification. Report actual scope, survivors and coverage limits honestly.
- Use this original checkout only. Do not create another checkout or worktree.
- Do not commit or push unless the user explicitly asks you to do so.
- Create no extra scratch reports, logs, backups or checkpoint files. Standard
  authorized Gradle outputs under build/ are allowed; update existing project
  documentation when needed.
