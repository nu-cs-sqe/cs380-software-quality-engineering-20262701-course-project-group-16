# Week 3 (10/12/2026-10/18/2026)
**Planning and Progress Tracking**:
1. [done] Person: Task (Links to PR)
2. [not started] Person: Task (Links to PR)
3. [80% done] Person: Task (Links to PR)
4. etc.

# Week 4 (10/19/2026-10/25/2026)
**Planning and Progress Tracking**:
1. [done] Person: Task (Links to PR)
2. [not started] Person: Task (Links to PR)
3. [80% done] Person: Task (Links to PR)
4. etc.

# Week 5 (10/26/2026-11/01/2026)

**Weekly goal:** Complete game setup so the application is initialized and ready for the first preflop turn.

**Planning and Progress Tracking**:

1. [in progress] Kevin Wang (Member A): Implement cards, deck creation, shuffling, and two-card initial dealing using BVA and TDD. Track work in [issue #2](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/2).
   - Completed the first RED/GREEN cycle for `Deck.remainingCards()` on a new deck. The current implementation returns 52; actual card storage and uniqueness are still pending.
   - Added [Deck BVA analysis](bva/Deck.md), with implemented cases linked to `DeckTest.newDeckHas52Cards()` and remaining cases marked pending.
   - Local checkpoints: `7d89634` (failing size test) and `3d10c55` (passing initial-size behavior and BVA).
2. [done locally] Kevin Wang: Updated the README with the project title, contributors, Java 11 requirements, and Gradle build/test commands in commit `4efc050`. This status does not imply the change has been reviewed or merged into `main`.
3. [draft for team review] Kevin Wang: Documented the proposed classes, shared interfaces, and initialization flow in [the setup design](design/game-setup.md). Chip/blind values, dealer policy, and Member B interfaces still need confirmation.
4. [planned; progress to be confirmed] Member B: Implement seats, human/bot assignment, chips, blinds, dealer selection, and the first actor. Track work in [issue #3](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/3).
5. [planned; progress to be confirmed] Shared: Integrate setup and the basic Swing interface, including private-card handling and first-turn readiness. Track work in [issue #4](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/4).

**Verification recorded so far:** The initial-size JUnit test passed, and the Gradle build succeeded. These checks do not yet verify a real 52-card deck or full game setup.

**Pull requests:** No PR link has been recorded in this report yet. Add the Draft PR link after opening it, then record review/merge progress.

**Next steps:** Review the setup design together; record PR links; continue Member A's next BVA/TDD checkpoint; confirm Member B progress; complete CI/review-rule setup tracked in [issue #5](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/5).

# One section per week until the last week of class (the week before the Finals week)


# Week X (XX/XX/2026-XX/XX/2026) TEMPLATE (You can change the format to whatever the team likes better)
**Planning and Progress Tracking**:
1. [done] Person: Task (Links to PR)
2. [not started] Person: Task (Links to PR)
3. [80% done] Person: Task (Links to PR)
