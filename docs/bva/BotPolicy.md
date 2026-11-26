# BotPolicy BVA

E05 is a documented probability-based heuristic, not a GTO or positive-profit
guarantee. PolicyRequest supplies a paired filtered snapshot, legal options and
call scenario. The policy does not read GameState or produce accepted actions.

| ID | Methods | Input/state | Expected | Status |
| --- | --- | --- | --- | --- |
| BP01 | distribution/sample | Equity 0 or hopeless call; free check | Fold paid hopeless calls, never fold a free check | Implemented, GREEN |
| BP02 | distribution/sample | Equity estimate 1, short all-in CALL | Policy selects the capped CALL | Implemented, GREEN |
| BP03 | distribution | Break-even call, closed raise rights | Nonzero fold/call probabilities; no RAISE | Implemented, GREEN |
| BP04 | distribution/sample | Three personalities, four streets, five equity values, 101 quantiles | Only legal actions; all targets accepted by Wager.allows | Implemented, GREEN |
| BP05 | distribution/sample/engine apply | Existing SB contribution; stack 25/35/40/50 around minimum 40 | Raise-to semantics, correct deductions and all-in classification | Implemented, GREEN |
| BP06 | distribution | Eligible pot reduced to 20; refundable new call | Minimum clamp; correct side-pot odds; no fold when no new call chips are at risk | Implemented, GREEN |
| BP07 | distribution | Three personalities with equity .9 | Modest ordered aggression; pot-fraction sizing 45/58/70 | Implemented, GREEN |
| BP08 | distribution | In/out of position; deep/short stack | Position and commitment pressure affect preferences | Implemented, GREEN |
| BP09 | distribution | Controlled passive/aggressive opponents; prior; unseen street | Confidence-weighted adaptation; prior and other streets do not fabricate evidence | Implemented, GREEN |
| BP10 | distribution/sample | 10,000 draws, fixed duplicated seed | Reproducible mixed actions; frequencies near published probabilities | Implemented, GREEN |

Tests map to BotPolicyTest. Real engine action acceptance in BP02/BP05 is integration
evidence; other cases test pure policy behavior on immutable values. No win-rate
or live-learning claim. BotPersonality is an enum and uses the rubric exemption.

## Later integration and quality regressions

| ID | Methods | Input/state | Expected | Status |
| --- | --- | --- | --- | --- |
| BP11 | distribution | Same paid decision, different public position | In-position call preference increases | Implemented |
| BP12 | distribution | Multiway versus heads-up fair shares | Avoid treating heads-up .4 equity as value | Implemented |
| BP13 | distribution | Full-stack versus deep-stack commitment, equity 1 | Lower aggressive weight for larger exposure | Implemented |
| BP14 | distribution/sample | Same whole-hand contributions and risk, round targets offset by 10 | Same probabilities; raise-to target offset only | Implemented |
| BP15 | distribution | Break-even paid decision, three personalities | Ordered call preferences | Implemented |
| BP16 | distribution | Equity estimate 1 at the accepted pot/cost boundary | No folds for every personality | Implemented |
| BP17 | distribution | Fair-share and thin multiway .25 equity | No value bet at fair share; cautious/loose behavior differs modestly | Implemented |
| BP18 | distribution | Learned flop aggression; balanced strong preflop hand | Learning reduces thin bets; balanced raises stay in calibrated .2–.5 range | Implemented |
| BI01 | estimate/distribution/engine apply | Four streets, real Monte Carlo, all personalities and quantiles | Engine accepts actions and conserves 6,000 chips | Implemented |
| BI02 | estimate/distribution/sample | Changed hidden opponent cards and reversed internal deck | Same seeded estimate, distribution and actions | Implemented |
| BI03 | distribution/engine/settlement | Short blind; 10 new call chips refunded, plus 5 older committed chips | CALL; settlement refund 15; new call cost 0; chips conserved | Implemented |

BP11–18 and BI01–03 were added after the first feature GREEN; BP15–18 strengthen
the first mutation findings. These are hardening/integration evidence, not extra
feature RED cycles. BI cases map to BotPolicyIntegrationTest.

Readability follow-up (November 15): private names now state paid call/fold
weights and break-even call equity explicitly. Raise sizing names the called
round total, remaining capacity and desired chips. Preference formulas, action
probabilities, limits and public API remain unchanged.

## Exact river decisions

`RiverOutcomeEvaluator` uses the existing best-five evaluator to compare the
board with every possible two-card holding. `BOARD_LOCKED_TIE` records a
universal tie when every holding plays the board. If a holding can improve,
a second enumeration removes the acting player's own cards and compares their
rank with the weakest and strongest remaining opponent ranks. `BotComputer`
uses these proofs before simulation and action sampling.

| ID | Method or workflow | Input boundary | Observable result | Test |
| --- | --- | --- | --- | --- |
| BR01 | `decide` through settlement | Reported Broadway board, 10/20 blinds, five live seats, 46,010 river bet | The preflop fold stands. Four bots call for 980, 4,005, 980 and 980 respectively. The 5,000 and 6,050 pots split among eligible players and the bettor receives 42,005 back. All six final stacks match their opening stacks. | `BotRiverIntegrationTest.reportedHandCallsOnTheLockedRiverAndRestoresEveryLivePlayersContribution` |
| BR02 | Settlement | The original two river folds on the same deal | The original 3,040 and 6,050 pots, three payouts and 42,005 refund are preserved. | `BotRiverIntegrationTest.manuallyReplayingTheReportedFoldsPreservesTheReportedOriginalSettlement` |
| BR03 | `BotComputer.decide` | Two to six live seats, bets of 0, 20, 80, 980 and 46,010. Stack boundaries of 979, 980 and 981 against 980. All personalities and three seeds for each case. | `CHECK` for zero. `CALL` for paid cases. The stack boundary costs are 979, 980 and 980 chips respectively. The 25-chip short stack calls 25 against 80. Checks and calls omit a wager target and consume no simulation or random draw. | `BotRiverDecisionsTest.lockedDecisionsUseTheExactLegalCheckOrCallWithoutSampling` |
| BR04 | `evaluate` and `decide` | Board royal flush, board quads with an ace kicker, Broadway with three cards of one suit, lower straight flush, private royal and tieable nuts | The first two boards lock. The next two are uncertain. The private royal guarantees a win and the tieable nuts guarantee non-loss. On the uncertain river, the lowest and highest valid sample quantiles select Fold and Call respectively. | `RiverOutcomeEvaluatorTest` board cases and `BotRiverDecisionsTest.anUncertainRiverStillUsesSimulationAndRandomizedFoldCallWeights` |
| BR05 | `classify` | Own rank 100. Opponent bounds 99–99, 100–100, 99–100, 101–101, 99–101 and 100–101. | Win, tie, non-loss, loss, uncertain and uncertain respectively. | `RiverOutcomeEvaluatorTest.comparesOwnRankWithTheExactOpponentBounds` |
| BR06 | `actionForKnownOutcome` | For a 20-chip call, sole-eligible payout 19/20/21 with no refund, payout 19 with a 1-chip refund, or payout 0 with a 20-chip refund | Fold, Call, Call, Call and Call respectively. Contested and opponent-only pots contribute zero to the calculation. | `BotRiverDecisionsTest.knownLossUsesOnlyItsSoleEligiblePayoutAndIncrementalRefund` |
| BR07 | `evaluate` | Board/holding counts 4/2, 5/1, 4/3 or 6/1, or 5/2 with one card repeated across the lists | Reject each with `IllegalArgumentException`. Valid cases in BR04 exercise 5/2 with distinct cards. | `RiverOutcomeEvaluatorTest.rejectsWrongCardCountsAndADuplicateAcrossBoardAndHolding` and `sevenDistinctCardsMustStillBeSplitIntoFiveBoardCardsAndTwoHoleCards` |
| BR08 | `BotComputer.decide` | Deadline expires after the board proof | Cancellation prevents the bot from returning an action. | `BotRiverDecisionsTest.deadlineCanExpireAfterTheBoardProofBeforeTheActionIsReturned` |
| BR09 | `evaluate` | Cancellation after two first-card groups, or thread interruption with a clear flag | Throw `CancellationException` in both cases. | `RiverOutcomeEvaluatorTest.cancellationStopsAnEnumerationAlreadyInProgress` and `interruptionCancelsEvenWhenTheSuppliedFlagIsClear` |

### Test scope and local TDD record

`BotRiverIntegrationTest` exercises the real bot, session, betting and settlement
components on a fixed deal. `BotRiverDecisionsTest` uses real betting snapshots
with injected estimate and random sources. BR06 directly tests the action rule
with a supplied outcome and public pot preview. The evaluator tests call real
ranking code, and BR05 tests just the rank comparison with explicit bounds.

- RED. `test --tests application.BotRiverIntegrationTest -x jacocoTestReport`
  ran before the bot change. BR01 expected Call for Bot 5 and received Fold.
  BR02 passed as the settlement control.
- GREEN. The targeted bot, ranking and settlement run passed after the exact
  river decision was added. The full build then confirmed both fixed-deal cases.
- Follow-up. The rank, cancellation and invalid-input cases strengthen boundary
  and mutation coverage. The style review added the 979/980/981 stack cases and
  explicit expected costs. These are post-GREEN checks. The RED and GREEN
  evidence for this fix is local, and the rubric's GitHub-history criterion
  still needs published evidence.

## Hybrid postflop proof and wager strategy

| ID | Component and boundary | Observable assertion | Test |
| --- | --- | --- | --- |
| HB01 | `ExactOutcomeEvaluator.evaluate`, Q♥ J♥ 10♥ with A♥ K♥ on the flop or turn, comparison limit 0 | Guaranteed win even with zero comparison budget. A completed public royal gives `BOARD_LOCKED_TIE`. | `ExactOutcomeEvaluatorTest.aPrivateRoyalProvesAWinEvenWithZeroComparisonBudget`, `aPublicRoyalFlushLocksTheRiverBoard` |
| HB02 | `evaluate`, private ace quads on flop and turn with enough public suit slots for a straight flush, then turn with all four suits present | The vulnerable boards are uncertain. With at most two final community cards of a suit, the private ace quads win. | `ExactOutcomeEvaluatorTest.aceQuadsNeedTheSuitLimitToBeSafeBeforeTheRiver` |
| HB03 | `evaluate`, private K-high straight flush. Turn comparison limits 0, 45,539 and 45,540. | Uncertain at the first two limits and a guaranteed win after all 45,540 comparisons. | `ExactOutcomeEvaluatorTest.aTurnSearchNeedsEveryLegalComparisonBeforeProvingAWin` |
| HB04 | Validation and cancellation. Limit −1, boards of 2 or 6 cards, short holding, duplicate card, cancellation in a search or before a shortcut, or interruption with a clear flag | Reject invalid inputs or cancel. No proof action is returned. | `ExactOutcomeEvaluatorTest.rejectsOutOfRangeAndRepeatedKnownCards`, `cancellationAbortsWorkOnAnIncompleteBoard`, `interruptionAbortsAnIncompleteBoardDespiteAClearFlag` |
| HB05 | `BotComputer.decide`, private royal on flop and turn across all personalities | Legal value bet below the full stack. No simulation or random draw. | `BotRiverDecisionsTest.privateRoyalSelectsALegalValueBetWithoutSimulationOrSampling` |
| HB06 | `decide`, A♠ K♣ Q♦ J♥ with a low holding and a zero budget | One estimate, one sample and a legal Call for the selected quantile. | `BotRiverDecisionsTest.anUnresolvedTurnUsesEquityAndSamplingWhenTheProofBudgetIsZero` |
| HB07 | `KnownOutcomePolicy`, 20 prior Folds versus 20 prior Calls. Wager cap 19 with minimum 20. Facing 20 with raising available. | Larger value target against the caller. The short opening is exactly 19. The raise uses the engine's permitted target. | `BotRiverDecisionsTest.aPrivateRoyalPricesBetsUsingLearnedResponses`, `aProvenWinCanRaiseOrUseTheOnlyLegalShortOpeningBet` |
| HB08 | Guaranteed non-loss at capped call odds and pot layers 101/61/19/23 with eligibility counts 3/2/1/0 for the acting bot | Covered Call without sampling. Payout floors 181 for a win, 82 for non-loss and 19 for loss. | `BotRiverDecisionsTest.aNonLosingStraightCallsWhenTheWorstTieCoversTheCost`, `payoutFloorsUseOnlyEligibleLayersAndRoundTiesDown` |
| HB09 | Loss and heads-up public evidence of 7 folds, 8 folds or 8 calls | Check, Bet and Check respectively. BR06 retains the loss call-risk boundaries. | `BotRiverDecisionsTest.aLosingHandOnlyBluffsWithEvidenceOfFolds` |
| HB10 | Public accepted raises on the turn, ask for turn or flop continuation evidence | Eight qualifying continuations on the turn and zero flop opportunities. | `BotRiverDecisionsTest.continuationCountsRaisesAndCallsOnTheirObservedStreet` |
| HB11 | Different hidden opponent holdings and undealt order, then a session with an invalidated exact action | Same action and wager target for identical public input. Stale action is rejected with empty accepted history. | `BotRiverDecisionsTest.changingHiddenHoldingsAndDeckOrderPreservesTheExactAction`, `invalidatingTheSessionRejectsTheProvenFlopAction` |
| HB12 | `KnownOutcomePolicy.choose`, 20 observed Folds, Calls or Raises on the flop. Separate profile of 20 Folds facing a bet and 60 opening Bets. | Targets 20, 40 and 30 with modeled net chip values 40.472727234, 58.071367894 and 62.917026635. The opening bettor can be induced by Check, with net 46.546341463. Only the 20 facing-wager decisions affect continuation. | `KnownOutcomePolicyTest.valuesBothTheSelectedWagerAndItsProjectedReturn`, `aWinnerCanCheckToInduceAPlayerWhoOpensButFoldsToBets` |
| HB13 | Winner faces an 80-chip bet with 10 behind, winner can only bet 19 all-in, or supplied non-loss floor 19/20/21 at a call cost of 20 | The capped call costs 10 and has net value 50. The short bet has net value 46.073925513 with no future or raise increment. The non-loss decisions are Fold, Call and Call. | `KnownOutcomePolicyTest.aCappedWinningCallCostsOnlyTheRemainingStack`, `aShortValueBetHasNoFutureOrRaiseContributionWhenTheBotIsAllIn`, `aSuppliedNonLossFloorMustCoverTheCallBeforeThePolicyContinues` |
| HB14 | Losing hand facing a 20-chip bet, public history of eight Folds and a 60-chip pot before the call. Separate multiway and 50% continuation cases. | A raise-to 80 reaches the exact allowed bluff cap and has projected net value 55.456391577. Multiway and equal Fold/Call history choose Check. A capped losing hand without wager rights folds a paid call. | `KnownOutcomePolicyTest.facingABetTheBluffCreditsTheRefundAdjustedPrizeAndCapsTheRaise`, `aBluffNeedsAHeadsUpOpponentWithMoreFoldsThanContinuations`, `aCappedWinningCallCostsOnlyTheRemainingStack` |
| HB15 | `ExactOutcomeEvaluator.evaluate`, four public aces on the turn and K♣ 2♦ in hand. Default limit 2,048 and full limit 45,540. | Uncertain at the production limit. A complete search proves guaranteed non-loss because an opponent can tie the king kicker. | `ExactOutcomeEvaluatorTest.publicAceQuadsWithAPrivateKingCanStillTie` |

HB05 supplied the initial local RED. Both cases failed when the old bot called
the injected simulation. Both passed after the proof and value decision were
wired into `BotComputer`. The remaining HB cases verify boundaries and
integration after that initial implementation. HB12–14 were added after the
mutation pass showed that action-only assertions missed changes in projected
chip values. The evaluator tests run real
ranking code. Action tests use real filtered betting snapshots and controlled
estimate, random and opponent observation inputs. The fixed river regression
still checks actual calls, payouts, refunds and conservation through settlement.
HB15 promoted the standalone public-aces probe into a permanent regression
during the style review. It is post-GREEN verification.
