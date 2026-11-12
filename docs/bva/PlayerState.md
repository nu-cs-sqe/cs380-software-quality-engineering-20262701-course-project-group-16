# PlayerState boundary value analysis

Each case maps to the JUnit method with the same ID (hyphens become underscores).
Grouped assertions exercise the public getters at the stated boundary; no mutable state is exposed.

| ID | Public methods | State/input boundary | Expected result | Implemented |
| --- | --- | --- | --- | --- |
| PS-01 | PlayerState(...); getPlayer(); getStatus(); getHoleCards(); getRoundContribution(); getHandContribution(); canAct(); isParticipating() | Active player with two cards; round 10 within hand 30 | Retain immutable player/cards; can act and participates | Yes |
| PS-02 | PlayerState; getHoleCards | Setup: zero cards and zero contributions | Accept empty card state | Yes |
| PS-03 | PlayerState | Null player | IllegalArgumentException | Yes |
| PS-04 | PlayerState | Null status | IllegalArgumentException | Yes |
| PS-05 | PlayerState | Null card list | IllegalArgumentException | Yes |
| PS-06 | PlayerState | Null card element | IllegalArgumentException | Yes |
| PS-07 | PlayerState | One hole card | IllegalArgumentException | Yes |
| PS-08 | PlayerState | Three hole cards | IllegalArgumentException | Yes |
| PS-09 | PlayerState | Duplicate hole cards | IllegalArgumentException | Yes |
| PS-10 | PlayerState | negative round | IllegalArgumentException | Yes |
| PS-11 | PlayerState | negative hand | IllegalArgumentException | Yes |
| PS-12 | PlayerState | round exceeds hand by one | IllegalArgumentException | Yes |
| PS-13 | PlayerState | active player has zero chips | IllegalArgumentException | Yes |
| PS-14 | PlayerState | folded player has zero chips | IllegalArgumentException | Yes |
| PS-15 | PlayerState | all-in retains one chip | IllegalArgumentException | Yes |
| PS-16 | PlayerState | all-in without investment | IllegalArgumentException | Yes |
| PS-17 | PlayerState | eliminated retains one chip | IllegalArgumentException | Yes |
| PS-18 | PlayerState | eliminated retains commitment | IllegalArgumentException | Yes |
| PS-19 | PlayerState; canAct; isParticipating | All-in with zero stack and committed chips | Participates but cannot act | Yes |
| PS-20 | PlayerState; canAct; isParticipating | Folded retains previous investment | Cannot act/participate; investment stays | Yes |
| PS-21 | PlayerState; canAct; isParticipating | Eliminated zero-stack empty seat | Cannot act/participate; seat number retained | Yes |
| PS-22 | PlayerState | Eliminated retains private cards | IllegalArgumentException | Yes |
| PS-23 | PlayerState; getHandContribution | round equals hand at positive boundary | Accept without double counting | Yes |
| PS-24 | PlayerState; getHandContribution | New round with earlier hand investment | round=0, hand=20 | Yes |
| PS-25 | PlayerState | Stack plus hand contribution exceeds long | IllegalArgumentException | Yes |
| PS-26 | PlayerState; getPlayer | Largest non-overflowing per-player total | Accept | Yes |
| PS-27 | PlayerState; getHoleCards | Caller mutates card list after construction | Stored cards unaffected | Yes |
| PS-28 | getHoleCards | Caller mutates returned card list | UnsupportedOperationException | Yes |
| PS-29 | PlayerState; isDealtSeat; canAct; isParticipating | Departed seat with retained stack; zero or nonzero commitments; cards absent or present | Retain stack only with no cards/commitments; never deal or act | Yes; BettingStateBoundaryTest.departedSeatsCannotRetainCardsOrCommitChipsInLaterHands |
