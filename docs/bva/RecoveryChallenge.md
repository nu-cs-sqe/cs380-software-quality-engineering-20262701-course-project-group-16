# W8-E04 chip recovery BVA

The desktop offers practice for any signed-in profile. A profile below the
400-chip buy-in minimum can earn 200 chips for at least four correct answers
out of five. Each account can earn up to three successful rewards per local
calendar day. The existing Week 8 table contract includes balances of 1–399,
so recovery is not limited to exactly zero.

| ID | Boundary | Expected outcome |
| --- | --- | --- |
| R01 | Five distinct questions from the ten-template bank; missing or out-of-range answer | Both locales resolve every prompt and choice; reject incomplete/invalid answers. |
| R02 | Three, four and five correct answers | Three earns nothing; four and five pass. Failed attempts do not count toward the daily cap. |
| R03 | Balance 0, 199, 399 or 400 | Eligible below 400; 400 is practice only. Each success adds exactly 200. |
| R04 | Third versus fourth success on the same local day; next local midnight | Third succeeds; fourth is blocked while underfunded; a new claim can earn a reward next day. |
| R05 | Replay same challenge after midnight; same challenge from another account; foreign login | Do not double-credit. Reject reuse across accounts and unauthorized handles. |
| R06 | SQLite rejects the claim row; retry after correction; schema 3 database | No partial credit/history; same challenge may be retried. Upgrade keeps saved balances. |
| R07 | Before sign-in; unanswered choices; switch from English to Chinese mid-quiz | No public quiz access. Show incomplete notice. Preserve answers and show a localized reward in history after claim. |
| R08 | Active hand; settled table; unseated and closed accounts | No credit may invalidate an unpaid hand's opening balance. A paid table can use a saved reward for a top-up without changing session net. Unseated/closed account credits do not reopen a table. |
| R09 | New quiz with a known random seed; English source answers | Five distinct questions per quiz; subsequent draw differs; each answer index matches the rule. |
| R10 | Earlier hand, later reward, malformed hand event type | Unified history lists the reward first without changing session net; malformed event type is rejected. |

`RecoveryIntegrationTest` uses a real SQLite database and fixed local clock.
The cap test restores a sub-minimum balance between successful attempts to
model spending chips in intervening hands. `AccountTableIntegrationTest`
exercises the paid-table balance update with a fake store.
`ProfileGamePanelTest` drives the Swing view and repository together.

Question IDs and the shared answer key live in `RecoveryQuestion`. The English
bundle is the source text; the Chinese bundle translates the same keys and
choice order. Topics come from these existing rule tests:

| Topics | Rule tests |
| --- | --- |
| Blinds and flop | GameConfigTest; SessionBettingTest.lastPreflopDecisionAdvancesToFlopAndPreservesCommitments |
| Fold, refunds and side pots | PotLayoutTest.pl02FoldedMoneyStaysInPotsAndFoldedUncalledMoneyIsRefunded; PotSettlementTest.paysEachSidePotOnlyToEligiblePlayersAndReturnsUncalledChips |
| Ties | PotSettlementTest.splitsTiedPotAndAwardsOddChipClockwiseAfterDealer |
| Private-card handoff | SessionBettingTest.ordinaryClockwiseHandoffWrapsToSeatZeroAndConcealsCards |
| Immediate quit and all-in eligibility | QuitIntegrationTest.departingPlayersFoldImmediatelyAndKeepTheirStacksWithoutBeingDealtAgain; QuitIntegrationTest.anAllInDepartureKeepsTheCurrentHandAndItsPayout |
| Top-up | AccountTableIntegrationTest.topUpsOnlyMoveExistingFundsBetweenHandsAndRejectInvalidAmounts |
