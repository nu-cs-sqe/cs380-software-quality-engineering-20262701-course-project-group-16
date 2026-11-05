# ShowdownResolver BVA

`ShowdownResolverTest` exercises both constructors and `resolve(GameState)`.
All states use real validated cards/player/state objects and conserve 6,000
chips. Default construction supplies the real PokerHandEvaluator. One fail-fast
HandEvaluator stub verifies the uncontested path does not compare hands; it is
not used for contested winner expectations.

| ID | Method | Input / system state | Expected output | Implemented / status |
| --- | --- | --- | --- | --- |
| SR-C01 | injected constructor | Null evaluator | NullPointerException | Yes, GREEN: SR_C01_rejectsNullEvaluator |
| SR-N01 | resolve | Null state | NullPointerException | Yes, GREEN: SR_N01_rejectsNullState |
| SR-V01 | default constructor; resolve | Six active players, closed SHOWDOWN, p0 pair of aces strongest | Only p0; same hand ID; AWAITING_SETTLEMENT; immutable winners; no chip/state change | Yes, GREEN: SR_V01_realEvaluatorFindsWinnerWithoutSettling |
| SR-V02 | resolve | Strongest hand folded; p1 ALL_IN; p2 ELIMINATED | Only p1; folded/eliminated excluded | Yes, GREEN: SR_V02_includesAllInButNotFoldedOrEliminated |
| SR-V03 | resolve | Royal flush shared board, all six eligible | Six tied IDs in seat order | Yes, GREEN: SR_V03_sharedBoardCanTieAllSixPlayers |
| SR-V04 | resolve | p0/p1 share best strength | Both tied IDs in seat order | Yes, GREEN: SR_V04_twoWinnersRetainedInSeatOrder |
| SR-V05 | resolve | p0/p1 early tie; later p2 has straight flush | Only p2; earlier ties removed | Yes, GREEN: SR_V05_laterStrongerHandClearsEarlierTies |
| SR-U01-PREFLOP | resolve | PREFLOP; only p4 participates; no pending decisions | p4 uncontested; no evaluator call; AWAITING_SETTLEMENT; no chips moved | Yes, GREEN: SR_U01_uncontestedNeedsNoEvaluationOrCompleteBoard |
| SR-U01-FLOP | resolve | FLOP; only p4 participates; no pending decisions | p4 uncontested; no evaluator call; AWAITING_SETTLEMENT; no chips moved | Yes, GREEN: SR_U01_uncontestedNeedsNoEvaluationOrCompleteBoard |
| SR-U01-TURN | resolve | TURN; only p4 participates; no pending decisions | p4 uncontested; no evaluator call; AWAITING_SETTLEMENT; no chips moved | Yes, GREEN: SR_U01_uncontestedNeedsNoEvaluationOrCompleteBoard |
| SR-U01-RIVER | resolve | RIVER; only p4 participates; no pending decisions | p4 uncontested; no evaluator call; AWAITING_SETTLEMENT; no chips moved | Yes, GREEN: SR_U01_uncontestedNeedsNoEvaluationOrCompleteBoard |
| SR-U01-SHOWDOWN | resolve | SHOWDOWN; only p4 participates; no pending decisions | p4 uncontested; no evaluator call; AWAITING_SETTLEMENT; no chips moved | Yes, GREEN: SR_U01_uncontestedNeedsNoEvaluationOrCompleteBoard |
| SR-I01-SETUP | resolve | SETUP; six eligible contenders | IllegalStateException; no state/chip changes | Yes, GREEN: SR_I01_rejectsUnreadyOrCompletedContestedStates |
| SR-I01-COMPLETION | resolve | COMPLETION; six eligible contenders | IllegalStateException; no state/chip changes | Yes, GREEN: SR_I01_rejectsUnreadyOrCompletedContestedStates |
| SR-I01-PREFLOP | resolve | PREFLOP; six eligible contenders | IllegalStateException; no state/chip changes | Yes, GREEN: SR_I01_rejectsUnreadyOrCompletedContestedStates |
| SR-I01-FLOP | resolve | FLOP; six eligible contenders | IllegalStateException; no state/chip changes | Yes, GREEN: SR_I01_rejectsUnreadyOrCompletedContestedStates |
| SR-I01-TURN | resolve | TURN; six eligible contenders | IllegalStateException; no state/chip changes | Yes, GREEN: SR_I01_rejectsUnreadyOrCompletedContestedStates |
| SR-I01-RIVER | resolve | RIVER; six eligible contenders | IllegalStateException; no state/chip changes | Yes, GREEN: SR_I01_rejectsUnreadyOrCompletedContestedStates |
| SR-I02 | resolve | SHOWDOWN with no eligible player | IllegalStateException | Yes, GREEN: SR_I02_rejectsNoEligiblePlayer |
| SR-I03 | resolve | Sole participant still has pending action | IllegalStateException before uncontested result | Yes, GREEN: SR_I03_rejectsPendingActionBeforeUncontestedResult |
| SR-I04 | resolve | Sole participant still has open raise rights | IllegalStateException before uncontested result | Yes, GREEN: SR_I04_rejectsUnclearedRaiseRights |

RED `29442e4`: all 21 invocations failed as expected against the scaffold.
GREEN `0d5172e`: all 21 pass. Phase-specific suffixes above uniquely identify
parameterized invocations. The constructors have no numeric boundary.

[Showdown integration](../design/showdown.md) adds two real-component boundary
fixtures in `ShowdownIntegrationTest` (commit `428b161`). It does not simulate a
played hand or award main/side pots. Settlement remains a separate operation.
