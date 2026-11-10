package bot;

import application.GameInitializer;
import domain.model.*;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EquityViewIntegrationTest {
    @Test
    void evC01BuildsFromOwnCardsAndPublicSeatOrder() {
        GameState state = new GameInitializer().start(2);
        String hero = state.getPlayers().get(0).getId();
        GameView view = state.viewFor(hero);
        List<EquityPot> pots = pots(view);
        EquityRequest request = EquityRequest.fromView(view, pots, 1);
        assertEquals(hero, request.getHeroId());
        assertEquals(state.getPlayerStates().get(0).getHoleCards(), request.getHoleCards());
        assertEquals(List.of(), request.getBoard());
        assertEquals(state.getPlayers().subList(1, 6).stream().map(Player::getId)
                .collect(Collectors.toList()), request.getOpponents());
        assertEquals(pots, request.getPots());
        assertEquals(1, request.getSimulations());
        assertTrue(view.getPlayers().subList(1, 6).stream().allMatch(p -> p.getHoleCards().isEmpty()));
    }
    @Test
    void evC02RetainsAllInsAndExcludesFoldedAndEliminatedSeats() {
        GameState state = mixedState();
        EquityRequest request = EquityRequest.fromView(state.viewFor("p0"),
                List.of(new EquityPot("main", 100, Set.of("p0", "p1", "p4", "p5"))), 2);
        assertEquals(List.of("p1", "p4", "p5"), request.getOpponents());
        EquityRequest allIn = EquityRequest.fromView(state.viewFor("p1"),
                List.of(new EquityPot("main", 100, Set.of("p0", "p1"))), 1);
        assertEquals("p1", allIn.getHeroId());
        assertEquals(List.of("p0", "p4", "p5"), allIn.getOpponents());
        for (String inactive : List.of("p2", "p3")) {
            assertThrows(IllegalArgumentException.class, () -> EquityRequest.fromView(state.viewFor(inactive),
                    List.of(new EquityPot("only", 1, Set.of(inactive))), 1));
        }
        assertThrows(IllegalArgumentException.class, () -> EquityRequest.fromView(null, List.of(), 1));
    }
    @Test
    void evC03HiddenCardAndDeckChangesCannotAffectSeededEstimates() {
        GameState state = new GameInitializer(new GameConfig(), new Random(1), new Random(2)).start(1);
        List<PlayerState> changed = new ArrayList<>(state.getPlayerStates());
        for (int seat = 1; seat <= 2; seat++) {
            PlayerState old = changed.get(seat);
            changed.set(seat, new PlayerState(old.getPlayer(), old.getStatus(),
                    state.getPlayerStates().get(3 - seat).getHoleCards(),
                    old.getRoundContribution(), old.getHandContribution()));
        }
        List<Card> reversed = new ArrayList<>(state.getRemainingDeck());
        Collections.reverse(reversed);
        GameState altered = new GameState(state.getConfig(), state.getHandId(), state.getPhase(),
                changed, state.getPositions(), state.getCurrentActorId().orElseThrow(),
                state.getCommunityCards(), reversed, state.getBettingProgress());
        String hero = state.getPlayers().get(0).getId();
        EquityRequest first = EquityRequest.fromView(state.viewFor(hero), pots(state.viewFor(hero)), 5);
        EquityRequest second = EquityRequest.fromView(altered.viewFor(hero), pots(altered.viewFor(hero)), 5);
        assertEquals(first.getHoleCards(), second.getHoleCards());
        assertEquals(first.getBoard(), second.getBoard());
        assertEquals(first.getOpponents(), second.getOpponents());
        EquityEstimate a = seeded(17).estimate(first);
        EquityEstimate b = seeded(17).estimate(second);
        assertEquals(a.getPotShares(), b.getPotShares());
        assertEquals(a.getEquity(), b.getEquity());
        assertEquals(a.getExpectedPayout(), b.getExpectedPayout());
    }
    private static MonteCarloEquityEstimator seeded(long seed) {
        return new MonteCarloEquityEstimator(new PokerHandEvaluator(),
                new UniformDealSampler(new Random(seed)::nextInt));
    }
    private static List<EquityPot> pots(GameView view) {
        return List.of(new EquityPot("scenario", 120, view.getPlayers().stream()
                .map(PlayerView::getId).collect(Collectors.toSet())));
    }
    private static GameState mixedState() {
        List<Card> cards = new Deck().deal(52);
        long[] stacks = {1500, 0, 1000, 0, 2000, 1400};
        PlayerStatus[] statuses = {PlayerStatus.ACTIVE, PlayerStatus.ALL_IN, PlayerStatus.FOLDED,
                PlayerStatus.ELIMINATED, PlayerStatus.ACTIVE, PlayerStatus.ACTIVE};
        List<PlayerState> players = new ArrayList<>();
        int next = 0;
        for (int i = 0; i < 6; i++) {
            List<Card> holes = i == 3 ? List.of() : cards.subList(next, next + 2);
            next += holes.size();
            players.add(new PlayerState(new Player("p" + i, i, PlayerType.HUMAN, stacks[i]),
                    statuses[i], holes, i == 1 ? 100 : 0, i == 1 ? 100 : 0));
        }
        Set<String> pending = Set.of("p0", "p4", "p5");
        return new GameState(new GameConfig(), "mixed", GamePhase.PREFLOP, players,
                new HandPositions(0, 1, 2), "p0", List.of(), cards.subList(next, 52),
                new BettingProgress(100, 20, pending, pending));
    }
}
