# BettingResult BVA — shared engine cases

`BettingResult` is the proposed immutable output of an accepted action. Its
constructor is package-private; callers receive a result from `BettingEngine`.
Rejected requests produce no result. No independent statistics mutation belongs
in this data object.

The following are references to the unique cases defined in
[BettingEngine.md](BettingEngine.md), not additional duplicate test cases.

| Public method | Referenced cases | State/input | Expected result | Status |
| --- | --- | --- | --- | --- |
| getState() | BE-CALL01, BE-CALL02 | Accepted seat-3 CALL in the shared dealt setup | New immutable state with stack/contribution/actor/pot changes and conserved chips; original state retained | Test written, RED; implementation pending |
| getAcceptedAction() | BE-CALL03 | Same accepted CALL | Immutable accepted-action record containing actual chips added and matching hand/player context | Test written, RED; implementation pending |

The first increment deliberately stops at an API scaffold. Round outcomes and
completion metadata will be specified when their TDD increments begin.
