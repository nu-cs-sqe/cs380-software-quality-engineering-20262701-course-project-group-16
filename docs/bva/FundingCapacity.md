# FundingCapacity BVA

| ID | Methods | Input/state | Expected | Status / evidence |
| --- | --- | --- | --- | --- |
| FC01 | accepts / maximumAddition | Raw roster total equals Long.MAX_VALUE or exceeds by one | Detect overflow without wrapping; no funding offered | GREEN, FundingCapacityTest.rawFundingTotalsRejectOverflowWithoutWrapping |
| FC02 | accepts | Large outside balance would make winnings or replacements unpayable | Reject opening before replacing current hand | GREEN, openingFundsReserveRoomForPotentialWinnings / openingFundsReserveRoomForReplacementBots |
| FC03 | maximumAddition | Shared safe budget allows 2,000; account has 9,000 available | Offer/accept 2,000; reject 2,001 | GREEN, reportedTopUpMaximumIsAcceptedWithinTheSharedFundingBudget |
| FC04 | accepts / session continuation | Replacement roster exceeds numeric headroom after payment | No next hand; closing the paid table still works | GREEN, unsafeFutureFundingStopsBeforeStartingAnotherHand |
| FC05 | maximumAddition | Unfunded account cannot receive a representable possible win; another account exhausts shared budget | Offer zero instead of an invalid deposit | GREEN, FundingCapacityTest |

These are signed-64-bit safety bounds, not a fixed game buy-in maximum. Capacity
reserves room for account credits and up to five 1,000-chip replacement seats.
The policy is conservative at the numeric boundary. No real SQLite behavior is
claimed by these unit/real-game integration cases.

Refresh validates the new balances with the same capacity budget before publishing
its snapshot. The maximum for a second human is checked against their own
stack and balance rather than the first seat's values.
