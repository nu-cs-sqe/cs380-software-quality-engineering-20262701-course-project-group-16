# Persistent lifetime statistics BVA

| ID | Methods | System/input state | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| LS-C01 | login; getLifetimeStatistics | Valid/wrong password, null/foreign handle, missing record | Authoritative authenticated access only | Yes - locally passing |
| LS-C02 | saveCompleted | One hand delivered twice | One durable increment; duplicate causes no write | Yes - locally passing |
| LS-C03 | saveCompleted | Write fails then retry succeeds | Old totals retained; one increment on retry | Yes - locally passing |
| LS-C04 | saveCompleted | Missing delta, foreign/missing account | Reject without saving | Yes - locally passing |
| LS-C05 | saveCompleted | Two bound accounts, one unbound; missing second delta then valid pair | Failed validation changes none; one atomic save changes both; unbound preserved | Yes - locally passing |
| PM-C01 | startProfiles; settleHand; views; nextHand | Real setup/actions/showdown/save/restart | Persisted totals survive; stacks/match totals reset; bindings stable across hands | Yes - locally passing |
| PM-C02 | settleHand | File save fails before publication, then retry | No chip payment, match/lifetime totals or settlement published on failure; retry once | Yes - locally passing |
| PM-C03 | startProfiles/start; views | Invalid/duplicate/foreign login; restart | Rejected start retains bindings; unprofiled start clears them | Yes - locally passing |
| PM-C04 | startProfiles | Initializer human-count mismatch | Reject before publishing | Yes - locally passing |
| ST-C01 | FileProfileRepository.save/load | Raw counters, opaque IDs, pending next hand | Exact completed round trip only | Yes - locally passing |
| ST-C02 | load/save | Existing version-2 password account | Empty lifetime data; upgrade on successful save only; hashes retained | Yes - locally passing |
| ST-C03 | load | Negative/inconsistent/nonnumeric counters or huge history count | IOException; original bytes retained | Yes - locally passing |
| ST-C05 | save/load; PlayStats.fromCompleted | All raw counts and duplicate/missing history | Exact restored rates; invalid history rejected | Yes - locally passing |
| ST-C04 | StoredProfile constructors/getters | Null statistics, valid immutable snapshot | Reject null; retain immutable values | Yes - locally passing |

Isolated orchestration: LifetimeStatisticsTest injects storage and hashing.
Real integration: ProfiledMatchIntegrationTest and LifetimeStorageTest use JUnit
TempDir directories; no user profile directory is created. SetupSession exposes
UncheckedIOException from failed profile saves so its existing non-checked
settleHand API can preserve the pre-payment state for retry.

Observed TDD: 11 new service/application/storage tests failed against compiling
stubs with UnsupportedOperationException, then the same 11 passed after
implementation. Later LS-C05, PM-C04 and ST-C05 are regression hardening. The
snapshot increment has its own 28 RED → GREEN observation. No commits or pushes
are performed by the agent; these local observations are not separate Git history.

LS-C06 uses seven distinct authenticated accounts: six are accepted and correctly
mapped, seven rejected. PM-C03 also reveals cards before a rejected restart and
asserts concealment. SP-C01 isolates duplicate zero-counter history in the codec.
These are mutation-driven regression improvements, not additional feature RED
cycles.

PM-C05 injects an IOException after the real file save has committed. A new
service reload sees exactly one completed hand, while the current session still
has no paid state. Retry publishes settlement without another write or increment,
even while the failing adapter remains armed. This is an additional failure-window
regression, not a claimed feature RED cycle.

Latest verification: full build 1,196 passing tests; Checkstyle/SpotBugs clean;
new/modified E02 component mutants all killed. See the existing
[quality record](../quality.md#completed-lifetime-statistics--november-9-2026).
These are local results; commit/CI/review/merge gates are not marked complete.
