# PlayStats BVA

## Contract and fixtures

One instance aggregates completed hands for one player/profile. Methods follow
beginHand -> zero or more recordAction calls -> completeHand. IDs must be
nonblank and unique across matches for the instance. Incomplete hands do not
change lifetime totals. Invalid observations/completions preserve all aggregates
and the active hand; completed IDs cannot be replayed. Rejected engine actions
are ignored. Counter overflow is checked before any totals are installed.

Percentages return OptionalDouble.empty() with no denominator. AF uses the same
unavailable policy when calls=0, even with positive bets/raises. With a positive
denominator and no qualifying action, the result is numeric zero. No rounding
or display strings are returned.

All cases have IDs matching PlayStatsTest. The helper fixtures are described in
the statistics requirements document. In action tuples, pre/post/raise helpers
create accepted actions; opportunity flags are explicit engine context. The
symbols bet/raise/call/check/fold in AF cases are single FLOP observations with
positive chip amounts for chip-adding actions and zero otherwise. Rejected
observations have accepted=false. A null expected percentage means unavailable.

The populated two-hand seed has a successful explicit preflop three-bet and
flop c-bet followed by a showdown payout; a second hand faces/calls an actual
c-bet and reaches showdown without payout. All percentage denominators are then
available, allowing atomic-rejection cases to compare every getter.

Each row overrides a fresh/default instance unless it explicitly uses that seed
or a multi-hand sequence. Public constructor and all public methods are covered.

| ID | Methods under test | State / inputs | Expected outputs | Implemented? |
| --- | --- | --- | --- | --- |
| PS-A01 | recordAction(), completeHand(), getVpipPercent() | Accepted fold, attempted accepted call, then rejected observation | Accepted call rejected atomically; completed VPIP=0% | Yes |
| PS-A02 | recordAction(), completeHand(), all getters | dealtIn=false; attempted accepted call, then rejected raise | Accepted call rejected; rejected observation ignored; no hands counted | Yes |
| PS-A03 | recordAction(), completeHand(), getVpipPercent() | Accepted TURN call, then attempted accepted PREFLOP call | Backward action rejected without contamination; VPIP=0% | Yes |
| PS-A04 | recordAction(), completeHand(), getVpipPercent() | Accepted TURN call followed by rejected PREFLOP raise | Rejected action ignored, no street rewind; VPIP=0% | Yes |
| PS-A05 | recordAction(), all getters | Attempt accepted action after completion | IllegalStateException; all totals unchanged | Yes |
| PS-A06 | recordAction(), completeHand(), getVpipPercent(), getPfrPercent() | Valid pending call, then construction of invalid three-bet context | Invalid input rejected; completed VPIP=100%, PFR=0% | Yes |
| PS-C01 | recordAction(), completeHand(), getFlopCBetPercent() | One completed dealt-in hand that saw flop: "eligible opening flop bet is a c-bet", actions(open, cBetDecision(ResolvedActionType.BET, 20)), 100.0 | 100.0% | Yes |
| PS-C02 | recordAction(), completeHand(), getFlopCBetPercent() | One completed dealt-in hand that saw flop: "checking an eligible opportunity counts as declined", actions(open, cBetDecision(ResolvedActionType.CHECK, 0)), 0.0 | 0.0% | Yes |
| PS-C03 | recordAction(), completeHand(), getFlopCBetPercent() | One completed dealt-in hand that saw flop: "facing another opening bet is not an opening opportunity", actions(open, post(BettingStreet.FLOP, ResolvedActionType.CALL, 20)), null | Unavailable | Yes |
| PS-C04 | recordAction(), completeHand(), getFlopCBetPercent() | One completed dealt-in hand that saw flop: "all-in before the flop has no betting opportunity", actions(open), null | Unavailable | Yes |
| PS-C05 | recordAction(), completeHand(), getFlopCBetPercent() | One completed dealt-in hand that saw flop: "an ordinary flop bet is not automatically a c-bet", actions(post(BettingStreet.FLOP, ResolvedActionType.BET, 20)), null | Unavailable | Yes |
| PS-C06 | recordAction(), completeHand(), getFlopCBetPercent() | One completed dealt-in hand that saw flop: "rejected c-bet followed by check is declined", actions(open, rejectedCBet, cBetDecision(ResolvedActionType.CHECK, 0)), 0.0 | 0.0% | Yes |
| PS-C07 | recordAction(), completeHand(), getFlopCBetPercent() | One completed dealt-in hand that saw flop: "eligible fold declines the opening bet", actions(open, cBetDecision(ResolvedActionType.FOLD, 0)), 0.0 | 0.0% | Yes |
| PS-C08 | getFlopCBetPercent() | One eligible bet hand, one eligible check hand, one ineligible call hand | 50%; ineligible hands do not dilute denominator | Yes |
| PS-D01 | recordAction(), completeHand(), c-bet/AF getters | Accepted opening c-bet context, then attempted faced-c-bet response | Second observation rejected atomically; c-bet=100%, faced-c-bet/AF unavailable | Yes |
| PS-D02 | recordAction(), completeHand(), c-bet/AF getters | Accepted faced-c-bet call, then attempted opening c-bet | Second observation rejected; c-bet unavailable, fold-to-c-bet=0%, AF=0 | Yes |
| PS-D03 | recordAction(), getFlopCBetPercent(), getAggressionFrequencyPercent() | Rejected-only opening-flop-bet observation | No c-bet or aggression denominator | Yes |
| PS-D04 | recordAction(), getFoldToFlopCBetPercent() | Rejected-only faced-c-bet fold observation | No faced-c-bet denominator | Yes |
| PS-D05 | beginHand(), completeHand(), getHandsPlayed() | Completed nonparticipant ID replayed as dealt-in | IllegalStateException; hands played remains 0 | Yes |
| PS-D06 | recordAction(), completeHand(), all getters | Populated totals; pending accepted three-bet and c-bet | Before completion all totals unchanged; after completion hands=3 | Yes |
| PS-D07 | recordAction(), completeHand(), all getters | Populated totals; attempt inconsistent zero-chip preflop raise | IllegalArgumentException; totals preserved and valid completion succeeds | Yes |
| PS-F01 | recordAction(), completeHand(), getFoldToFlopCBetPercent() | One completed dealt-in hand that saw flop: "fold to an eligible actual c-bet", actions(facedCBetDecision(ResolvedActionType.FOLD, 0)), 100.0 | 100.0% | Yes |
| PS-F02 | recordAction(), completeHand(), getFoldToFlopCBetPercent() | One completed dealt-in hand that saw flop: "calling an eligible c-bet is not folding", actions(facedCBetDecision(ResolvedActionType.CALL, 20)), 0.0 | 0.0% | Yes |
| PS-F03 | recordAction(), completeHand(), getFoldToFlopCBetPercent() | One completed dealt-in hand that saw flop: "raising an eligible c-bet is not folding", actions(facedCBetDecision(ResolvedActionType.RAISE, 60)), 0.0 | 0.0% | Yes |
| PS-F04 | recordAction(), completeHand(), getFoldToFlopCBetPercent() | One completed dealt-in hand that saw flop: "ordinary folds create no c-bet opportunity", actions(post(BettingStreet.FLOP, ResolvedActionType.FOLD, 0)), null | Unavailable | Yes |
| PS-F05 | recordAction(), completeHand(), getFoldToFlopCBetPercent() | One completed dealt-in hand that saw flop: "all-in player has no response decision", actions(pre(ResolvedActionType.CALL, 100)), null | Unavailable | Yes |
| PS-F06 | recordAction(), completeHand(), getFoldToFlopCBetPercent() | One completed dealt-in hand that saw flop: "rejected fold followed by call is not a fold", actions(rejectedFold, facedCBetDecision(ResolvedActionType.CALL, 20)), 0.0 | 0.0% | Yes |
| PS-F07 | getFoldToFlopCBetPercent() | One eligible fold, one eligible call, one ordinary fold | 50%; only actual eligible faced-c-bet hands count | Yes |
| PS-G01 | recordAction(), completeHand(), getAggressionFactor(), getAggressionFrequencyPercent() | One completed dealt-in hand that saw flop: "checks alone provide no denominator", actions(check), null, null | AF=unavailable; AFq=unavailable | Yes |
| PS-G02 | recordAction(), completeHand(), getAggressionFactor(), getAggressionFrequencyPercent() | One completed dealt-in hand that saw flop: "positive aggression with zero calls has unavailable AF", actions(bet), null, 100.0 | AF=unavailable; AFq=100.0% | Yes |
| PS-G03 | recordAction(), completeHand(), getAggressionFactor(), getAggressionFrequencyPercent() | One completed dealt-in hand that saw flop: "call-only aggression is zero", actions(call), 0.0, 0.0 | AF=0.0; AFq=0.0% | Yes |
| PS-G04 | recordAction(), completeHand(), getAggressionFactor(), getAggressionFrequencyPercent() | One completed dealt-in hand that saw flop: "fold-only frequency is zero but AF unavailable", actions(fold), null, 0.0 | AF=unavailable; AFq=0.0% | Yes |
| PS-G05 | recordAction(), completeHand(), getAggressionFactor(), getAggressionFrequencyPercent() | One completed dealt-in hand that saw flop: "one bet and one call", actions(bet, call), 1.0, 50.0 | AF=1.0; AFq=50.0% | Yes |
| PS-G06 | recordAction(), completeHand(), getAggressionFactor(), getAggressionFrequencyPercent() | One completed dealt-in hand that saw flop: "bets and raises are action counts", actions(bet, raise, call), 2.0, 200.0 / 3.0 | AF=2.0; AFq=200.0 / 3.0% | Yes |
| PS-G07 | recordAction(), completeHand(), getAggressionFactor(), getAggressionFrequencyPercent() | One completed dealt-in hand that saw flop: "preflop actions are excluded", actions(raise(PreflopRaiseType.OPENING_RAISE, 60), pre(ResolvedActionType.CALL, 20), bet, call, post(BettingStreet.TURN, ResolvedActionType.CALL, 20)), 0.5, 100.0 / 3.0 | AF=0.5; AFq=100.0 / 3.0% | Yes |
| PS-G08 | recordAction(), completeHand(), getAggressionFactor(), getAggressionFrequencyPercent() | One completed dealt-in hand that saw flop: "checks do not dilute frequency", actions(check, bet, post(BettingStreet.TURN, ResolvedActionType.CHECK, 0), post(BettingStreet.TURN, ResolvedActionType.CALL, 20), post(BettingStreet.RIVER, ResolvedActionType.CHECK, 0)), 1.0, 50.0 | AF=1.0; AFq=50.0% | Yes |
| PS-G09 | recordAction(), completeHand(), getAggressionFactor(), getAggressionFrequencyPercent() | One completed dealt-in hand that saw flop: "all-in resolved as a postflop call", actions(post(BettingStreet.FLOP, ResolvedActionType.CALL, 100)), 0.0, 0.0 | AF=0.0; AFq=0.0% | Yes |
| PS-G10 | recordAction(), completeHand(), getAggressionFactor(), getAggressionFrequencyPercent() | One completed dealt-in hand that saw flop: "all-in resolved as a postflop raise", actions(post(BettingStreet.FLOP, ResolvedActionType.RAISE, 100)), null, 100.0 | AF=unavailable; AFq=100.0% | Yes |
| PS-G11 | recordAction(), completeHand(), getAggressionFactor(), getAggressionFrequencyPercent() | One completed dealt-in hand that saw flop: "rejected aggression is ignored", actions(rejected, call), 0.0, 0.0 | AF=0.0; AFq=0.0% | Yes |
| PS-G12 | getAggressionFactor(), getAggressionFrequencyPercent() | Hand 1: bet, raise, raise, call; hand 2: call | AF=1.5; AFq=60%, using lifetime action counts | Yes |
| PS-I01 | completeHand(), all getters | Populated two-hand seed; pending hand inputs=true, actions(), false, false, -1L, 0L | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I02 | completeHand(), all getters | Populated two-hand seed; pending hand inputs=true, actions(), false, false, Long.MIN_VALUE, 0L | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I03 | completeHand(), all getters | Populated two-hand seed; pending hand inputs=true, actions(), false, false, 0L, -1L | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I04 | completeHand(), all getters | Populated two-hand seed; pending hand inputs=true, actions(), false, false, 0L, Long.MIN_VALUE | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I05 | completeHand(), all getters | Populated two-hand seed; pending hand inputs=true, actions(), false, true, 0L, 0L | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I06 | completeHand(), all getters | Populated two-hand seed; pending hand inputs=true, actions(), true, false, 1L, 0L | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I07 | completeHand(), all getters | Populated two-hand seed; pending hand inputs=true, actions(), false, false, 1L, 0L | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I08 | completeHand(), all getters | Populated two-hand seed; pending hand inputs=false, actions(), true, false, 0L, 0L | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I09 | completeHand(), all getters | Populated two-hand seed; pending hand inputs=false, actions(), true, true, 0L, 0L | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I10 | completeHand(), all getters | Populated two-hand seed; pending hand inputs=false, actions(), true, true, 1L, 0L | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I11 | completeHand(), all getters | Populated two-hand seed; pending hand inputs=false, actions(), false, false, 0L, 1L | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I12 | completeHand(), all getters | Populated two-hand seed; pending hand inputs=true, actions(post(BettingStreet.FLOP, ResolvedActionType.CHECK, 0)), false, false, 0L, 0L | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I13 | completeHand(), all getters | Populated two-hand seed; pending hand inputs=true, actions(pre(ResolvedActionType.FOLD, 0)), true, false, 0L, 0L | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I14 | completeHand(), all getters | Populated two-hand seed; pending hand inputs=true, actions(post(BettingStreet.FLOP, ResolvedActionType.FOLD, 0)), true, true, 0L, 0L | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I15 | completeHand(), all getters | Populated two-hand seed; pending hand inputs=(String) null | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I16 | completeHand(), all getters | Populated two-hand seed; pending hand inputs="" | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-I17 | completeHand(), all getters | Populated two-hand seed; pending hand inputs="another-hand" | IllegalArgumentException; all existing totals unchanged; corrected completion succeeds once | Yes |
| PS-L01 | Constructor, getHandsPlayed() | New instance | 0 completed hands | Yes |
| PS-L02 | beginHand(), all getters | New instance; begin a dealt-in hand but do not complete it | All lifetime values unchanged | Yes |
| PS-L03 | beginHand(), completeHand(), getHandsPlayed() | One dealt-in hand completes without flop/showdown | Hands played=1 | Yes |
| PS-L04 | beginHand(), completeHand(), all getters | One completed hand, dealtIn=false | No counters or available metrics change | Yes |
| PS-L05 | beginHand(), completeHand() | An unfinished hand already exists; try beginning another | IllegalStateException; old hand still completes correctly | Yes |
| PS-L06 | completeHand() | No active hand | IllegalStateException; hands played stays 0 | Yes |
| PS-L07 | recordAction() | No active hand | IllegalStateException; hands played stays 0 | Yes |
| PS-L08 | completeHand() | Active ID differs from supplied completion ID | IllegalArgumentException; old totals/active hand preserved | Yes |
| PS-L09 | completeHand(), all getters | Complete a hand, then complete it again | IllegalStateException; all totals unchanged | Yes |
| PS-L10 | beginHand(), all getters | Begin an already completed ID | IllegalStateException; all totals unchanged | Yes |
| PS-L11 | beginHand(), getHandsPlayed() | (String) null | IllegalArgumentException; hands played remains 0 | Yes |
| PS-L12 | beginHand(), getHandsPlayed() | "" | IllegalArgumentException; hands played remains 0 | Yes |
| PS-L13 | beginHand(), getHandsPlayed() | " " | IllegalArgumentException; hands played remains 0 | Yes |
| PS-L14 | beginHand(), getHandsPlayed() | "\t" | IllegalArgumentException; hands played remains 0 | Yes |
| PS-L15 | beginHand(), getHandsPlayed() | "\u2003" | IllegalArgumentException; hands played remains 0 | Yes |
| PS-L16 | recordAction(), completeHand() | Active hand; null action | IllegalArgumentException; hand remains completable | Yes |
| PS-L17 | beginHand(), completeHand(), getHandsPlayed() | Complete match-1/hand-1 and match-2/hand-1 | Hands played=2 | Yes |
| PS-S01 | recordAction(), completeHand(), getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | "folding after seeing the flop is not showdown", actions(post(BettingStreet.FLOP, ResolvedActionType.FOLD, 0)), true, false, 0L, 0L, 0.0, null | Hands=1; WTSD=0.0%; W$SD=unavailable | Yes |
| PS-S02 | recordAction(), completeHand(), getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | "showdown without payout", actions(check), true, true, 0L, 0L, 100.0, 0.0 | Hands=1; WTSD=100.0%; W$SD=0.0% | Yes |
| PS-S03 | recordAction(), completeHand(), getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | "positive contested showdown payout", actions(check), true, true, 1L, 0L, 100.0, 100.0 | Hands=1; WTSD=100.0%; W$SD=100.0% | Yes |
| PS-S04 | recordAction(), completeHand(), getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | "split-pot payout qualifies", actions(post(BettingStreet.FLOP, ResolvedActionType.CALL, 10)), true, true, 5L, 0L, 100.0, 100.0 | Hands=1; WTSD=100.0%; W$SD=100.0% | Yes |
| PS-S05 | recordAction(), completeHand(), getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | "side-pot payout qualifies", actions(post(BettingStreet.TURN, ResolvedActionType.CALL, 10)), true, true, 7L, 0L, 100.0, 100.0 | Hands=1; WTSD=100.0%; W$SD=100.0% | Yes |
| PS-S06 | recordAction(), completeHand(), getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | "positive payout with negative net result qualifies", actions(pre(ResolvedActionType.CALL, 100)), true, true, 10L, 0L, 100.0, 100.0 | Hands=1; WTSD=100.0%; W$SD=100.0% | Yes |
| PS-S07 | recordAction(), completeHand(), getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | "combined payouts from multiple pots qualify once", actions(check), true, true, 30L, 0L, 100.0, 100.0 | Hands=1; WTSD=100.0%; W$SD=100.0% | Yes |
| PS-S08 | recordAction(), completeHand(), getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | "uncalled-bet refund alone is not a showdown payout", actions(check), true, true, 0L, 100L, 100.0, 0.0 | Hands=1; WTSD=100.0%; W$SD=0.0% | Yes |
| PS-S09 | recordAction(), completeHand(), getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | "winning when others fold is not showdown", actions(check), true, false, 0L, 10L, 0.0, null | Hands=1; WTSD=0.0%; W$SD=unavailable | Yes |
| PS-S10 | recordAction(), completeHand(), getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | "an all-in participant sees the flop", actions(pre(ResolvedActionType.CALL, 100)), true, true, 0L, 0L, 100.0, 0.0 | Hands=1; WTSD=100.0%; W$SD=0.0% | Yes |
| PS-S11 | recordAction(), completeHand(), getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | "a preflop fold never saw the flop", actions(pre(ResolvedActionType.FOLD, 0)), false, false, 0L, 0L, null, null | Hands=1; WTSD=unavailable; W$SD=unavailable | Yes |
| PS-S12 | completeHand(), all getters | Two completed seed hands; complete another hand after elimination with dealtIn=false | All lifetime values unchanged | Yes |
| PS-S13 | getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | Five hands: one no-flop, two flop/no-showdown, two showdowns with one positive payout | Hands=5; WTSD=50%; W$SD=50% | Yes |
| PS-S14 | getWtsdPercent(), getWonAtShowdownPercent() | Three showdowns with one payout; one additional flop/no-showdown hand | WTSD=75%; W$SD=100/3, unrounded | Yes |
| PS-S15 | recordAction(), completeHand(), getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | "an all-in forced blind still participates in showdown", actions(pre(ResolvedActionType.FORCED_POST, 20)), true, true, 0L, 0L, 100.0, 0.0 | Hands=1; WTSD=100.0%; W$SD=0.0% | Yes |
| PS-S16 | recordAction(), completeHand(), getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | "a rejected fold does not end showdown participation", actions(pre(ResolvedActionType.CALL, 100), rejectedFold), true, true, 5L, 0L, 100.0, 100.0 | Hands=1; WTSD=100.0%; W$SD=100.0% | Yes |
| PS-S17 | recordAction(), completeHand(), getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | "largest positive payout counts as one payout hand", actions(check), true, true, Long.MAX_VALUE, 0L, 100.0, 100.0 | Hands=1; WTSD=100.0%; W$SD=100.0% | Yes |
| PS-S18 | recordAction(), completeHand(), getHandsPlayed(), getWtsdPercent(), getWonAtShowdownPercent() | "largest refund alone still does not qualify", actions(check), true, true, 0L, Long.MAX_VALUE, 100.0, 0.0 | Hands=1; WTSD=100.0%; W$SD=0.0% | Yes |
| PS-T01 | getThreeBetPercent() | Completed opening-raise hand; engine reports no three-bet opportunity | Unavailable | Yes |
| PS-T02 | recordAction(), completeHand(), getThreeBetPercent() | One eligible completed hand; response=ResolvedActionType.FOLD, 0L | 0%, with an available opportunity denominator | Yes |
| PS-T03 | recordAction(), completeHand(), getThreeBetPercent() | One eligible completed hand; response=ResolvedActionType.CALL, 40L | 0%, with an available opportunity denominator | Yes |
| PS-T04 | getThreeBetPercent() | Completed explicit THREE_BET with 1 added chip and eligible opportunity | 100%; size alone does not determine raise type | Yes |
| PS-T05 | getThreeBetPercent(), getPfrPercent() | Eligible call followed by an explicit FOUR_BET | Three-bet=0%; PFR=100% | Yes |
| PS-T06 | getThreeBetPercent() | Huge FOUR_BET with no opportunity; later tiny THREE_BET with opportunity | Initially unavailable, then 100% | Yes |
| PS-T07 | getThreeBetPercent() | Multiple engine-flagged eligible decisions in one hand; one three-bet in another hand | 50%, counting opportunity hands once | Yes |
| PS-T08 | recordAction(), getThreeBetPercent(), getVpipPercent() | Only observation is a rejected eligible call | VPIP=0%; three-bet unavailable | Yes |
| PS-V01 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "forced small blind alone", actions(pre(ResolvedActionType.FORCED_POST, 10)), 0.0, 0.0 | VPIP=0.0%; PFR=0.0%; gap=0.0 points; hands=1 | Yes |
| PS-V02 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "forced big blind alone", actions(pre(ResolvedActionType.FORCED_POST, 20)), 0.0, 0.0 | VPIP=0.0%; PFR=0.0%; gap=0.0 points; hands=1 | Yes |
| PS-V03 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "forced ante alone", actions(pre(ResolvedActionType.FORCED_POST, 1)), 0.0, 0.0 | VPIP=0.0%; PFR=0.0%; gap=0.0 points; hands=1 | Yes |
| PS-V04 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "checking the big blind", actions(pre(ResolvedActionType.FORCED_POST, 20), pre(ResolvedActionType.CHECK, 0)), 0.0, 0.0 | VPIP=0.0%; PFR=0.0%; gap=0.0 points; hands=1 | Yes |
| PS-V05 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "completing a 10 small blind to 20", actions(pre(ResolvedActionType.FORCED_POST, 10), pre(ResolvedActionType.CALL, 10)), 100.0, 0.0 | VPIP=100.0%; PFR=0.0%; gap=100.0 points; hands=1 | Yes |
| PS-V06 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "calling a raise from the big blind", actions(pre(ResolvedActionType.FORCED_POST, 20), pre(ResolvedActionType.CALL, 40)), 100.0, 0.0 | VPIP=100.0%; PFR=0.0%; gap=100.0 points; hands=1 | Yes |
| PS-V07 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "ordinary preflop call", actions(pre(ResolvedActionType.CALL, 20)), 100.0, 0.0 | VPIP=100.0%; PFR=0.0%; gap=100.0 points; hands=1 | Yes |
| PS-V08 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "opening raise", actions(raise(PreflopRaiseType.OPENING_RAISE, 60)), 100.0, 100.0 | VPIP=100.0%; PFR=100.0%; gap=0.0 points; hands=1 | Yes |
| PS-V09 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "call then raise counts once", actions(pre(ResolvedActionType.CALL, 20), raise(PreflopRaiseType.THREE_BET, 80)), 100.0, 100.0 | VPIP=100.0%; PFR=100.0%; gap=0.0 points; hands=1 | Yes |
| PS-V10 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "multiple raises count once", actions(raise(PreflopRaiseType.OPENING_RAISE, 60), raise(PreflopRaiseType.FOUR_BET, 200)), 100.0, 100.0 | VPIP=100.0%; PFR=100.0%; gap=0.0 points; hands=1 | Yes |
| PS-V11 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "multiple calls count once", actions(pre(ResolvedActionType.CALL, 20), pre(ResolvedActionType.CALL, 40)), 100.0, 0.0 | VPIP=100.0%; PFR=0.0%; gap=100.0 points; hands=1 | Yes |
| PS-V12 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "later fold retains voluntary investment", actions(pre(ResolvedActionType.CALL, 20), pre(ResolvedActionType.FOLD, 0)), 100.0, 0.0 | VPIP=100.0%; PFR=0.0%; gap=100.0 points; hands=1 | Yes |
| PS-V13 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "later fold retains the raise", actions(raise(PreflopRaiseType.OPENING_RAISE, 60), pre(ResolvedActionType.FOLD, 0)), 100.0, 100.0 | VPIP=100.0%; PFR=100.0%; gap=0.0 points; hands=1 | Yes |
| PS-V14 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "folding without investment", actions(pre(ResolvedActionType.FOLD, 0)), 0.0, 0.0 | VPIP=0.0%; PFR=0.0%; gap=0.0 points; hands=1 | Yes |
| PS-V15 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "rejected raise is ignored", actions(rejectedRaise()), 0.0, 0.0 | VPIP=0.0%; PFR=0.0%; gap=0.0 points; hands=1 | Yes |
| PS-V16 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "all-in resolved as a call", actions(pre(ResolvedActionType.CALL, 100)), 100.0, 0.0 | VPIP=100.0%; PFR=0.0%; gap=100.0 points; hands=1 | Yes |
| PS-V17 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "all-in resolved as a raise", actions(raise(PreflopRaiseType.THREE_BET, 100)), 100.0, 100.0 | VPIP=100.0%; PFR=100.0%; gap=0.0 points; hands=1 | Yes |
| PS-V18 | beginHand(), recordAction(), completeHand(), VPIP/PFR/gap getters | One completed dealt-in hand: "stack-exhausting forced blind", actions(pre(ResolvedActionType.FORCED_POST, 100)), 0.0, 0.0 | VPIP=0.0%; PFR=0.0%; gap=0.0 points; hands=1 | Yes |
| PS-V19 | getVpipPercent(), getPfrPercent(), getVpipPfrGapPercentagePoints() | Four hands: fold, call, raise, check; then two more folds | Initially VPIP=50, PFR=25, gap=25; later VPIP=100/3 without rounding | Yes |
| PS-Z01 | Constructor, getVpipPercent() | New instance; denominator is zero | OptionalDouble.empty() | Yes |
| PS-Z02 | Constructor, getPfrPercent() | New instance; denominator is zero | OptionalDouble.empty() | Yes |
| PS-Z03 | Constructor, getVpipPfrGapPercentagePoints() | New instance; denominator is zero | OptionalDouble.empty() | Yes |
| PS-Z04 | Constructor, getThreeBetPercent() | New instance; denominator is zero | OptionalDouble.empty() | Yes |
| PS-Z05 | Constructor, getAggressionFactor() | New instance; denominator is zero | OptionalDouble.empty() | Yes |
| PS-Z06 | Constructor, getAggressionFrequencyPercent() | New instance; denominator is zero | OptionalDouble.empty() | Yes |
| PS-Z07 | Constructor, getFlopCBetPercent() | New instance; denominator is zero | OptionalDouble.empty() | Yes |
| PS-Z08 | Constructor, getFoldToFlopCBetPercent() | New instance; denominator is zero | OptionalDouble.empty() | Yes |
| PS-Z09 | Constructor, getWtsdPercent() | New instance; denominator is zero | OptionalDouble.empty() | Yes |
| PS-Z10 | Constructor, getWonAtShowdownPercent() | New instance; denominator is zero | OptionalDouble.empty() | Yes |
