# PlayerView boundary value analysis

Each case maps to the JUnit method with the same ID (hyphens become underscores).
Grouped assertions exercise the public getters at the stated boundary; no mutable state is exposed.

| ID | Public methods | State/input boundary | Expected result | Implemented |
| --- | --- | --- | --- | --- |
| PV-01 | getId(); getSeat(); getType(); getChipStack(); getStatus(); getRoundContribution(); getHandContribution(); getHoleCards() | Own player after blind posting and dealing | Correct identity, stack, status, contributions and two private cards | Yes |
| PV-02 | getHoleCards | Attempt to clear own visible cards | UnsupportedOperationException | Yes |
| PV-03 | getHoleCards; getChipStack; getSeat | Opponent view | Public stack/seat remain visible; private cards hidden | Yes |
