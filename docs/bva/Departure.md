# Departure and later hands (K04)

| ID | Input / system state | Expected result | Test |
| --- | --- | --- | --- |
| DEP-01 | An active human leaves out of turn | Preserve current actor and log, hide their cards, reject reselection; one FOLD at their next decision in the automatic turn pump | QuitIntegrationTest.departingPlayersFoldInTurnAndKeepTheirStacksWithoutBeingDealtAgain |
| DEP-02 | The human actor leaves | One normal FOLD, then next actor; no stack transfer | Same |
| DEP-03 | Two funded departed seats after payment | Same IDs/stacks, no cards or blinds, 44 cards remaining after four seats receive cards; departed seats gain no later hand count | Same |
| DEP-04 | Already-all-in big blind leaves | Retains eligibility and receives payout; no extra FOLD; next hand skips the seat | QuitIntegrationTest.anAllInDepartureKeepsTheCurrentHandAndItsPayout |
| DEP-05 | Paid match with one or zero funded nondeparted seats | One remaining player wins; zero ends without a winner; neither can continue; new match clears departures | QuitIntegrationTest.paidMatchStopsWhenFewerThanTwoFundedPlayersRemainAndRestartClearsDepartures |
| DEP-06 | Unknown human ID, bot ID, or repeated leave | Reject without advancing the turn or appending an action | QuitIntegrationTest.invalidDeparturePreservesTheTurnAndAcceptedActions |
| DEP-07 | Swing leave in both languages | Fold current human, conceal cards, remove from viewer list; later seat shows departed and no card backs | PlayedGamePanelTest.leaveFoldsTheCurrentHumanAndRemovesTheirPrivateViewer |

A queued leave is resolved at that seat's next decision. If the hand finishes
first, there is no artificial action. Payment and completed statistics still
use the hand's actual cards, wagers and accepted observations.
