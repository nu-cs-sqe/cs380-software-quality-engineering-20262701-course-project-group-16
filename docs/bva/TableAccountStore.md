# TableAccountStore contract BVA

| ID | Methods | State/boundary | Expected | Status / evidence |
| --- | --- | --- | --- | --- |
| TAS01 | getBalances | Exact requested accounts; missing/zero; I/O failure | No partial table publication; account totals include reserved chips | GREEN, AccountTableIntegrationTest invalid balance/read cases |
| TAS02 | save | First completion, retry before/after durable commit | Atomic human balance/history/statistics update; deduplicate session/hand | Consumer retry GREEN with fake; SQLite implementation pending Estella |
| TAS03 | save | Conflicting duplicate or stale expected balance | Reject transaction without partial writes | Required adapter acceptance case; pending SQLite |

Storage migration, initial 2,000 grants, recovery claims and transaction timestamps
belong to the adapter. Current tests do not claim database-level verification.
