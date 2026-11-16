# ActionDistribution BVA

One positive weight per available action, at most one wager target. Probabilities
are numeric and unrounded; enum order defines half-open sampling intervals.

| ID | Methods | Input/state | Expected | Status |
| --- | --- | --- | --- | --- |
| AD01 | constructor/getProbabilities/sample | Weights 2/3/5; q=0, below .2, .2, .5, nextDown(1) | Normalize .2/.3/.5; exact interval boundaries; immutable output | Implemented, GREEN |
| AD02 | sample | Negative, 1, NaN and infinities | Reject | Implemented, GREEN |
| AD03 | constructor | Nonpositive or nonfinite weight | Reject | Implemented, GREEN |
| AD04 | constructor | Blank identity, empty options, weight overflow or normalized underflow | Reject | Implemented, GREEN |
| AD05 | constructor/sample | Missing, extra, zero/negative or double wager; CHECK and BET singleton | Reject mismatches; singleton probability 1 and correct target semantics | Implemented, GREEN |

Cases map to ActionDistributionTest. Production construction is package-private.

| AD06 | constructor/getProbabilities/sample | Caller mutates source weights after construction | Detached immutable probabilities and unchanged sampling | Implemented |

AD06 is a later ownership regression. Overflowing input totals normalize to zero
probabilities and are rejected by the same positive-probability validation as
underflow; there is no redundant total-finiteness guard.
