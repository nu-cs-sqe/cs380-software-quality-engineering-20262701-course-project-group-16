# PotSettlement and Settlement BVA

`settle(GameState)` accepts only a decision-free showdown or uncontested hand.
It returns immutable paid/refund maps and a COMPLETION state; it never mutates
the input. Contribution layers restrict each pot to eligible live contributors.
Odd chips go clockwise after the dealer; uncalled excess is a refund, not a win.
Reference: [Poker TDA rules](https://www.pokertda.com/view-poker-tda-rules/).

| ID | Methods | State/input | Expected output | Implemented test (PotSettlementTest) |
| --- | --- | --- | --- | --- |
| PAY01 | settle; result getters | Contributions10/20/30/20; seat3 folded; strongest eligible0, then1 | Main40 to0, side30 to1, refund10 to2; immutable maps, unchanged input, completed pot0/total6000 | Yes; paysEachSidePotOnlyToEligiblePlayersAndReturnsUncalledChips |
| PAY02-0/1/5 | settle | Pot15; seats0/1 tie; dealer0/1/5 | Shares7/8 or8/7 according to clockwise order; folded2 gets0 | Yes; splitsTiedPotAndAwardsOddChipClockwiseAfterDealer |
| PAY03 | settle | Only seat1 live; contributions10/20 | Award20 plus refund10; no comparison or contested payout | Yes; uncontestedAwardAndRefundNeverCountAsContestedShowdownPayouts |
| PAY04 | settle | Contributions10/20/20; last player folded | Main30 to stronger0, sole-eligible side20 to1; side is not a contested win | Yes; soleEligibleSidePotIsPaidWithoutCreditingAContestedWin |
| PAY05 | settle | Decision-free showdown, pot0 | Complete without an invented payout | Yes; zeroPotFinishesWithoutInventingAPayout |
| PAY06 | constructors; settle | SETUP or already paid state | Reject; never settle twice | Yes; rejectsUnfinishedAndAlreadyCompletedHands |
| PAY07 | settle | Evaluator fails during ranking | Original phase and pot retained | Yes; failedComparisonDoesNotModifyTheUnpaidState |

TDD: all nine test invocations failed against the unimplemented boundary, then
passed after implementation. The first full build caught an unused test import;
it was removed before committing. Domain fixtures use literal chip expectations
and injected rank comparisons to isolate pot allocation; real-evaluator played
integration is covered at the session boundary.

Additional regression cases cover safe near-long-limit total payouts, a side pot
with no eligible contributor (reject without mutation), stronger contenders
appearing later, nonadjacent tied seats with an odd chip, zero-pot settlement
without ranking, and rejection for pending/raise rights, zero contenders or an
early contested board. These extend the original TDD cases; they are not new
claimed failing-before-implementation cycles.

## Instructor follow-up — November 15

Clockwise odd-chip ordering uses the shared GameConfig.SEAT_COUNT constant.
Existing tie and separated-seat payout cases verify the unchanged behavior.

E06 extracts the existing contribution-level algorithm into
[PotLayout](PotLayout.md), shared with bot call previews. PL01–05 verify pot and
refund totals, eligibility, immutable layers and unchanged settlement payouts.
The payout/odd-chip and completed-state rules remain here.
