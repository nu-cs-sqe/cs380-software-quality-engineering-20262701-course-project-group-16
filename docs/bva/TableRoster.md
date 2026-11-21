# TableRoster BVA

| ID | Methods | State/boundary | Expected | Status / evidence |
| --- | --- | --- | --- | --- |
| TR01 | opening/startingStacks | Human balances 500/2,000; posted blinds | Buy-ins 500/1,000; starting stacks include blinds | GREEN, AccountTableIntegrationTest.buyInsUseAccountTotalsAndSettledChangesAreSavedExactlyOnce |
| TR02 | next | Departed human; busted bot; surviving funded seat | New 1,000-chip bot ID; survivor keeps stack/learning; no account binding on replacement | GREEN, departedHumansAreReplacedBetweenHandsAndBindingsDoNotReachTheBot / newBotsReplaceBustedBotsAndRetainOnlyActualAccountBindings / survivingBotsRetainTheirLearningWhenAnotherSeatIsReplaced |
| TR03 | preview/totalChips | Replacement funding; raw total at Long.MAX_VALUE / one above | Calculate future seats without new identities; reject overflow without wrapping | GREEN, FundingCapacityTest / topUpsLeaveRoomForBotsAfterAPlayerDeparts |

November 19: `opening` consumes already validated chosen amounts rather than
clamping humans to 1,000. `preview` computes replacement funding with the old
seat identity for capacity checks; `next` assigns fresh replacement identities.
The former replacementReserve calculation is superseded by FundingCapacity,
which also accounts for payable winnings. `totalChips` returns explicit absence
on overflow, including exact-limit/one-above tests.
