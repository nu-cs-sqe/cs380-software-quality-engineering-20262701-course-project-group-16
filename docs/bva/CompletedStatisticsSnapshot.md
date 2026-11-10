# CompletedStatistics BVA

Immutable completed raw counters and globally unique completed-hand IDs. Pending
observations and match stacks are excluded. All 16 named counters are required;
counts are nonnegative and must satisfy the poker-statistics subset relationships.
addHand accepts exactly one completed ID (zero/one dealt-in hands), adds checked
long counters, and treats a previously completed ID as an idempotent retry.

| ID | Methods | Input/system state | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| CS-C01 | empty constructor; getters; PlayStats.fromCompleted | No hands | All zero; unavailable rates | Yes - locally passing |
| CS-C02 | constructor; getters | Mutable valid inputs then caller mutation | Detached immutable data, exact opaque ID | Yes - locally passing |
| CS-C03–11 | constructor | Null/missing/negative counts, too few IDs, null/blank ID | Reject without publishing | Yes - locally passing |
| CS-C12–24 | constructor | Inconsistent counters/opportunities/participation | Reject | Yes - locally passing |
| CS-C29–34 | constructor | Incompatible c-bet histories, action counts or missing flop | Reject | Yes - locally passing |
| CS-C25 | addHand; getters; restoration | Two distinct completed hands then retry | Sum once; earlier snapshots unchanged; correct rates | Yes - locally passing |
| CS-C26 | addHand | Null/empty/multiple-hand delta or long overflow | Reject without altering earlier totals/history | Yes - locally passing |
| CS-C27 | PlayStats.completedSnapshot/fromCompleted/beginHand | Completed plus active hand | Export completed only; restore dedup history; no active state | Yes - locally passing |
| CS-C28 | snapshot/getters | Eliminated nonparticipant | Record completion ID; no hands played | Yes - locally passing |

Tests: domain.model.CompletedStatisticsTest. The original 28 cases ran RED with 28 expected failures, then GREEN unchanged.
Six additional validation cases refine independent subset/action partitions.
These are local executions; no RED/GREEN Git commits are claimed. Enum Statistic defines
stable storage keys and is exempt from a separate executable-class BVA.

CS-C35 additionally checks an extra null counter key and a negative postflop-raise
counter, independently of other subset checks. It was added after mutation
analysis exposed masked rejection paths.

Latest verification: full build 1,196 passing tests; Checkstyle/SpotBugs clean;
new/modified E02 component mutants all killed. See the existing
[quality record](../quality.md#completed-lifetime-statistics--november-9-2026).
These are local results; commit/CI/review/merge gates are not marked complete.
