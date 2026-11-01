# StatsAction BVA

## Contract

Immutable accepted/rejected action observation. Inputs use resolved betting meaning, not an ALL_IN category. Chip additions must match action semantics; street and context must agree. Structurally invalid input is rejected even when accepted=false. Engine legality and opportunity eligibility are authoritative; this type does not reconstruct full betting rules.

Inputs are `(accepted, street, type, chipsAdded, context)`. Invalid-case rows use accepted=true. Context aliases: none=(NONE,false,false,false); open=(OPENING_RAISE,false,false,false); three=(THREE_BET,true,false,false); four=(FOUR_BET,false,false,false); opportunity/threeOpportunity=(NONE,true,false,false); cBet=(NONE,false,true,false); faced=(NONE,false,false,true).

All case IDs appear in StatsActionTest. Valid-value observations also exercise package-private accessors used by PlayStats.

| ID | Methods under test | State / input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| SA-I01 | Constructor | `null, ResolvedActionType.CHECK, 0L, none` | IllegalArgumentException | Yes |
| SA-I02 | Constructor | `BettingStreet.PREFLOP, null, 0L, none` | IllegalArgumentException | Yes |
| SA-I03 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.CHECK, 0L, null` | IllegalArgumentException | Yes |
| SA-I04 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.CALL, Long.MIN_VALUE, none` | IllegalArgumentException | Yes |
| SA-I05 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.CALL, -1L, none` | IllegalArgumentException | Yes |
| SA-I06 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.CHECK, 1L, none` | IllegalArgumentException | Yes |
| SA-I07 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.FOLD, 1L, none` | IllegalArgumentException | Yes |
| SA-I08 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.CALL, 0L, none` | IllegalArgumentException | Yes |
| SA-I09 | Constructor | `BettingStreet.FLOP, ResolvedActionType.BET, 0L, none` | IllegalArgumentException | Yes |
| SA-I10 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.RAISE, 0L, open` | IllegalArgumentException | Yes |
| SA-I11 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.FORCED_POST, 0L, none` | IllegalArgumentException | Yes |
| SA-I12 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.BET, 1L, none` | IllegalArgumentException | Yes |
| SA-I13 | Constructor | `BettingStreet.FLOP, ResolvedActionType.FORCED_POST, 1L, none` | IllegalArgumentException | Yes |
| SA-I14 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.RAISE, 1L, none` | IllegalArgumentException | Yes |
| SA-I15 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.CALL, 1L, open` | IllegalArgumentException | Yes |
| SA-I16 | Constructor | `BettingStreet.FLOP, ResolvedActionType.RAISE, 1L, open` | IllegalArgumentException | Yes |
| SA-I17 | Constructor | `BettingStreet.FLOP, ResolvedActionType.CALL, 1L, threeOpportunity` | IllegalArgumentException | Yes |
| SA-I18 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.CHECK, 0L, threeOpportunity` | IllegalArgumentException | Yes |
| SA-I19 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.FORCED_POST, 1L, threeOpportunity` | IllegalArgumentException | Yes |
| SA-I20 | Constructor | `BettingStreet.TURN, ResolvedActionType.BET, 1L, cBet` | IllegalArgumentException | Yes |
| SA-I21 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.CALL, 1L, faced` | IllegalArgumentException | Yes |
| SA-I22 | Constructor | `BettingStreet.RIVER, ResolvedActionType.FOLD, 0L, faced` | IllegalArgumentException | Yes |
| SA-I23 | Constructor | `BettingStreet.FLOP, ResolvedActionType.CALL, 1L, cBet` | IllegalArgumentException | Yes |
| SA-I24 | Constructor | `BettingStreet.FLOP, ResolvedActionType.RAISE, 1L, cBet` | IllegalArgumentException | Yes |
| SA-I25 | Constructor | `BettingStreet.FLOP, ResolvedActionType.CHECK, 0L, faced` | IllegalArgumentException | Yes |
| SA-I26 | Constructor | `BettingStreet.FLOP, ResolvedActionType.BET, 1L, faced` | IllegalArgumentException | Yes |
| SA-I27 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.FOLD, 0L, cBet` | IllegalArgumentException | Yes |
| SA-I28 | Constructor | `BettingStreet.PREFLOP, ResolvedActionType.CHECK, 0L, faced` | IllegalArgumentException | Yes |
| SA-V01 | Constructor | `true, BettingStreet.PREFLOP, ResolvedActionType.FORCED_POST, 1L, none` | Constructed; input values retained exactly | Yes |
| SA-V02 | Constructor | `true, BettingStreet.PREFLOP, ResolvedActionType.CHECK, 0L, none` | Constructed; input values retained exactly | Yes |
| SA-V03 | Constructor | `true, BettingStreet.PREFLOP, ResolvedActionType.CALL, 1L, none` | Constructed; input values retained exactly | Yes |
| SA-V04 | Constructor | `true, BettingStreet.PREFLOP, ResolvedActionType.RAISE, 1L, open` | Constructed; input values retained exactly | Yes |
| SA-V05 | Constructor | `true, BettingStreet.PREFLOP, ResolvedActionType.RAISE, 1L, three` | Constructed; input values retained exactly | Yes |
| SA-V06 | Constructor | `true, BettingStreet.PREFLOP, ResolvedActionType.RAISE, 1L, four` | Constructed; input values retained exactly | Yes |
| SA-V07 | Constructor | `true, BettingStreet.FLOP, ResolvedActionType.BET, 1L, none` | Constructed; input values retained exactly | Yes |
| SA-V08 | Constructor | `true, BettingStreet.TURN, ResolvedActionType.RAISE, 1L, none` | Constructed; input values retained exactly | Yes |
| SA-V09 | Constructor | `true, BettingStreet.RIVER, ResolvedActionType.CALL, Long.MAX_VALUE, none` | Constructed; input values retained exactly | Yes |
| SA-V10 | Constructor | `true, BettingStreet.RIVER, ResolvedActionType.FOLD, 0L, none` | Constructed; input values retained exactly | Yes |
| SA-V11 | Constructor | `false, BettingStreet.PREFLOP, ResolvedActionType.RAISE, 1L, open` | Constructed; input values retained exactly | Yes |
| SA-V12 | Constructor | `true, BettingStreet.FLOP, ResolvedActionType.CHECK, 0L, cBet` | Constructed; input values retained exactly | Yes |
| SA-V13 | Constructor | `true, BettingStreet.FLOP, ResolvedActionType.CALL, 1L, faced` | Constructed; input values retained exactly | Yes |
| SA-V14 | Constructor | `true, BettingStreet.FLOP, ResolvedActionType.FOLD, 0L, faced` | Constructed; input values retained exactly | Yes |
| SA-V15 | Constructor | `true, BettingStreet.PREFLOP, ResolvedActionType.CALL, 1L, opportunity` | Constructed; input values retained exactly | Yes |
| SA-V16 | Constructor | `true, BettingStreet.PREFLOP, ResolvedActionType.FOLD, 0L, opportunity` | Constructed; input values retained exactly | Yes |
| SA-V17 | Constructor | `true, BettingStreet.FLOP, ResolvedActionType.FOLD, 0L, cBet` | Constructed; input values retained exactly | Yes |
