package application;

import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.BettingEngine;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.Player;
import domain.model.PlayerState;
import domain.model.PlayerStatus;
import domain.model.BettingProgress;
import domain.model.Settlement;
import domain.model.ShowdownResolver;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CompletedStatisticsIntegrationTest {
    @Test
    void aMatchWinnerIsAbsentBeforeStartingTheSession() {
        SetupSession session = session();
        assertTrue(session.getMatchWinner().isEmpty());
        assertFalse(session.canStartNextHand());
    }

    @Test
    void settlementCompletesOnceAndNextHandKeepsIdentityChipsAndTotals() {
        SetupSession session = session();
        assertTrue(session.getStatistics().isEmpty());
        assertThrows(IllegalStateException.class, session::settleHand);
        session.start(6);
        List<String> ids = view(session).getPlayers().stream().map(p -> p.getId())
                .collect(java.util.stream.Collectors.toList());
        assertEquals(0, session.getStatistics().get(ids.get(0)).getHandsPlayed());
        assertThrows(IllegalStateException.class, session::settleHand);
        session.revealCards();
        assertThrows(IllegalStateException.class, session::nextHand);
        assertTrue(view(session).getHoleCards().isEmpty());
        for (int i = 0; i < 5; i++) act(session, ActionType.FOLD);
        var pending = session.getStatistics();
        session.revealCards();
        assertEquals(2, view(session).getHoleCards().size());
        Settlement paid = session.settleHand();
        assertSame(paid, session.getSettlement().orElseThrow());
        assertTrue(view(session).getHoleCards().isEmpty());
        assertEquals(GamePhase.COMPLETION, view(session).getPhase());
        assertEquals(0, view(session).getPot());
        assertEquals(1010, view(session).getPlayers().get(2).getChipStack());
        assertEquals(10L, paid.getRefunds().get(ids.get(2)));
        assertEquals(20L, paid.getPayouts().get(ids.get(2)));
        assertEquals(0, pending.get(ids.get(0)).getHandsPlayed());
        for (var stats : session.getStatistics().values()) {
            assertEquals(1, stats.getHandsPlayed());
            assertEquals(0, stats.getRates().get(0).orElseThrow());
            assertTrue(stats.getRates().get(9).isEmpty());
        }
        assertThrows(IllegalStateException.class, session::settleHand);
        var first = session.getStatistics();
        String hand = view(session).getHandId();
        session.revealCards();
        session.nextHand();
        assertEquals(view(session).getCurrentActorSeat(), view(session).getViewerSeat());
        assertNotEquals(hand, view(session).getHandId());
        assertEquals(ids, view(session).getPlayers().stream().map(p -> p.getId())
                .collect(java.util.stream.Collectors.toList()));
        assertEquals(1, view(session).getPositions().getDealerSeat());
        assertEquals(40, view(session).getRemainingCardCount());
        assertTrue(view(session).getHoleCards().isEmpty());
        assertTrue(session.getAcceptedActions().isEmpty());
        assertTrue(session.getSettlement().isEmpty());
        assertEquals(1, session.getStatistics().get(ids.get(0)).getHandsPlayed());
        for (int i = 0; i < 5; i++) act(session, ActionType.FOLD);
        session.settleHand();
        assertEquals(2, session.getStatistics().get(ids.get(0)).getHandsPlayed());
        assertEquals(1, first.get(ids.get(0)).getHandsPlayed());
        assertThrows(UnsupportedOperationException.class, () -> first.clear());
        session.start(6);
        assertTrue(session.getStatistics().values().stream().allMatch(s -> s.getHandsPlayed() == 0));
    }

    @Test
    void realShowdownCountsCallsSeeingFlopAndTiedPayouts() {
        SetupSession session = session();
        session.start(6);
        for (int i = 0; i < 5; i++) act(session, ActionType.CALL);
        act(session, ActionType.CHECK);
        for (int i = 0; i < 18; i++) act(session, ActionType.CHECK);
        assertTrue(session.getStatistics().values().stream().allMatch(s -> s.getHandsPlayed() == 0));
        session.settleHand();
        for (int seat = 0; seat < 6; seat++) {
            var stats = session.getStatistics().get(view(session).getPlayers().get(seat).getId());
            assertEquals(1, stats.getHandsPlayed());
            assertEquals(seat == 2 ? 0 : 100, stats.getRates().get(0).orElseThrow());
            assertEquals(100, stats.getRates().get(8).orElseThrow());
            // No-op shuffle yields a shared board flush, so everyone receives a split pot.
            assertEquals(100, stats.getRates().get(9).orElseThrow());
        }
        assertEquals(6000, view(session).getPlayers().stream().mapToLong(p -> p.getChipStack()).sum());
    }

    @Test
    void preflopAndPostflopFoldersHaveDifferentFlopParticipation() {
        SetupSession session = session();
        session.start(6);
        act(session, ActionType.FOLD); // seat3 never sees flop.
        for (int i = 0; i < 4; i++) act(session, ActionType.CALL);
        act(session, ActionType.CHECK);
        for (int i = 0; i < 4; i++) act(session, ActionType.FOLD);
        session.settleHand();
        var pre = session.getStatistics().get(view(session).getPlayers().get(3).getId());
        var post = session.getStatistics().get(view(session).getPlayers().get(1).getId());
        assertTrue(pre.getRates().get(8).isEmpty());
        assertEquals(0, post.getRates().get(8).orElseThrow());
        assertEquals(1, post.getHandsPlayed());
        assertTrue(post.getRates().get(9).isEmpty());
    }

    @Test
    void finalHeadsUpWinEliminatesTheLoserAndCompletesOnlyDealtPlayers() {
        domain.model.GameState hand = headsUpHand();
        SetupSession session = new SetupSession(new GameInitializer(count -> hand,
                java.util.function.UnaryOperator.identity()));
        session.start(1);
        assertTrue(session.getMatchWinner().isEmpty());
        act(session, ActionType.CHECK);
        session.settleHand();
        assertEquals("p0", session.getMatchWinner().orElseThrow().getId());
        assertEquals(6000, session.getMatchWinner().orElseThrow().getChipStack());
        assertEquals(domain.model.PlayerStatus.ELIMINATED, view(session).getStatuses().get(1));
        assertTrue(view(session).getHoleCards().isEmpty());
        assertEquals(1, session.getStatistics().get("p0").getHandsPlayed());
        assertEquals(1, session.getStatistics().get("p1").getHandsPlayed());
        assertEquals(0, session.getStatistics().get("p2").getHandsPlayed());
        assertEquals(100, session.getStatistics().get("p0").getRates().get(9).orElseThrow());
        assertEquals(0, session.getStatistics().get("p1").getRates().get(9).orElseThrow());
        assertThrows(IllegalStateException.class, session::nextHand);
        assertEquals(6000, session.getMatchWinner().orElseThrow().getChipStack());
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 1000})
    void aRemainingPlayerBelowTheFullChipTotalCannotBeDeclaredMatchWinner(long retired) {
        GameState hand = closingHandWithRetiredChips(retired);
        SetupSession session = new SetupSession(new GameInitializer(count -> hand,
                java.util.function.UnaryOperator.identity()));
        session.start(1);
        session.quit("p2");
        act(session, ActionType.CHECK);
        session.settleHand();
        assertEquals(6000 - retired, view(session).getPlayers().get(0).getChipStack());
        assertEquals(retired, view(session).getPlayers().get(2).getChipStack());
        assertEquals(6000, view(session).getPlayers().stream()
                .mapToLong(Player::getChipStack).sum());
        assertTrue(session.getMatchWinner().isEmpty());
    }

    @Test
    void anAllInDepartureCanWinTheCompleteChipTotal() {
        GameState hand = allInClosingHand();
        SetupSession session = new SetupSession(new GameInitializer(count -> hand,
                java.util.function.UnaryOperator.identity()));
        session.start(1);
        session.quit("p0");
        act(session, ActionType.CALL);
        assertEquals(List.of("p0"), session.getResult().orElseThrow().getWinnerIds());
        assertTrue(session.getMatchWinner().isEmpty());
        session.settleHand();
        assertEquals(6000, view(session).getPlayers().get(0).getChipStack());
        assertEquals("p0", session.getMatchWinner().orElseThrow().getId());
        assertFalse(session.canStartNextHand());
    }

    @Test
    void aCompletedMatchWinnerRetainsTheResultAfterLeaving() {
        GameState hand = headsUpHand();
        SetupSession session = new SetupSession(new GameInitializer(count -> hand,
                java.util.function.UnaryOperator.identity()));
        session.start(1);
        act(session, ActionType.CHECK);
        session.settleHand();
        assertEquals(6000, session.getMatchWinner().orElseThrow().getChipStack());
        session.quit("p0");
        assertEquals("p0", session.getMatchWinner().orElseThrow().getId());
        assertEquals(6000, session.getMatchWinner().orElseThrow().getChipStack());
        assertFalse(session.canStartNextHand());
        session.start(6);
        assertTrue(session.getMatchWinner().isEmpty());
        assertTrue(session.getSettlement().isEmpty());
        assertFalse(session.hasDeparted("p0"));
    }

    @Test
    void theWinningStackUsesTheConfiguredMatchTotal() {
        GameConfig config = new GameConfig(20, 10, 20);
        GameState hand = headsUpHand(config);
        SetupSession session = new SetupSession(new GameInitializer(count -> hand,
                java.util.function.UnaryOperator.identity()));
        session.start(1);
        act(session, ActionType.CHECK);
        session.settleHand();
        assertEquals(config.getInitialTotalChips(),
                session.getMatchWinner().orElseThrow().getChipStack());
        assertEquals(120, session.getMatchWinner().orElseThrow().getChipStack());
        assertFalse(session.canStartNextHand());
    }

    private static GameState closingHandWithRetiredChips(long retired) {
        GameState hand = headsUpHand();
        List<PlayerState> players = new ArrayList<>(hand.getPlayerStates());
        PlayerState hero = players.get(0);
        Player player = hero.getPlayer();
        players.set(0, new PlayerState(new Player(player.getId(), player.getSeat(),
                player.getType(), player.getChipStack() - retired), hero.getStatus(),
                hero.getHoleCards(), hero.getRoundContribution(), hero.getHandContribution()));
        Player retiredPlayer = players.get(2).getPlayer();
        players.set(2, new PlayerState(new Player(retiredPlayer.getId(), retiredPlayer.getSeat(),
                retiredPlayer.getType(), retired), PlayerStatus.DEPARTED, List.of(), 0, 0));
        return GameState.create(hand.getConfig(), hand.getHandId(), hand.getPhase(), players,
                hand.getPositions(), hand.getCurrentActorId(), hand.getCommunityCards(),
                hand.getRemainingDeck(), hand.getBettingProgress());
    }

    private static GameState allInClosingHand() {
        GameState hand = headsUpHand();
        List<PlayerState> players = new ArrayList<>(hand.getPlayerStates());
        Player first = players.get(0).getPlayer();
        Player second = players.get(1).getPlayer();
        players.set(0, new PlayerState(new Player(first.getId(), 0, first.getType(), 0),
                PlayerStatus.ALL_IN, players.get(0).getHoleCards(), 3000, 3000));
        players.set(1, new PlayerState(new Player(second.getId(), 1, second.getType(), 2990),
                PlayerStatus.ACTIVE, players.get(1).getHoleCards(), 10, 10));
        return GameState.create(hand.getConfig(), hand.getHandId(), hand.getPhase(), players,
                hand.getPositions(), Optional.of("p1"), hand.getCommunityCards(),
                hand.getRemainingDeck(), new BettingProgress(3000, 20, Set.of("p1"), Set.of()));
    }

    private static domain.model.GameState headsUpHand() {
        return headsUpHand(new GameConfig());
    }

    private static domain.model.GameState headsUpHand(GameConfig config) {
        List<domain.model.Card> deck = new java.util.ArrayList<>(new domain.model.Deck().deal(52));
        var first = List.of(new domain.model.Card(domain.model.Rank.ACE, domain.model.Suit.CLUBS),
                new domain.model.Card(domain.model.Rank.ACE, domain.model.Suit.DIAMONDS));
        var second = List.of(new domain.model.Card(domain.model.Rank.KING, domain.model.Suit.CLUBS),
                new domain.model.Card(domain.model.Rank.QUEEN, domain.model.Suit.HEARTS));
        deck.removeAll(first);
        deck.removeAll(second);
        List<domain.model.PlayerState> players = new java.util.ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            long stack = seat == 0 ? config.getInitialTotalChips() - 20 : 0;
            var status = seat == 0 ? domain.model.PlayerStatus.ACTIVE : seat == 1
                    ? domain.model.PlayerStatus.ALL_IN : domain.model.PlayerStatus.ELIMINATED;
            players.add(new domain.model.PlayerState(new domain.model.Player("p" + seat, seat,
                    domain.model.PlayerType.HUMAN, stack), status,
                    seat == 0 ? first : seat == 1 ? second : List.of(), seat < 2 ? 10 : 0,
                    seat < 2 ? 10 : 0));
        }
        // Board after burns is 3C 4C 5C 7C 9C; p0's AC makes the stronger flush.
        return domain.model.GameState.create(config, "last", GamePhase.PREFLOP, players,
                new domain.model.HandPositions(0, 0, 1), java.util.Optional.of("p0"), List.of(), deck,
                new domain.model.BettingProgress(10, 20, java.util.Set.of("p0"), java.util.Set.of()));
    }

    private static SetupSession session() {
        Random dealer = new Random() {
            private static final long serialVersionUID = 1L;
            @Override public int nextInt(int bound) { return 0; }
        };
        Random deck = new Random() {
            private static final long serialVersionUID = 1L;
            @Override public int nextInt(int bound) { return bound - 1; }
        };
        return new SetupSession(new GameInitializer(new GameConfig(), dealer, deck),
                new BettingEngine(), new ShowdownResolver());
    }

    private static SetupView view(SetupSession session) { return session.getView().orElseThrow(); }
    private static void act(SetupSession session, ActionType type) {
        SetupView before = view(session);
        session.submitAction(new ActionRequest(before.getPlayers()
                .get(before.getCurrentActorSeat()).getId(), type));
    }
}
