# AcceptedAction boundary value analysis

Each case maps to the JUnit method with the same ID (hyphens become underscores).
Grouped assertions exercise the public getters at the stated boundary; no mutable state is exposed.

| ID | Public methods | State/input boundary | Expected result | Implemented |
| --- | --- | --- | --- | --- |
| AA-01 | AcceptedAction(...); getHandId(); getPlayerId(); getStreet(); getType(); getChipsAdded(); getRoundContributionAfter(); becameAllIn(); getStatsContext(); toStatsAction() | Call adds 10 after prior contribution 10 | Accepted call observation; after=20; all fields retained | Yes |
| AA-02 | AcceptedAction; toStatsAction | Raise-to 60 from contribution 10 adds 50 | Resolved raise; target total differs from chips added | Yes |
| AA-03 | AcceptedAction | null hand ID | IllegalArgumentException | Yes |
| AA-04 | AcceptedAction | empty hand ID | IllegalArgumentException | Yes |
| AA-05 | AcceptedAction | blank hand ID | IllegalArgumentException | Yes |
| AA-06 | AcceptedAction | null player ID | IllegalArgumentException | Yes |
| AA-07 | AcceptedAction | empty player ID | IllegalArgumentException | Yes |
| AA-08 | AcceptedAction | blank player ID | IllegalArgumentException | Yes |
| AA-09 | AcceptedAction | null street | IllegalArgumentException | Yes |
| AA-10 | AcceptedAction | null resolved type | IllegalArgumentException | Yes |
| AA-11 | AcceptedAction | null context | IllegalArgumentException | Yes |
| AA-12 | AcceptedAction | negative added chips | IllegalArgumentException | Yes |
| AA-13 | AcceptedAction | negative resulting contribution | IllegalArgumentException | Yes |
| AA-14 | AcceptedAction | added chips exceed resulting contribution | IllegalArgumentException | Yes |
| AA-15 | AcceptedAction | zero-chip call | IllegalArgumentException | Yes |
| AA-16 | AcceptedAction | check claims to become all-in | IllegalArgumentException | Yes |
| AA-17 | AcceptedAction | fold claims to become all-in | IllegalArgumentException | Yes |
| AA-18 | AcceptedAction | unresolved preflop raise context | IllegalArgumentException | Yes |
| AA-19 | AcceptedAction | preflop BET rather than RAISE | IllegalArgumentException | Yes |
| AA-20 | AcceptedAction | forced post on flop | IllegalArgumentException | Yes |
| AA-21 | AcceptedAction; becameAllIn; toStatsAction | All-in resolved as CALL | Preserve underlying meaning and all-in flag | Yes |
| AA-22 | AcceptedAction; becameAllIn; toStatsAction | All-in resolved as RAISE | Preserve underlying meaning and all-in flag | Yes |
| AA-23 | AcceptedAction; becameAllIn; toStatsAction | All-in resolved as BET | Preserve underlying meaning and all-in flag | Yes |
| AA-24 | AcceptedAction; becameAllIn; toStatsAction | All-in resolved as FORCED_POST | Preserve underlying meaning and all-in flag | Yes |
| AA-25 | AcceptedAction; getRoundContributionAfter | Maximum long chip value | Accept without arithmetic overflow | Yes |
| AA-26 | AcceptedAction; toStatsAction | Check with existing contribution | Zero added chips; retain existing contribution | Yes |
| AA-27 | AcceptedAction; toStatsAction; PlayStats integration | Forced blind exhausts a stack; completed hand | Hands=1; VPIP=0%; PFR=0% | Yes |
| AA-28 | AcceptedAction; toStatsAction; PlayStats integration | Accepted three-bet followed by all-in flop continuation bet | Correct VPIP/PFR/3-bet/c-bet/AFq; AF unavailable without calls | Yes |
