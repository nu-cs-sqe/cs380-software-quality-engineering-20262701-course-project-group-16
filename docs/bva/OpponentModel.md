# OpponentBeliefs BVA

| ID | Methods | State/input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| OB-C01 | constructor/getters | No data, all tendencies, missing query context/tendency | Priors with zero observations; invalid query rejected | Yes |
| OB-C02 | withDecision/getPrediction/getObservedDecisions | Accepted calls/raises | Correct normalized prediction; old snapshot unchanged | Yes |
| OB-C03 | withDecision/getPrediction | Different streets, position and legal choices | Separate contextual counts; unseen context uses prior | Yes |
| OB-C04 | withDecision/getTendency | Eligible three-bet call/fold/raise; other raise levels | One success in three opportunities; no inferred opportunities | Yes |
| OB-C05 | withDecision/getTendency | Eligible flop check/bet/fold, other flop/turn bets | One c-bet in three opportunities | Yes |
| OB-C06 | withDecision/getTendency | Actual c-bet call/raise/fold and ordinary fold | One fold in three eligible responses | Yes |
| OB-C07 | withDecision/getTendency/getPrediction | All-in CALL without a raise opportunity | Zero three-bet opportunities, learned CALL | Yes |

| OB-C08 | getPrediction/getObservedDecisions | Missing context after one learned choice | Reject with IllegalArgumentException and preserve count | Yes |

OB-C08 is a mutation-driven regression. The original all-in-call test was corrected
after review: an all-in call cannot invent a legal raise/three-bet opportunity.
Counts are eligible **decisions**, not PlayStats completed-hand denominators.
Context queries and old snapshots cannot mutate the model.

## Integration and learning evaluation

| ID | Methods | State/input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| OL-C01 | public constructor/observe/snapshot | Real initialization and engine reject CHECK, accept CALL | No rejected observation; learn accepted call once; exact completed-hand statistics remain separate; chips conserved | Yes |
| OL-C02 | public constructor/observe/snapshot | Two observers, different opponents, new match | Opponent/observer isolation; immutable prior snapshot; new match starts fresh | Yes |
| OL-C03 | observe/snapshot | Automatic departure facing a c-bet, replay, then voluntary fold | Departure/replay teach nothing; genuine fold counts once | Yes |
| LE-C01 | public constructor/observe/snapshot and belief queries | Four permutations of fixed 100-decision training/held-out compositions | Add-one posterior, reduced held-out multiclass Brier error versus disabled learning, smaller uncertainty | Yes |
| LE-C02 | observe/snapshot | Later aggressive evidence; unseen river context | Raise probability updates gradually; unseen context retains zero-data prior | Yes |

OM-C01–04/06 isolate coordination using an injected updater. OB tests exercise
real aggregation separately. OL tests use real model/value components; OL-C01
also uses real initialization, betting and PlayStats. LE tests are controlled
prediction experiments, not poker match or win-rate evidence.

Local TDD observed 13 context/uncertainty failures, 9 event/prediction failures,
13 belief/coordinator failures, 2 opportunity-consistency failures and 3 automatic-
event failures, each followed by implementation and GREEN. One all-in fixture was
later corrected as documented in OpponentBeliefs.md. Integration/evaluation cases
and mutation-driven assertions were added later; they are not claimed feature RED
cycles. No synthetic Git checkpoints or extra scratch reports were created.
