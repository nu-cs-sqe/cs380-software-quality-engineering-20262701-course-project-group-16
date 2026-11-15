# CallValue BVA

E05 public call scenario: equity is unrounded, eligible pots exclude refunds and
pots the hero cannot win. Refund is the return of newly called chips in the supplied scenario
only, conditional on its no-further-betting assumption. The application owns scenario construction; this class does not settle pots.

| ID | Methods | Input/state | Expected | Status |
| --- | --- | --- | --- | --- |
| CV01 | constructor | Equity below 0, above 1, NaN, infinities | Reject | Implemented, GREEN |
| CV02 | constructor/getters/riskedChips | Equity 0, epsilon, .5, 1-epsilon, 1; maximum long chips | Preserve exact values; fully refunded cost 0 | Implemented, GREEN |
| CV03 | constructor/riskedChips/breakEvenEquity | Negative chips; refund exceeds call; cost exceeds eligible pot | Reject | Implemented, GREEN |
| CV04 | riskedChips/breakEvenEquity | Call 30, refund 10, eligible pot 100; zero/entire-pot costs; empty pot | Cost 20, odds .2; odds 0/1; empty optional for 0/0 | Implemented, GREEN |

Cases map to CallValueTest. No programmer-null cases.
