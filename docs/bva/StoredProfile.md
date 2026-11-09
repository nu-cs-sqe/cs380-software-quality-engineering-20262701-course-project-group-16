# StoredProfile BVA

StoredProfile groups one immutable PlayerProfile with one immutable PasswordHash
so the repository can publish both in the same catalog replacement. Credentials
are not included in ProfileService.list/find/authenticate identity results.

| ID | Public method(s) | Input/state | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| SP-C01 | Constructor | Null profile or null credential | IllegalArgumentException | Yes - locally passing |
| SP-C02 | Constructor; getProfile; getPasswordHash | Valid identity and credential | Retain the supplied immutable objects | Yes - locally passing |

StoredProfileTest covers both cases. It contains no match or pending-hand state.
