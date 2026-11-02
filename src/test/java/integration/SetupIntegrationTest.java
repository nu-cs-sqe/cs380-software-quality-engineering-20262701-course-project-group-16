package integration;

import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.GameSetup;
import domain.model.GameState;
import domain.model.GameView;
import domain.model.PlayerState;
import domain.model.Card;
import domain.model.Deck;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SetupIntegrationTest {
    @Test
    void IG_01() {
        GameState setup = setup();
        Deck deck = new Deck(new Random(7));
        deck.shuffle();
        List<List<Card>> hands = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            hands.add(new ArrayList<>());
        }
        for (int round = 0; round < 2; round++) {
            for (int distance = 1; distance <= 6; distance++) {
                int seat = (setup.getDealerSeat() + distance) % 6;
                hands.get(seat).addAll(deck.deal(1));
            }
        }
        assertEquals(40, deck.remainingCards());
        List<PlayerState> players = new ArrayList<>();
        for (PlayerState before : setup.getPlayerStates()) {
            players.add(new PlayerState(before.getPlayer(), before.getStatus(),
                    hands.get(before.getPlayer().getSeat()),
                    before.getRoundContribution(), before.getHandContribution()));
        }
        // Transfer ownership of the remaining order into the immutable engine snapshot.
        GameState state = preflop(setup, players, deck.deal(40));
        Deck reference = new Deck(new Random(7));
        reference.shuffle();
        List<Card> order = reference.deal(52);
        assertEquals(List.of(order.get(0), order.get(6)), state.getPlayerStates().get(1).getHoleCards());
        assertEquals(List.of(order.get(5), order.get(11)), state.getPlayerStates().get(0).getHoleCards());
        assertEquals(GamePhase.PREFLOP, state.getPhase());
        assertEquals(3, state.getCurrentActorSeat().orElseThrow());
        assertEquals(990, state.getPlayers().get(1).getChipStack());
        assertEquals(980, state.getPlayers().get(2).getChipStack());
        assertEquals(30, state.getPot());
        assertEquals(6000, state.getTotalChips());
        Set<Card> seen = new HashSet<>();
        for (PlayerState player : state.getPlayerStates()) {
            assertEquals(2, player.getHoleCards().size());
            for (Card card : player.getHoleCards()) {
                assertTrue(seen.add(card));
            }
        }
        assertEquals(12, seen.size());
        for (Card card : state.getRemainingDeck()) {
            assertTrue(seen.add(card));
        }
        assertEquals(52, seen.size());
        for (int viewerSeat : new int[]{0, 5}) {
            GameView view = state.viewFor(state.getPlayers().get(viewerSeat).getId());
            for (int seat = 0; seat < 6; seat++) {
                assertEquals(seat == viewerSeat ? 2 : 0, view.getPlayers().get(seat).getHoleCards().size());
            }
        }
        assertEquals(GamePhase.SETUP, setup.getPhase());
        assertTrue(setup.getPlayerStates().get(0).getHoleCards().isEmpty());
    }

    @Test
    void IG_02() {
        GameState setup = setup();
        Deck deck = new Deck(new Random(7));
        List<PlayerState> players = new ArrayList<>();
        for (PlayerState before : setup.getPlayerStates()) {
            players.add(new PlayerState(before.getPlayer(), before.getStatus(), deck.deal(2),
                    before.getRoundContribution(), before.getHandContribution()));
        }
        List<Card> remaining = deck.deal(40);
        GameState valid = preflop(setup, players, remaining);
        Card first = players.get(0).getHoleCards().get(0);
        Card sameValue = new Card(first.getRank(), first.getSuit());
        assertNotSame(first, sameValue);
        PlayerState other = players.get(1);
        players.set(1, new PlayerState(other.getPlayer(), other.getStatus(),
                List.of(sameValue, other.getHoleCards().get(1)),
                other.getRoundContribution(), other.getHandContribution()));
        assertThrows(IllegalArgumentException.class, () -> preflop(setup, players, remaining));
        assertEquals(40, valid.getRemainingDeck().size());
        assertEquals(6000, valid.getTotalChips());
    }

    private static GameState setup() {
        Random dealer = new Random() {
            @Override public int nextInt(int bound) { return 0; }
        };
        return new GameSetup(new GameConfig(), dealer).create(2);
    }

    private static GameState preflop(GameState setup, List<PlayerState> players, List<Card> remaining) {
        return new GameState(setup.getConfig(), setup.getHandId(), GamePhase.PREFLOP, players,
                setup.getPositions(), setup.getCurrentActorId().orElseThrow(),
                List.of(), remaining, setup.getBettingProgress());
    }
}
