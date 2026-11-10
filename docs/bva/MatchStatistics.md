# MatchStatistics BVA

Immutable match totals. start creates fresh per-player collectors; complete
replays accepted actions and paid outcomes into a completed single-hand delta.
addHand combines that delta with prior match totals through CompletedStatistics,
retaining deduplication IDs. snapshot produces immutable display rates;
completedSnapshots produces immutable raw completed deltas for profile saving.

| ID | Methods | System/input state | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| PM-C01 | start/complete/addHand/snapshot/completedSnapshots | Real played showdown, next hand, fresh match | Correct hand delta/rates; totals distinct from lifetime; new match resets | Yes - locally passing |
| PM-C02 | start/complete/addHand | Profile save fails before session publication | Earlier match collectors unchanged; retry adds once | Yes - locally passing |
| Existing completion cases | all match methods | Folds/showdown/all-in/elimination | Existing behavior retained; see [completion BVA](CompletedStatistics.md) | Yes - locally passing |

ProfiledMatchIntegrationTest and CompletedStatisticsIntegrationTest are real
application integration tests, not isolated coordinator unit tests.

Latest verification: full build 1,196 passing tests; Checkstyle/SpotBugs clean;
new/modified E02 component mutants all killed. See the existing
[quality record](../quality.md#completed-lifetime-statistics--november-9-2026).
These are local results; commit/CI/review/merge gates are not marked complete.
