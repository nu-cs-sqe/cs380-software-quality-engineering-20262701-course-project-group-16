# ActionRequest boundary value analysis

Each case maps to the JUnit method with the same ID (hyphens become underscores).
Grouped assertions exercise the public getters at the stated boundary; no mutable state is exposed.

| ID | Public methods | State/input boundary | Expected result | Implemented |
| --- | --- | --- | --- | --- |
| AR-01 | ActionRequest constructors | null player ID | IllegalArgumentException | Yes |
| AR-02 | ActionRequest constructors | empty player ID | IllegalArgumentException | Yes |
| AR-03 | ActionRequest constructors | blank player ID | IllegalArgumentException | Yes |
| AR-04 | ActionRequest constructors | whitespace player ID | IllegalArgumentException | Yes |
| AR-05 | ActionRequest constructors | Null action type | IllegalArgumentException | Yes |
| AR-06 | ActionRequest(String,ActionType); getPlayerId(); getType(); getTargetAmount() | CHECK without amount | Retain ID/type; target unavailable | Yes |
| AR-07 | ActionRequest(String,ActionType); getters | CALL without amount | Retain ID/type; target unavailable | Yes |
| AR-08 | ActionRequest(String,ActionType); getters | FOLD without amount | Retain ID/type; target unavailable | Yes |
| AR-09 | ActionRequest(String,ActionType) | BET without target | IllegalArgumentException | Yes |
| AR-10 | ActionRequest(String,ActionType,long) | BET target -1 | IllegalArgumentException | Yes |
| AR-11 | ActionRequest(String,ActionType,long) | BET target 0 | IllegalArgumentException | Yes |
| AR-12 | ActionRequest(String,ActionType,long); getTargetAmount | BET: minimum positive | Target is total round contribution, retained exactly | Yes |
| AR-13 | ActionRequest(String,ActionType,long); getTargetAmount | BET: raise-to example | Target is total round contribution, retained exactly | Yes |
| AR-14 | ActionRequest(String,ActionType,long); getTargetAmount | BET: maximum long | Target is total round contribution, retained exactly | Yes |
| AR-15 | ActionRequest(String,ActionType) | RAISE without target | IllegalArgumentException | Yes |
| AR-16 | ActionRequest(String,ActionType,long) | RAISE target -1 | IllegalArgumentException | Yes |
| AR-17 | ActionRequest(String,ActionType,long) | RAISE target 0 | IllegalArgumentException | Yes |
| AR-18 | ActionRequest(String,ActionType,long); getTargetAmount | RAISE: minimum positive | Target is total round contribution, retained exactly | Yes |
| AR-19 | ActionRequest(String,ActionType,long); getTargetAmount | RAISE: raise-to example | Target is total round contribution, retained exactly | Yes |
| AR-20 | ActionRequest(String,ActionType,long); getTargetAmount | RAISE: maximum long | Target is total round contribution, retained exactly | Yes |
| AR-21 | ActionRequest(String,ActionType,long) | CHECK with supplied target | IllegalArgumentException | Yes |
| AR-22 | ActionRequest(String,ActionType,long) | CALL with supplied target | IllegalArgumentException | Yes |
| AR-23 | ActionRequest(String,ActionType,long) | FOLD with supplied target | IllegalArgumentException | Yes |
| AR-24 | getPlayerId | Nonblank ID with surrounding whitespace | Retain ID without silently changing identity | Yes |
