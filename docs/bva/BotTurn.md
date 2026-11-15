# BotTurn and session bot boundary BVA

The session owner captures an immutable filtered turn; workers never receive
GameState, Deck, human cards or a mutable opponent registry.

| ID | Methods | Input/state | Expected | Status |
| --- | --- | --- | --- | --- |
| SB01 | getBotTurn/getters | No match; trial counts -1/0/1/64/10000/10001; initial bot | Empty/reject invalid; filtered immutable request; no state/privacy mutation | Implemented, GREEN |
| SB02 | applyBotDecision/getBotBeliefs | Invalid/foreign action, valid action, replay | No learning on rejection; accept and learn once; immutable old beliefs | Implemented, GREEN |
| SB03 | invalidateBotTurns/applyBotDecision | Cancel without a game action | Old revision ignored without privacy/chip/log changes | Implemented, GREEN |
| SB04 | start/applyBotDecision | Restart same hand/player fixture | Match identity still rejects old result | Implemented, GREEN |
| SB05 | nextHand/getBotTurn | Human, result, paid hand, next hand | No unnecessary job; old hand rejected; match beliefs persist | Implemented, GREEN |
| SB06 | quit/playDepartureTurns | Current and queued departures | Accepted folds advance normally but never train strategic beliefs | Implemented, GREEN |
| SB07 | applyBotDecision/getBotBeliefs | Before start or another match | Ignore old output; no unrelated mutation | Implemented, GREEN |
| SB08 | getBotTurn/applyBotDecision | Short blind, gross call 10, refund 15 including old chips | Preview only new-call refund 10; actual payment conserves chips | Implemented, GREEN |
| SB09 | isCurrentBotTurn | Same revision, different hand or actor | Reject without engine/state mutation | Implemented, GREEN |

Cases map to SessionBotIntegrationTest; these use real initialization, rules,
statistics and models and are integration tests.

SB08 covers short-blind scenario refunds; SB09 supplies same-revision mismatched
hand/viewer captures and verifies that neither reaches the engine. Capture is
owner-only; this value object introduces no independent mutation API.

| ID | Methods | Input/state | Expected | Status |
| --- | --- | --- | --- | --- |
| SB10 | getBotTurn | Closed round with no actor | Empty; no fabricated job | Implemented, GREEN |
| SB11 | submitAction/getBotBeliefs/getBotTurn | Every street, position true/false, free CHECK preview | Learn exact public context; zero-cost preview has no hypothetical CALL | Implemented, GREEN |
| SB12 | playBotTurns | Legacy helper consumes departed human | Never learn automatic fold | Implemented, GREEN |
| SB13 | playDepartureTurns | Six humans; viewer differs from actor | Select actor with hidden cards | Implemented, GREEN |
| SB14 | applyBotDecision/getBotBeliefs | Injected recorder failure, retry same captured turn | No chip/log/learning publication or consumed revision on failure; retry once | Implemented, GREEN |

BP01/BP02 in BotPlayedIntegrationTest connect real equity/evaluation, learned
policy, accepted actions, settlement, completed statistics and continuation;
a separate capped all-in CALL verifies hero-ineligible side pots and refunds.
