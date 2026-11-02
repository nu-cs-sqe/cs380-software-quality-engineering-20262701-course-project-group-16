package application;

import domain.GameConfig;
import domain.GamePhase;
import domain.GameState;
import domain.GameView;
import domain.PlayerState;
import domain.PlayerStatus;
import domain.PlayerType;
import domain.model.Card;
import domain.model.Deck;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class GameInitializerTest {
    @Test
    void defaultInitializerStartsADealtMatch() {
        GameState state = new GameInitializer().start(2);

        assertEquals(GamePhase.PREFLOP, state.getPhase());
        assertEquals(2, state.getPlayers().stream()
                .filter(player -> player.getType() == PlayerType.HUMAN).count());
        assertEquals(6000, state.getTotalChips());
        assertFullInventory(state);
    }

    @ParameterizedTest(name = "reject null {0}")
    @ValueSource(strings = {"config", "dealer", "deck"})
    void rejectsNullDependencies(String dependency) {
        GameConfig config = dependency.equals("config") ? null : new GameConfig();
        Random dealer = dependency.equals("dealer") ? null : new Random(1);
        Random deck = dependency.equals("deck") ? null : new Random(7);

        assertThrows(IllegalArgumentException.class,
                () -> new GameInitializer(config, dealer, deck));
    }

    @ParameterizedTest(name = "{0}: humans {1}, dealer {2}")
    @CsvSource({
        "INIT-V01, 1, 0",
        "INIT-V02, 2, 1",
        "INIT-V03, 5, 2",
        "INIT-V04, 6, 3",
        "INIT-V05, 2, 4",
        "INIT-V06, 2, 5"
    })
    void startsDealtMatchClockwiseAfterDealer(String caseId, int humanCount, int dealer) {
        GameInitializer initializer = new GameInitializer(
                new GameConfig(), new FixedDealerRandom(dealer), new Random(7));

        GameState state = initializer.start(humanCount);

        int smallBlind = (dealer + 1) % 6;
        int bigBlind = (dealer + 2) % 6;
        int actor = (dealer + 3) % 6;
        assertAll(
            () -> assertEquals(GamePhase.PREFLOP, state.getPhase()),
            () -> assertEquals(dealer, state.getDealerSeat()),
            () -> assertEquals(smallBlind, state.getSmallBlindSeat()),
            () -> assertEquals(bigBlind, state.getBigBlindSeat()),
            () -> assertEquals(actor, state.getCurrentActorSeat().orElseThrow()),
            () -> assertEquals(30, state.getPot()),
            () -> assertEquals(6000, state.getTotalChips()),
            () -> assertEquals(20, state.getCurrentBet()),
            () -> assertEquals(20, state.getLastFullRaiseSize()),
            () -> assertTrue(state.getCommunityCards().isEmpty())
        );
        assertFullInventory(state);
        assertDealOrder(state, shuffledCards(new Random(7)));
        Set<String> expectedActors = new HashSet<>();
        for (int seat = 0; seat < 6; seat++) {
            PlayerState player = state.getPlayerStates().get(seat);
            long contribution = seat == smallBlind ? 10 : seat == bigBlind ? 20 : 0;
            assertEquals(seat, player.getPlayer().getSeat());
            assertEquals(seat < humanCount ? PlayerType.HUMAN : PlayerType.BOT,
                    player.getPlayer().getType());
            assertEquals(PlayerStatus.ACTIVE, player.getStatus());
            assertEquals(1000 - contribution, player.getPlayer().getChipStack());
            assertEquals(contribution, player.getRoundContribution());
            assertEquals(contribution, player.getHandContribution());
            expectedActors.add(player.getPlayer().getId());

            GameView view = state.viewFor(player.getPlayer().getId());
            for (int other = 0; other < 6; other++) {
                assertEquals(other == seat ? player.getHoleCards() : List.of(),
                        view.getPlayers().get(other).getHoleCards());
            }
        }
        assertEquals(expectedActors, state.getBettingProgress().getPlayersToAct());
        assertEquals(expectedActors, state.getBettingProgress().getPlayersAllowedToRaise());
        assertEquals(state.getPlayers().get(actor).getId(), state.getCurrentActorId().orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, 0, 7, Integer.MAX_VALUE})
    void rejectsInvalidHumanCountBeforeUsingRandomness(int humanCount) {
        Random mustNotBeUsed = new Random() {
            @Override
            public int nextInt(int bound) {
                throw new AssertionError("Invalid setup must not consume random choices");
            }
        };
        GameInitializer initializer = new GameInitializer(
                new GameConfig(), mustNotBeUsed, mustNotBeUsed);

        assertThrows(IllegalArgumentException.class, () -> initializer.start(humanCount));
    }

    @Test
    void dealsToAnAllInBlindWithoutChangingItsStatus() {
        GameInitializer initializer = new GameInitializer(
                new GameConfig(2, 1, 2), new FixedDealerRandom(0), new Random(7));

        GameState state = initializer.start(1);

        assertEquals(GamePhase.PREFLOP, state.getPhase());
        assertFullInventory(state);
        PlayerState bigBlind = state.getPlayerStates().get(2);
        assertEquals(PlayerStatus.ALL_IN, bigBlind.getStatus());
        assertEquals(0, bigBlind.getPlayer().getChipStack());
        assertEquals(2, bigBlind.getRoundContribution());
        assertEquals(2, bigBlind.getHandContribution());
        assertEquals(3, state.getPot());
        assertEquals(12, state.getTotalChips());
        assertEquals(3, state.getCurrentActorSeat().orElseThrow());
        assertFalse(state.getBettingProgress().getPlayersToAct().contains(bigBlind.getPlayer().getId()));
        assertFalse(state.getBettingProgress().getPlayersAllowedToRaise().contains(bigBlind.getPlayer().getId()));
    }

    @Test
    void repeatedStartsUseFreshDecksAndPreserveEarlierMatches() {
        GameInitializer initializer = new GameInitializer(
                new GameConfig(), new FixedDealerRandom(5), new Random(7));
        Random expectedRandom = new Random(7);
        GameState first = initializer.start(1);
        List<List<Card>> firstHands = new ArrayList<>();
        first.getPlayerStates().forEach(player -> firstHands.add(player.getHoleCards()));
        List<Card> firstRemainder = List.copyOf(first.getRemainingDeck());
        assertEquals(GamePhase.PREFLOP, first.getPhase());
        assertDealOrder(first, shuffledCards(expectedRandom));

        assertThrows(IllegalArgumentException.class, () -> initializer.start(7));
        GameState second = initializer.start(6);

        assertEquals(GamePhase.PREFLOP, second.getPhase());
        assertFullInventory(first);
        assertFullInventory(second);
        assertDealOrder(second, shuffledCards(expectedRandom));
        assertNotEquals(first.getHandId(), second.getHandId());
        Set<String> firstIds = new HashSet<>();
        first.getPlayers().forEach(player -> firstIds.add(player.getId()));
        second.getPlayers().forEach(player -> assertFalse(firstIds.contains(player.getId())));
        for (int seat = 0; seat < 6; seat++) {
            assertEquals(firstHands.get(seat), first.getPlayerStates().get(seat).getHoleCards());
        }
        assertEquals(firstRemainder, first.getRemainingDeck());
        assertEquals(1, first.getPlayers().stream()
                .filter(player -> player.getType() == PlayerType.HUMAN).count());
        assertEquals(6, second.getPlayers().stream()
                .filter(player -> player.getType() == PlayerType.HUMAN).count());
        assertEquals(6000, first.getTotalChips());
        assertEquals(6000, second.getTotalChips());
    }

    private static void assertFullInventory(GameState state) {
        assertEquals(6, state.getPlayerStates().size());
        assertEquals(40, state.getRemainingDeck().size());
        List<Card> all = new ArrayList<>(state.getRemainingDeck());
        for (PlayerState player : state.getPlayerStates()) {
            assertEquals(2, player.getHoleCards().size());
            all.addAll(player.getHoleCards());
        }
        assertEquals(52, all.size());
        assertEquals(new HashSet<>(new Deck().deal(52)), new HashSet<>(all));
    }

    private static List<Card> shuffledCards(Random random) {
        Deck reference = new Deck(random);
        reference.shuffle();
        return reference.deal(52);
    }

    private static void assertDealOrder(GameState state, List<Card> expected) {
        for (int distance = 1; distance <= 6; distance++) {
            int seat = (state.getDealerSeat() + distance) % 6;
            assertEquals(List.of(expected.get(distance - 1), expected.get(distance + 5)),
                    state.getPlayerStates().get(seat).getHoleCards());
        }
        assertEquals(expected.subList(12, 52), state.getRemainingDeck());
    }

    private static final class FixedDealerRandom extends Random {
        private static final long serialVersionUID = 1L;
        private final int seat;

        private FixedDealerRandom(int seat) {
            this.seat = seat;
        }

        @Override
        public int nextInt(int bound) {
            if (bound != 6) {
                throw new AssertionError("First dealer must be selected among six occupied seats");
            }
            return seat;
        }
    }
}
