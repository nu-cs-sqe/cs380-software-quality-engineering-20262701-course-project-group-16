# GameView boundary value analysis

Each case maps to the JUnit method with the same ID (hyphens become underscores).
Grouped assertions exercise the public getters at the stated boundary; no mutable state is exposed.

| ID | Public methods | State/input boundary | Expected result | Implemented |
| --- | --- | --- | --- | --- |
| GV-01 | GameState.viewFor(); getViewerId(); getConfig(); getHandId(); getPhase(); getPositions(); getCurrentActorId(); getPlayers(); getCommunityCards(); getPot(); getCurrentBet(); getLastFullRaiseSize(); getPlayersToAct(); getPlayersAllowedToRaise() | Dealt flop; viewer p0 | Public state and own cards only; engine retains private deck | Yes |
| GV-02 | GameState.viewFor; getPlayers | Different human/bot viewer | Only selected viewer cards revealed | Yes |
| GV-03 | GameState.viewFor; getPlayers | Before dealing | Own cards also empty | Yes |
| GV-04 | GameState.viewFor | null viewer ID | IllegalArgumentException | Yes |
| GV-05 | GameState.viewFor | empty viewer ID | IllegalArgumentException | Yes |
| GV-06 | GameState.viewFor | blank viewer ID | IllegalArgumentException | Yes |
| GV-07 | GameState.viewFor | unknown viewer ID | IllegalArgumentException | Yes |
| GV-08 | getPlayers; getCommunityCards; getPlayersToAct; getPlayersAllowedToRaise | Attempt to modify view collections | UnsupportedOperationException | Yes |
