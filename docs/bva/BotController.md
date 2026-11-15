# BotController and scheduling/session ports BVA

Owner-thread coordinator; tests supply fake BotSession and BotScheduler ports
rather than executing the real engine or worker. Callback generations prevent
old successes or errors from disrupting a replacement job.

| ID | Methods | Input/state | Expected | Status |
| --- | --- | --- | --- | --- |
| CT01 | advance/getters | Human/no work; two successive bot turns | Schedule only current bot; resume after accepted result | Implemented, GREEN |
| CT02 | advance | Match changes before success/failure | Ignore stale output and error | Implemented, GREEN |
| CT03 | advance/getFailure | Timeout, then explicit retry | No fallback action; clear error and retry once | Implemented, GREEN |
| CT04 | advance | Capture/apply failure | Recoverable failure, no invented action or retry loop | Implemented, GREEN |
| CT05 | advance/cancel/close | Replacement, old callbacks, cancellation, repeated close | Preserve current job; invalidate old work; close once | Implemented, GREEN |

Cases map to BotControllerTest. Real Swing integration is tested separately.
