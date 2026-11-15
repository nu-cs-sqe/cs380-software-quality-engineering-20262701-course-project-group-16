# OpponentBeliefs BVA

| ID | Methods | State/input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| OB-C01 | constructor/getters | No data, all tendencies | Priors with zero observations | Yes |
| OB-C02 | withDecision/getPrediction/getObservedDecisions | Accepted calls/raises | Correct normalized prediction; old snapshot unchanged | Yes |
| OB-C03 | withDecision/getPrediction | Different streets, position and legal choices | Separate contextual counts; unseen context uses prior | Yes |
| OB-C04 | withDecision/getTendency | Eligible three-bet call/fold/raise; other raise levels | One success in three opportunities; no inferred opportunities | Yes |
| OB-C05 | withDecision/getTendency | Eligible flop check/bet/fold, other flop/turn bets | One c-bet in three opportunities | Yes |
| OB-C06 | withDecision/getTendency | Actual c-bet call/raise/fold and ordinary fold | One fold in three eligible responses | Yes |
| OB-C07 | withDecision/getTendency/getPrediction | All-in CALL without a raise opportunity | Zero three-bet opportunities, learned CALL | Yes |

The original all-in-call test was corrected after review: an all-in call cannot invent a legal raise/three-bet opportunity.
Counts are eligible **decisions**, not PlayStats completed-hand denominators.
Context queries and old snapshots cannot mutate the model.

## E05 public aggression query

| ID | Methods | Input/state | Expected | Status |
| --- | --- | --- | --- | --- |
| AE01 | getAggressionEstimate | Unseen street | Prior .5, zero successes/opportunities/evidence | Implemented, GREEN |
| AE02 | getAggressionEstimate | BET/CHECK/RAISE with aggressive choices, CALL without one, other street, later updates | Count eligible decisions only; correct per-street posterior; old snapshots unchanged | Implemented, GREEN |

Cases map to AggressionEstimateTest. The query derives counts from immutable
contextual predictions; it does not introduce another mutable counter registry.
