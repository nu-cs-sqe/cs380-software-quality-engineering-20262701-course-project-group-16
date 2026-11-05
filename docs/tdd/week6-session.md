# Week 6 played-session TDD record — November 5, 2026

These are local test-run records, not separate RED/GREEN Git commits. No commits
or pushes were made for this follow-up. The baseline was the pending merge of
main at 11a4e00 into feature/basic-betting at b03784c, with 897 passing tests.

| Step | Behavior / command | Observed result |
| --- | --- | --- |
| BVA | SessionBetting.md and PlayedGamePanel.md written before their implementations | Input/lifecycle boundaries, expected results and test mappings defined |
| Session RED | `test --tests application.SessionBettingTest --tests integration.PlayedHandIntegrationTest` against unimplemented session APIs |29 tests,28 expected failures; existing null-initializer rejection already passed |
| Session GREEN | Same tests plus `application.SetupSessionTest` |49 passing:29 new and 20 original setup/privacy cases |
| UI RED | `test --tests ui.PlayedGamePanelTest` before action controls |10 tests,10 expected failures |
| UI GREEN | Same10 bilingual played-screen cases |10 passing |
| Combined review | `clean build --continue` |936 tests; three old setup assertions expected pot 30/first bot; new behavior is pot 90/current human after automatic bot turns. Two static findings required a direct immutable status copy and a stable Throwable local in the EDT test helper. No rules/exclusions weakened. |
| Hardening | Real evaluator retry after injected failure; zero-cost CALL rejection | Full build938 passing; static checks pass |
| Mutation review | `pitest` with session and played-hand tests included |228 mutants,217 killed; exposed missing preflop-bet assertion and privacy check for a human handoff with zero bot actions |
| Status display RED | Existing result/all-in cases assert visible player statuses | Four cases fail for missing labels, then pass with translated statuses |
| Refactor / final build | Remove a redundant actor-empty guard: published non-result states always have a decision; add focused mutation-driven checks | `clean build --continue`:939 passing; Checkstyle/SpotBugs clean |
| Final mutation | `pitest` |227 generated,219 killed,8 previously documented survivors; zero uncovered/error/timeout mutants. Session43/43 and view 18/18 killed. |

All game components in the played winner/tie tests are real. Tests inject legal
shuffle/dealer choices; a separate explicit fault test makes one evaluator call
fail, then retries against the real evaluator. The final checks use Java 25 for
Gradle 9.7.1 and the Java 11 compilation/test toolchain.

Test/BVA sources:

- [SessionBettingTest](../../src/test/java/application/SessionBettingTest.java):30 cases.
- [PlayedHandIntegrationTest](../../src/test/java/integration/PlayedHandIntegrationTest.java):2 cases.
- [PlayedGamePanelTest](../../src/test/java/ui/PlayedGamePanelTest.java):10 cases.
- [Session BVA](../bva/SessionBetting.md), [Swing cases](../bva/PlayedGamePanel.md).

The new 42 cases extend897 to 939. The existing setup/privacy suite remains;
only the old UI's pot/current-actor expectations change to reflect automatic
bot actions. Local records do not establish separate committed RED/GREEN
history, teammate approval, remote CI success or branch-protection enforcement.
No completed-hand/payout statistics or settled pot are claimed.
