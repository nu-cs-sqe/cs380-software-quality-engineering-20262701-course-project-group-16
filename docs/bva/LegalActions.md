# Legal action queries (K05)

| ID | Input / system state | Expected result | Test |
| --- | --- | --- | --- |
| LA-01 | Owes 20; stacks 19/20 | CALL costs 19/20; CHECK and RAISE unavailable | LegalActionsTest.optionsDistinguishCappedCallsShortAllInsAndFullRaises |
| LA-02 | Owes 20; stacks 21/39; full target 40 | RAISE available only at the exact all-in target; under-minimum non-all-ins and amounts over stack reject | Same |
| LA-03 | Owes 20; stacks 40/41 | Full targets 40 through stack, no separate short target; quoted and accepted call costs agree | Same |
| LA-04 | Unopened flop, BB 20, stack 960 | CHECK costs zero; BET target 20–960, reject 19 | LegalActionsTest.openingOptionsAreBetTargetsAndCheckingCostsNothing |
| LA-05 | Closed raise rights, setup state, missing state | No RAISE with closed rights; no setup actions; null rejected | LegalActionsTest.closedRaiseRightsAndNonBettingPhasesDoNotAdvertiseWagers |
| LA-06 | Full target exceeds long range | Minimum absent; only exact short all-in is offered and accepted | LegalActionsTest.unrepresentableFullMinimumStillOffersExactShortAllIn |
| LA-07 | Swing wager entry in English and Chinese; invalid text, target 39, target 40 | Both invalid inputs preserve the log and hide cards; 40 raises; UI moves to next viewer | PlayedGamePanelTest.wagerUsesEngineRangeAndRejectsBadInputWithoutLeavingCardsRevealed |
| LA-08 | Swing at short-all-in-only decision | Exact target shown; greater amount rejected; exact amount accepted | PlayedGamePanelTest.shortAllInHintOffersTheExactTargetAndNoIntermediateRaise |

The engine queries immutable options without applying a request or creating an
observation. The same wager bounds validate submissions. A maximum target is
the current round contribution plus the player's stack. The full minimum is the
nominal target, even if unaffordable; it is absent only if unrepresentable.
The call cost is the actual deduction, capped by the stack.
