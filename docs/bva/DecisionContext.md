# DecisionContext BVA

| ID | Methods | State/input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| DC-C01 | constructor/getters | TURN, two legal choices, in position; mutate source | Exact public values, immutable choices | Yes |
| DC-C02 | constructor | Missing street/choices; empty, null or forced-post choices | Reject without a usable context | Yes |
| DC-C03 | matches | Same/different street, legal set, position | Match only the same public situation | Yes |

Legal choices and position come from authoritative public engine/application context.
