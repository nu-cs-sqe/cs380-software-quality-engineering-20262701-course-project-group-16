# AccountTable BVA

Real-component cases in AccountTableIntegrationTest; implemented/GREEN.

| ID | Methods | State/boundary | Expected / test |
| --- | --- | --- | --- |
| AT01 | open/openingRoster/begin/profileIds/displayedFunds | Balances 2,000/500 | Buy-ins 1,000/500, six seats, funds conserved; buyInsUseAccountTotalsAndSettledChangesAreSavedExactlyOnce |
| AT02 | open/refresh | Zero, missing, unavailable or below-reservation saved totals | Reject; prior state retained; invalidBalancesAndAccountsCannotReplaceTheExistingHand / invalidOrUnavailableBalanceReadsPreserveTheCurrentHand / restoredBalancesCannotInvalidateChipsAlreadyReservedForTheTable |
| AT03 | addChips/nextRoster | -1, 0, available, available+1; unknown/departed account; before payment | Only affordable between-hand reservations; topUpsOnlyMoveExistingFundsBetweenHandsAndRejectInvalidAmounts |
| AT04 | completion/afterPayment/save | Before/after durable-save failure, retry, second hand | No early payment; one saved hand and statistics delta; failedOrAmbiguousSavePreservesUnpaidStateAndRetryDoesNotDoubleCredit |
| AT05 | displayedFunds/nextRoster | Quit before/after payment | Hold pending reservation, release paid funds; replace identity without transferring account; departedHumansAreReplacedBetweenHandsAndBindingsDoNotReachTheBot |
| AT06 | canContinue/refresh | One funded human; recovered second human | No champion; resume after recovery/top-up; aBustedHumanCanUseARecoveredBalanceWithoutRestartingTheTable |
| AT07 | addChips | Long.MAX_VALUE account balance | Reject overflowing future roster before reservation; topUpsLeaveRoomForBotsAfterAPlayerDeparts |

The storage collaborator is an in-memory fake; real SQLite atomicity and desktop
worker integration are separate acceptance gates. No programmer-null cases.

Mutation follow-up cases cover foreign/duplicate account rejection, card hiding
on failed starts, between-hand top-up retention after balance refresh and the
exact largest safe replacement reserve. These are additional boundary checks,
not claimed as original feature RED/GREEN cycles.

`aDepartedWinnersStackDoesNotReduceAnotherAccountsTopUpLimit` checks the
largest safe top-up after a winner leaves: reserve replacement funding, not the
retired stack. Zero-balance selection is checked in a nondealer seat so another
valid player cannot mask the validation. Both cases are implemented/GREEN.

## Chosen 400+ funding — November 19

Initial choices require at least 400 and exact selected-account IDs. No fixed
upper cap: the 3,000/400 integration preserves outside funds and pre-blind stacks;
the 390-chip post-loss stack continues normally. Invalid/missing/extra choices
preserve the previous hand. Busted 399 balances require recovery; 400 can rebuy.
The 6,000-chip win and later 7,000-chip funded stack remain playable.

Seven review regressions cover numeric settlement/replacement room, honest
funding maxima, card concealment during pending saves, blank IDs and refreshing
already released balances. All were observed RED then GREEN. Three earlier
near-Long.MAX_VALUE fixtures were adjusted to preserve payable wallet headroom;
those old unlimited numeric acceptance expectations are superseded by
[FundingCapacity](FundingCapacity.md), not removed to hide failures.

Mutation hardening also verifies that an unsafe refreshed balance is rejected
before publication and that the second human's maximum uses their actual
990-chip stack. The unsafe-refresh case was observed failing with the capacity
check removed, then passing after restoring that check.
