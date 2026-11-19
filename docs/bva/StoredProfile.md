# StoredProfile BVA

StoredProfile groups one immutable PlayerProfile with one immutable PasswordHash and CompletedStatistics
so the repository can publish all fields in the same catalog replacement. Credentials
are not included in ProfileService.list/find/authenticate identity results.

| ID | Public method(s) | Input/state | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| SP-C01 | Constructor | Null profile or null credential | Non-null caller contract; no runtime null guarantee | Retired November 16, 2026 |
| SP-C02 | Constructor; getProfile; getPasswordHash | Valid identity and credential | Retain the supplied immutable objects | Yes - locally passing |

StoredProfileTest retains SP-C02; SP-C01 is a historical retired caller-null case. It contains no match or pending-hand state.

The two-argument constructor creates empty completed data; the three-argument
constructor accepts an immutable completed snapshot. getStatistics exposes that
snapshot without pending match data. ST-C04 in LifetimeStorageTest checks exact immutable
identity/credential/statistics retention under the non-null caller contract;
ST-C01/02/05 cover save/reload and legacy empty totals.

| ID | Method | Input / state | Expected outcome |
| --- | --- | --- | --- |
| SP-W8-01 | Constructor; getChipBalance | New or legacy record without an explicit balance | Start with 2,000 virtual chips. |
| SP-W8-02 | Constructor; getChipBalance | Explicit balance of −1 or 0 | Reject negative balance; preserve zero. |
