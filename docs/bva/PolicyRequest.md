# PolicyRequest BVA

Only a filtered GameView, authoritative LegalActions, public CallValue and
immutable public beliefs enter this boundary. The application must supply a
matching snapshot/scenario; E06 owns result identity and stale-result rejection.

| ID | Methods | Input/state | Expected | Status |
| --- | --- | --- | --- | --- |
| PR01 | constructor/getters | Four streets; supplied belief map then mutated | Retain immutable inputs, copied beliefs and private-card filtering | Implemented, GREEN |
| PR02 | constructor | Human, nonactor, folded bot, uncontested hand | Reject | Implemented, GREEN |
| PR03 | constructor | Setup, no options, call cost one below/above actual | Reject | Implemented, GREEN |
| PR04 | constructor/call getters | Oversized or too-small pot, excess refund, unknown/self beliefs; side pot and valid refund | Reject inconsistencies; correct net pot odds | Implemented, GREEN |
| PR05 | constructor/legal getter | Mismatched wager type/maximum; closed raise rights | Reject mismatches; preserve passive-only options | Implemented, GREEN |
| PR06 | position/participant getters | Dealer/blind wrap, folds, all-in contenders | Public street-order position; only active responders affect position | Implemented, GREEN |

Cases map to PolicyRequestTest. Non-null references are caller contracts.
