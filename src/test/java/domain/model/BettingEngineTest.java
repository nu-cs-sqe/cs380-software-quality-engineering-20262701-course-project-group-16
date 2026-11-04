package domain.model;

import application.GameInitializer;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BettingEngineTest {
    @Test
    @DisplayName("BE-CALL01: move the call delta and retain the original snapshot")
    void callMovesOnlyTheChipsOwedAndPreservesTheInput() {
        GameState before = initialState();
        ActionRequest request = callFromSeat(before, 3);

        GameState after = new BettingEngine().apply(before, request).getState();

        assertNotSame(before, after);
        PlayerState caller = after.getPlayerStates().get(3);
        assertEquals(980, caller.getPlayer().getChipStack());
        assertEquals(20, caller.getRoundContribution());
        assertEquals(20, caller.getHandContribution());
        assertEquals(PlayerStatus.ACTIVE, caller.getStatus());
        assertEquals(before.getPlayers().get(3).getId(), caller.getPlayer().getId());
        assertEquals(before.getPlayerStates().get(3).getHoleCards(), caller.getHoleCards());
        assertEquals(before.getRemainingDeck(), after.getRemainingDeck());
        assertEquals(before.getCommunityCards(), after.getCommunityCards());
        assertEquals(before.getConfig().getStartingChips(), after.getConfig().getStartingChips());
        assertEquals(before.getConfig().getSmallBlind(), after.getConfig().getSmallBlind());
        assertEquals(before.getConfig().getBigBlind(), after.getConfig().getBigBlind());
        assertEquals(before.getHandId(), after.getHandId());
        assertEquals(GamePhase.PREFLOP, after.getPhase());
        assertEquals(1000, before.getPlayers().get(3).getChipStack());
        assertEquals(0, before.getPlayerStates().get(3).getRoundContribution());
        assertEquals(0, before.getPlayerStates().get(3).getHandContribution());
        assertEquals(30, before.getPot());
    }

    @Test
    @DisplayName("BE-CALL02: advance to seat 4 with pot 50 and total chips 6000")
    void callAdvancesTheActorAndConservesAllChips() {
        GameState before = initialState();

        GameState after = new BettingEngine().apply(before, callFromSeat(before, 3)).getState();

        assertEquals(50, after.getPot());
        assertEquals(6000, after.getTotalChips());
        assertEquals(4, after.getCurrentActorSeat().orElseThrow());
        assertEquals(after.getPlayers().get(4).getId(), after.getCurrentActorId().orElseThrow());
        assertFalse(after.getBettingProgress().getPlayersToAct().contains(before.getPlayers().get(3).getId()));
        assertTrue(after.getBettingProgress().getPlayersToAct().contains(before.getPlayers().get(4).getId()));
        assertEquals(5, after.getBettingProgress().getPlayersToAct().size());
        assertEquals(20, after.getCurrentBet());
        assertEquals(20, after.getLastFullRaiseSize());
        for (int seat : new int[]{0, 1, 2, 4, 5}) {
            PlayerState original = before.getPlayerStates().get(seat);
            PlayerState retained = after.getPlayerStates().get(seat);
            assertEquals(original.getPlayer().getId(), retained.getPlayer().getId());
            assertEquals(original.getPlayer().getSeat(), retained.getPlayer().getSeat());
            assertEquals(original.getPlayer().getType(), retained.getPlayer().getType());
            assertEquals(original.getPlayer().getChipStack(), retained.getPlayer().getChipStack());
            assertEquals(original.getStatus(), retained.getStatus());
            assertEquals(original.getRoundContribution(), retained.getRoundContribution());
            assertEquals(original.getHandContribution(), retained.getHandContribution());
            assertEquals(original.getHoleCards(), retained.getHoleCards());
        }
        assertEquals(3, before.getCurrentActorSeat().orElseThrow());
        assertEquals(6, before.getBettingProgress().getPlayersToAct().size());
    }

    @Test
    @DisplayName("BE-CALL03: report actual added chips and authoritative accepted context")
    void callProducesAnAcceptedObservationWithActualChipDelta() {
        GameState before = initialState();

        BettingResult result = new BettingEngine().apply(before, callFromSeat(before, 3));
        AcceptedAction action = result.getAcceptedAction();

        assertEquals(before.getHandId(), action.getHandId());
        assertEquals(before.getPlayers().get(3).getId(), action.getPlayerId());
        assertEquals(BettingStreet.PREFLOP, action.getStreet());
        assertEquals(ResolvedActionType.CALL, action.getType());
        assertEquals(20, action.getChipsAdded());
        assertEquals(20, action.getRoundContributionAfter());
        assertFalse(action.becameAllIn());
        assertEquals(PreflopRaiseType.NONE, action.getStatsContext().getPreflopRaiseType());
        assertFalse(action.getStatsContext().hasThreeBetOpportunity());
        assertFalse(action.getStatsContext().hasFlopCBetOpportunity());
        assertFalse(action.getStatsContext().hasFacedFlopCBetOpportunity());
        assertTrue(action.toStatsAction().isAccepted());
        assertEquals(20, result.getState().getPlayerStates().get(3).getRoundContribution());
    }

    @Test
    @DisplayName("BE-REJECT01: reject seat 4 while seat 3 owns the turn")
    void outOfTurnCallIsRejectedWithoutChangingTheState() {
        GameState before = initialState();

        assertThrows(IllegalArgumentException.class,
                () -> new BettingEngine().apply(before, callFromSeat(before, 4)));

        assertEquals(3, before.getCurrentActorSeat().orElseThrow());
        assertEquals(1000, before.getPlayers().get(4).getChipStack());
        assertEquals(0, before.getPlayerStates().get(4).getRoundContribution());
        assertEquals(0, before.getPlayerStates().get(4).getHandContribution());
        assertEquals(30, before.getPot());
        assertEquals(6000, before.getTotalChips());
        assertEquals(6, before.getBettingProgress().getPlayersToAct().size());
        assertEquals(2, before.getPlayerStates().get(4).getHoleCards().size());
        assertEquals(40, before.getRemainingDeck().size());
    }

    @Test
    @DisplayName("BE-INPUT01: reject a null state")
    void nullStateIsRejected() {
        GameState before = initialState();

        assertThrows(IllegalArgumentException.class,
                () -> new BettingEngine().apply(null, callFromSeat(before, 3)));
    }

    @Test
    @DisplayName("BE-INPUT02: reject a null request without changing the state")
    void nullRequestIsRejectedWithoutChangingTheState() {
        GameState before = initialState();

        assertThrows(IllegalArgumentException.class, () -> new BettingEngine().apply(before, null));

        assertEquals(3, before.getCurrentActorSeat().orElseThrow());
        assertEquals(1000, before.getPlayers().get(3).getChipStack());
        assertEquals(30, before.getPot());
        assertEquals(6000, before.getTotalChips());
    }

    private static GameState initialState() {
        Random dealer = new Random() {
            @Override public int nextInt(int bound) { return 0; }
        };
        return new GameInitializer(new GameConfig(), dealer, new Random(7)).start(2);
    }

    private static ActionRequest callFromSeat(GameState state, int seat) {
        return new ActionRequest(state.getPlayers().get(seat).getId(), ActionType.CALL);
    }
}
