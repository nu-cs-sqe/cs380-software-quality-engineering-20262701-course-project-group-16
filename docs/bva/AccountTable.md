# AccountTable BVA

Real-component cases in AccountTableIntegrationTest; implemented/GREEN.

| ID | Methods | State/boundary | Expected / test |
| --- | --- | --- | --- |
| AT01 | open/openingRoster/begin/profileIds/displayedFunds | Balances 2,000/500 | Buy-ins 1,000/500, six seats, funds conserved; buyInsUseAccountTotalsAndSettledChangesAreSavedExactlyOnce |
| AT02 | open/refresh | Zero, missing, unavailable or below-reservation saved totals | Reject; prior state retained; invalidBalancesAndAccountsCannotReplaceTheExistingHand / invalidOrUnavailableBalanceReadsPreserveTheCurrentHand / restoredBalancesCannotInvalidateChipsAlreadyReservedForTheTable |
| AT03 | addChips/nextRoster | -1, 0, available, available+1; unknown/departed account; before payment | Only affordable between-hand reservations; topUpsOnlyMoveExistingFundsBetweenHandsAndRejectInvalidAmounts |
| AT04 | completion/afterPayment/save | Before/after durable-save failure, retry, second hand | No early payment; one saved hand and statistics delta; failedOrAmbiguousSavePreservesUnpaidStateAndRetryDoesNotDoubleCredit |
| AT05 | displayedFunds/nextRoster | Quit before/after payment | Hold pending reservation, release paid funds; replace identity without transferring account; departedHumansAreReplacedBetweenHandsAndBindingsDoNotReachTheBot |
| AT06 | canContinue/refresh | One funded human; recovered second human | Resume after recovery/top-up; aBustedHumanCanUseARecoveredBalanceWithoutRestartingTheTable |
| AT07 | addChips | Long.MAX_VALUE account balance | Reject overflowing future roster before reservation; topUpsLeaveRoomForBotsAfterAPlayerDeparts |
| AT08 | canStartNextHand/nextHand/endTable | A sole human leaves a paid table with five bots | Reject a bot-only deal, keep the paid hand, and release reservations without writing another hand. |
| AT09 | canStartNextHand/nextHand/addTableChips | One human with zero table chips and an affordable 400-chip top-up | Block the deal at zero. Permit it after the top-up while preserving the same account and saved balance. |

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
A 7,000-chip funded stack can keep playing.

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

## Desktop opening buy-in — November 24

| Boundary | Expected outcome | Evidence |
| --- | --- | --- |
| Empty amount with a 2,000-chip balance | Mark Ready uses the 1,000-chip default. Start reserves 1,000 chips for the account in the chosen chair. | `startup renders buy-in validation and grouped balances (en)`. The test failed before the form applied the default. |
| Explicit 400, 399, 2,001 and 400.5 with a 2,000-chip balance | Accept the inclusive minimum. Reject each invalid amount and leave Start disabled, with a validation message in the selected locale. | The English and Chinese startup walkthroughs and `validates whole-chip buy-ins exactly through the user’s available funds`. |
| Default with balances of 399, 400, 999, 1,000 and 1,001 | An underfunded account cannot use the default. Funded accounts use their balance up to 1,000 chips. | `the default buy-in is affordable for funded accounts and rejects underfunding`. |
