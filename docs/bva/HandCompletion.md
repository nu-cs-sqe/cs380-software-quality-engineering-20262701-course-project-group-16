# HandCompletion / AccountChange BVA

| ID | Methods | State/boundary | Expected | Status / evidence |
| --- | --- | --- | --- | --- |
| HC01 | getters / AccountChange getters | Paid hand including blinds, refunds and bots | Per-account net = end - start; balance after = before + net; combined bot/human changes sum to zero; completed statistics delta | GREEN, AccountTableIntegrationTest.buyInsUseAccountTotalsAndSettledChangesAreSavedExactlyOnce |
| HC02 | getSessionId/getHandId | Retry and next hand | Same identity for retry; same session/new hand on continuation | GREEN, failedOrAmbiguousSavePreservesUnpaidStateAndRetryDoesNotDoubleCredit |
| HC03 | getAccounts/getBotChanges/getStatistics | Caller attempts map mutation; next hand | Immutable payload; historical snapshots unchanged | GREEN, returnedCompletionsAreDetachedAndReportPreBlindStacksAndSessionNet |

Session IDs must be nonblank and differ between independently started tables.
Retries retain the same completion identity. Covered by the original buy-in
and prepared-worker integration cases (GREEN).
