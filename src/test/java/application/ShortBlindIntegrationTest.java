package application;

import domain.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShortBlindIntegrationTest {
    @Test
    void headsUpPlayerCanActuallyCallAShortBigBlind() {
        GameState state = new NextHand(new Random(1)).start(completed(0, 15, 0, 0, 5985, 0, 0));
        BettingResult accepted = new BettingEngine().apply(state, new ActionRequest("p3", ActionType.CALL));
        assertEquals(5, accepted.getAcceptedAction().getChipsAdded());
        assertTrue(accepted.getState().getCurrentActorId().isEmpty());
    }

    @Test
    void multiwayShortBigBlindKeepsTheFullMinimumCall() {
        GameState state = new NextHand(new Random(1)).start(completed(5, 5885, 100, 15, 0, 0, 0));
        assertEquals(20, state.getCurrentBet());
        BettingResult accepted = new BettingEngine().apply(state, new ActionRequest("p0", ActionType.CALL));
        assertEquals(20, accepted.getAcceptedAction().getChipsAdded());
    }

    @Test
    void foldingClosesTheMatchedLoneActiveDecisionBeforeAnEligibleSidePotCanBeLost() {
        List<Card> cards = new Deck().deal(52);
        List<PlayerState> players = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            PlayerStatus status = seat < 2 ? PlayerStatus.ACTIVE : seat == 2
                    ? PlayerStatus.ALL_IN : PlayerStatus.ELIMINATED;
            long contribution = seat < 2 ? 20 : seat == 2 ? 5 : 0;
            players.add(new PlayerState(new Player("p" + seat, seat, PlayerType.HUMAN,
                    seat == 0 ? 5940 : seat == 1 ? 15 : 0), status,
                    seat < 3 ? cards.subList(3 + seat * 2, 5 + seat * 2) : List.of(), 0, contribution));
        }
        GameState before = new GameState(new GameConfig(), "short", GamePhase.FLOP, players,
                new HandPositions(0, 1, 2), "p1", cards.subList(0, 3), cards.subList(9, 52),
                new BettingProgress(0, 20, Set.of("p0", "p1"), Set.of("p0", "p1")));
        BettingEngine engine = new BettingEngine();
        GameState after = engine.apply(before, new ActionRequest("p1", ActionType.FOLD)).getState();
        assertTrue(after.getCurrentActorId().isEmpty());
        while (after.getPhase() != GamePhase.SHOWDOWN) after = engine.advanceStreet(after);
        assertEquals(6000, new PotSettlement().settle(after).getState().getTotalChips());
    }

    private static GameState completed(int dealer, long... stacks) {
        List<PlayerState> players = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) players.add(new PlayerState(
                new Player("p" + seat, seat, PlayerType.HUMAN, stacks[seat]),
                stacks[seat] == 0 ? PlayerStatus.ELIMINATED : PlayerStatus.ACTIVE, List.of(), 0, 0));
        return new GameState(new GameConfig(), "old", GamePhase.COMPLETION, players,
                new HandPositions(dealer, (dealer + 1) % 6, (dealer + 2) % 6), null,
                List.of(), List.of(), new BettingProgress(0, 20, Set.of(), Set.of()));
    }
}
