package application;

import bot.MonteCarloEquityEstimator;
import bot.UniformDealSampler;
import domain.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real engine/evaluator/policy/model/statistics integration, with controlled randomness. */
class BotPlayedIntegrationTest {
    @Test
    void bp01AdaptiveActionsReachPaymentAndRetainLearningForTheNextHand() {
        SetupSession session = BotFixtures.started(1);
        Random deals = new Random(380);
        Random decisions = new Random(16);
        MonteCarloEquityEstimator equity = new MonteCarloEquityEstimator(new PokerHandEvaluator(),
                new UniformDealSampler(deals::nextInt));
        BotComputer computer = new BotComputer(equity::estimate, decisions::nextDouble);
        String observer = session.getView().orElseThrow().getPlayers().get(5).getId();
        int actions = 0;
        while (session.getResult().isEmpty() && actions++ < 200) {
            Optional<BotTurn> turn = session.getBotTurn(32);
            if (turn.isPresent()) {
                BotTurn captured = turn.orElseThrow();
                assertTrue(session.applyBotDecision(captured,
                        computer.decide(captured, () -> false)));
            } else {
                session.submitAction(BotFixtures.passive(session));
            }
            assertEquals(6000, chipTotal(session));
        }
        assertTrue(session.getResult().isPresent());
        assertTrue(actions < 200);
        long observed = session.getBotBeliefs(observer).values().stream()
                .mapToLong(beliefs -> beliefs.getObservedDecisions()).sum();
        assertTrue(observed > 0);
        session.settleHand();
        assertEquals(6000, chipTotal(session));
        assertTrue(session.getStatistics().values().stream()
                .allMatch(stats -> stats.getHandsPlayed() == 1));
        assertTrue(session.canStartNextHand());
        session.nextHand();
        assertEquals(observed, session.getBotBeliefs(observer).values().stream()
                .mapToLong(beliefs -> beliefs.getObservedDecisions()).sum());
        assertEquals(6000, chipTotal(session));
    }

    @Test
    void bp02CappedAllInCallExcludesIneligibleSidePotsAndMatchesSettlement() {
        GameState base = BotFixtures.initializer().start(1);
        List<PlayerState> players = new ArrayList<>();
        for (PlayerState old : base.getPlayerStates()) {
            int seat = old.getPlayer().getSeat();
            long stack = seat == 3 ? 10 : seat == 4 ? 5820 : 0;
            Player player = new Player(old.getPlayer().getId(), seat,
                    old.getPlayer().getType(), stack);
            PlayerStatus status = seat == 0 ? PlayerStatus.ALL_IN
                    : seat == 3 || seat == 4 ? PlayerStatus.ACTIVE : PlayerStatus.ELIMINATED;
            long hand = seat == 0 ? 50 : seat == 3 ? 20 : seat == 4 ? 100 : 0;
            players.add(new PlayerState(player, status,
                    hand == 0 ? List.of() : old.getHoleCards(), seat == 4 ? 100 : 0, hand));
        }
        String hero = players.get(3).getPlayer().getId();
        GameState raised = GameState.create(new GameConfig(), "side-call", GamePhase.FLOP,
                players, new HandPositions(0, 3, 4), Optional.of(hero),
                base.getRemainingDeck().subList(0, 3),
                base.getRemainingDeck().subList(3, base.getRemainingDeck().size()),
                new BettingProgress(100, 20, Set.of(hero), Set.of(hero)));
        SetupSession session = new SetupSession(
                new GameInitializer(count -> raised, state -> state));
        session.start(1);
        BotTurn turn = session.getBotTurn(64).orElseThrow();
        assertEquals(10, turn.getLegalActions().getCallCost());
        assertEquals(0, turn.getCallRefund());
        assertEquals(90, turn.getScenario().getEligiblePotAmount());
        assertEquals(130, turn.getScenario().getTotalPotAmount());
        assertEquals(2, turn.getScenario().getPots().size());
        assertFalse(turn.getScenario().getPots().get(1).getEligiblePlayers().contains(hero));
        assertTrue(session.applyBotDecision(turn, new ActionRequest(hero, ActionType.CALL)));
        assertTrue(session.getAcceptedActions().get(0).becameAllIn());
        assertEquals(ResolvedActionType.CALL, session.getAcceptedActions().get(0).getType());
        assertTrue(session.getResult().isPresent());
        session.settleHand();
        String refund = players.get(4).getPlayer().getId();
        assertEquals(50, session.getSettlement().orElseThrow().getRefunds().get(refund));
        assertEquals(6000, chipTotal(session));
    }

    private static long chipTotal(SetupSession session) {
        SetupView view = session.getView().orElseThrow();
        return view.getPot() + view.getPlayers().stream().mapToLong(Player::getChipStack).sum();
    }
}
