# GameState BVA

## Contract

`GameState(List<Player> players, int dealerSeat, int smallBlindSeat,
int bigBlindSeat, int currentActorSeat, long[] committedChips)` is an immutable
snapshot of the six-seat table.

- Exactly six non-null players must occupy the six unique seats 0..5 and have
  distinct IDs. The constructor accepts any input order; `getPlayers()` returns
  an unmodifiable list in seat order. Existing Player objects are immutable.
- Dealer, small-blind, big-blind, and current-actor seat numbers must be 0..5.
  The small and big blinds must use different seats.
- The six per-seat commitments must be nonnegative and are copied defensively.
  `getCommittedChips(int seat)` accepts only seats 0..5.
- `getPot()` is the sum of commitments for the current hand. `getTotalChips()`
  is that pot plus the remaining player stacks, counting each chip once.
  A combined value exceeding Long.MAX_VALUE is rejected, never wrapped.
- Invalid constructor arguments or lookup seats throw IllegalArgumentException.
- Game setup/turn logic determines role placement, human/bot counts, legal
  actors, blind amounts, and conservation against a match's initial chip total.
  This class does not choose seats, post blinds, deal cards, or execute actions.

## Cases

Each case creates a fresh state unless it describes a getter/mutation check.
Defaults: six immutable players with IDs player-0 through player-5 in matching
seats, HUMAN type, stack=1000 each; dealer=0, small blind=1, big blind=2,
current actor=3; six zero commitments. Role-case rows specify all four positions
in that order. All IDs also appear in GameStateTest display names.

| ID | Methods under test | State / input | Expected result | Implemented? |
| --- | --- | --- | --- | --- |
| G-P01 | Constructor | Players list is null | IllegalArgumentException | Yes |
| G-P02 | Constructor | 0 players | IllegalArgumentException | Yes |
| G-P03 | Constructor | 1 player | IllegalArgumentException | Yes |
| G-P04 | Constructor | 5 players | IllegalArgumentException | Yes |
| G-P05 | Constructor | 7 players | IllegalArgumentException | Yes |
| G-P06 | Constructor | Null player in first position | IllegalArgumentException | Yes |
| G-P07 | Constructor | Null player in last position | IllegalArgumentException | Yes |
| G-P08 | Constructor | Two players use seat 0; seat 5 is missing | IllegalArgumentException | Yes |
| G-P09 | Constructor | Seats 0 and 5 have the same player ID | IllegalArgumentException | Yes |
| G-P10 | Constructor, getPlayers() | Six players supplied in seat order | Six unchanged players returned in seat order 0..5 | Yes |
| G-P11 | Constructor, getPlayers() | Six players supplied in reverse order | Six unchanged players returned in seat order 0..5 | Yes |
| G-I01 | Constructor, getPlayers() | Replace an entry in the source list after construction | Snapshot still contains the original player | Yes |
| G-I02 | getPlayers() | Attempt to replace an entry in the returned list | UnsupportedOperationException; snapshot cannot be changed | Yes |
| G-R01 | Constructor | dealer seat=-1; positions=[-1, 1, 2, 3] | IllegalArgumentException | Yes |
| G-R02 | Constructor, four position getters | dealer seat=0; positions=[0, 1, 2, 3] | All four positions retained exactly | Yes |
| G-R03 | Constructor, four position getters | dealer seat=1; positions=[1, 1, 2, 3] | All four positions retained exactly | Yes |
| G-R04 | Constructor, four position getters | dealer seat=4; positions=[4, 1, 2, 3] | All four positions retained exactly | Yes |
| G-R05 | Constructor, four position getters | dealer seat=5; positions=[5, 1, 2, 3] | All four positions retained exactly | Yes |
| G-R06 | Constructor | dealer seat=6; positions=[6, 1, 2, 3] | IllegalArgumentException | Yes |
| G-R07 | Constructor | small blind seat=-1; positions=[0, -1, 2, 3] | IllegalArgumentException | Yes |
| G-R08 | Constructor, four position getters | small blind seat=0; positions=[0, 0, 2, 3] | All four positions retained exactly | Yes |
| G-R09 | Constructor, four position getters | small blind seat=1; positions=[0, 1, 2, 3] | All four positions retained exactly | Yes |
| G-R10 | Constructor, four position getters | small blind seat=4; positions=[0, 4, 2, 3] | All four positions retained exactly | Yes |
| G-R11 | Constructor, four position getters | small blind seat=5; positions=[0, 5, 2, 3] | All four positions retained exactly | Yes |
| G-R12 | Constructor | small blind seat=6; positions=[0, 6, 2, 3] | IllegalArgumentException | Yes |
| G-R13 | Constructor | big blind seat=-1; positions=[0, 2, -1, 3] | IllegalArgumentException | Yes |
| G-R14 | Constructor, four position getters | big blind seat=0; positions=[0, 2, 0, 3] | All four positions retained exactly | Yes |
| G-R15 | Constructor, four position getters | big blind seat=1; positions=[0, 2, 1, 3] | All four positions retained exactly | Yes |
| G-R16 | Constructor, four position getters | big blind seat=4; positions=[0, 2, 4, 3] | All four positions retained exactly | Yes |
| G-R17 | Constructor, four position getters | big blind seat=5; positions=[0, 2, 5, 3] | All four positions retained exactly | Yes |
| G-R18 | Constructor | big blind seat=6; positions=[0, 2, 6, 3] | IllegalArgumentException | Yes |
| G-R19 | Constructor | current actor seat=-1; positions=[0, 1, 2, -1] | IllegalArgumentException | Yes |
| G-R20 | Constructor, four position getters | current actor seat=0; positions=[0, 1, 2, 0] | All four positions retained exactly | Yes |
| G-R21 | Constructor, four position getters | current actor seat=1; positions=[0, 1, 2, 1] | All four positions retained exactly | Yes |
| G-R22 | Constructor, four position getters | current actor seat=4; positions=[0, 1, 2, 4] | All four positions retained exactly | Yes |
| G-R23 | Constructor, four position getters | current actor seat=5; positions=[0, 1, 2, 5] | All four positions retained exactly | Yes |
| G-R24 | Constructor | current actor seat=6; positions=[0, 1, 2, 6] | IllegalArgumentException | Yes |
| G-R25 | Constructor | Small blind and big blind both use seat 2 | IllegalArgumentException | Yes |
| G-C01 | Constructor | Committed-chip array is null | IllegalArgumentException | Yes |
| G-C02 | Constructor | Committed-chip array length=0 | IllegalArgumentException | Yes |
| G-C03 | Constructor | Committed-chip array length=5 | IllegalArgumentException | Yes |
| G-C04 | Constructor | Committed-chip array length=7 | IllegalArgumentException | Yes |
| G-C05 | Constructor | Seat 0 commitment=-1 | IllegalArgumentException | Yes |
| G-C06 | Constructor | Seat 2 commitment=-1 | IllegalArgumentException | Yes |
| G-C07 | Constructor | Seat 5 commitment=-1 | IllegalArgumentException | Yes |
| G-C08 | Constructor | Seat 3 commitment=Long.MIN_VALUE | IllegalArgumentException | Yes |
| G-C09 | Constructor, getCommittedChips(int) | Six zero commitments | All six getters return zero | Yes |
| G-Q01 | getCommittedChips(int) | Commitments=[10,20,30,40,50,60]; queried seat=-1 | IllegalArgumentException | Yes |
| G-Q02 | getCommittedChips(int) | Commitments=[10,20,30,40,50,60]; queried seat=0 | 10 | Yes |
| G-Q03 | getCommittedChips(int) | Commitments=[10,20,30,40,50,60]; queried seat=1 | 20 | Yes |
| G-Q04 | getCommittedChips(int) | Commitments=[10,20,30,40,50,60]; queried seat=4 | 50 | Yes |
| G-Q05 | getCommittedChips(int) | Commitments=[10,20,30,40,50,60]; queried seat=5 | 60 | Yes |
| G-Q06 | getCommittedChips(int) | Commitments=[10,20,30,40,50,60]; queried seat=6 | IllegalArgumentException | Yes |
| G-I03 | Constructor, getCommittedChips(int) | Mutate the supplied commitment array after construction | Snapshot retains its original commitments | Yes |
| G-A01 | Constructor, getPot(), getTotalChips() | All stacks and commitments are zero | Pot=0; total chips=0 | Yes |
| G-A02 | Constructor, getPot(), getTotalChips() | Six stacks of 1000; all commitments zero | Pot=0; total chips=6000 | Yes |
| G-A03 | Constructor, getPot(), getTotalChips() | Stacks=[1000,990,980,1000,1000,1000]; commitments=[0,10,20,0,0,0] | Pot=30; total chips=6000 | Yes |
| G-A04 | Constructor, getPot(), getTotalChips() | Stacks=[999,998,997,996,995,994]; commitments=[1,2,3,4,5,6] | Pot=21; total chips=6000 | Yes |
| G-A05 | Constructor, getPot(), getTotalChips() | All stacks zero; commitments=[Long.MAX_VALUE,0,0,0,0,0] | Pot=Long.MAX_VALUE; total chips=Long.MAX_VALUE | Yes |
| G-A06 | Constructor, getPot(), getTotalChips() | First stack=Long.MAX_VALUE-1, other stacks zero; first commitment=1 | Pot=1; total chips=Long.MAX_VALUE | Yes |
| G-A07 | Constructor | All stacks zero; commitments start with Long.MAX_VALUE and 1 | IllegalArgumentException: pot overflow | Yes |
| G-A08 | Constructor | Stacks start with Long.MAX_VALUE and 1; commitments zero | IllegalArgumentException: total-stack overflow | Yes |
| G-A09 | Constructor | First stack=Long.MAX_VALUE; last commitment=1 | IllegalArgumentException: combined-total overflow | Yes |
