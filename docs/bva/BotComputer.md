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
