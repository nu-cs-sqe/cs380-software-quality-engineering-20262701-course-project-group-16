# BettingProgress boundary value analysis

Each case maps to the JUnit method with the same ID (hyphens become underscores).
Grouped assertions exercise the public getters at the stated boundary; no mutable state is exposed.

| ID | Public methods | State/input boundary | Expected result | Implemented |
| --- | --- | --- | --- | --- |
| BP-01 | BettingProgress(...); getCurrentBet(); getLastFullRaiseSize(); getPlayersToAct(); getPlayersAllowedToRaise() | Current bet 20; full raise 20; action and raise sets differ | Retain both sets independently | Yes |
| BP-02 | BettingProgress; getters | Unopened round: currentBet 0 and min raise 1; empty sets | Accept | Yes |
| BP-03 | BettingProgress | Current bet -1 | IllegalArgumentException | Yes |
| BP-04 | BettingProgress | Last full raise -1 | IllegalArgumentException | Yes |
| BP-05 | BettingProgress | Last full raise 0 | IllegalArgumentException | Yes |
| BP-06 | BettingProgress; getters | Largest long current bet and last full raise | Retain without summation overflow | Yes |
| BP-07 | BettingProgress | pending: null set | IllegalArgumentException | Yes |
| BP-08 | BettingProgress | pending: null member | IllegalArgumentException | Yes |
| BP-09 | BettingProgress | pending: empty member | IllegalArgumentException | Yes |
| BP-10 | BettingProgress | pending: blank member | IllegalArgumentException | Yes |
| BP-11 | BettingProgress | raise: null set | IllegalArgumentException | Yes |
| BP-12 | BettingProgress | raise: null member | IllegalArgumentException | Yes |
| BP-13 | BettingProgress | raise: empty member | IllegalArgumentException | Yes |
| BP-14 | BettingProgress | raise: blank member | IllegalArgumentException | Yes |
| BP-15 | BettingProgress; getPlayersToAct; getPlayersAllowedToRaise | Caller mutates source sets | Stored sets unaffected | Yes |
| BP-16 | getPlayersToAct; getPlayersAllowedToRaise | Caller mutates returned sets | UnsupportedOperationException | Yes |
