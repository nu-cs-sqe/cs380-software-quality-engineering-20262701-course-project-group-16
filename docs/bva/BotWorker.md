# BotWorker BVA

The owner serializes lifecycle calls. A bounded single background worker receives
immutable input; callbacks are dispatched to the owner and gated by job identity.

| ID | Methods | Input/state | Expected | Status |
| --- | --- | --- | --- | --- |
| BW01 | start | Controlled worker and owner queues | Computation and callback occur in their respective steps | Implemented, GREEN |
| BW02 | start/cancel | Replace queued completion; cancel queued task | No stale callback or duplicate publication | Implemented, GREEN |
| BW03 | cancel | Cancel while computation runs | Late output ignored | Implemented, GREEN |
| BW04 | start | Elapsed 9/10/11 against budget 10; nanoTime wrap; expiry after computation | Boundary timeout; no expired output | Implemented, GREEN |
| BW05 | start | Runtime/cancellation failure; rejected scheduling | Owner receives FAILED | Implemented, GREEN |
| BW06 | close/start | Budget -1/0/1; closed worker; duplicate close | Reject invalid lifecycle; close once; no late work | Implemented, GREEN |
| BW08 | start | Interrupted worker thread | Cancel before computation; no accepted action | Implemented |
| BW07 | daemonThread | Thread creation without starting it | Named idle daemon | Implemented, GREEN |

BG01–03 in BotBackgroundIntegrationTest additionally exercise a real background
thread, Swing callback dispatch, bounded replacement queue and three live bot
turns followed by a private human handoff.

Cases map to BotWorkerTest. Timing-sensitive production behavior also has separate
real-thread integration checks; unit tests use a controllable clock, not sleeps.

Style follow-up: constructor budget validation is delegated to requirePositiveDecisionBudget;
BW06 still covers -1/0/1 without adding programmer-null cases. BG03 now seeds
real simulation/policy randomness. Its test driver allows at most two explicit
retries after TIMED_OUT on a busy machine; other failures fail immediately.
Production still requires the user to request retry, and BW04 checks exact
budgets with a deterministic clock rather than a wall-clock speed assertion.
