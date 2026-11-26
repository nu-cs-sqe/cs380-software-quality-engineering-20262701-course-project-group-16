# BotComputer BVA

Calculation consumes only BotTurn and a cooperative cancellation source.
No session, Swing component or internal GameState is supplied.

| ID | Methods | Input/state | Expected | Status |
| --- | --- | --- | --- | --- |
| BC01 | decide | Controlled estimate and random quantile | Same captured scenario; legal actor/target; no session mutation | Implemented, GREEN |
| BC02 | decide | Cancellation before and after estimator | Abort without a random draw or action publication | Implemented, GREEN |
| BC03 | decide | Estimator failure; random quantile 1 | Propagate failure; no invented fallback action | Implemented, GREEN |

Cases map to BotComputerTest. Default real computation is covered by the
background integration tests.

The [exact river BVA](BotPolicy.md#exact-river-decisions) maps the free check and
stack-capped call boundaries (BR03), uncertain fallback (BR04), certain-loss
call value (BR06) and cancellation after the proof (BR08). BR01 exercises
`decide` through accepted actions and actual settlement. BR02 controls for
settlement on the original action sequence.

The [hybrid postflop cases](BotPolicy.md#hybrid-postflop-proof-and-wager-strategy)
cover the earlier-street proof before simulation, the zero-budget fallback,
the value decision, the pot-floor boundary, and stale exact-result rejection.
BR01–02 remain the fixed-deal river settlement controls.
