package application;

import bot.BotEstimateValues;
import bot.EquityPot;
import bot.EquityRequest;
import bot.learning.DecisionContext;
import bot.learning.DecisionOrigin;
import bot.learning.OpponentBeliefs;
import bot.learning.OpponentModel;
import bot.learning.PublicDecision;
import bot.policy.BotPersonality;
import bot.policy.KnownOutcomePolicy;
import domain.model.AcceptedAction;
import domain.model.ActionType;
import domain.model.BettingEngine;
import domain.model.BettingProgress;
import domain.model.BettingStreet;
import domain.model.Card;
import domain.model.Deck;
import domain.model.ExactOutcomeEvaluator;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.Player;
import domain.model.PlayerState;
import domain.model.PlayerStatus;
import domain.model.PlayerType;
import domain.model.PreflopRaiseType;
import domain.model.Rank;
import domain.model.ResolvedActionType;
import domain.model.RiverOutcomeEvaluator.Outcome;
import domain.model.SeatOrder;
import domain.model.StatsContext;
import domain.model.Suit;
import java.util.ArrayList;
import java.util.Collections;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class BotRiverDecisionsTest {
    private static final String HERO = "p0";
    private static final String OPPONENT = "p1";
    private static final int PRIOR_COMMITMENT = 20;
    private static final long OPPONENT_STACK = 50_000;
    private static final List<Card> BEATABLE = List.of(new Card(Rank.TEN, Suit.SPADES),
            new Card(Rank.JACK, Suit.SPADES), new Card(Rank.QUEEN, Suit.SPADES),
            new Card(Rank.KING, Suit.DIAMONDS), new Card(Rank.ACE, Suit.HEARTS));
    private static final List<Card> LOW_HOLES = List.of(new Card(Rank.TWO, Suit.CLUBS),
            new Card(Rank.THREE, Suit.CLUBS));
    static final List<Card> ROYAL_FLOP = List.of(new Card(Rank.QUEEN, Suit.HEARTS),
            new Card(Rank.JACK, Suit.HEARTS), new Card(Rank.TEN, Suit.HEARTS));
    static final List<Card> ROYAL_HOLES = List.of(new Card(Rank.ACE, Suit.HEARTS),
            new Card(Rank.KING, Suit.HEARTS));

    @ParameterizedTest
    @CsvSource({"FLOP", "TURN"})
    void privateRoyalSelectsALegalValueBetWithoutSimulationOrSampling(GamePhase phase) {
        List<Card> board = new ArrayList<>(ROYAL_FLOP);
        if (phase == GamePhase.TURN) {
            board.add(new Card(Rank.SIX, Suit.CLUBS));
        }
        BotTurn turn = capture(3, 0, 600, board, ROYAL_HOLES);
        BotComputer bot = new BotComputer((request, cancelled) -> fail("The royal is proven."),
                () -> fail("The value policy selects a single legal action."));

        for (BotPersonality personality : BotPersonality.values()) {
            var option = replaceScenario(turn, turn.getScenario(), 0, personality);
            var decision = bot.decide(option, () -> false);
            assertEquals(ActionType.BET, decision.getType());
            long target = decision.getTargetAmount().orElseThrow();
            assertTrue(turn.getLegalActions().getWager().orElseThrow().allows(target));
            assertTrue(target < 600);
        }
    }

    @Test
    void anUnresolvedTurnUsesEquityAndSamplingWhenTheProofBudgetIsZero() {
        List<Card> board = List.of(new Card(Rank.ACE, Suit.SPADES), new Card(Rank.KING, Suit.CLUBS),
                new Card(Rank.QUEEN, Suit.DIAMONDS), new Card(Rank.JACK, Suit.HEARTS));
        BotTurn turn = capture(2, 20, 300, board, LOW_HOLES);
        AtomicInteger estimates = new AtomicInteger();
        AtomicInteger draws = new AtomicInteger();
        BotComputer bot = new BotComputer(new ExactOutcomeEvaluator(0), (scenario, cancelled) -> {
            estimates.incrementAndGet();
            return BotEstimateValues.fixed(scenario, 0.25);
        }, () -> {
            draws.incrementAndGet();
            return Math.nextDown(1.0);
        });
        assertEquals(ActionType.CALL, bot.decide(turn, () -> false).getType());
        assertEquals(1, estimates.get());
        assertEquals(1, draws.get());
    }

    @Test
    void aPrivateRoyalPricesBetsUsingLearnedResponses() {
        BotTurn turn = capture(2, 0, 600, ROYAL_FLOP, ROYAL_HOLES);
        BotTurn folds = withOpponentBeliefs(turn, learnedResponse(BettingStreet.FLOP, 20,
                ResolvedActionType.FOLD));
        BotTurn calls = withOpponentBeliefs(turn, learnedResponse(BettingStreet.FLOP, 20,
                ResolvedActionType.CALL));
        long foldTarget = BotComputer.actionForKnownOutcome(folds, Outcome.GUARANTEED_WIN)
                .getTargetAmount().orElseThrow();
        long callTarget = BotComputer.actionForKnownOutcome(calls, Outcome.GUARANTEED_WIN)
                .getTargetAmount().orElseThrow();
        assertEquals(20, foldTarget);
        assertTrue(callTarget > foldTarget && callTarget < 600);
    }

    @Test
    void aProvenWinCanRaiseOrUseTheOnlyLegalShortOpeningBet() {
        var raise = capture(2, 20, 600, ROYAL_FLOP, ROYAL_HOLES);
        var shortBet = capture(2, 0, 19, ROYAL_FLOP, ROYAL_HOLES);
        BotComputer bot = new BotComputer((request, cancelled) -> fail("Known winning hand."),
                () -> fail("The choice is deterministic."));
        var raised = bot.decide(raise, () -> false);
        assertEquals(ActionType.RAISE, raised.getType());
        assertTrue(raise.getLegalActions().getWager().orElseThrow()
                .allows(raised.getTargetAmount().orElseThrow()));
        var shortOpening = bot.decide(shortBet, () -> false);
        assertEquals(ActionType.BET, shortOpening.getType());
        assertEquals(19, shortOpening.getTargetAmount().orElseThrow());
    }

    @Test
    void aNonLosingStraightCallsWhenTheWorstTieCoversTheCost() {
        List<Card> board = List.of(new Card(Rank.TWO, Suit.CLUBS),
                new Card(Rank.SEVEN, Suit.DIAMONDS), new Card(Rank.NINE, Suit.HEARTS),
                new Card(Rank.JACK, Suit.SPADES), new Card(Rank.KING, Suit.CLUBS));
        List<Card> holes = List.of(new Card(Rank.QUEEN, Suit.DIAMONDS),
                new Card(Rank.TEN, Suit.HEARTS));
        BotTurn turn = capture(3, 980, 1_000, board, holes);
        BotComputer bot = new BotComputer((request, cancelled) -> fail("Known non-loss."),
                () -> fail("A covered call needs no draw."));
        assertEquals(ActionType.CALL, bot.decide(turn, () -> false).getType());
    }

    @Test
    void continuationCountsRaisesAndCallsOnTheirObservedStreet() {
        OpponentBeliefs raised = learnedResponse(BettingStreet.TURN, 8, ResolvedActionType.RAISE);
        assertEquals(8, raised.getContinuationEstimate(BettingStreet.TURN).getSuccesses());
        assertEquals(8, raised.getContinuationEstimate(BettingStreet.TURN).getOpportunities());
        assertEquals(0, raised.getContinuationEstimate(BettingStreet.FLOP).getOpportunities());
    }

    @ParameterizedTest
    @CsvSource({"7,FOLD,CHECK", "8,FOLD,BET", "8,CALL,CHECK"})
    void aLosingHandOnlyBluffsWithEvidenceOfFolds(int observations, ResolvedActionType response,
            ActionType expected) {
        BotTurn turn = capture(2, 0, 600, ROYAL_FLOP, LOW_HOLES);
        BotTurn observed = withOpponentBeliefs(turn, learnedResponse(BettingStreet.FLOP,
                observations, response));
        assertEquals(expected, BotComputer.actionForKnownOutcome(observed, Outcome.GUARANTEED_LOSS)
                .getType());
    }

    @Test
    void payoutFloorsUseOnlyEligibleLayersAndRoundTiesDown() {
        BotTurn turn = capture(3, 0, 600, ROYAL_FLOP, ROYAL_HOLES);
        EquityRequest scenario = EquityRequest.fromView(turn.getView(), List.of(
                new EquityPot("main", 101, Set.of(HERO, OPPONENT, "p2")),
                new EquityPot("side", 61, Set.of(HERO, OPPONENT)),
                new EquityPot("sole", 19, Set.of(HERO)),
                new EquityPot("ineligible", 23, Set.of(OPPONENT, "p2"))), 1);
        assertEquals(181, KnownOutcomePolicy.minimumPayout(scenario, Outcome.GUARANTEED_WIN));
        assertEquals(82, KnownOutcomePolicy.minimumPayout(scenario, Outcome.GUARANTEED_NON_LOSS));
        assertEquals(19, KnownOutcomePolicy.minimumPayout(scenario, Outcome.GUARANTEED_LOSS));
        assertThrows(IllegalArgumentException.class,
                () -> KnownOutcomePolicy.minimumPayout(scenario, Outcome.UNCERTAIN));
    }

    @Test
    void changingHiddenHoldingsAndDeckOrderPreservesTheExactAction() {
        GameState first = playingState(3, 0, 600, ROYAL_FLOP, ROYAL_HOLES, 12);
        GameState second = playingState(3, 0, 600, ROYAL_FLOP, ROYAL_HOLES, 91);
        assertFalse(first.getRemainingDeck().equals(second.getRemainingDeck()));
        assertFalse(first.getPlayerStates().get(1).getHoleCards()
                .equals(second.getPlayerStates().get(1).getHoleCards()));
        var firstAction = new BotComputer().decide(capture(first), () -> false);
        var secondAction = new BotComputer().decide(capture(second), () -> false);
        assertEquals(firstAction.getType(), secondAction.getType());
        assertEquals(firstAction.getTargetAmount(), secondAction.getTargetAmount());
    }

    @Test
    void invalidatingTheSessionRejectsTheProvenFlopAction() {
        GameState fixed = playingState(3, 0, 600, ROYAL_FLOP, ROYAL_HOLES, 12);
        GameInitializer initializer = new GameInitializer(count -> fixed, state -> state);
        SetupSession session = new SetupSession(initializer);
        session.start(1);
        BotTurn stale = session.getBotTurn(1).orElseThrow();
        var decision = new BotComputer().decide(stale, () -> false);
        session.invalidateBotTurns();
        assertFalse(session.applyBotDecision(stale, decision));
        assertTrue(session.getAcceptedActions().isEmpty());
    }

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
        assertEquals(expected, BotComputer.actionForKnownOutcome(turn, Outcome.GUARANTEED_LOSS)
                .getType());
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

    private static BotTurn withOpponentBeliefs(BotTurn turn, OpponentBeliefs beliefs) {
        return new BotTurn(turn.getMatchId(), turn.getRevision(), turn.getView(),
                turn.getLegalActions(), turn.getScenario(), turn.getCallRefund(),
                Map.of(OPPONENT, beliefs), turn.getPersonality());
    }

    static OpponentBeliefs learnedResponse(BettingStreet street, int count,
            ResolvedActionType response) {
        return learnedResponses(street, Collections.nCopies(count, response));
    }

    static OpponentBeliefs learnedResponses(BettingStreet street,
            List<ResolvedActionType> replies) {
        String match = "observed-replies";
        OpponentModel model = new OpponentModel(match, HERO, Set.of(OPPONENT));
        for (int index = 0; index < replies.size(); index++) {
            model.observe(observedResponse(match, street, replies.get(index), index + 1));
        }
        return model.snapshot(OPPONENT);
    }

    private static PublicDecision observedResponse(String match, BettingStreet street,
            ResolvedActionType reply, int sequence) {
        var choices = reply == ResolvedActionType.BET
                ? Set.of(ResolvedActionType.CHECK, ResolvedActionType.BET)
                : Set.of(ResolvedActionType.FOLD, ResolvedActionType.CALL,
                        ResolvedActionType.RAISE);
        var context = new DecisionContext(street, choices, true);
        long chips = reply == ResolvedActionType.FOLD ? 0 : 20;
        var action = new AcceptedAction("observed-hand", OPPONENT, street, reply, chips, chips,
                false, new StatsContext(PreflopRaiseType.NONE, false, false, false));
        return new PublicDecision(match, sequence, action, context, DecisionOrigin.PLAYER_CHOICE);
    }

    private static BotTurn capture(int players, long bet, long stack, List<Card> board,
            List<Card> holes) {
        return capture(playingState(players, bet, stack, board, holes, 12));
    }

    static BotTurn capture(GameState state) {
        return BotTable.start(state).capture(state, new BettingEngine().getLegalActions(state),
                1, 1);
    }

    static GameState playingState(int players, long bet, long stack, List<Card> board,
            List<Card> holes, long seed) {
        Deck fullDeck = new Deck(new Random(seed));
        fullDeck.shuffle();
        List<Card> deck = new ArrayList<>(fullDeck.deal(fullDeck.remainingCards()));
        deck.removeAll(board);
        deck.removeAll(holes);
        List<PlayerState> seats = new ArrayList<>();
        for (int index = 0; index < GameConfig.SEAT_COUNT; index++) {
            seats.add(postflopSeat(index, players, bet, stack, holes, deck));
        }
        long total = seats.stream().mapToLong(seat ->
                seat.getPlayer().getChipStack() + seat.getHandContribution()).sum();
        Set<String> pending = seats.stream().filter(PlayerState::canAct)
                .map(seat -> seat.getPlayer().getId())
                .filter(id -> bet == 0 || !id.equals(OPPONENT)).collect(Collectors.toSet());
        GameConfig config = new GameConfig().withTableTotal(total);
        GamePhase phase = List.of(GamePhase.FLOP, GamePhase.TURN, GamePhase.RIVER)
                .get(board.size() - ROYAL_FLOP.size());
        return GameState.create(config, "postflop-boundary", phase,
                seats, new SeatOrder(seats).positionsFor(players - 1), Optional.of(HERO), board,
                deck, new BettingProgress(bet, Math.max(bet, config.getBigBlind()),
                        pending, pending));
    }

    private static PlayerState postflopSeat(int index, int players, long bet, long stack,
            List<Card> holes, List<Card> deck) {
        String id = "p" + index;
        if (index >= players) {
            return new PlayerState(new Player(id, index, PlayerType.BOT, 0),
                    PlayerStatus.ELIMINATED, List.of(), 0, 0);
        }
        long round = index == 1 ? bet : 0;
        long balance = index == 0 ? stack : OPPONENT_STACK - round;
        Player player = new Player(id, index, PlayerType.BOT, balance);
        List<Card> cards = index == 0 ? holes : List.of(deck.remove(0), deck.remove(0));
        return new PlayerState(player, PlayerStatus.ACTIVE, cards, round,
                PRIOR_COMMITMENT + round);
    }
}
