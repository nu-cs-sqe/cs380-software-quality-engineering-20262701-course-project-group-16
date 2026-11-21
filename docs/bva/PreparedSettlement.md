# PreparedSettlement BVA

| ID | Methods | State/boundary | Expected | Status / evidence |
| --- | --- | --- | --- | --- |
| PS01 | save/isSaved/getHandCompletion | Prepared, unsaved, worker save, repeated save | No early session payment; immutable completion; one durable request after success | GREEN, AccountTableIntegrationTest.preparedSettlementSavesInBackgroundAndPublishesOnlyOnTheOwner |
| PS02 | SetupSession.prepareSettlement/publishSettlement | Unsaved token, pending restart/quit, already-published/stale token | Reject invalid publication/mutations; owner publishes saved result without I/O | GREEN, same case |
| PS03 | save | Failure before/after durable commit | Retain token for safe retry; no duplicate credit/statistics | GREEN, failedOrAmbiguousSavePreservesUnpaidStateAndRetryDoesNotDoubleCredit |

Worker tests use an injected fake store. Session ownership is a caller contract;
these APIs do not introduce an executor or change existing Swing callbacks.

The worker/publication case also verifies the pending completion preview and
rejects a saved token from another session, plus profile/table restarts during
a pending save. Legacy sessions reject account-only operations explicitly.

After publication and continuation, the next bot turn's revision must exceed the
previous hand's accepted-decision count; a paid hand cannot reset session revisions.

Pending-save privacy now covers cards revealed after preparation: rejected
legacy/profile/chosen-table restarts, next-hand, close, refresh and funding
operations conceal them. The review regression was observed RED then GREEN.
