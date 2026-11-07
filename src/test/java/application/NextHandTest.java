package application;

import domain.model.BettingProgress;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.HandPositions;
import domain.model.Player;
import domain.model.PlayerState;
import domain.model.PlayerStatus;
import domain.model.PlayerType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NextHandTest {
    @Test
    void rotatesDealerAndDealsOnlySurvivingFixedSeatsWithCarriedStacks() {
        GameState before = completed(1000, 1000, 1000, 1000, 1000, 1000);
        GameState next = new NextHand(new Random(1)).start(before);
        assertEquals(1, next.getDealerSeat());
        assertEquals(2, next.getSmallBlindSeat());
        assertEquals(3, next.getBigBlindSeat());
        assertEquals(4, next.getCurrentActorSeat().orElseThrow());
        assertEquals(Set.of("p0", "p1", "p2", "p3", "p4", "p5"),
                next.getBettingProgress().getPlayersAllowedToRaise());
        assertEquals(990, next.getPlayers().get(2).getChipStack());
        assertEquals(980, next.getPlayers().get(3).getChipStack());
        assertEquals(6000, next.getTotalChips());
        assertEquals(40, next.getRemainingDeck().size());
        assertNotEquals(before.getHandId(), next.getHandId());
        for (int i = 0; i < 6; i++) assertEquals("p" + i, next.getPlayers().get(i).getId());
    }

    @Test
    void headsUpShortSmallBlindRunsOutWithoutInventingAnAction() {
        GameState next = new NextHand(new Random(1)).start(completed(5995, 0, 0, 5, 0, 0));
        assertEquals(3, next.getDealerSeat());
        assertEquals(3, next.getSmallBlindSeat());
        assertEquals(0, next.getBigBlindSeat());
        assertTrue(next.getCurrentActorId().isEmpty());
        assertEquals(48, next.getRemainingDeck().size());
        assertEquals(PlayerStatus.ALL_IN, next.getPlayerStates().get(3).getStatus());
        for (int seat : new int[]{1, 2, 4, 5}) {
            assertEquals(PlayerStatus.ELIMINATED, next.getPlayerStates().get(seat).getStatus());
            assertTrue(next.getPlayerStates().get(seat).getHoleCards().isEmpty());
        }
    }

    @Test
    void loneActiveSmallBlindStillRespondsToShortAllInBigBlind() {
        GameState next = new NextHand(new Random(1)).start(completed(15, 0, 0, 5985, 0, 0));
        assertEquals(3, next.getCurrentActorSeat().orElseThrow());
        assertEquals(15, next.getCurrentBet());
        assertTrue(next.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
        assertEquals(10, next.getPlayerStates().get(3).getRoundContribution());
    }

    @Test
    void refusesUnfinishedHandAndFinishedMatch() {
        NextHand service = new NextHand(new Random(1));
        assertThrows(IllegalStateException.class,
                () -> service.start(new domain.model.GameSetup().create(1)));
        assertThrows(IllegalStateException.class, () -> service.start(completed(6000, 0, 0, 0, 0, 0)));
    }

    private static GameState completed(long... stacks) {
        List<PlayerState> players = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            players.add(new PlayerState(new Player("p" + seat, seat, PlayerType.HUMAN, stacks[seat]),
                    stacks[seat] == 0 ? PlayerStatus.ELIMINATED : PlayerStatus.ACTIVE,
                    List.of(), 0, 0));
        }
        return new GameState(new GameConfig(), "old", GamePhase.COMPLETION, players,
                new HandPositions(0, 1, 2), null, List.of(), List.of(),
                new BettingProgress(0, 20, Set.of(), Set.of()));
    }
}
