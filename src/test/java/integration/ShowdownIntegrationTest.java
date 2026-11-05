package integration;

import application.GameInitializer;
import domain.model.BettingProgress;
import domain.model.Card;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.PlayerState;
import domain.model.Rank;
import domain.model.ShowdownResolver;
import domain.model.ShowdownResult;
import domain.model.Suit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Exercises the engine boundary; the betting engine will supply the closed SHOWDOWN state. */
class ShowdownIntegrationTest {
    @Test
    void initializedDeckAndHandsProduceKnownWinnerWithoutMovingChips() {
        GameState initial = initializer().start(2);
        // With an unchanged deck, the board is AC 2D 3D 4D 5D. Seat5's 6C makes six-high.
        List<Card> board = initial.getRemainingDeck().subList(0, 5);
        GameState showdown = closed(initial, board);
        ShowdownResult result = new ShowdownResolver().resolve(showdown);
        assertEquals(List.of(initial.getPlayers().get(5).getId()), result.getWinnerIds());
        assertBoundary(initial, showdown, result);
    }

    @Test
    void boardOnlyRoyalFlushReturnsAllPlayersAsTiedContenders() {
        GameState initial = initializer().start(6);
        List<Card> board = List.of(new Card(Rank.TEN, Suit.HEARTS),
                new Card(Rank.JACK, Suit.HEARTS), new Card(Rank.QUEEN, Suit.HEARTS),
                new Card(Rank.KING, Suit.HEARTS), new Card(Rank.ACE, Suit.HEARTS));
        GameState showdown = closed(initial, board);
        ShowdownResult result = new ShowdownResolver().resolve(showdown);
        assertEquals(initial.getPlayers().stream().map(player -> player.getId())
                .collect(Collectors.toList()), result.getWinnerIds());
        assertBoundary(initial, showdown, result);
    }

    private static void assertBoundary(GameState initial, GameState showdown, ShowdownResult result) {
        assertFalse(result.isUncontested());
        assertEquals(ShowdownResult.Status.AWAITING_SETTLEMENT, result.getStatus());
        assertEquals(initial.getHandId(), result.getHandId());
        assertEquals(6000, showdown.getTotalChips());
        assertEquals(30, showdown.getPot());
        assertEquals(initial.getPlayers(), showdown.getPlayers());
        assertEquals(GamePhase.PREFLOP, initial.getPhase());
        assertEquals(40, initial.getRemainingDeck().size());
        assertEquals(35, showdown.getRemainingDeck().size());
        assertEquals(GamePhase.SHOWDOWN, showdown.getPhase());
    }

    private static GameState closed(GameState initial, List<Card> board) {
        List<PlayerState> players = initial.getPlayerStates().stream()
                .map(player -> new PlayerState(player.getPlayer(), player.getStatus(),
                        player.getHoleCards(), 0, player.getHandContribution()))
                .collect(Collectors.toList());
        List<Card> remaining = new ArrayList<>(initial.getRemainingDeck());
        remaining.removeAll(board);
        return new GameState(initial.getConfig(), initial.getHandId(), GamePhase.SHOWDOWN,
                players, initial.getPositions(), null, board, remaining,
                new BettingProgress(0, 20, Set.of(), Set.of()));
    }

    private static GameInitializer initializer() {
        return new GameInitializer(new GameConfig(), new FirstChoiceRandom(), new LastChoiceRandom());
    }

    private static final class FirstChoiceRandom extends Random {
        private static final long serialVersionUID = 1L;
        @Override public int nextInt(int bound) { return 0; }
    }

    private static final class LastChoiceRandom extends Random {
        private static final long serialVersionUID = 1L;
        @Override public int nextInt(int bound) { return bound - 1; }
    }
}
