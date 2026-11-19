# ProfileService BVA

## Contract

ProfileService registers and authenticates password-protected local human
accounts. The default constructor accepts ProfileRepository and uses UUID IDs
and Pbkdf2PasswordHasher. The injected constructor accepts storage, PasswordHasher
and Supplier<String>; required collaborators follow the non-null caller contract. There is no match state
or mutable account cache.

create(name, password) validates identity/name, loads the catalog, rejects an ID
collision, hashes the password, and saves identity plus credential together before
returning. Duplicate display names are allowed; the profile selector uses stable
IDs. authenticate(id, password) returns Optional<PlayerProfile> only for the
selected account and matching password. Unknown IDs and wrong/invalid passwords
return Optional.empty. Storage and derivation failures propagate as IOException
or GeneralSecurityException. No partial account is saved after failure.

find(id) and list() expose public identity metadata for selection; they do not
authenticate and never return credentials. Blank lookup IDs are rejected;
unknown IDs are unavailable. Lists are immutable independent snapshots. The
application must serialize writers and use authenticate before opening a profile.
The desktop profile screen uses these APIs. login returns an issuer-bound AuthenticatedProfile;
getLifetimeStatistics requires that handle. Application-only saveCompleted merges
one completed hand per bound human and saves all updates atomically. See
[lifetime BVA](LifetimeStatistics.md) for LS-C01–05 and PM-C01–04.

W8-E02 adds `getChipBalance(AuthenticatedProfile)`. A handle from the current
service can read its saved balance (including zero); a foreign handle is
rejected. Completed statistics writes retain the existing balance. See the
[SQLite integration cases](SqliteProfileRepository.md).

## Cases

A fresh service and in-memory repository are used unless stated otherwise.

| ID | Public method(s) | System state / input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| PS-C01 | injected constructor; create; list; find | Existing older profile; injected new-id; Unicode name | Save before returning; preserve both IDs/names | Yes - locally passing |
| PS-C02 | create; list; find | Two generated IDs share display name 玩家 | Both remain independently retrievable | Yes - locally passing |
| PS-C03 | create | Generated ID already exists | IllegalStateException; original data retained | Yes - locally passing |
| PS-C04 | create | Name null; storage would fail if accessed | Non-null caller contract; no runtime null guarantee | Retired November 16, 2026 |
| PS-C05 | create | Name empty | IllegalArgumentException before storage | Yes - locally passing |
| PS-C06 | create | Name one space | IllegalArgumentException before storage | Yes - locally passing |
| PS-C07 | create | Name tab | IllegalArgumentException before storage | Yes - locally passing |
| PS-C08 | create | Name newline | IllegalArgumentException before storage | Yes - locally passing |
| PS-C09 | create | Name Unicode em space U+2003 | IllegalArgumentException before storage | Yes - locally passing |
| PS-C10 | create | ID source returns null | Non-null caller contract; no runtime null guarantee | Retired November 16, 2026 |
| PS-C11 | create | ID source returns empty text | IllegalArgumentException before storage | Yes - locally passing |
| PS-C12 | create | ID source returns a space | IllegalArgumentException before storage | Yes - locally passing |
| PS-C13 | list; create | Repository exposes a mutable list; then another profile is added | Result immutable; earlier result remains detached | Yes - locally passing |
| PS-C14 | find | Exact padded ID exists; unpadded/name/missing queries | Only exact ID matches; others unavailable | Yes - locally passing |
| PS-C15 | find | ID null | Non-null caller contract; no runtime null guarantee | Retired November 16, 2026 |
| PS-C16 | find | ID empty | IllegalArgumentException before storage | Yes - locally passing |
| PS-C17 | find | ID one space | IllegalArgumentException before storage | Yes - locally passing |
| PS-C18 | find | ID tab | IllegalArgumentException before storage | Yes - locally passing |
| PS-C19 | find | ID newline | IllegalArgumentException before storage | Yes - locally passing |
| PS-C20 | find | ID Unicode em space | IllegalArgumentException before storage | Yes - locally passing |
| PS-C21-create | create | Repository read throws IOException | Same failure propagates; no profile saved | Yes - locally passing |
| PS-C21-find | find | Repository read throws IOException | Same failure propagates | Yes - locally passing |
| PS-C21-list | list | Repository read throws IOException | Same failure propagates | Yes - locally passing |
| PS-C22 | create; list; find | Save fails, then retry succeeds | Existing data retained; one new profile after retry | Yes - locally passing |
| PS-C23 | both constructors | Null repository/default or injected; null ID source | Non-null caller contract; no runtime null guarantee | Retired November 16, 2026 |
| PI-C01 | default constructor; create; list; find | Real repository, UUID source, real hashing, new service instances | Stable IDs/names and password authentication survive reload | Yes - locally passing |
| PS-A01 | create; authenticate; list | Correct/wrong password and unknown selected ID | Only matching credentials return identity; public metadata retained | Yes - locally passing |
| PS-A02 | create; authenticate | Derivation or verification provider fails | Same checked failure; no partially created account | Yes - locally passing |
| PS-A03 | authenticate; injected constructor | Blank ID or read failure | Reject blank IDs; preserve read failure | Yes - locally passing |
| PI-C04 | create; list | 5-character password with real hasher/repository | No account or catalog created | Yes - locally passing |

## Mapping

ProfileServiceTest isolates coordination with an in-memory repository and supplied
IDs and a fake hasher. ProfilePersistenceIntegrationTest runs the real service,
file repository, domain models, password provider and default UUID source; it is not claimed as an isolated unit test.
No test reads or writes the user's profile directory.

## TDD status

Metadata orchestration/storage first ran 53 expected RED failures, then 53 GREEN
cases. The password-account increment ran 84 expected RED failures, then the same
84 cases GREEN. Later provider-rejection, passwordless-registration and damaged-
credential checks are regression hardening, not additional claimed feature RED
cycles. No separate RED/GREEN Git commits were made. See
[quality results](../quality.md#local-password-accounts--november-9-2026).

## Main integration repair — November 16, 2026

requireBoundHandDeltas rejects a missing completed-hand entry for any bound human
before loading/saving records. mergeBoundStatistics prepares local replacement
records, retaining issuer checks, unknown-account rejection and idempotent retry.
LS-C04/05 retain the missing-one/all and two-account no-partial-write cases.
PS-C04/10/15/23 and null-only portions of mixed tests are retired; blank, unknown,
foreign, collision, read/write failure and password-rejection cases remain.
