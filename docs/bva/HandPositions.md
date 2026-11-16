# HandPositions boundary value analysis

Each case maps to the JUnit method with the same ID (hyphens become underscores).
Grouped assertions exercise the public getters at the stated boundary; no mutable state is exposed.

| ID | Public methods | State/input boundary | Expected result | Implemented |
| --- | --- | --- | --- | --- |
| HP-01 | HandPositions | dealer/SB/BB = -1/1/2 | IllegalArgumentException | Yes |
| HP-02 | HandPositions | dealer/SB/BB = 6/1/2 | IllegalArgumentException | Yes |
| HP-03 | HandPositions | dealer/SB/BB = 0/-1/2 | IllegalArgumentException | Yes |
| HP-04 | HandPositions | dealer/SB/BB = 0/6/2 | IllegalArgumentException | Yes |
| HP-05 | HandPositions | dealer/SB/BB = 0/1/-1 | IllegalArgumentException | Yes |
| HP-06 | HandPositions | dealer/SB/BB = 0/1/6 | IllegalArgumentException | Yes |
| HP-07 | HandPositions | dealer/SB/BB = 0/1/1 | IllegalArgumentException | Yes |
| HP-08 | HandPositions | dealer/SB/BB = 0/1/0 | IllegalArgumentException | Yes |
| HP-09 | HandPositions(...); getDealerSeat(); getSmallBlindSeat(); getBigBlindSeat() | dealer/SB/BB = 0/1/2 | Retain normal/wrapped/heads-up roles | Yes |
| HP-10 | HandPositions; all getters | dealer/SB/BB = 5/0/1 | Retain normal/wrapped/heads-up roles | Yes |
| HP-11 | HandPositions; all getters | dealer/SB/BB = 0/0/5 | Retain normal/wrapped/heads-up roles | Yes |

## Instructor follow-up — November 15

Bounds use the shared GameConfig.SEAT_COUNT constant, and named validation is
separate from constructor assignment. Existing first/last/out-of-range seat
cases retain their expected results. checkstyleSeatRules enforces MagicNumber
for this class and the shared table-rule classes.

## Shared public decision position (E06)

| ID | Method | Input/state | Expected | Implemented |
| --- | --- | --- | --- | --- |
| DP01 | isInPosition | Preflop/postflop, missing/folded seats, last active responder | Uses BB/dealer street anchor; only last active responder is in position | Yes |
| DP02 | isInPosition | Heads-up, zero/one active responders | Correct HU order; no response position with fewer than two actors | Yes |
| DP03 | isInPosition | Seat or set entry -1/6 | Reject out-of-range public seat | Yes |

Cases map to DecisionPositionTest. Policy and event contexts use this same method.
