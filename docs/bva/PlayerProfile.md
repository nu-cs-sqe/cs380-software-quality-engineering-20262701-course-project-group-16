# PlayerProfile BVA

## Contract and scope

`PlayerProfile(String profileId, String displayName)` is an immutable human
profile identity. It is separate from a match's `Player`, seat, chip stack and
pending hand. Both inputs are non-null and nonblank according to Java 11
`String.isBlank()`. Invalid input throws `IllegalArgumentException`.

The supplied strings are retained exactly: this model does not trim, normalize,
restrict scripts, require UUID syntax or impose an arbitrary length limit.
Display names need not be unique; lookup and identity use `profileId`.

Automatic ID generation and cross-profile uniqueness belong to
[ProfileService](ProfileService.md). The same constructor can represent a profile loaded with its
existing ID. No persistence or service operation is implemented by this model.
Profile statistics and match state remain separate.

The public constructor, `getProfileId()` and `getDisplayName()` are mapped below.
Getters return the exact supplied valid values. Fields are private/final,
strings are immutable, and the final class exposes no mutation operation.

## Cases

Each case uses a fresh profile. Unless overridden, `profileId="profile-1"`
and `displayName="Estella"`. Rejection produces no constructed profile.

| ID | Method(s) | System state / input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| PP-C01 | Constructor | ID is null | IllegalArgumentException | Yes - locally passing |
| PP-C02 | Constructor | ID is empty | IllegalArgumentException | Yes - locally passing |
| PP-C03 | Constructor | ID is one space | IllegalArgumentException | Yes - locally passing |
| PP-C04 | Constructor | ID is one tab | IllegalArgumentException | Yes - locally passing |
| PP-C05 | Constructor | ID is one newline | IllegalArgumentException | Yes - locally passing |
| PP-C06 | Constructor | ID is Unicode em space U+2003 | IllegalArgumentException | Yes - locally passing |
| PP-C07 | Constructor | Display name is null | IllegalArgumentException | Yes - locally passing |
| PP-C08 | Constructor | Display name is empty | IllegalArgumentException | Yes - locally passing |
| PP-C09 | Constructor | Display name is one space | IllegalArgumentException | Yes - locally passing |
| PP-C10 | Constructor | Display name is one tab | IllegalArgumentException | Yes - locally passing |
| PP-C11 | Constructor | Display name is one newline | IllegalArgumentException | Yes - locally passing |
| PP-C12 | Constructor | Display name is Unicode em space U+2003 | IllegalArgumentException | Yes - locally passing |
| PP-C13 | Constructor; both getters | ID p; name A: one nonblank character each | Retain both exactly | Yes - locally passing |
| PP-C14 | Constructor; both getters | UUID-shaped ID; English name Estella Xie | Retain both exactly | Yes - locally passing |
| PP-C15 | Constructor; both getters | Name 玩家一 | Preserve Chinese characters | Yes - locally passing |
| PP-C16 | Constructor; both getters | Name Estella 玩家 | Preserve mixed scripts | Yes - locally passing |
| PP-C17 | Constructor; both getters | Name Zoë 🃏 | Preserve accented/supplementary characters | Yes - locally passing |
| PP-C18 | Constructor; both getters | ID and name have surrounding spaces | Retain supplied spaces | Yes - locally passing |
| PP-C19 | Constructor; both getters | ID: 512 p characters; name: 512 repetitions of 玩家 | Retain without an invented length cap | Yes - locally passing |
| PP-C20 | Constructor; both getters | IDs profile-1/profile-2 share name 玩家 | Retain separate IDs and the same name | Yes - locally passing |

## JUnit mapping

`domain.model.PlayerProfileTest` includes the case IDs in parameterized display
names. `rejectsInvalidProfileIds` covers PP-C01–06;
`rejectsInvalidDisplayNames` covers PP-C07–12;
`preservesValidProfileData` covers PP-C13–19; and
`separateIdentitiesMayShareADisplayName` covers PP-C20.

## Verification status

Local TDD on November 9, 2026, used the unchanged
`domain.model.PlayerProfileTest` suite against the compiling stub first:
**20 tests, 20 expected failures, zero errors/skips**. Every failure included
the stub's UnsupportedOperationException. After implementing the immutable
fields and shared nonblank validation, the same command passed **20/20**:

```sh
bash ./gradlew --no-daemon test --tests domain.model.PlayerProfileTest
```

Gradle runs on JDK 25; compilation/tests use the existing JDK 11 toolchain.
These are observed local RED/GREEN runs, not separate Git commits. No commits
or pushes were made. This was the first model-only checkpoint. The following service/storage increment
implements ID generation, registration, authentication and local persistence; see
[ProfileService](ProfileService.md). Remote CI and peer review remain pending.

## Historical model-only verification

The same local increment passed `clean build pitest`: 1,053 tests with zero
failures/errors/skips, clean Checkstyle/SpotBugs, all 9 profile lines and 4
branches covered, and 5/5 profile mutants killed. The shared 18-component
measurement retains seven pre-existing survivors outside PlayerProfile; see
[quality results](../quality.md#playerprofile-model--november-9-2026).
