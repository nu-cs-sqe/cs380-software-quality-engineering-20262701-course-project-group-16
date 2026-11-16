package bot.policy;

import bot.*;
import domain.model.*;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class BotPolicyIntegrationTest {
    @ParameterizedTest
    @EnumSource(value = GamePhase.class, names = {"PREFLOP", "FLOP", "TURN", "RIVER"})
    void bi01RealEquityAndPolicyActionsAreAcceptedByTheEngine(GamePhase phase) {
        PolicyFixtures.Fixture fixture = PolicyFixtures.table();
        if (phase != GamePhase.PREFLOP) fixture.street(phase);
        GameState before = fixture.state();
        PolicyRequest request = estimate(before, 380);
        BotPolicy policy = new BotPolicy();
        for (BotPersonality personality : BotPersonality.values()) {
            ActionDistribution distribution = timedPolicy(policy, request, personality);
            for (int index = 0; index < 30; index++) {
                ActionRequest action = distribution.sample(index / 30.0);
                BettingResult result = new BettingEngine().apply(before, action);
                assertEquals(6000, result.getState().getTotalChips());
                assertEquals("p3", result.getAcceptedAction().getPlayerId());
                assertEquals(action.getType().name(), result.getAcceptedAction().getType().name());
                assertEquals(6000, before.getTotalChips());
                assertEquals("p3", before.getCurrentActorId().orElseThrow());
            }
        }
    }

    @Test
    void bi02ChangingHiddenCardsAndDeckDoesNotChangeSeededDecisions() {
        GameState first = PolicyFixtures.table().state();
        List<PlayerState> changed = new ArrayList<>(first.getPlayerStates());
        for (int seat : new int[] {1, 4}) {
            PlayerState player = first.getPlayerStates().get(seat);
            List<Card> cards = first.getPlayerStates().get(seat == 1 ? 4 : 1).getHoleCards();
            changed.set(seat, new PlayerState(player.getPlayer(), player.getStatus(), cards,
                    player.getRoundContribution(), player.getHandContribution()));
        }
        List<Card> reversed = new ArrayList<>(first.getRemainingDeck());
        Collections.reverse(reversed);
        GameState second = GameState.create(first.getConfig(), first.getHandId(), first.getPhase(),
                changed, first.getPositions(), first.getCurrentActorId(), first.getCommunityCards(),
                reversed, first.getBettingProgress());
        PolicyRequest one = estimate(first, 42);
        PolicyRequest two = estimate(second, 42);
        assertEquals(one.getCallValue().getEquity(), two.getCallValue().getEquity());
        for (BotPersonality personality : BotPersonality.values()) {
            ActionDistribution a = new BotPolicy().distribution(one, personality);
            ActionDistribution b = new BotPolicy().distribution(two, personality);
            assertEquals(a.getProbabilities(), b.getProbabilities());
            for (double draw : new double[] {0, 0.2, 0.5, Math.nextDown(1.0)}) {
                assertEquals(a.sample(draw).getType(), b.sample(draw).getType());
                assertEquals(a.sample(draw).getTargetAmount(), b.sample(draw).getTargetAmount());
            }
        }
    }

    @Test
    void bi03ShortBlindRefundUsesOnlyNewCallRefundAndSettlesConservedChips() {
        PolicyFixtures.Fixture fixture = PolicyFixtures.table();
        fixture.dealer = 3;
        for (int seat : new int[] {1, 2, 4, 5}) fixture.eliminated(seat);
        fixture.round[0] = 5;
        fixture.hand[0] = 5;
        fixture.stacks[0] = 0;
        fixture.statuses[0] = PlayerStatus.ALL_IN;
        fixture.round[3] = 10;
        fixture.hand[3] = 10;
        fixture.stacks[3] = 5985;
        GameState base = fixture.state();
        GameState before = GameState.create(base.getConfig(), base.getHandId(), base.getPhase(),
                base.getPlayerStates(), base.getPositions(), base.getCurrentActorId(),
                base.getCommunityCards(), base.getRemainingDeck(),
                new BettingProgress(20, 20, Set.of("p3"), Set.of()));
        BettingEngine engine = new BettingEngine();
        LegalActions legal = engine.getLegalActions(before);
        assertEquals(10, legal.getCallCost());
        PolicyRequest request = new PolicyRequest(before.viewFor("p3"), legal,
                new CallValue(0, 10, 10), Map.of());
        ActionRequest action = new BotPolicy().distribution(request, BotPersonality.BALANCED)
                .sample(Math.nextDown(1.0));
        assertEquals(ActionType.CALL, action.getType());
        GameState after = engine.apply(before, action).getState();
        while (after.getPhase() != GamePhase.SHOWDOWN) after = engine.advanceStreet(after);
        Settlement paid = new PotSettlement().settle(after);
        assertEquals(15, paid.getRefunds().get("p3"));
        assertEquals(6000, paid.getState().getTotalChips());
    }

    private static ActionDistribution timedPolicy(BotPolicy policy, PolicyRequest request,
            BotPersonality personality) {
        long start = System.nanoTime();
        ActionDistribution result = policy.distribution(request, personality);
        double millis = (System.nanoTime() - start) / 1_000_000.0;
        System.out.printf(Locale.ROOT, "E05 policy %s %s: %.6f ms%n",
                request.getStreet(), personality, millis);
        return result;
    }

    private static PolicyRequest estimate(GameState state, long seed) {
        GameView view = state.viewFor("p3");
        LegalActions legal = new BettingEngine().getLegalActions(state);
        Set<String> players = view.getPlayers().stream().filter(player ->
                player.getStatus() == PlayerStatus.ACTIVE
                || player.getStatus() == PlayerStatus.ALL_IN).map(PlayerView::getId)
                .collect(Collectors.toSet());
        List<EquityPot> pots = List.of(new EquityPot("main",
                view.getPot() + legal.getCallCost(), players));
        EquityRequest scenario = EquityRequest.fromView(view, pots, 64);
        EquityEstimate result = new MonteCarloEquityEstimator(new PokerHandEvaluator(),
                new UniformDealSampler(new Random(seed)::nextInt)).estimate(scenario);
        return new PolicyRequest(view, legal,
                new CallValue(result.getEquity(), scenario.getEligiblePotAmount(), 0), Map.of());
    }
}
