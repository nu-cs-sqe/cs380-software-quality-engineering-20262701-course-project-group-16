package domain.model;

import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class BettingEngineAllInCallTest {
    private final BettingEngine engine = new BettingEngine();

    @ParameterizedTest(name = "{0}: seat {1}, stack {2}, adds {3}, remains {4}")
    @CsvSource({
        "BE-AIC01, 3,  1,  1, 0,  1, ALL_IN, true,   31",
        "BE-AIC02, 3, 19, 19, 0, 19, ALL_IN, true,   49",
        "BE-AIC03, 3, 20, 20, 0, 20, ALL_IN, true,   50",
        "BE-AIC04, 3, 21, 20, 1, 20, ACTIVE, false,  50",
        "BE-AIC05, 1,  1,  1, 0, 11, ALL_IN, true,  111",
        "BE-AIC06, 1,  9,  9, 0, 19, ALL_IN, true,  119",
        "BE-AIC07, 1, 10, 10, 0, 20, ALL_IN, true,  120",
        "BE-AIC08, 1, 11, 10, 1, 20, ACTIVE, false, 120"
    })
    void callUsesOnlyAvailableChipsAndRetainsItsBettingMeaning(String caseId, int seat,
            long available, long added, long remaining, long contribution,
            PlayerStatus status, boolean allIn, long pot) {
        GameState before = callFixture(seat, available).build();
        PlayerState original = before.getPlayerStates().get(seat);
        long previousContribution = seat == 1 ? 10 : 0;
        Set<String> pending = seat == 1 ? Set.of("p2") : Set.of("p0", "p1", "p2", "p4", "p5");

        BettingResult result = engine.apply(before, new ActionRequest("p" + seat, ActionType.CALL));
        GameState after = result.getState();
        PlayerState caller = after.getPlayerStates().get(seat);

        assertNotSame(before, after);
        assertEquals(remaining, caller.getPlayer().getChipStack(), caseId);
        assertEquals(contribution, caller.getRoundContribution());
        assertEquals(contribution, caller.getHandContribution());
        assertEquals(status, caller.getStatus());
        assertTrue(caller.isParticipating());
        assertEquals(!allIn, caller.canAct());
        assertEquals(original.getPlayer().getId(), caller.getPlayer().getId());
        assertEquals(original.getPlayer().getSeat(), caller.getPlayer().getSeat());
        assertEquals(original.getPlayer().getType(), caller.getPlayer().getType());
        assertEquals(original.getHoleCards(), caller.getHoleCards());
        assertEquals(pending, after.getBettingProgress().getPlayersToAct());
        assertEquals(pending, after.getBettingProgress().getPlayersAllowedToRaise());
        assertEquals(seat == 1 ? 2 : 4, after.getCurrentActorSeat().orElseThrow());
        assertEquals(20, after.getCurrentBet());
        assertEquals(20, after.getLastFullRaiseSize());
        assertEquals(pot, after.getPot());
        assertEquals(6000, after.getTotalChips());
        assertEquals(GamePhase.PREFLOP, after.getPhase());
        assertEquals(before.getHandId(), after.getHandId());
        assertEquals(before.getDealerSeat(), after.getDealerSeat());
        assertEquals(before.getSmallBlindSeat(), after.getSmallBlindSeat());
        assertEquals(before.getBigBlindSeat(), after.getBigBlindSeat());
        assertEquals(before.getCommunityCards(), after.getCommunityCards());
        assertEquals(before.getRemainingDeck(), after.getRemainingDeck());
        for (int other = 0; other < 6; other++) {
            if (other != seat) {
                assertSamePlayerValues(before.getPlayerStates().get(other), after.getPlayerStates().get(other));
            }
        }

        AcceptedAction accepted = result.getAcceptedAction();
        assertEquals(before.getHandId(), accepted.getHandId());
        assertEquals(original.getPlayer().getId(), accepted.getPlayerId());
        assertEquals(BettingStreet.PREFLOP, accepted.getStreet());
        assertEquals(ResolvedActionType.CALL, accepted.getType());
        assertEquals(added, accepted.getChipsAdded());
        assertEquals(contribution, accepted.getRoundContributionAfter());
        assertEquals(allIn, accepted.becameAllIn());
        assertEquals(PreflopRaiseType.NONE, accepted.getStatsContext().getPreflopRaiseType());
        assertFalse(accepted.getStatsContext().hasThreeBetOpportunity());
        assertFalse(accepted.getStatsContext().hasFlopCBetOpportunity());
        assertFalse(accepted.getStatsContext().hasFacedFlopCBetOpportunity());
        assertTrue(accepted.toStatsAction().isAccepted());
        assertEquals(ResolvedActionType.CALL, accepted.toStatsAction().getType());
        assertEquals(added, accepted.toStatsAction().getChipsAdded());

        assertEquals(available, original.getPlayer().getChipStack());
        assertEquals(PlayerStatus.ACTIVE, original.getStatus());
        assertEquals(previousContribution, original.getRoundContribution());
        assertEquals(previousContribution, original.getHandContribution());
        assertEquals(seat == 1 ? 110 : 30, before.getPot());
        assertEquals(seat, before.getCurrentActorSeat().orElseThrow());
        assertTrue(before.getBettingProgress().getPlayersToAct().contains("p" + seat));
        assertTrue(before.getBettingProgress().getPlayersAllowedToRaise().contains("p" + seat));
        assertEquals(6000, before.getTotalChips());
    }

    @ParameterizedTest(name = "BE-AIC09-{0}: reject an out-of-turn short/exact all-in caller")
    @ValueSource(longs = {19, 20})
    void anAllInCallStillRequiresTheCorrectTurn(long available) {
        FoundationFixtures.Fixture fixture = callFixture(3, 1000);
        redistributeStack(fixture, 4, available);
        GameState before = fixture.build();

        assertThrows(IllegalArgumentException.class,
                () -> engine.apply(before, new ActionRequest("p4", ActionType.CALL)));

        assertEquals(available, before.getPlayers().get(4).getChipStack());
        assertEquals(0, before.getPlayerStates().get(4).getRoundContribution());
        assertEquals(0, before.getCommittedChips(4));
        assertEquals(PlayerStatus.ACTIVE, before.getPlayerStates().get(4).getStatus());
        assertEquals(3, before.getCurrentActorSeat().orElseThrow());
        assertEquals(FoundationFixtures.allIds(), before.getBettingProgress().getPlayersToAct());
        assertEquals(FoundationFixtures.allIds(), before.getBettingProgress().getPlayersAllowedToRaise());
        assertEquals(30, before.getPot());
        assertEquals(6000, before.getTotalChips());
    }

    @ParameterizedTest(name = "BE-AIC10-{0}: a player who became all-in cannot act again")
    @ValueSource(longs = {19, 20})
    void anAllInPlayerCannotSubmitAnotherAction(long available) {
        GameState before = callFixture(3, available).build();
        GameState after = engine.apply(before, new ActionRequest("p3", ActionType.CALL)).getState();
        PlayerState allIn = after.getPlayerStates().get(3);

        assertThrows(IllegalArgumentException.class,
                () -> engine.apply(after, new ActionRequest("p3", ActionType.CALL)));

        assertEquals(0, allIn.getPlayer().getChipStack());
        assertEquals(PlayerStatus.ALL_IN, allIn.getStatus());
        assertEquals(available, allIn.getRoundContribution());
        assertEquals(available, allIn.getHandContribution());
        assertEquals(30 + available, after.getPot());
        assertEquals(4, after.getCurrentActorSeat().orElseThrow());
        assertEquals(Set.of("p0", "p1", "p2", "p4", "p5"), after.getBettingProgress().getPlayersToAct());
        assertEquals(Set.of("p0", "p1", "p2", "p4", "p5"), after.getBettingProgress().getPlayersAllowedToRaise());
        assertEquals(6000, after.getTotalChips());
    }

    private static FoundationFixtures.Fixture callFixture(int seat, long available) {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.deal();
        fixture.player(1, 990, PlayerStatus.ACTIVE, 10, 10);
        fixture.player(2, 980, PlayerStatus.ACTIVE, 20, 20);
        if (seat == 1) {
            for (int caller : new int[]{3, 4, 5, 0}) {
                fixture.player(caller, 980, PlayerStatus.ACTIVE, 20, 20);
            }
            fixture.actor = "p1";
            fixture.betting = new BettingProgress(20, 20, Set.of("p1", "p2"), Set.of("p1", "p2"));
        } else {
            fixture.betting = new BettingProgress(20, 20, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        }
        redistributeStack(fixture, seat, available);
        return fixture;
    }

    private static void redistributeStack(FoundationFixtures.Fixture fixture, int seat, long available) {
        PlayerState caller = fixture.players.get(seat);
        PlayerState donor = fixture.players.get(0);
        long transferred = caller.getPlayer().getChipStack() - available;
        fixture.player(seat, available, PlayerStatus.ACTIVE,
                caller.getRoundContribution(), caller.getHandContribution());
        fixture.player(0, donor.getPlayer().getChipStack() + transferred, donor.getStatus(),
                donor.getRoundContribution(), donor.getHandContribution());
    }

    private static void assertSamePlayerValues(PlayerState expected, PlayerState actual) {
        assertEquals(expected.getPlayer().getId(), actual.getPlayer().getId());
        assertEquals(expected.getPlayer().getSeat(), actual.getPlayer().getSeat());
        assertEquals(expected.getPlayer().getType(), actual.getPlayer().getType());
        assertEquals(expected.getPlayer().getChipStack(), actual.getPlayer().getChipStack());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getHoleCards(), actual.getHoleCards());
        assertEquals(expected.getRoundContribution(), actual.getRoundContribution());
        assertEquals(expected.getHandContribution(), actual.getHandContribution());
    }
}
