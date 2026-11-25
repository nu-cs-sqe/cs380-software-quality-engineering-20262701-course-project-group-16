package application;

import bot.MonteCarloEquityEstimator;
import bot.UniformDealSampler;
import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.BettingProgress;
import domain.model.Card;
import domain.model.Deck;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.HandPositions;
import domain.model.Player;
import domain.model.PlayerState;
import domain.model.PlayerStatus;
import domain.model.PlayerType;
import domain.model.PokerHandEvaluator;
import domain.model.Rank;
import domain.model.Settlement;
import domain.model.Suit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotRiverIntegrationTest {
    private static final String BOT_1 = "bot-1";
    private static final String BOT_2 = "bot-2";
    private static final String BOT_3 = "bot-3";
    private static final String HUMAN = "human";
    private static final String BOT_5 = "bot-5";
    private static final String BOT_6 = "bot-6";
    private static final List<String> PLAYERS = List.of(BOT_1, BOT_2, BOT_3, HUMAN, BOT_5, BOT_6);
    private static final long SMALL_BLIND = 10;
    private static final long BIG_BLIND = 20;
    private static final long RIVER_BET = 46_010;
    private static final long SHORT_CALL = 980;
    private static final long DEEP_CALL = 4_005;
    private static final long HUMAN_REFUND = 42_005;
    private static final int TRIALS = 512;
    static final List<Card> BOARD = List.of(new Card(Rank.JACK, Suit.DIAMONDS),
            new Card(Rank.QUEEN, Suit.SPADES), new Card(Rank.ACE, Suit.SPADES),
            new Card(Rank.TEN, Suit.DIAMONDS), new Card(Rank.KING, Suit.CLUBS));
    // The history omits Bot 1 and Bot 5's stacks. This fixture gives each a 1,000 buy-in.
    private static final Map<String, Long> OPENING = Map.of(BOT_1, 1_000L, BOT_2, 1_000L,
            BOT_3, 1_000L, HUMAN, RIVER_BET + BIG_BLIND, BOT_5, 1_000L,
            BOT_6, DEEP_CALL + BIG_BLIND);

    @Test
    void reportedHandCallsOnTheLockedRiverAndRestoresEveryLivePlayersContribution() {
        SetupSession session = facingRiverAllIn();
        BotComputer exact = new BotComputer(new MonteCarloEquityEstimator()::estimate, () -> 0);
        callWithBot(session, exact, BOT_5, SHORT_CALL);
        callWithBot(session, exact, BOT_6, DEEP_CALL);
        callWithBot(session, exact, BOT_1, SHORT_CALL);
        callWithBot(session, exact, BOT_2, SHORT_CALL);

        Settlement paid = session.settleHand();
        assertEquals(List.of(Map.of(BOT_1, 1_000L, BOT_2, 1_000L, HUMAN, 1_000L,
                BOT_5, 1_000L, BOT_6, 1_000L), Map.of(HUMAN, 3_025L, BOT_6, 3_025L)),
                paid.getPots());
        assertEquals(Map.of(BOT_1, 1_000L, BOT_2, 1_000L, BOT_3, 0L, HUMAN, 4_025L,
                BOT_5, 1_000L, BOT_6, 4_025L), paid.getPayouts());
        assertEquals(HUMAN_REFUND, paid.getRefunds().get(HUMAN));
        assertStacksRestored(paid);
    }

    @Test
    void manuallyReplayingTheReportedFoldsPreservesTheReportedOriginalSettlement() {
        SetupSession session = facingRiverAllIn();
        act(session, BOT_5, ActionType.FOLD);
        act(session, BOT_6, ActionType.CALL);
        act(session, BOT_1, ActionType.FOLD);
        act(session, BOT_2, ActionType.CALL);

        Settlement paid = session.settleHand();
        assertEquals(List.of(Map.of(BOT_2, 1_014L, HUMAN, 1_013L, BOT_6, 1_013L),
                Map.of(HUMAN, 3_025L, BOT_6, 3_025L)), paid.getPots());
        assertEquals(Map.of(BOT_1, 0L, BOT_2, 1_014L, BOT_3, 0L, HUMAN, 4_038L,
                BOT_5, 0L, BOT_6, 4_038L), paid.getPayouts());
        assertEquals(HUMAN_REFUND, paid.getRefunds().get(HUMAN));
        assertEquals(totalOpening(), paid.getState().getTotalChips());
    }

    private static void callWithBot(SetupSession session, BotComputer bot, String id, long cost) {
        BotTurn turn = session.getBotTurn(TRIALS).orElseThrow();
        assertEquals(id, turn.getView().getViewerId());
        assertEquals(cost, turn.getLegalActions().getCallCost());
        ActionRequest decision = bot.decide(turn, () -> false);
        assertEquals(ActionType.CALL, decision.getType());
        assertTrue(session.applyBotDecision(turn, decision));
        var accepted = session.getAcceptedActions().get(session.getAcceptedActions().size() - 1);
        assertEquals(cost, accepted.getChipsAdded());
        assertTrue(accepted.becameAllIn());
    }

    private static SetupSession facingRiverAllIn() {
        GameInitializer initializer = new GameInitializer(seats -> firstDeal(), state -> state);
        SetupSession session = new SetupSession(initializer);
        session.start(1);
        foldWeakBotBeforeTheFlop(session);
        for (String caller : List.of(HUMAN, BOT_5, BOT_6, BOT_1)) {
            act(session, caller, ActionType.CALL);
        }
        act(session, BOT_2, ActionType.CHECK);
        checkThrough(session, GamePhase.FLOP);
        checkThrough(session, GamePhase.TURN);
        assertEquals(BOARD, session.getView().orElseThrow().getCommunityCards());
        act(session, BOT_1, ActionType.CHECK);
        act(session, BOT_2, ActionType.CHECK);
        assertEquals(HUMAN, BotFixtures.actor(session));
        assertTrue(session.submitAction(new ActionRequest(HUMAN, ActionType.BET, RIVER_BET))
                .becameAllIn());
        return session;
    }

    private static void foldWeakBotBeforeTheFlop(SetupSession session) {
        MonteCarloEquityEstimator equity = new MonteCarloEquityEstimator(new PokerHandEvaluator(),
                new UniformDealSampler(new Random(24)::nextInt));
        BotTurn preflop = session.getBotTurn(TRIALS).orElseThrow();
        BotComputer computer = new BotComputer(equity::estimate, () -> 0);
        ActionRequest fold = computer.decide(preflop, () -> false);
        assertEquals(BOT_3, fold.getPlayerId());
        assertEquals(ActionType.FOLD, fold.getType());
        assertTrue(session.applyBotDecision(preflop, fold));
    }

    private static void checkThrough(SetupSession session, GamePhase street) {
        assertEquals(street, session.getView().orElseThrow().getPhase());
        for (String id : List.of(BOT_1, BOT_2, HUMAN, BOT_5, BOT_6)) {
            act(session, id, ActionType.CHECK);
        }
    }

    private static void act(SetupSession session, String id, ActionType action) {
        assertEquals(id, BotFixtures.actor(session));
        session.submitAction(new ActionRequest(id, action));
    }

    private static GameState firstDeal() {
        Deck fullDeck = new Deck();
        List<Card> undealt = new ArrayList<>(fullDeck.deal(fullDeck.remainingCards()));
        undealt.removeAll(BOARD);
        List<Card> weak = List.of(new Card(Rank.TWO, Suit.CLUBS),
                new Card(Rank.SEVEN, Suit.HEARTS));
        undealt.removeAll(weak);
        List<PlayerState> seats = new ArrayList<>();
        for (int seat = 0; seat < PLAYERS.size(); seat++) {
            String id = PLAYERS.get(seat);
            List<Card> holes = id.equals(BOT_3) ? weak
                    : List.of(undealt.remove(0), undealt.remove(0));
            long blind = id.equals(BOT_1) ? SMALL_BLIND : id.equals(BOT_2) ? BIG_BLIND : 0;
            Player player = new Player(id, seat, id.equals(HUMAN) ? PlayerType.HUMAN
                    : PlayerType.BOT, OPENING.get(id) - blind);
            seats.add(new PlayerState(player, PlayerStatus.ACTIVE, holes, blind, blind));
        }
        List<Card> deck = new ArrayList<>(List.of(undealt.remove(0), BOARD.get(0), BOARD.get(1),
                BOARD.get(2), undealt.remove(0), BOARD.get(3), undealt.remove(0), BOARD.get(4)));
        deck.addAll(undealt);
        Set<String> pending = Set.copyOf(PLAYERS);
        return GameState.create(new GameConfig().withTableTotal(totalOpening()), "river-regression",
                GamePhase.PREFLOP, seats, new HandPositions(5, 0, 1), Optional.of(BOT_3),
                List.of(), deck, new BettingProgress(BIG_BLIND, BIG_BLIND, pending, pending));
    }

    private static long totalOpening() {
        return OPENING.values().stream().mapToLong(Long::longValue).sum();
    }

    private static void assertStacksRestored(Settlement paid) {
        assertEquals(0, paid.getState().getPot());
        assertEquals(totalOpening(), paid.getState().getTotalChips());
        paid.getState().getPlayers().forEach(player ->
                assertEquals(OPENING.get(player.getId()), player.getChipStack()));
    }
}
