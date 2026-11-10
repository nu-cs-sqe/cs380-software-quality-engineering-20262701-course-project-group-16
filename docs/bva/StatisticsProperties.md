# StatisticsProperties BVA

Package codec for completed data in version-3 profile catalogs. read requires all
16 long raw counters, a nonnegative completedCount and unique nonblank completed
IDs. CompletedStatistics validates logical subset/participation relationships.
write uses stable enum names and preserves opaque IDs as a set; no rounded rates or active
hand data. propertyCount includes 16 counters, one count and all completion IDs.

| ID | Methods | System/input state | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| ST-C01 | read/write/propertyCount | One completed opaque Unicode/newline ID plus active hand | Exact completed round trip; no active hand persisted | Yes - locally passing |
| ST-C02 | legacy path; write | Existing V2 password account | No stats properties required; successful save upgrades to V3 | Yes - locally passing |
| ST-C03 | read | Negative/inconsistent/nonnumeric counts; huge completed count | IOException at repository boundary; data retained | Yes - locally passing |
| ST-C05 | read/write/propertyCount | All counter types, two IDs; duplicate/missing history, negative count, missing counter | Exact counters/rates; reject corrupt variants | Yes - locally passing |

LifetimeStorageTest runs real filesystem encoding/decoding in JUnit TempDir;
CompletedStatisticsTest isolates model validation. All implemented methods are
included in coverage and mutation measurement; no serialization-library magic
or access to the user's data directory is used.

SP-C01 in StatisticsPropertiesTest isolates the codec with zero-hand/nonparticipant
history: two unique IDs round-trip, a duplicate is rejected even though all
counters remain zero. This avoids relying on a later hands/history inequality to
mask a missing duplicate-ID check. ID serialization order is not an API contract.

Latest verification: full build 1,196 passing tests; Checkstyle/SpotBugs clean;
new/modified E02 component mutants all killed. See the existing
[quality record](../quality.md#completed-lifetime-statistics--november-9-2026).
These are local results; commit/CI/review/merge gates are not marked complete.
