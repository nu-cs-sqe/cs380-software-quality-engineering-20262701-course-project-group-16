# FileProfileRepository BVA

## Contract

FileProfileRepository(Path directory) implements ProfileRepository. It stores a
version-3 UTF-8 profiles.properties catalog in the supplied directory.
The header contains formatVersion=3 and profileCount. V2 catalogs are also readable
with empty lifetime data; reads do not rewrite them. The next successful save
upgrades to V3 without changing identity/credentials. Each indexed record has
id, name, salt, hash, 16 named raw counters, a completed-ID count and its IDs; the version fixes PBKDF2-HMAC-SHA256 at 600,000 iterations,
a 16-byte salt and a 32-byte derived hash. No raw password is serialized.
Profile IDs are values, never path segments; names and IDs retain exact text.

load() returns an immutable list in catalog order; an absent catalog is empty.
Invalid schema/version/counts/values/duplicate IDs or malformed encoding produces
IOException without resetting the file. save(profiles) rejects null collections,
null elements and duplicate IDs before file access. It writes a temporary file in
the same directory, closes it, then requests atomic replacement. Unsupported atomic
replacement fails rather than falling back to an unprotected overwrite. Cleanup
removes the temporary file on success or failure. Access is serialized by the
application; multiple-process writers are outside this first adapter contract.

## Cases

All filesystem cases use JUnit-owned temporary directories.

| ID | Public method(s) | System state / input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| FP-C01 | constructor; load | Storage directory/catalog missing | Empty list; read creates no directory | Yes - locally passing |
| FP-C02 | save; load | Empty catalog, count zero | File written; new repository loads empty list | Yes - locally passing |
| FP-C03 | save; load | Opaque path-like ID, Unicode/padded/punctuation names | Exact round trip in order; immutable result; no ID-derived path | Yes - locally passing |
| FP-C04 | save; load | Save one profile, then replace with two | New complete snapshot; no temporary files | Yes - locally passing |
| FP-C05 | save | Existing catalog; null list, null element or duplicate IDs | IllegalArgumentException; bytes unchanged | Yes - locally passing |
| FP-C06 | load | Empty existing file | IOException; file unchanged | Yes - locally passing |
| FP-C07 | load | Unsupported version 99 | Same failure | Yes - locally passing |
| FP-C08 | load | Count -1 | Same failure | Yes - locally passing |
| FP-C09 | load | Nonnumeric count | Same failure | Yes - locally passing |
| FP-C10 | load | Integer.MAX_VALUE count with missing records | Reject promptly without huge allocation | Yes - locally passing |
| FP-C11 | load | Missing count | IOException | Yes - locally passing |
| FP-C12 | load | Missing version | IOException | Yes - locally passing |
| FP-C13 | load | Missing name | IOException | Yes - locally passing |
| FP-C14 | load | Blank profile ID | IOException | Yes - locally passing |
| FP-C15 | load | Blank display name | IOException | Yes - locally passing |
| FP-C16 | load | Duplicate IDs in two records | IOException | Yes - locally passing |
| FP-C17 | load | Empty catalog with unexpected property | IOException | Yes - locally passing |
| FP-C18 | load | One-record catalog with extra property | IOException | Yes - locally passing |
| FP-C19 | load | Invalid Unicode escape | IOException with parsing failure | Yes - locally passing |
| FP-C20 | load | Wrong property name despite matching property count | IOException | Yes - locally passing |
| FP-C21 | load | Invalid UTF-8 bytes | IOException | Yes - locally passing |
| FP-C22 | load | Catalog path is a nonempty directory | IOException; contents retained | Yes - locally passing |
| FP-C23 | save | Storage directory path is an ordinary file | IOException; contents retained | Yes - locally passing |
| FP-C24 | save | Existing nonempty catalog directory prevents replacement | IOException; target retained; temporary file cleaned | Yes - locally passing |
| FP-C25 | constructor | Null directory | IllegalArgumentException | Yes - locally passing |
| PI-C01 | save; load | Create and reopen real service/repository instances | Stable IDs/Unicode names and password verification survive reload; independent salts; no plaintext password | Yes - locally passing |
| PI-C02 | load via service create | Existing corrupt catalog | Creation fails without overwrite | Yes - locally passing |
| PI-C03 | save; load via services | Second instance adds profile after first snapshot | Original data/snapshot retained; fresh reads see both | Yes - locally passing |
| FP-C26 | load | Missing salt under a wrong key despite matching property count | IOException; bytes unchanged | Yes - locally passing |
| FP-C27 | load | Missing hash under a wrong key despite matching property count | Same failure | Yes - locally passing |
| FP-C28 | load | Invalid Base64 salt | Same failure | Yes - locally passing |
| FP-C29 | load | Hash decodes to 31 bytes | Same failure | Yes - locally passing |
| FP-C30 | load | Old metadata-only version 1 | Unsupported version rejected; no silent migration | Yes - locally passing |
| PI-C04 | create via service | Null or 5-character password | No account or catalog created | Yes - locally passing |

## Mapping and lifecycle

FileProfileRepositoryTest tests the real filesystem adapter.
ProfilePersistenceIntegrationTest exercises real service/storage/domain boundaries.
Identity/name metadata, salted credentials, completed counters and duplicate-
prevention IDs are saved together. No active-hand state, cards or chip stacks are
serialized. New V3/corruption/migration cases ST-C01–05 are mapped in
[lifetime BVA](LifetimeStatistics.md) and [the codec BVA](StatisticsProperties.md).
Profile selection UI remains later work.

## TDD status

See [service TDD observations](ProfileService.md#tdd-status) and the latest
[quality results](../quality.md#local-password-accounts--november-9-2026).
Regression cases FP-C26–30 were added after implementation; no new RED cycle is claimed.
