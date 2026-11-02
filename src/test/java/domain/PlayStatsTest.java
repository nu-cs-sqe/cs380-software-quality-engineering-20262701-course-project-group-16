package domain;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayStatsTest {

    @ParameterizedTest(name = "{0}: a new statistic is unavailable")
    @MethodSource("emptyStatistics")
    void startsWithUnavailableStatistics(String caseId,
            Function<PlayStats, OptionalDouble> statistic) {
        assertFalse(statistic.apply(new PlayStats()).isPresent(), caseId);
    }

    static Stream<Arguments> emptyStatistics() {
        return Stream.of(
                metric("PS-Z01", PlayStats::getVpipPercent),
                metric("PS-Z02", PlayStats::getPfrPercent),
                metric("PS-Z03", PlayStats::getVpipPfrGapPercentagePoints),
                metric("PS-Z04", PlayStats::getThreeBetPercent),
                metric("PS-Z05", PlayStats::getAggressionFactor),
                metric("PS-Z06", PlayStats::getAggressionFrequencyPercent),
                metric("PS-Z07", PlayStats::getFlopCBetPercent),
                metric("PS-Z08", PlayStats::getFoldToFlopCBetPercent),
                metric("PS-Z09", PlayStats::getWtsdPercent),
                metric("PS-Z10", PlayStats::getWonAtShowdownPercent));
    }

    @Test
    @DisplayName("PS-L01: a new instance has played zero hands")
    void startsWithZeroHandsPlayed() {
        assertEquals(0L, new PlayStats().getHandsPlayed());
    }

    @Test
    @DisplayName("PS-L02: an unfinished hand does not affect lifetime statistics")
    void doesNotCountAnUnfinishedHand() {
        PlayStats stats = new PlayStats();
        List<Object> before = snapshot(stats);

        stats.beginHand("match-1/hand-1", true);

        assertEquals(before, snapshot(stats));
    }

    @Test
    @DisplayName("PS-L03: a completed dealt-in hand counts once")
    void countsACompletedDealtInHand() {
        PlayStats stats = new PlayStats();
        stats.beginHand("match-1/hand-1", true);

        stats.completeHand("match-1/hand-1", false, false, 0L, 0L);

        assertEquals(1L, stats.getHandsPlayed());
    }

    @Test
    @DisplayName("PS-L04: a completed hand without participation does not count")
    void doesNotCountAHandWithoutParticipation() {
        PlayStats stats = new PlayStats();
        List<Object> before = snapshot(stats);
        stats.beginHand("match-1/hand-1", false);

        stats.completeHand("match-1/hand-1", false, false, 0L, 0L);

        assertEquals(before, snapshot(stats));
    }

    @Test
    @DisplayName("PS-L05: starting a second active hand is rejected atomically")
    void rejectsASecondActiveHand() {
        PlayStats stats = new PlayStats();
        stats.beginHand("match-1/hand-1", true);
        List<Object> before = snapshot(stats);

        assertThrows(IllegalStateException.class,
                () -> stats.beginHand("match-1/hand-2", true));

        assertEquals(before, snapshot(stats));
        stats.completeHand("match-1/hand-1", false, false, 0L, 0L);
        assertEquals(1L, stats.getHandsPlayed());
    }

    @Test
    @DisplayName("PS-L06: completing without an active hand is rejected")
    void rejectsCompletionWithoutAnActiveHand() {
        PlayStats stats = new PlayStats();

        assertThrows(IllegalStateException.class,
                () -> stats.completeHand("match-1/hand-1", false, false, 0L, 0L));

        assertEquals(0L, stats.getHandsPlayed());
    }

    @Test
    @DisplayName("PS-L07: recording an action without an active hand is rejected")
    void rejectsActionWithoutAnActiveHand() {
        PlayStats stats = new PlayStats();

        assertThrows(IllegalStateException.class, () -> stats.recordAction(null));

        assertEquals(0L, stats.getHandsPlayed());
    }

    @Test
    @DisplayName("PS-L08: a mismatched completion ID is rejected without losing the hand")
    void rejectsAMismatchedCompletionId() {
        PlayStats stats = new PlayStats();
        stats.beginHand("match-1/hand-1", true);
        List<Object> before = snapshot(stats);

        assertThrows(IllegalArgumentException.class,
                () -> stats.completeHand("match-1/hand-2", false, false, 0L, 0L));

        assertEquals(before, snapshot(stats));
        stats.completeHand("match-1/hand-1", false, false, 0L, 0L);
        assertEquals(1L, stats.getHandsPlayed());
    }

    @Test
    @DisplayName("PS-L09: completing the same hand twice cannot change totals")
    void rejectsDoubleCompletion() {
        PlayStats stats = new PlayStats();
        stats.beginHand("match-1/hand-1", true);
        stats.completeHand("match-1/hand-1", false, false, 0L, 0L);
        List<Object> before = snapshot(stats);

        assertThrows(IllegalStateException.class,
                () -> stats.completeHand("match-1/hand-1", false, false, 0L, 0L));

        assertEquals(before, snapshot(stats));
    }

    @Test
    @DisplayName("PS-L10: a previously completed hand ID cannot be reused")
    void rejectsAReusedHandId() {
        PlayStats stats = new PlayStats();
        stats.beginHand("match-1/hand-1", true);
        stats.completeHand("match-1/hand-1", false, false, 0L, 0L);
        List<Object> before = snapshot(stats);

        assertThrows(IllegalStateException.class,
                () -> stats.beginHand("match-1/hand-1", true));

        assertEquals(before, snapshot(stats));
    }

    @ParameterizedTest(name = "{0}: invalid hand IDs are rejected")
    @MethodSource("invalidHandIds")
    void rejectsInvalidHandIds(String caseId, String handId) {
        PlayStats stats = new PlayStats();

        assertThrows(IllegalArgumentException.class,
                () -> stats.beginHand(handId, true), caseId);

        assertEquals(0L, stats.getHandsPlayed(), caseId);
    }

    static Stream<Arguments> invalidHandIds() {
        return Stream.of(
                Arguments.of("PS-L11", (String) null),
                Arguments.of("PS-L12", ""),
                Arguments.of("PS-L13", " "),
                Arguments.of("PS-L14", "\t"),
                Arguments.of("PS-L15", "\u2003"));
    }

    @Test
    @DisplayName("PS-L16: a null action does not corrupt the active hand")
    void rejectsANullActionDuringAnActiveHand() {
        PlayStats stats = new PlayStats();
        stats.beginHand("match-1/hand-1", true);
        List<Object> before = snapshot(stats);

        assertThrows(IllegalArgumentException.class, () -> stats.recordAction(null));

        assertEquals(before, snapshot(stats));
        stats.completeHand("match-1/hand-1", false, false, 0L, 0L);
        assertEquals(1L, stats.getHandsPlayed());
    }

    @Test
    @DisplayName("PS-L17: separate completed hand IDs count separately")
    void countsSeparateCompletedHands() {
        PlayStats stats = new PlayStats();
        stats.beginHand("match-1/hand-1", true);
        stats.completeHand("match-1/hand-1", false, false, 0L, 0L);
        stats.beginHand("match-2/hand-1", true);
        stats.completeHand("match-2/hand-1", false, false, 0L, 0L);

        assertEquals(2L, stats.getHandsPlayed());
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("preflopCases")
    void classifiesPreflopInvestmentOncePerHand(String id, String description,
            StatsAction[] actions, double vpip, double pfr) {
        PlayStats stats = new PlayStats();
        playPreflop(stats, id, actions);

        assertEquals(1L, stats.getHandsPlayed(), id);
        assertAvailable(vpip, stats.getVpipPercent());
        assertAvailable(pfr, stats.getPfrPercent());
        assertAvailable(vpip - pfr, stats.getVpipPfrGapPercentagePoints());
    }

    static Stream<Arguments> preflopCases() {
        return Stream.of(
                Arguments.of("PS-V01", "forced small blind alone", actions(pre(ResolvedActionType.FORCED_POST, 10)), 0.0, 0.0),
                Arguments.of("PS-V02", "forced big blind alone", actions(pre(ResolvedActionType.FORCED_POST, 20)), 0.0, 0.0),
                Arguments.of("PS-V03", "forced ante alone", actions(pre(ResolvedActionType.FORCED_POST, 1)), 0.0, 0.0),
                Arguments.of("PS-V04", "checking the big blind", actions(pre(ResolvedActionType.FORCED_POST, 20), pre(ResolvedActionType.CHECK, 0)), 0.0, 0.0),
                Arguments.of("PS-V05", "completing a 10 small blind to 20", actions(pre(ResolvedActionType.FORCED_POST, 10), pre(ResolvedActionType.CALL, 10)), 100.0, 0.0),
                Arguments.of("PS-V06", "calling a raise from the big blind", actions(pre(ResolvedActionType.FORCED_POST, 20), pre(ResolvedActionType.CALL, 40)), 100.0, 0.0),
                Arguments.of("PS-V07", "ordinary preflop call", actions(pre(ResolvedActionType.CALL, 20)), 100.0, 0.0),
                Arguments.of("PS-V08", "opening raise", actions(raise(PreflopRaiseType.OPENING_RAISE, 60)), 100.0, 100.0),
                Arguments.of("PS-V09", "call then raise counts once", actions(pre(ResolvedActionType.CALL, 20), raise(PreflopRaiseType.THREE_BET, 80)), 100.0, 100.0),
                Arguments.of("PS-V10", "multiple raises count once", actions(raise(PreflopRaiseType.OPENING_RAISE, 60), raise(PreflopRaiseType.FOUR_BET, 200)), 100.0, 100.0),
                Arguments.of("PS-V11", "multiple calls count once", actions(pre(ResolvedActionType.CALL, 20), pre(ResolvedActionType.CALL, 40)), 100.0, 0.0),
                Arguments.of("PS-V12", "later fold retains voluntary investment", actions(pre(ResolvedActionType.CALL, 20), pre(ResolvedActionType.FOLD, 0)), 100.0, 0.0),
                Arguments.of("PS-V13", "later fold retains the raise", actions(raise(PreflopRaiseType.OPENING_RAISE, 60), pre(ResolvedActionType.FOLD, 0)), 100.0, 100.0),
                Arguments.of("PS-V14", "folding without investment", actions(pre(ResolvedActionType.FOLD, 0)), 0.0, 0.0),
                Arguments.of("PS-V15", "rejected raise is ignored", actions(rejectedRaise()), 0.0, 0.0),
                Arguments.of("PS-V16", "all-in resolved as a call", actions(pre(ResolvedActionType.CALL, 100)), 100.0, 0.0),
                Arguments.of("PS-V17", "all-in resolved as a raise", actions(raise(PreflopRaiseType.THREE_BET, 100)), 100.0, 100.0),
                Arguments.of("PS-V18", "stack-exhausting forced blind", actions(pre(ResolvedActionType.FORCED_POST, 100)), 0.0, 0.0));
    }

    @Test
    @DisplayName("PS-V19: mixed hands use hand denominators and a percentage-point gap")
    void aggregatesPreflopHandRatesWithoutRounding() {
        PlayStats stats = new PlayStats();
        playPreflop(stats, "1", pre(ResolvedActionType.FOLD, 0));
        playPreflop(stats, "2", pre(ResolvedActionType.CALL, 20));
        playPreflop(stats, "3", raise(PreflopRaiseType.OPENING_RAISE, 60));
        playPreflop(stats, "4", pre(ResolvedActionType.CHECK, 0));

        assertEquals(4L, stats.getHandsPlayed());
        assertAvailable(50.0, stats.getVpipPercent());
        assertAvailable(25.0, stats.getPfrPercent());
        assertAvailable(25.0, stats.getVpipPfrGapPercentagePoints());
        playPreflop(stats, "5", pre(ResolvedActionType.FOLD, 0));
        playPreflop(stats, "6", pre(ResolvedActionType.FOLD, 0));
        assertAvailable(100.0 / 3.0, stats.getVpipPercent());
    }

    @Test
    @DisplayName("PS-T01: opening raises create no three-bet denominator")
    void doesNotTreatAnOpeningRaiseAsAThreeBet() {
        PlayStats stats = new PlayStats();
        playPreflop(stats, "1", raise(PreflopRaiseType.OPENING_RAISE, 100));
        assertFalse(stats.getThreeBetPercent().isPresent());
    }

    @ParameterizedTest(name = "{0}: declining an eligible three-bet is a zero percent opportunity")
    @MethodSource("declinedThreeBets")
    void countsDeclinedThreeBetOpportunities(String id, ResolvedActionType response,
            long chips) {
        PlayStats stats = new PlayStats();
        playPreflop(stats, id, opportunityResponse(response, chips));
        assertAvailable(0.0, stats.getThreeBetPercent());
    }

    static Stream<Arguments> declinedThreeBets() {
        return Stream.of(
                Arguments.of("PS-T02", ResolvedActionType.FOLD, 0L),
                Arguments.of("PS-T03", ResolvedActionType.CALL, 40L));
    }

    @Test
    @DisplayName("PS-T04: an explicitly resolved three-bet qualifies")
    void countsAnExplicitThreeBet() {
        PlayStats stats = new PlayStats();
        playPreflop(stats, "1", raise(PreflopRaiseType.THREE_BET, 1));
        assertAvailable(100.0, stats.getThreeBetPercent());
    }

    @Test
    @DisplayName("PS-T05: a later four-bet does not undo an earlier declined opportunity")
    void distinguishesFourBetsAfterAnOpportunity() {
        PlayStats stats = new PlayStats();
        playPreflop(stats, "1", opportunityResponse(ResolvedActionType.CALL, 40),
                raise(PreflopRaiseType.FOUR_BET, 100_000));
        assertAvailable(0.0, stats.getThreeBetPercent());
        assertAvailable(100.0, stats.getPfrPercent());
    }

    @Test
    @DisplayName("PS-T06: a four-bet alone is not a three-bet opportunity")
    void doesNotInferAThreeBetFromBetSize() {
        PlayStats stats = new PlayStats();
        playPreflop(stats, "1", raise(PreflopRaiseType.FOUR_BET, 100_000));
        assertFalse(stats.getThreeBetPercent().isPresent());
        playPreflop(stats, "2", raise(PreflopRaiseType.THREE_BET, 1));
        assertAvailable(100.0, stats.getThreeBetPercent());
    }

    @Test
    @DisplayName("PS-T07: eligible decisions contribute one opportunity hand")
    void countsOpportunityHandsRatherThanDecisions() {
        PlayStats stats = new PlayStats();
        playPreflop(stats, "1", opportunityResponse(ResolvedActionType.CALL, 10),
                opportunityResponse(ResolvedActionType.CALL, 20),
                opportunityResponse(ResolvedActionType.FOLD, 0));
        playPreflop(stats, "2", raise(PreflopRaiseType.THREE_BET, 1));
        assertAvailable(50.0, stats.getThreeBetPercent());
    }

    @Test
    @DisplayName("PS-T08: rejected opportunity observations do not create a denominator")
    void ignoresRejectedOpportunities() {
        PlayStats stats = new PlayStats();
        StatsAction rejected = new StatsAction(false, BettingStreet.PREFLOP,
                ResolvedActionType.CALL, 20,
                new StatsContext(PreflopRaiseType.NONE, true, false, false));
        playPreflop(stats, "1", rejected);
        assertAvailable(0.0, stats.getVpipPercent());
        assertFalse(stats.getThreeBetPercent().isPresent());
    }

    @Test
    @DisplayName("PS-A01: accepted actions after a fold are rejected without contaminating the hand")
    void rejectsAcceptedActionsAfterFold() {
        PlayStats stats = new PlayStats();
        stats.beginHand("1", true);
        stats.recordAction(pre(ResolvedActionType.FOLD, 0));
        List<Object> before = snapshot(stats);
        assertThrows(IllegalArgumentException.class,
                () -> stats.recordAction(pre(ResolvedActionType.CALL, 20)));
        assertEquals(before, snapshot(stats));
        stats.recordAction(rejectedRaise());
        stats.completeHand("1", false, false, 0, 0);
        assertAvailable(0.0, stats.getVpipPercent());
    }

    @Test
    @DisplayName("PS-A02: nonparticipants cannot contribute accepted actions")
    void rejectsAcceptedActionsWhenNotDealtIn() {
        PlayStats stats = new PlayStats();
        stats.beginHand("1", false);
        assertThrows(IllegalArgumentException.class,
                () -> stats.recordAction(pre(ResolvedActionType.CALL, 20)));
        stats.recordAction(rejectedRaise());
        stats.completeHand("1", false, false, 0, 0);
        assertEquals(0L, stats.getHandsPlayed());
        assertFalse(stats.getVpipPercent().isPresent());
    }

    @Test
    @DisplayName("PS-A03: accepted streets cannot move backwards")
    void rejectsBackwardAcceptedStreets() {
        PlayStats stats = new PlayStats();
        stats.beginHand("1", true);
        stats.recordAction(post(BettingStreet.TURN, ResolvedActionType.CALL, 20));
        List<Object> before = snapshot(stats);
        assertThrows(IllegalArgumentException.class,
                () -> stats.recordAction(pre(ResolvedActionType.CALL, 20)));
        assertEquals(before, snapshot(stats));
        stats.completeHand("1", true, false, 0, 0);
        assertAvailable(0.0, stats.getVpipPercent());
    }

    @Test
    @DisplayName("PS-A04: rejected actions do not advance or rewind the accepted street")
    void ignoresTheStreetOfRejectedActions() {
        PlayStats stats = new PlayStats();
        stats.beginHand("1", true);
        stats.recordAction(post(BettingStreet.TURN, ResolvedActionType.CALL, 20));
        stats.recordAction(rejectedRaise());
        stats.completeHand("1", true, false, 0, 0);
        assertAvailable(0.0, stats.getVpipPercent());
    }

    @Test
    @DisplayName("PS-A05: actions after completion cannot change aggregates")
    void rejectsActionsAfterCompletion() {
        PlayStats stats = new PlayStats();
        playPreflop(stats, "1", pre(ResolvedActionType.CALL, 20));
        List<Object> before = snapshot(stats);
        assertThrows(IllegalStateException.class,
                () -> stats.recordAction(raise(PreflopRaiseType.OPENING_RAISE, 100)));
        assertEquals(before, snapshot(stats));
    }

    @Test
    @DisplayName("PS-A06: invalid engine context does not contaminate a valid pending hand")
    void rejectsInvalidContextWithoutChangingTheHand() {
        PlayStats stats = new PlayStats();
        stats.beginHand("1", true);
        stats.recordAction(pre(ResolvedActionType.CALL, 20));
        List<Object> before = snapshot(stats);
        assertThrows(IllegalArgumentException.class,
                () -> new StatsContext(PreflopRaiseType.THREE_BET, false, false, false));
        assertEquals(before, snapshot(stats));
        stats.completeHand("1", false, false, 0, 0);
        assertAvailable(100.0, stats.getVpipPercent());
        assertAvailable(0.0, stats.getPfrPercent());
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("aggressionCases")
    void distinguishesAggressionRatioAndFrequency(String id, String description,
            StatsAction[] actions, Double factor, Double frequency) {
        PlayStats stats = new PlayStats();
        playPostflop(stats, id, actions);
        assertOptional(factor, stats.getAggressionFactor());
        assertOptional(frequency, stats.getAggressionFrequencyPercent());
    }

    static Stream<Arguments> aggressionCases() {
        StatsAction bet = post(BettingStreet.FLOP, ResolvedActionType.BET, 20);
        StatsAction raise = post(BettingStreet.FLOP, ResolvedActionType.RAISE, 40);
        StatsAction call = post(BettingStreet.FLOP, ResolvedActionType.CALL, 20);
        StatsAction check = post(BettingStreet.FLOP, ResolvedActionType.CHECK, 0);
        StatsAction fold = post(BettingStreet.FLOP, ResolvedActionType.FOLD, 0);
        StatsAction rejected = new StatsAction(false, BettingStreet.FLOP,
                ResolvedActionType.RAISE, 40, noContext());
        return Stream.of(
                Arguments.of("PS-G01", "checks alone provide no denominator", actions(check), null, null),
                Arguments.of("PS-G02", "positive aggression with zero calls has unavailable AF", actions(bet), null, 100.0),
                Arguments.of("PS-G03", "call-only aggression is zero", actions(call), 0.0, 0.0),
                Arguments.of("PS-G04", "fold-only frequency is zero but AF unavailable", actions(fold), null, 0.0),
                Arguments.of("PS-G05", "one bet and one call", actions(bet, call), 1.0, 50.0),
                Arguments.of("PS-G06", "bets and raises are action counts", actions(bet, raise, call), 2.0, 200.0 / 3.0),
                Arguments.of("PS-G07", "preflop actions are excluded", actions(raise(PreflopRaiseType.OPENING_RAISE, 60), pre(ResolvedActionType.CALL, 20), bet, call, post(BettingStreet.TURN, ResolvedActionType.CALL, 20)), 0.5, 100.0 / 3.0),
                Arguments.of("PS-G08", "checks do not dilute frequency", actions(check, bet, post(BettingStreet.TURN, ResolvedActionType.CHECK, 0), post(BettingStreet.TURN, ResolvedActionType.CALL, 20), post(BettingStreet.RIVER, ResolvedActionType.CHECK, 0)), 1.0, 50.0),
                Arguments.of("PS-G09", "all-in resolved as a postflop call", actions(post(BettingStreet.FLOP, ResolvedActionType.CALL, 100)), 0.0, 0.0),
                Arguments.of("PS-G10", "all-in resolved as a postflop raise", actions(post(BettingStreet.FLOP, ResolvedActionType.RAISE, 100)), null, 100.0),
                Arguments.of("PS-G11", "rejected aggression is ignored", actions(rejected, call), 0.0, 0.0));
    }

    @Test
    @DisplayName("PS-G12: lifetime action counts are not averages of hand percentages")
    void aggregatesActionsAcrossHands() {
        PlayStats stats = new PlayStats();
        playPostflop(stats, "1", post(BettingStreet.FLOP, ResolvedActionType.BET, 20),
                post(BettingStreet.FLOP, ResolvedActionType.RAISE, 40),
                post(BettingStreet.FLOP, ResolvedActionType.RAISE, 80),
                post(BettingStreet.FLOP, ResolvedActionType.CALL, 80));
        playPostflop(stats, "2", post(BettingStreet.FLOP, ResolvedActionType.CALL, 20));
        assertAvailable(1.5, stats.getAggressionFactor());
        assertAvailable(60.0, stats.getAggressionFrequencyPercent());
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("cBetCases")
    void usesExplicitFlopCBetEligibility(String id, String description,
            StatsAction[] actions, Double expected) {
        PlayStats stats = new PlayStats();
        playPostflop(stats, id, actions);
        assertOptional(expected, stats.getFlopCBetPercent());
    }

    static Stream<Arguments> cBetCases() {
        StatsAction open = raise(PreflopRaiseType.OPENING_RAISE, 60);
        StatsAction rejectedCBet = new StatsAction(false, BettingStreet.FLOP,
                ResolvedActionType.BET, 20, new StatsContext(PreflopRaiseType.NONE,
                        false, true, false));
        return Stream.of(
                Arguments.of("PS-C01", "eligible opening flop bet is a c-bet", actions(open, cBetDecision(ResolvedActionType.BET, 20)), 100.0),
                Arguments.of("PS-C02", "checking an eligible opportunity counts as declined", actions(open, cBetDecision(ResolvedActionType.CHECK, 0)), 0.0),
                Arguments.of("PS-C03", "facing another opening bet is not an opening opportunity", actions(open, post(BettingStreet.FLOP, ResolvedActionType.CALL, 20)), null),
                Arguments.of("PS-C04", "all-in before the flop has no betting opportunity", actions(open), null),
                Arguments.of("PS-C05", "an ordinary flop bet is not automatically a c-bet", actions(post(BettingStreet.FLOP, ResolvedActionType.BET, 20)), null),
                Arguments.of("PS-C06", "rejected c-bet followed by check is declined", actions(open, rejectedCBet, cBetDecision(ResolvedActionType.CHECK, 0)), 0.0),
                Arguments.of("PS-C07", "eligible fold declines the opening bet", actions(open, cBetDecision(ResolvedActionType.FOLD, 0)), 0.0));
    }

    @Test
    @DisplayName("PS-C08: hands without c-bet eligibility do not dilute its percentage")
    void countsOnlyEligibleCBetHands() {
        PlayStats stats = new PlayStats();
        playPostflop(stats, "1", raise(PreflopRaiseType.OPENING_RAISE, 60),
                cBetDecision(ResolvedActionType.BET, 20));
        playPostflop(stats, "2", raise(PreflopRaiseType.OPENING_RAISE, 60),
                cBetDecision(ResolvedActionType.CHECK, 0));
        playPostflop(stats, "3", post(BettingStreet.FLOP, ResolvedActionType.CALL, 20));
        assertAvailable(50.0, stats.getFlopCBetPercent());
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("facedCBetCases")
    void countsResponsesToActualFlopCBets(String id, String description,
            StatsAction[] actions, Double expected) {
        PlayStats stats = new PlayStats();
        playPostflop(stats, id, actions);
        assertOptional(expected, stats.getFoldToFlopCBetPercent());
    }

    static Stream<Arguments> facedCBetCases() {
        StatsAction rejectedFold = new StatsAction(false, BettingStreet.FLOP,
                ResolvedActionType.FOLD, 0, new StatsContext(PreflopRaiseType.NONE,
                        false, false, true));
        return Stream.of(
                Arguments.of("PS-F01", "fold to an eligible actual c-bet", actions(facedCBetDecision(ResolvedActionType.FOLD, 0)), 100.0),
                Arguments.of("PS-F02", "calling an eligible c-bet is not folding", actions(facedCBetDecision(ResolvedActionType.CALL, 20)), 0.0),
                Arguments.of("PS-F03", "raising an eligible c-bet is not folding", actions(facedCBetDecision(ResolvedActionType.RAISE, 60)), 0.0),
                Arguments.of("PS-F04", "ordinary folds create no c-bet opportunity", actions(post(BettingStreet.FLOP, ResolvedActionType.FOLD, 0)), null),
                Arguments.of("PS-F05", "all-in player has no response decision", actions(pre(ResolvedActionType.CALL, 100)), null),
                Arguments.of("PS-F06", "rejected fold followed by call is not a fold", actions(rejectedFold, facedCBetDecision(ResolvedActionType.CALL, 20)), 0.0));
    }

    @Test
    @DisplayName("PS-F07: only eligible faced-c-bet hands form the denominator")
    void usesEligibleFacedCBetHandDenominators() {
        PlayStats stats = new PlayStats();
        playPostflop(stats, "1", facedCBetDecision(ResolvedActionType.FOLD, 0));
        playPostflop(stats, "2", facedCBetDecision(ResolvedActionType.CALL, 20));
        playPostflop(stats, "3", post(BettingStreet.FLOP, ResolvedActionType.FOLD, 0));
        assertAvailable(50.0, stats.getFoldToFlopCBetPercent());
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("showdownCases")
    void separatesFlopParticipationShowdownAndPayout(String id, String description,
            StatsAction[] actions, boolean sawFlop, boolean showdown, long payout,
            long refund, Double wtsd, Double wonAtShowdown) {
        PlayStats stats = new PlayStats();
        stats.beginHand(id, true);
        for (StatsAction action : actions) {
            stats.recordAction(action);
        }
        stats.completeHand(id, sawFlop, showdown, payout, refund);
        assertEquals(1L, stats.getHandsPlayed());
        assertOptional(wtsd, stats.getWtsdPercent());
        assertOptional(wonAtShowdown, stats.getWonAtShowdownPercent());
    }

    static Stream<Arguments> showdownCases() {
        StatsAction check = post(BettingStreet.FLOP, ResolvedActionType.CHECK, 0);
        StatsAction rejectedFold = new StatsAction(false, BettingStreet.FLOP,
                ResolvedActionType.FOLD, 0,
                new StatsContext(PreflopRaiseType.NONE, false, false, true));
        return Stream.of(
                Arguments.of("PS-S01", "folding after seeing the flop is not showdown", actions(post(BettingStreet.FLOP, ResolvedActionType.FOLD, 0)), true, false, 0L, 0L, 0.0, null),
                Arguments.of("PS-S02", "showdown without payout", actions(check), true, true, 0L, 0L, 100.0, 0.0),
                Arguments.of("PS-S03", "positive contested showdown payout", actions(check), true, true, 1L, 0L, 100.0, 100.0),
                Arguments.of("PS-S04", "split-pot payout qualifies", actions(post(BettingStreet.FLOP, ResolvedActionType.CALL, 10)), true, true, 5L, 0L, 100.0, 100.0),
                Arguments.of("PS-S05", "side-pot payout qualifies", actions(post(BettingStreet.TURN, ResolvedActionType.CALL, 10)), true, true, 7L, 0L, 100.0, 100.0),
                Arguments.of("PS-S06", "positive payout with negative net result qualifies", actions(pre(ResolvedActionType.CALL, 100)), true, true, 10L, 0L, 100.0, 100.0),
                Arguments.of("PS-S07", "combined payouts from multiple pots qualify once", actions(check), true, true, 30L, 0L, 100.0, 100.0),
                Arguments.of("PS-S08", "uncalled-bet refund alone is not a showdown payout", actions(check), true, true, 0L, 100L, 100.0, 0.0),
                Arguments.of("PS-S09", "winning when others fold is not showdown", actions(check), true, false, 0L, 10L, 0.0, null),
                Arguments.of("PS-S10", "an all-in participant sees the flop", actions(pre(ResolvedActionType.CALL, 100)), true, true, 0L, 0L, 100.0, 0.0),
                Arguments.of("PS-S11", "a preflop fold never saw the flop", actions(pre(ResolvedActionType.FOLD, 0)), false, false, 0L, 0L, null, null),
                Arguments.of("PS-S15", "an all-in forced blind still participates in showdown", actions(pre(ResolvedActionType.FORCED_POST, 20)), true, true, 0L, 0L, 100.0, 0.0),
                Arguments.of("PS-S16", "a rejected fold does not end showdown participation", actions(pre(ResolvedActionType.CALL, 100), rejectedFold), true, true, 5L, 0L, 100.0, 100.0),
                Arguments.of("PS-S17", "largest positive payout counts as one payout hand", actions(check), true, true, Long.MAX_VALUE, 0L, 100.0, 100.0),
                Arguments.of("PS-S18", "largest refund alone still does not qualify", actions(check), true, true, 0L, Long.MAX_VALUE, 100.0, 0.0));
    }

    @Test
    @DisplayName("PS-S12: later nonparticipant hands do not change existing statistics")
    void doesNotCountHandsAfterElimination() {
        PlayStats stats = seededStats();
        List<Object> before = snapshot(stats);
        stats.beginHand("eliminated-hand", false);
        stats.completeHand("eliminated-hand", false, false, 0, 0);
        assertEquals(before, snapshot(stats));
    }

    @Test
    @DisplayName("PS-S13: WTSD and W$SD use different hand denominators")
    void usesFlopAndShowdownDenominators() {
        PlayStats stats = new PlayStats();
        playPreflop(stats, "1", pre(ResolvedActionType.FOLD, 0));
        playPostflop(stats, "2", post(BettingStreet.FLOP, ResolvedActionType.FOLD, 0));
        playPostflop(stats, "3", post(BettingStreet.FLOP, ResolvedActionType.CHECK, 0));
        stats.beginHand("4", true);
        stats.completeHand("4", true, true, 0, 0);
        stats.beginHand("5", true);
        stats.completeHand("5", true, true, 30, 0);
        assertEquals(5L, stats.getHandsPlayed());
        assertAvailable(50.0, stats.getWtsdPercent());
        assertAvailable(50.0, stats.getWonAtShowdownPercent());
    }

    @Test
    @DisplayName("PS-S14: percentages retain fractional precision")
    void leavesRoundingToPresentation() {
        PlayStats stats = new PlayStats();
        for (int hand = 0; hand < 3; hand++) {
            String id = "showdown-" + hand;
            stats.beginHand(id, true);
            stats.completeHand(id, true, true, hand == 0 ? 1 : 0, 0);
        }
        playPostflop(stats, "no-showdown", post(BettingStreet.FLOP, ResolvedActionType.CHECK, 0));
        assertAvailable(75.0, stats.getWtsdPercent());
        assertAvailable(100.0 / 3.0, stats.getWonAtShowdownPercent());
    }

    @ParameterizedTest(name = "{0}: rejects inconsistent completion atomically")
    @MethodSource("invalidCompletions")
    void rejectsInvalidCompletionsWithoutChangingExistingTotals(String id,
            boolean dealtIn, StatsAction[] actions, boolean sawFlop, boolean showdown,
            long payout, long refund) {
        PlayStats stats = seededStats();
        stats.beginHand(id, dealtIn);
        for (StatsAction action : actions) {
            stats.recordAction(action);
        }
        List<Object> before = snapshot(stats);

        assertThrows(IllegalArgumentException.class,
                () -> stats.completeHand(id, sawFlop, showdown, payout, refund), id);

        assertEquals(before, snapshot(stats), id);
        boolean actualSawFlop = false;
        for (StatsAction action : actions) {
            actualSawFlop |= action.isAccepted() && action.getStreet() != BettingStreet.PREFLOP;
        }
        stats.completeHand(id, actualSawFlop, false, 0, 0);
        assertEquals(dealtIn ? 3L : 2L, stats.getHandsPlayed(), id);
    }

    static Stream<Arguments> invalidCompletions() {
        return Stream.of(
                Arguments.of("PS-I01", true, actions(), false, false, -1L, 0L),
                Arguments.of("PS-I02", true, actions(), false, false, Long.MIN_VALUE, 0L),
                Arguments.of("PS-I03", true, actions(), false, false, 0L, -1L),
                Arguments.of("PS-I04", true, actions(), false, false, 0L, Long.MIN_VALUE),
                Arguments.of("PS-I05", true, actions(), false, true, 0L, 0L),
                Arguments.of("PS-I06", true, actions(), true, false, 1L, 0L),
                Arguments.of("PS-I07", true, actions(), false, false, 1L, 0L),
                Arguments.of("PS-I08", false, actions(), true, false, 0L, 0L),
                Arguments.of("PS-I09", false, actions(), true, true, 0L, 0L),
                Arguments.of("PS-I10", false, actions(), true, true, 1L, 0L),
                Arguments.of("PS-I11", false, actions(), false, false, 0L, 1L),
                Arguments.of("PS-I12", true, actions(post(BettingStreet.FLOP, ResolvedActionType.CHECK, 0)), false, false, 0L, 0L),
                Arguments.of("PS-I13", true, actions(pre(ResolvedActionType.FOLD, 0)), true, false, 0L, 0L),
                Arguments.of("PS-I14", true, actions(post(BettingStreet.FLOP, ResolvedActionType.FOLD, 0)), true, true, 0L, 0L));
    }

    @ParameterizedTest(name = "{0}: invalid completion IDs leave a populated instance unchanged")
    @MethodSource("invalidCompletionIds")
    void rejectsInvalidCompletionIdsWithExistingStatistics(String id, String badId) {
        PlayStats stats = seededStats();
        stats.beginHand("active", true);
        List<Object> before = snapshot(stats);
        assertThrows(IllegalArgumentException.class,
                () -> stats.completeHand(badId, false, false, 0, 0), id);
        assertEquals(before, snapshot(stats), id);
        stats.completeHand("active", false, false, 0, 0);
        assertEquals(3L, stats.getHandsPlayed());
    }

    static Stream<Arguments> invalidCompletionIds() {
        return Stream.of(
                Arguments.of("PS-I15", (String) null),
                Arguments.of("PS-I16", ""),
                Arguments.of("PS-I17", "another-hand"));
    }

    @Test
    @DisplayName("PS-D01: a c-bettor cannot also face another actual c-bet in the same hand")
    void rejectsFacingACBetAfterOpeningOne() {
        PlayStats stats = new PlayStats();
        stats.beginHand("1", true);
        stats.recordAction(cBetDecision(ResolvedActionType.BET, 20));
        assertThrows(IllegalArgumentException.class,
                () -> stats.recordAction(facedCBetDecision(ResolvedActionType.CALL, 20)));
        stats.completeHand("1", true, false, 0, 0);
        assertAvailable(100.0, stats.getFlopCBetPercent());
        assertFalse(stats.getFoldToFlopCBetPercent().isPresent());
        assertFalse(stats.getAggressionFactor().isPresent());
    }

    @Test
    @DisplayName("PS-D02: facing a c-bet cannot become an opening opportunity later")
    void rejectsOpeningACBetAfterFacingOne() {
        PlayStats stats = new PlayStats();
        stats.beginHand("1", true);
        stats.recordAction(facedCBetDecision(ResolvedActionType.CALL, 20));
        assertThrows(IllegalArgumentException.class,
                () -> stats.recordAction(cBetDecision(ResolvedActionType.BET, 20)));
        stats.completeHand("1", true, false, 0, 0);
        assertFalse(stats.getFlopCBetPercent().isPresent());
        assertAvailable(0.0, stats.getFoldToFlopCBetPercent());
        assertAvailable(0.0, stats.getAggressionFactor());
    }

    @Test
    @DisplayName("PS-D03: a rejected flop bet creates no c-bet denominator or aggression")
    void ignoresARejectedOnlyCBetOpportunity() {
        PlayStats stats = new PlayStats();
        StatsAction rejected = new StatsAction(false, BettingStreet.FLOP,
                ResolvedActionType.BET, 20, new StatsContext(PreflopRaiseType.NONE,
                        false, true, false));
        playPostflop(stats, "1", rejected);
        assertFalse(stats.getFlopCBetPercent().isPresent());
        assertFalse(stats.getAggressionFrequencyPercent().isPresent());
    }

    @Test
    @DisplayName("PS-D04: a rejected response creates no faced-c-bet denominator")
    void ignoresARejectedOnlyFacedCBetOpportunity() {
        PlayStats stats = new PlayStats();
        StatsAction rejected = new StatsAction(false, BettingStreet.FLOP,
                ResolvedActionType.FOLD, 0, new StatsContext(PreflopRaiseType.NONE,
                        false, false, true));
        playPostflop(stats, "1", rejected);
        assertFalse(stats.getFoldToFlopCBetPercent().isPresent());
    }

    @Test
    @DisplayName("PS-D05: completed nonparticipant IDs cannot be replayed as participating hands")
    void rejectsReplayingANonparticipantHand() {
        PlayStats stats = new PlayStats();
        stats.beginHand("1", false);
        stats.completeHand("1", false, false, 0, 0);
        assertThrows(IllegalStateException.class, () -> stats.beginHand("1", true));
        assertEquals(0L, stats.getHandsPlayed());
    }

    @Test
    @DisplayName("PS-D06: accepted pending actions remain invisible until valid completion")
    void doesNotPublishPendingActions() {
        PlayStats stats = seededStats();
        List<Object> before = snapshot(stats);
        stats.beginHand("pending", true);
        stats.recordAction(raise(PreflopRaiseType.THREE_BET, 100));
        stats.recordAction(cBetDecision(ResolvedActionType.BET, 20));
        assertEquals(before, snapshot(stats));
        stats.completeHand("pending", true, false, 0, 0);
        assertEquals(3L, stats.getHandsPlayed());
    }

    @Test
    @DisplayName("PS-D07: rejected constructor input cannot corrupt populated statistics")
    void rejectsBadActionDataWithoutChangingStatistics() {
        PlayStats stats = seededStats();
        stats.beginHand("pending", true);
        List<Object> before = snapshot(stats);
        assertThrows(IllegalArgumentException.class,
                () -> stats.recordAction(new StatsAction(true, BettingStreet.PREFLOP,
                        ResolvedActionType.RAISE, 0, noContext())));
        assertEquals(before, snapshot(stats));
        stats.completeHand("pending", false, false, 0, 0);
        assertEquals(3L, stats.getHandsPlayed());
    }

    private static PlayStats seededStats() {
        PlayStats stats = new PlayStats();
        stats.beginHand("seed-1", true);
        stats.recordAction(raise(PreflopRaiseType.THREE_BET, 60));
        stats.recordAction(cBetDecision(ResolvedActionType.BET, 20));
        stats.recordAction(post(BettingStreet.FLOP, ResolvedActionType.CALL, 20));
        stats.completeHand("seed-1", true, true, 10, 0);
        stats.beginHand("seed-2", true);
        stats.recordAction(facedCBetDecision(ResolvedActionType.CALL, 20));
        stats.completeHand("seed-2", true, true, 0, 0);
        return stats;
    }

    private static StatsAction cBetDecision(ResolvedActionType type, long chips) {
        return new StatsAction(true, BettingStreet.FLOP, type, chips,
                new StatsContext(PreflopRaiseType.NONE, false, true, false));
    }

    private static StatsAction facedCBetDecision(ResolvedActionType type, long chips) {
        return new StatsAction(true, BettingStreet.FLOP, type, chips,
                new StatsContext(PreflopRaiseType.NONE, false, false, true));
    }

    private static void playPostflop(PlayStats stats, String id, StatsAction... actions) {
        stats.beginHand(id, true);
        for (StatsAction action : actions) {
            stats.recordAction(action);
        }
        stats.completeHand(id, true, false, 0, 0);
    }

    private static void assertOptional(Double expected, OptionalDouble actual) {
        if (expected == null) {
            assertFalse(actual.isPresent());
        } else {
            assertAvailable(expected, actual);
        }
    }

    private static StatsAction[] actions(StatsAction... actions) {
        return actions;
    }

    private static StatsContext noContext() {
        return new StatsContext(PreflopRaiseType.NONE, false, false, false);
    }

    private static StatsAction pre(ResolvedActionType type, long chips) {
        return new StatsAction(true, BettingStreet.PREFLOP, type, chips, noContext());
    }

    private static StatsAction post(BettingStreet street, ResolvedActionType type, long chips) {
        return new StatsAction(true, street, type, chips, noContext());
    }

    private static StatsAction raise(PreflopRaiseType type, long chips) {
        StatsContext context = new StatsContext(type, type == PreflopRaiseType.THREE_BET,
                false, false);
        return new StatsAction(true, BettingStreet.PREFLOP, ResolvedActionType.RAISE,
                chips, context);
    }

    private static StatsAction opportunityResponse(ResolvedActionType type, long chips) {
        return new StatsAction(true, BettingStreet.PREFLOP, type, chips,
                new StatsContext(PreflopRaiseType.NONE, true, false, false));
    }

    private static StatsAction rejectedRaise() {
        return new StatsAction(false, BettingStreet.PREFLOP, ResolvedActionType.RAISE, 100,
                new StatsContext(PreflopRaiseType.OPENING_RAISE, false, false, false));
    }

    private static void playPreflop(PlayStats stats, String id, StatsAction... actions) {
        stats.beginHand(id, true);
        for (StatsAction action : actions) {
            stats.recordAction(action);
        }
        stats.completeHand(id, false, false, 0, 0);
    }

    private static void assertAvailable(double expected, OptionalDouble actual) {
        assertTrue(actual.isPresent());
        assertTrue(Double.isFinite(actual.getAsDouble()));
        assertEquals(expected, actual.getAsDouble(), 1.0e-12);
    }

    private static Arguments metric(String id,
            Function<PlayStats, OptionalDouble> statistic) {
        return Arguments.of(id, statistic);
    }

    private static List<Object> snapshot(PlayStats stats) {
        return List.of(stats.getHandsPlayed(), stats.getVpipPercent(),
                stats.getPfrPercent(), stats.getVpipPfrGapPercentagePoints(),
                stats.getThreeBetPercent(), stats.getAggressionFactor(),
                stats.getAggressionFrequencyPercent(), stats.getFlopCBetPercent(),
                stats.getFoldToFlopCBetPercent(), stats.getWtsdPercent(),
                stats.getWonAtShowdownPercent());
    }
}
