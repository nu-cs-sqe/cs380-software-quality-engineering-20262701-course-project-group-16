# PublicDecision BVA

| ID | Methods | State/input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| PD-C01 | context constructor/getMatchId/getSequence/getAction/getContext/isStrategic | Exact match ID, Long.MAX_VALUE sequence, accepted CHECK | Preserve immutable public event | Yes |
| PD-C02 | constructor | Blank match ID, sequence −1/0, wrong street/choice, non-fold departure | Reject | Yes |
| PD-C03 | isStrategic | Forced post, automatic departure fold, voluntary fold | Only voluntary fold is strategic | Yes |
| PD-C04 | constructor/getAction/isStrategic | All-in CALL/BET/RAISE | Preserve resolved action meaning | Yes |
| PD-C05 | context constructor | Three-bet opportunity but no legal RAISE | IllegalArgumentException | Yes |
| PD-C06 | context constructor | C-bet opportunity but no legal BET | IllegalArgumentException | Yes |
| PD-C07 | automatic-event constructor/getContext/isStrategic | Forced blind without decision context | Empty Optional context; no strategic preference | Yes |
| PD-C08 | automatic-event constructor/getContext/isStrategic | Departure fold outside the acting turn, no legal choices | Empty Optional context; no strategic preference | Yes |
| PD-C09 | automatic-event constructor | Ordinary voluntary choice without context | IllegalArgumentException | Yes |

Only AcceptedAction can enter this boundary; rejected requests produce no learning event.


The context constructor accepts a public legal-choice snapshot. The automatic-event
constructor avoids invented choices for forced posts or non-current departures.
The context overload requires a supplied context; the automatic-event overload
represents absence with Optional.empty(). A strategic choice cannot omit context.

PD-C05/06 ran two expected RED cases before consistency checks; PD-C07–09 ran
three expected RED cases before the automatic-event constructor. PublicDecision
contains accepted public metadata only: no GameState, private cards or deck.
