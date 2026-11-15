# AuthenticatedProfile BVA

Process-local immutable login handle with a package-only constructor. ProfileService
issues it only after password verification. getProfile exposes immutable public
identity; credentials/passwords are absent. belongsTo compares issuer identity,
so another ProfileService cannot reuse it, even against the same catalog.

| ID | Methods | System/input state | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| LS-C01 | constructor; getProfile; belongsTo; login | Correct/wrong password; same/foreign service | Valid login yields exact stable identity; wrong password gives no handle; foreign issuer rejected | Yes - locally passing |
| LS-C04 | ownership through saveCompleted | Foreign/missing account | No durable changes | Yes - locally passing |
| PM-C03 | ownership through startProfiles | Null account in supplied list | Reject before match publication | Yes - locally passing |

Covered by LifetimeStatisticsTest with injected hashing/storage, plus the real
ProfiledMatchIntegrationTest flow. Handles are never saved or resumed on restart.

Latest verification: full build 1,196 passing tests; Checkstyle/SpotBugs clean;
new/modified E02 component mutants all killed. See the existing
[quality record](../quality.md#completed-lifetime-statistics--november-9-2026).
These are local results; commit/CI/review/merge gates are not marked complete.
