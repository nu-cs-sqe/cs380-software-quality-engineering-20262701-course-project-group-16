# TendencyEstimate BVA

| ID | Methods | State/input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| TE-C01 | constructor/getters | 0/0, 0/1, 1/1, 2/4 successes/opportunities | Beta(1,1) posterior mean, uncertainty and evidence weight | Yes |
| TE-C02 | constructor | Negative counts or successes exceeding opportunities | Reject | Yes |
| TE-C03 | withOutcome/getters | One success then one decline | Both opportunities counted; detached old estimate | Yes |
| TE-C04 | constructor/withOutcome/getters | Long.MAX_VALUE counts | Finite estimates; checked increment overflow; unchanged original | Yes |

Beliefs have a prior even without observations. These are not exact PlayStats rates.
