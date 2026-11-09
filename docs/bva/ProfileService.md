# ProfileService BVA

## Contract

ProfileService registers and authenticates password-protected local human
accounts. The default constructor accepts ProfileRepository and uses UUID IDs
and Pbkdf2PasswordHasher. The injected constructor accepts storage, PasswordHasher
and Supplier<String>; invalid collaborators are rejected. There is no match state
or mutable account cache.

create(name, password) validates identity/name, loads the catalog, rejects an ID
collision, hashes the password, and saves identity plus credential together before
returning. Duplicate display names are allowed; the profile selector uses stable
IDs. authenticate(id, password) returns Optional<PlayerProfile> only for the
selected account and matching password. Unknown IDs and wrong/invalid passwords
return Optional.empty. Storage and derivation failures propagate as IOException
or GeneralSecurityException. No partial account is saved after failure.

find(id) and list() expose public identity metadata for selection; they do not
authenticate and never return credentials. Null/blank lookup IDs are rejected;
unknown IDs are unavailable. Lists are immutable independent snapshots. The
application must serialize writers and use authenticate before opening a profile.
The login screen/session binding and E02 lifetime statistics remain pending.

## Cases

A fresh service and in-memory repository are used unless stated otherwise.

| ID | Public method(s) | System state / input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| PS-C01 | injected constructor; create; list; find | Existing older profile; injected new-id; Unicode name | Save before returning; preserve both IDs/names | Yes - locally passing |
| PS-C02 | create; list; find | Two generated IDs share display name 玩家 | Both remain independently retrievable | Yes - locally passing |
| PS-C03 | create | Generated ID already exists | IllegalStateException; original data retained | Yes - locally passing |
| PS-C04 | create | Name null; storage would fail if accessed | IllegalArgumentException; storage untouched | Yes - locally passing |
| PS-C05 | create | Name empty | Same rejection | Yes - locally passing |
| PS-C06 | create | Name one space | Same rejection | Yes - locally passing |
| PS-C07 | create | Name tab | Same rejection | Yes - locally passing |
| PS-C08 | create | Name newline | Same rejection | Yes - locally passing |
| PS-C09 | create | Name Unicode em space U+2003 | Same rejection | Yes - locally passing |
| PS-C10 | create | ID source returns null | IllegalArgumentException before storage | Yes - locally passing |
| PS-C11 | create | ID source returns empty text | Same rejection | Yes - locally passing |
| PS-C12 | create | ID source returns a space | Same rejection | Yes - locally passing |
| PS-C13 | list; create | Repository exposes a mutable list; then another profile is added | Result immutable; earlier result remains detached | Yes - locally passing |
| PS-C14 | find | Exact padded ID exists; unpadded/name/missing queries | Only exact ID matches; others unavailable | Yes - locally passing |
| PS-C15 | find | ID null | IllegalArgumentException before storage | Yes - locally passing |
| PS-C16 | find | ID empty | Same rejection | Yes - locally passing |
| PS-C17 | find | ID one space | Same rejection | Yes - locally passing |
| PS-C18 | find | ID tab | Same rejection | Yes - locally passing |
| PS-C19 | find | ID newline | Same rejection | Yes - locally passing |
| PS-C20 | find | ID Unicode em space | Same rejection | Yes - locally passing |
| PS-C21-create | create | Repository read throws IOException | Same failure propagates; no profile saved | Yes - locally passing |
| PS-C21-find | find | Repository read throws IOException | Same failure propagates | Yes - locally passing |
| PS-C21-list | list | Repository read throws IOException | Same failure propagates | Yes - locally passing |
| PS-C22 | create; list; find | Save fails, then retry succeeds | Existing data retained; one new profile after retry | Yes - locally passing |
| PS-C23 | both constructors | Null repository/default or injected; null ID source | IllegalArgumentException for each invalid dependency | Yes - locally passing |
| PI-C01 | default constructor; create; list; find | Real repository, UUID source, real hashing, new service instances | Stable IDs/names and password authentication survive reload | Yes - locally passing |
| PS-A01 | create; authenticate; list | Correct/wrong/null password and unknown selected ID | Only matching credentials return identity; public metadata retained | Yes - locally passing |
| PS-A02 | create; authenticate | Derivation or verification provider fails | Same checked failure; no partially created account | Yes - locally passing |
| PS-A03 | authenticate; injected constructor | Null/blank ID, read failure or null hasher | Reject invalid dependencies; preserve read failure | Yes - locally passing |
| PI-C04 | create; list | Null or 14-character password with real hasher/repository | No account or catalog created | Yes - locally passing |

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
