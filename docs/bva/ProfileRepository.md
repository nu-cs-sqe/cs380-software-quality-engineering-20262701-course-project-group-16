# ProfileRepository contract BVA

ProfileRepository is the application-owned storage port for immutable
StoredProfile identity/credential records. It has no executable
default methods; its file adapter and service behavior are mapped separately.

| ID | Public method | State | Expected result | Implemented? |
| --- | --- | --- | --- | --- |
| PR-C01 | load | Missing catalog | Empty profile collection; see FP-C01 | Yes - locally passing |
| PR-C02 | load | Existing valid catalog | Saved identities/credentials in catalog order; see FP-C02–04 | Yes - locally passing |
| PR-C03 | load | Corrupt or unreadable catalog | IOException; data retained; see FP-C06–22 | Yes - locally passing |
| PR-C04 | save | Valid complete snapshot | Persist identity and credential together without partial publication; see FP-C02–04 | Yes - locally passing |
| PR-C05 | save | Null list/entry or duplicate ID | IllegalArgumentException before writes; see FP-C05 | Yes - locally passing |
| PR-C06 | save | Failed directory creation/replacement | IOException with prior target retained; see FP-C23–24 | Yes - locally passing |

Unit service tests supply an in-memory implementation. Real persistence is
covered by FileProfileRepositoryTest and ProfilePersistenceIntegrationTest.
The repository is used by a serialized application writer, not a multi-process
transaction service. See [file adapter BVA](FileProfileRepository.md) and
[service BVA](ProfileService.md).

Version-3 snapshots also include immutable completed raw counters and completion
IDs in each StoredProfile. Identity, credential and completed data share one
atomic replacement. ST-C01–05 and PM-C01–05 in
[lifetime BVA](LifetimeStatistics.md) verify migration, exact round trips and
failure/retry behavior. The port still serializes application writers.
