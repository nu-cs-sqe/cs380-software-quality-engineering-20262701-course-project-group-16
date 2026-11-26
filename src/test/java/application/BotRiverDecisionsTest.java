package application;

import bot.BotEstimateValues;
import bot.EquityPot;
import bot.EquityRequest;
import bot.policy.BotPersonality;
import domain.model.ActionType;
import domain.model.BettingEngine;
import domain.model.BettingProgress;
import domain.model.Card;
import domain.model.Deck;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.Player;
import domain.model.PlayerState;
import domain.model.PlayerStatus;
import domain.model.PlayerType;
import domain.model.Rank;
import domain.model.RiverOutcomeEvaluator.Outcome;
import domain.model.SeatOrder;
import domain.model.Suit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class BotRiverDecisionsTest {
    private static final String HERO = "p0";
    private static final String OPPONENT = "p1";
    private static final int PRIOR_COMMITMENT = 20;
    private static final long RIVER_STACK = 50_000;
    private static final List<Card> BEATABLE = List.of(new Card(Rank.TEN, Suit.SPADES),
            new Card(Rank.JACK, Suit.SPADES), new Card(Rank.QUEEN, Suit.SPADES),
            new Card(Rank.KING, Suit.DIAMONDS), new Card(Rank.ACE, Suit.HEARTS));
    private static final List<Card> LOW_HOLES = List.of(new Card(Rank.TWO, Suit.CLUBS),
            new Card(Rank.THREE, Suit.CLUBS));

    @ParameterizedTest
    @CsvSource({
        "2,0,150,CHECK,0",
        "2,20,150,CALL,20",
        "3,20,450,CALL,20",
        "4,80,25,CALL,25",
        "5,980,979,CALL,979",
        "5,980,980,CALL,980",
        "5,980,981,CALL,980",
        "6,46010,47000,CALL,46010"
    })
    void lockedDecisionsUseTheExactLegalCheckOrCallWithoutSampling(int players, long bet,
            long stack, ActionType expected, long expectedCallCost) {
        BotTurn captured = capture(players, bet, stack, BotRiverIntegrationTest.BOARD, LOW_HOLES);
        assertEquals(expectedCallCost, captured.getLegalActions().getCallCost());
        for (BotPersonality personality : BotPersonality.values()) {
            BotTurn turn = replaceScenario(captured, captured.getScenario(),
                    captured.getCallRefund(), personality);
            for (long seed : List.of(0L, 41L, Long.MAX_VALUE)) {
                assertLockedDecision(turn, expected, seed);
            }
        }
    }

    @Test
    void anUncertainRiverStillUsesSimulationAndRandomizedFoldCallWeights() {
        BotTurn turn = capture(2, 20, 300, BEATABLE, LOW_HOLES);
        AtomicInteger estimates = new AtomicInteger();
        assertEquals(ActionType.FOLD, sampled(turn, 0, estimates));
        assertEquals(ActionType.CALL, sampled(turn, Math.nextDown(1.0), estimates));
        assertEquals(2, estimates.get());
    }

    @Test
    void deadlineCanExpireAfterTheBoardProofBeforeTheActionIsReturned() {
        List<Card> board = BotRiverIntegrationTest.BOARD;
        BotTurn turn = capture(3, 20, 450, board, LOW_HOLES);
        AtomicInteger checks = new AtomicInteger();
        // Permit the initial check and one for each first card of a two-card holding.
        int checksThroughProof = new Deck().remainingCards() - board.size();
        BotComputer bot = new BotComputer((request, cancelled) -> fail("Unexpected simulation."),
                () -> fail("Unexpected random draw."));
        assertThrows(CancellationException.class,
                () -> bot.decide(turn, () -> checks.incrementAndGet() > checksThroughProof));
    }

    @ParameterizedTest
    @CsvSource({"19,0,FOLD", "20,0,CALL", "21,0,CALL", "19,1,CALL", "0,20,CALL"})
    void knownLossUsesOnlyItsSoleEligiblePayoutAndIncrementalRefund(long sole, long refund,
            ActionType expected) {
        BotTurn captured = capture(2, 20, 300, BEATABLE, LOW_HOLES);
        List<EquityPot> pots = new ArrayList<>(List.of(
                new EquityPot("contested", 35, Set.of(HERO, OPPONENT)),
                new EquityPot("opponent-only", 4, Set.of(OPPONENT))));
        if (sole > 0) {
            pots.add(new EquityPot("hero-only", sole, Set.of(HERO)));
        }
        EquityRequest scenario = EquityRequest.fromView(captured.getView(), pots, 1);
        BotTurn turn = replaceScenario(captured, scenario, refund, captured.getPersonality());
        assertEquals(expected, BotComputer.actionForKnownOutcome(turn, Outcome.GUARANTEED_LOSS));
    }

    private static void assertLockedDecision(BotTurn turn, ActionType expected, long seed) {
        Random random = new Random(seed);
        AtomicInteger draws = new AtomicInteger();
        BotComputer bot = new BotComputer((request, cancelled) ->
                fail("A locked board never needs simulations."), () -> {
                    draws.incrementAndGet();
                    return random.nextDouble();
                });
        var decision = bot.decide(turn, () -> false);
        assertEquals(HERO, decision.getPlayerId());
        assertEquals(expected, decision.getType());
        assertTrue(decision.getTargetAmount().isEmpty());
        assertEquals(0, draws.get());
    }

    private static ActionType sampled(BotTurn turn, double quantile, AtomicInteger estimates) {
        BotComputer bot = new BotComputer((request, cancelled) -> {
            estimates.incrementAndGet();
            return BotEstimateValues.fixed(request, 0.25);
        }, () -> quantile);
        return bot.decide(turn, () -> false).getType();
    }

    private static BotTurn replaceScenario(BotTurn turn, EquityRequest scenario, long refund,
            BotPersonality personality) {
        return new BotTurn(turn.getMatchId(), turn.getRevision(), turn.getView(),
                turn.getLegalActions(), scenario, refund, Map.of(), personality);
    }

    private static BotTurn capture(int players, long bet, long stack, List<Card> board,
            List<Card> holes) {
        Deck fullDeck = new Deck();
        List<Card> deck = new ArrayList<>(fullDeck.deal(fullDeck.remainingCards()));
        deck.removeAll(board);
        deck.removeAll(holes);
        List<PlayerState> seats = new ArrayList<>();
        for (int index = 0; index < GameConfig.SEAT_COUNT; index++) {
            seats.add(riverSeat(index, players, bet, stack, holes, deck));
        }
        long total = seats.stream().mapToLong(seat ->
                seat.getPlayer().getChipStack() + seat.getHandContribution()).sum();
        Set<String> pending = seats.stream().filter(PlayerState::canAct)
                .map(seat -> seat.getPlayer().getId())
                .filter(id -> bet == 0 || !id.equals(OPPONENT)).collect(Collectors.toSet());
        GameConfig config = new GameConfig().withTableTotal(total);
        GameState river = GameState.create(config, "river-boundary", GamePhase.RIVER,
                seats, new SeatOrder(seats).positionsFor(players - 1), Optional.of(HERO), board,
                List.of(), new BettingProgress(bet, Math.max(bet, config.getBigBlind()),
                        pending, pending));
        return BotTable.start(river).capture(river, new BettingEngine().getLegalActions(river),
                1, 1);
    }

    private static PlayerState riverSeat(int index, int players, long bet, long stack,
            List<Card> holes, List<Card> deck) {
        String id = "p" + index;
        if (index >= players) {
            return new PlayerState(new Player(id, index, PlayerType.BOT, 0),
                    PlayerStatus.ELIMINATED, List.of(), 0, 0);
        }
        long round = index == 1 ? bet : 0;
        long balance = index == 0 ? stack : RIVER_STACK - round;
        Player player = new Player(id, index, PlayerType.BOT, balance);
        List<Card> cards = index == 0 ? holes : List.of(deck.remove(0), deck.remove(0));
        return new PlayerState(player, PlayerStatus.ACTIVE, cards, round,
                PRIOR_COMMITMENT + round);
    }
}
