# BettingResult BVA — shared engine cases

`BettingResult` is the immutable output of an accepted action. Its
constructor is package-private; callers receive a result from `BettingEngine`.
Rejected requests produce no result. No independent statistics mutation belongs
in this data object.

The following are references to the unique cases defined in
[BettingEngine.md](BettingEngine.md), not additional duplicate test cases.

| Public method | Referenced cases | State/input | Expected result | Status |
| --- | --- | --- | --- | --- |
| getState() | BE-CALL01, BE-CALL02 | Accepted seat-3 CALL in the shared dealt setup | New immutable state with stack/contribution/actor/pot changes and conserved chips; original state retained | Implemented, GREEN |
| getAcceptedAction() | BE-CALL03 | Same accepted CALL | Immutable accepted-action record containing actual chips added and matching hand/player context | Implemented, GREEN |
| getState() | BE-FOLD01, BE-FOLD02-SB, BE-FOLD02-BB | Accepted FOLD with zero or existing blind commitment | Folded status, retained chips/cards/contributions and updated decision rights; earlier snapshot retained | Implemented, GREEN |
| getAcceptedAction() | BE-FOLD01, BE-FOLD02-SB, BE-FOLD02-BB | Same accepted FOLD | PREFLOP/FOLD, chips added 0, retained round contribution and no all-in or raise/c-bet context | Implemented, GREEN |
| getState() | BE-CHECK01, BE-TURN03, BE-TURN04, BE-SEQUENCE01 | Final check/call, or fold leaving one participant | No current actor once decisions finish; pot and cards retained awaiting a separate advance or settlement operation | Implemented, GREEN |
| getAcceptedAction() | BE-CHECK01 | Accepted big-blind CHECK with contribution 20 | PREFLOP/CHECK, chips added 0, contribution after 20, not all-in, matching hand/player context | Implemented, GREEN |
| getState() | BE-AIC01–BE-AIC08 | CALL with stack below, at or above the amount owed, including an existing SB contribution | Capped chip transfer; ALL_IN only on stack exhaustion; contributions and pot updated; prior snapshot retained | Implemented, GREEN |
| getAcceptedAction() | BE-AIC01–BE-AIC08 | Same accepted CALL | Actual chips added, matching resulting contribution, CALL classification and accurate becameAllIn flag; no raise/c-bet context | Implemented, GREEN |
| getState() | BE-AIC11, BE-AIC12 | Complete preflop sequences including exact/short all-in calls | All-in participants retained without further decisions; no actor once pending is empty; conserved chips, retained cards and no premature payout | Implemented, GREEN |
| getState() | BE-PF01/02/04/06/07/08 | Accepted postflop CHECK/FOLD, including final actions and existing commitments | Actual phase and betting history retained; chips/cards/hand contributions conserved; pending active actor advances or decisions close | Implemented, GREEN |
| getAcceptedAction() | BE-PF01/02, BE-PC01–08 | Accepted FLOP/TURN/RIVER CHECK/FOLD | Correct street/type, zero added chips, retained round contribution and explicit opening/faced-c-bet opportunity context | Implemented, GREEN |

Both getters return the immutable values produced by engine validation. There is
no settlement result or automatic street transition in this increment. An empty
actor/pending set means no more decisions remain in the current snapshot;
COMPLETION is reserved for a hand whose committed chips have been awarded.
