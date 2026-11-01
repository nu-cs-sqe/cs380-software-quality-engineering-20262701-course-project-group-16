# StatsContext BVA

## Contract

Immutable context supplied by the engine. Raise type is required. A three-bet requires an eligible opportunity; opening/four/later raises cannot describe that decision. Preflop flags cannot be mixed with flop flags, and opening a c-bet cannot also face one.

Inputs are `(raiseType, threeBetOpportunity, flopCBetOpportunity, facedFlopCBetOpportunity)`.

All case IDs appear in StatsContextTest. Valid-value observations also exercise package-private accessors used by PlayStats.

| ID | Methods under test | State / input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| SC-I01 | Constructor | `null, false, false, false` | IllegalArgumentException | Yes |
| SC-I02 | Constructor | `PreflopRaiseType.NONE, false, true, true` | IllegalArgumentException | Yes |
| SC-I03 | Constructor | `PreflopRaiseType.NONE, true, true, false` | IllegalArgumentException | Yes |
| SC-I04 | Constructor | `PreflopRaiseType.NONE, true, false, true` | IllegalArgumentException | Yes |
| SC-I05 | Constructor | `PreflopRaiseType.OPENING_RAISE, false, true, false` | IllegalArgumentException | Yes |
| SC-I06 | Constructor | `PreflopRaiseType.THREE_BET, false, false, false` | IllegalArgumentException | Yes |
| SC-I07 | Constructor | `PreflopRaiseType.OPENING_RAISE, true, false, false` | IllegalArgumentException | Yes |
| SC-I08 | Constructor | `PreflopRaiseType.FOUR_BET, true, false, false` | IllegalArgumentException | Yes |
| SC-I09 | Constructor | `PreflopRaiseType.LATER_RAISE, true, false, false` | IllegalArgumentException | Yes |
| SC-I10 | Constructor | `PreflopRaiseType.FOUR_BET, false, false, true` | IllegalArgumentException | Yes |
| SC-V01 | Constructor | `PreflopRaiseType.NONE, false, false, false` | Constructed; input values retained exactly | Yes |
| SC-V02 | Constructor | `PreflopRaiseType.NONE, true, false, false` | Constructed; input values retained exactly | Yes |
| SC-V03 | Constructor | `PreflopRaiseType.OPENING_RAISE, false, false, false` | Constructed; input values retained exactly | Yes |
| SC-V04 | Constructor | `PreflopRaiseType.THREE_BET, true, false, false` | Constructed; input values retained exactly | Yes |
| SC-V05 | Constructor | `PreflopRaiseType.FOUR_BET, false, false, false` | Constructed; input values retained exactly | Yes |
| SC-V06 | Constructor | `PreflopRaiseType.LATER_RAISE, false, false, false` | Constructed; input values retained exactly | Yes |
| SC-V07 | Constructor | `PreflopRaiseType.NONE, false, true, false` | Constructed; input values retained exactly | Yes |
| SC-V08 | Constructor | `PreflopRaiseType.NONE, false, false, true` | Constructed; input values retained exactly | Yes |
