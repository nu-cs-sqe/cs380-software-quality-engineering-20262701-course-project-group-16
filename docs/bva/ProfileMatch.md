# ProfileMatch BVA

Application binding from stable match-player IDs to password-verified profiles.
requireAccounts accepts 1–6 distinct handles from the supplied service. Constructor
checks the initializer's human count, then maps humans in stable seat order.
Bots have no profile binding. Immutable views expose profile IDs/lifetime rates.
save persists all bound human deltas in one repository save; IOException becomes
UncheckedIOException before SetupSession publishes paid state or match totals.

| ID | Methods | System/input state | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| PM-C01 | constructor; profileIds; snapshot; save | Real human/bot setup, showdown, next hand, restart | Correct immutable human-only binding; lifetime persistence; fresh-match stacks/totals | Yes - locally passing |
| PM-C02 | save | Repository write failure then retry | Original IOException retained as cause; no paid state/statistics published; exactly one durable increment | Yes - locally passing |
| PM-C03 | requireAccounts; views | Before start, valid one, zero/seven, duplicates, null service/list/entry, foreign issuer, unprofiled restart | Reject invalid starts without replacing binding; unprofiled start clears it | Yes - locally passing |
| LS-C06 | requireAccounts; constructor | Six/seven distinct accounts | Six accepted/mapped; seven rejected independently of duplicate checks | Yes - locally passing |
| PM-C05 | save | Durable real-file save succeeds, then adapter throws | Fresh service sees one completion; retry publishes paid state without another write | Yes - locally passing |
| PM-C04 | constructor | Initializer produces two humans for one account | Reject before publishing match | Yes - locally passing |

ProfiledMatchIntegrationTest exercises real composition; it is an integration
test, not an isolated ProfileMatch unit test. Public entry points are SetupSession
and ProfileService. There is no persistence of cards, stacks or active hands.

Latest verification: full build 1,196 passing tests; Checkstyle/SpotBugs clean;
new/modified E02 component mutants all killed. See the existing
[quality record](../quality.md#completed-lifetime-statistics--november-9-2026).
These are local results; commit/CI/review/merge gates are not marked complete.
