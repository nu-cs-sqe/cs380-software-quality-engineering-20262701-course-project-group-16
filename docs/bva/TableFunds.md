# TableFunds BVA

| ID | Methods | State/boundary | Expected | Status / evidence |
| --- | --- | --- | --- | --- |
| TF01 | constructor/getBalance/getTableChips/getAvailableChips | Negative balance/reservation; reservation above balance; equal/zero | Invalid ranges reject; available = total - reserved | GREEN, AccountTableIntegrationTest.tableFundingBoundariesRejectNegativeOrUnreservedAmounts / buyInsUseAccountTotalsAndSettledChangesAreSavedExactlyOnce |
| TF02 | needsRecovery/needsTopUp | Busted balance 399/400; funded stack below 400 | Recovery below 400 only when busted; top-up/rebuy when the minimum is affordable | GREEN, tableFundingBoundariesRejectNegativeOrUnreservedAmounts / aBustedHumanCanUseARecoveredBalanceWithoutRestartingTheTable |
| TF03 | getSessionNetChange | Paid hand then buy-in/top-up | Hand gain/loss only; funding excluded | GREEN, returnedCompletionsAreDetachedAndReportPreBlindStacksAndSessionNet |

Current `getMinimumTopUp`/`getMaximumTopUp` boundaries are parameterized in
TableFundsTest: 0/399/400/401 balances, partial stacks 1/200/399, and an existing
3,000-chip stack. The session-supplied maximum also includes shared numeric
headroom and becomes zero during play, after paid departure or table close.
FundingCapacityTest validates negative and above-available supplied limits.
