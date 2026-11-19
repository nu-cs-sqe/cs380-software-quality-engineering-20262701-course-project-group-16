# SqliteProfileRepository BVA

Tests use the real JDBC driver and isolated JUnit data directories. No live
profiles are accessed. The application serializes the read/modify/write flow.

| ID | Methods / boundary | Input or state | Expected outcome |
| --- | --- | --- | --- |
| SQL-C01 | Constructor, load, save | Fresh data directory with spaces and URI characters; new name with apostrophe | Constructor performs no I/O. Registration survives reopen; one account, empty completed history, 2,000 chips, immutable load result. |
| SQL-C02 | load, save; existing ProfileService.login | Version-3 catalog with existing password, Unicode name and one completed hand | Preserve ID, name, salt, hash, counters and hand IDs; original password authenticates and the wrong password does not. Original file stays byte-for-byte unchanged. |
| SQL-C03 | load after reopen | Imported account has two completed hands and zero balance; legacy file still exists | Reopen returns one account with both completed hands and zero chips. No re-import or second grant. |
| SQL-C04 | load; initial import transaction | Version-2 catalog missing required fields; correct the catalog and retry | IOException preserves source and leaves import retryable. Valid v2 account receives 2,000 chips with empty completed history. |
| SQL-C05 | save | Two row updates; SQLite aborts the second | IOException; earlier update rolls back, preserving the previous balance and statistics. |
| SQL-C06 | save | Duplicate IDs in one request before first open | Reject duplicates without creating the database. |
| SQL-C07 | load | Invalid counter text or missing external statistics column value | IOException; no fallback or reset; original account remains readable after restoring valid data. |
| SQL-C08 | load | Invalid SQLite file with a valid legacy catalog also present | IOException; both files retain their contents. |
| SQL-C09 | load, save | Empty database marked with unsupported version 99 | Refuse to initialize on read or write. Once reset to the supported fresh state in the fixture, it still contains no accounts. |

`ProfiledMatchIntegrationTest` verifies that completed settlement and restart
preserve a zero balance while advancing lifetime statistics. UI-AC03 uses SQLite
in both locales to verify registration, sign-in, settlement and reopen.

## W8-E03 storage, handoff and profile history

| ID | Methods / types | State or boundary | Expected outcome |
| --- | --- | --- | --- |
| SQL-H01 | ProfileHandStore.saveHand; SqliteProfileRepository.loadChipHistory; SqliteChipLedger | Two accounts, positive/negative/zero changes, repeated hand and two sessions | Update each account and statistics once; retain timestamp, type, IDs, resulting balance and signed session net. Reopen shows the same private history in reverse settlement order. |
| SQL-H02 | saveHand | Failure on the second history row; retry twice | No account, marker or event commits on failure. Retry commits both accounts exactly once. |
| SQL-H03 | saveHand | Unknown account, debit beyond balance, overflow, or already recorded statistics without a ledger marker | Storage error and no changes. Exact debit to zero succeeds. |
| SQL-H04 | load, saveHand | Existing version 1 database with saved accounts | Upgrade preserves balances and starts with empty history. The next ledger settlement persists normally. |
| SQL-H05 | ProfileService.getOverview; ProfileOverview | Two signed-in accounts and a handle from another service | Return a read-only view of only the caller's account and reject the foreign handle. |
| SQL-H06 | loadChipHistory | Malformed saved timestamp | Storage error; the saved balance and statistics are not reset. |
| SQL-H07 | ProfileHandSettlement; AccountHandResult | Blank ID, mismatched or absent completed hand, zero, six or seven accounts | Require one to six accounts with matching completion IDs; retain an immutable copy. |
| SQL-H08 | saveHand | A database trigger tries to delete an account as its history is saved | Foreign key enforcement rejects an orphaned event; both accounts and their histories remain unchanged. |
| UI-H01 | Profiles chip history | Before sign-in, after sign-in, after roster removal; English and Chinese | No history before sign-in or after removal. Signed-in account sees its balance and read-only localized changes. |

SqliteHandSettlementTest uses the real database with an injected clock.
ProfileHandSettlementTest checks the proposed command without database I/O.
The UI integration case seeds a committed hand directly through the storage
port; it does not claim that live game settlement is connected.
