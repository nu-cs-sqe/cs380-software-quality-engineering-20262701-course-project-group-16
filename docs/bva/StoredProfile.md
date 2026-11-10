# StoredProfile BVA

StoredProfile groups one immutable PlayerProfile with one immutable PasswordHash and CompletedStatistics
so the repository can publish all fields in the same catalog replacement. Credentials
are not included in ProfileService.list/find/authenticate identity results.

| ID | Public method(s) | Input/state | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| SP-C01 | Constructor | Null profile or null credential | IllegalArgumentException | Yes - locally passing |
| SP-C02 | Constructor; getProfile; getPasswordHash | Valid identity and credential | Retain the supplied immutable objects | Yes - locally passing |

StoredProfileTest covers both cases. It contains no match or pending-hand state.

The two-argument constructor creates empty completed data; the three-argument
constructor accepts an immutable completed snapshot. getStatistics exposes that
snapshot without pending match data. ST-C04 in LifetimeStorageTest rejects null
statistics and checks exact immutable identity/credential/statistics retention;
ST-C01/02/05 cover save/reload and legacy empty totals.
