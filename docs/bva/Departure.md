# Departure and later hands (K04)

| ID | Input / system state | Expected result | Test |
| --- | --- | --- | --- |
| DEP-01 | An active human leaves out of turn | Immediately record one automatic FOLD, preserve another pending actor, hide cards and reject reselection | QuitIntegrationTest.departingPlayersFoldImmediatelyAndKeepTheirStacksWithoutBeingDealtAgain |
| DEP-02 | The human actor leaves | One automatic FOLD, then next actor; no stack transfer | Same |
| DEP-03 | Two funded departed seats after payment | Same IDs/stacks, no cards or blinds, 44 cards remaining after four seats receive cards; departed seats gain no later hand count | Same |
| DEP-04 | Already-all-in big blind leaves | Retains eligibility and receives payout; no extra FOLD; next hand skips the seat | QuitIntegrationTest.anAllInDepartureKeepsTheCurrentHandAndItsPayout |
| DEP-05 | Paid match with one or zero funded nondeparted seats | Zero/one playing seat with chips in departed seats ends without a winner; continuation stops and a new match clears departures | QuitIntegrationTest.paidMatchEndsWithoutAWinnerWhenDepartedSeatsRetainChips |
| DEP-06 | Unknown human ID, bot ID, or repeated leave | Reject without advancing the turn or appending an action | QuitIntegrationTest.invalidDeparturePreservesTheTurnAndAcceptedActions |
| DEP-07 | Swing leave in both languages | Fold current human, conceal cards, remove from viewer list; later seat shows departed and no card backs | PlayedGamePanelTest.leaveFoldsTheCurrentHumanAndRemovesTheirPrivateViewer |

The original K04 checkpoint queued a non-current fold until that seat's next
decision. That historical policy allowed a quitter to win if betting ended
before the queued fold. The November 16 I03 correction makes active departure
folds immediate. Cards, committed chips, fixed seats and completed statistics
are retained. Already-all-in players remain eligible for the current payout.

## Immediate departure correction — November 16, 2026

| ID | Input / system state | Expected result | Test |
| --- | --- | --- | --- |
| DEP-08 | Non-current active human at seat 4 leaves while seat 3 acts | FOLD immediately, retain actor 3 and all chips/cards; later action skips seat 4 | dep08NonCurrentQuitFoldsImmediatelyAndPreservesTheCurrentDecision |
| DEP-09 | Caller at seat 0 leaves after all but that caller and the big blind folded | Big blind wins immediately; quitter retains 980 chips, winner receives 1,030; total 6,000 and statistics complete once | dep09AQuitterWhoAlreadyActedCannotBecomeTheUncontestedWinner |
| DEP-10 | Out-of-turn quit while a bot turn is captured; recorder fails then succeeds | Failure preserves eligibility, log, learning and captured revision; retry folds once, invalidates old work and excludes quitter from the new scenario | dep10FailedDepartureRecordingPreservesEligibilityAndSupportsRetry |
| DEP-11 | Human already folded before leaving | Retire seat without another FOLD; preserve current actor and chips | dep11AnAlreadyFoldedQuitterCreatesNoDuplicateFold |
| DEP-12 | Human leaves after winning a resolved, unpaid hand | Preserve winner/result and accepted log; original payout and completion still occur once | dep12LeavingAfterAResolvedHandPreservesItsWinnerAndPendingPayment |
| DEP-13-en/zh | Non-current caller leaves through Swing after only the big blind remains | Winner/result and Settle hand become available immediately; controls/cards remain private and payout conserves chips | PlayedGamePanelTest.leavingOutOfTurnAwardsTheUncontestedHandToTheRemainingPlayer |

DEP-08/09/10 were observed as three assertion failures against the prior source.
The engine's fourteen DB invocations then failed compilation because the dedicated
automatic-departure API did not exist. Subsequent execution exposed a test fixture
mistake: Player uses identity equality, so ownership assertions now compare IDs,
seats, types, stacks and commitments. The fourteen engine cases and three session
regressions pass after implementation/assertion correction. DEP-11/12 and the
bilingual UI cases are additional boundary/regression hardening. No synthetic
Git RED/GREEN commits are claimed. Existing all-in, privacy, next-hand, invalid-ID
and repeated-quit assertions are retained under the revised immediate-fold timing.

The I03 checkpoint preceded the I07 terminal correction below. Local verification
leaves the issue's review, remote-CI and merge gates open.

## Full-total match winner — November 16, 2026

The historical DEP-05 policy awarded victory to the last funded nondeparted seat.
It is superseded by the full-chip-total requirement. Departed stacks stay owned;
zero/one playing seat and no full-total owner means play ends without a match
winner. A departed all-in winner who receives the complete total is still the
match winner. A completed winner's later departure preserves that result.

I07-01–04 in CompletedStatisticsIntegrationTest cover these boundaries, and the
six UI-I07 cases cover zero/one/two playing seats in both languages, including
real heads-up continuation. The previous stack, privacy, continuation-stop and
restart assertions remain; the old below-total winner assertion is corrected.

## Account-table departure and dead pots — Week 8

DEP-W8-01: `twoDepartingHighContributorsCannotBlockPaymentOrTheNextHand`
uses 40-chip raise/call followed by departures; the remaining player receives
110 chips and continuation deals normally. DEP-W8-02:
`aPlayerWhoHasNotInvestedCanWinAfterAllContributorsLeave` awards the same
110-chip pot to a zero-investment survivor. Both implemented/GREEN.
Account-table replacement/cash-out boundaries are in [AccountTable](AccountTable.md).
Legacy full-total match rules above apply only to legacy session entry points.
