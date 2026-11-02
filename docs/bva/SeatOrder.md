# SeatOrder boundary value analysis

Each case maps to the JUnit method with the same ID (hyphens become underscores).
Grouped assertions exercise the public getters at the stated boundary; no mutable state is exposed.

| ID | Public methods | State/input boundary | Expected result | Implemented |
| --- | --- | --- | --- | --- |
| SO-01 | SeatOrder; positionsFor; firstPreflopActor; firstPostflopActor | Six occupied seats; dealer 0 | Correct clockwise roles and first actors | Yes |
| SO-02 | SeatOrder; positionsFor; firstPreflopActor; firstPostflopActor | Six occupied seats; dealer 1 | Correct clockwise roles and first actors | Yes |
| SO-03 | SeatOrder; positionsFor; firstPreflopActor; firstPostflopActor | Six occupied seats; dealer 2 | Correct clockwise roles and first actors | Yes |
| SO-04 | SeatOrder; positionsFor; firstPreflopActor; firstPostflopActor | Six occupied seats; dealer 3 | Correct clockwise roles and first actors | Yes |
| SO-05 | SeatOrder; positionsFor; firstPreflopActor; firstPostflopActor | Six occupied seats; dealer 4 | Correct clockwise roles and first actors | Yes |
| SO-06 | SeatOrder; positionsFor; firstPreflopActor; firstPostflopActor | Six occupied seats; dealer 5 | Correct clockwise roles and first actors | Yes |
| SO-07 | positionsFor; firstPreflopActor; firstPostflopActor | Heads-up seats 0 and 4; dealer 0 | Dealer posts SB/acts first preflop; other acts first postflop | Yes |
| SO-08 | positionsFor; firstPreflopActor; firstPostflopActor | Heads-up seats 0 and 4; dealer 4 | Dealer posts SB/acts first preflop; other acts first postflop | Yes |
| SO-09 | positionsFor; firstPreflopActor; firstPostflopActor | Three players in stable seats 0,2,5; dealer 5 | Skip eliminated seats without renumbering | Yes |
| SO-10 | nextDealerAfter | Old dealer eliminated; players 2,5 remain | Move to next remaining seat 2 | Yes |
| SO-11 | nextDealerAfter | Dealer 5 with players 0,2,5 remaining | Wrap to seat 0 | Yes |
| SO-12 | nextActorAfter; firstPostflopActor | Seat 1 folded; seat 2 all-in | Skip both; first actor seat 3 | Yes |
| SO-13 | firstPreflopActor | Expected actor 3 all-in; 4 folded | First actor seat 5 | Yes |
| SO-14 | nextActorAfter; firstPreflopActor; firstPostflopActor | All remaining players all-in | No actor: OptionalInt.empty | Yes |
| SO-15 | nextDealerAfter; nextActorAfter | No remaining players | Unavailable dealer and actor | Yes |
| SO-16 | positionsFor; firstPreflopActor; firstPostflopActor | Only one player remains | IllegalStateException: no new hand | Yes |
| SO-17 | positionsFor | Dealer seat eliminated while 2 remain | IllegalArgumentException | Yes |
| SO-18 | positionsFor | Seat -1 | IllegalArgumentException | Yes |
| SO-19 | positionsFor | Seat 6 | IllegalArgumentException | Yes |
| SO-20 | nextDealerAfter | Seat -1 | IllegalArgumentException | Yes |
| SO-21 | nextDealerAfter | Seat 6 | IllegalArgumentException | Yes |
| SO-22 | nextActorAfter | Seat -1 | IllegalArgumentException | Yes |
| SO-23 | nextActorAfter | Seat 6 | IllegalArgumentException | Yes |
| SO-24 | firstPreflopActor | Seat -1 | IllegalArgumentException | Yes |
| SO-25 | firstPreflopActor | Seat 6 | IllegalArgumentException | Yes |
| SO-26 | firstPostflopActor | Seat -1 | IllegalArgumentException | Yes |
| SO-27 | firstPostflopActor | Seat 6 | IllegalArgumentException | Yes |
| SO-28 | SeatOrder | null table | IllegalArgumentException | Yes |
| SO-29 | SeatOrder | five seat records | IllegalArgumentException | Yes |
| SO-30 | SeatOrder | seven seat records | IllegalArgumentException | Yes |
| SO-31 | SeatOrder | null seat record | IllegalArgumentException | Yes |
| SO-32 | SeatOrder | duplicate seat | IllegalArgumentException | Yes |
| SO-33 | SeatOrder | duplicate ID | IllegalArgumentException | Yes |
| SO-34 | SeatOrder; positionsFor; nextDealerAfter | Unsorted input subsequently cleared | Snapshot uses seats; caller changes cannot affect it | Yes |
