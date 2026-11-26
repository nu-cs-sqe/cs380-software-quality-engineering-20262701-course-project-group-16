package application;

import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.BettingEngine;
import domain.model.BettingProgress;
import domain.model.BettingResult;
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
import domain.model.PotSettlement;
import domain.model.Rank;
import domain.model.ResolvedActionType;
import domain.model.Settlement;
import domain.model.ShowdownResult;
import domain.model.Suit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewedHandTest {
    private static final int PASSIVE_ACTION_LIMIT = 4 * GameConfig.SEAT_COUNT;
    private static final long DEALER_SEED = 17;
    private static final long DECK_SEED = 42;
    private static final long FIVE_WAY_POT = 100;

    @Test
    void foldAndShowRequiresReviewAndReturnsTheAcceptedFold() {
        SetupSession session = new SetupSession();
        session.start(GameConfig.SEAT_COUNT);
        String folder = current(session);
        assertThrows(IllegalStateException.class, () -> session.foldAndShow(folder));
        assertTrue(session.getAcceptedActions().isEmpty());

        ReviewedHand review = session.beginReview(true, false);
        assertThrows(IllegalStateException.class, () -> session.beginReview(false, true));
        var fold = session.foldAndShow(folder);
        assertEquals(ResolvedActionType.FOLD, fold.getType());
        assertEquals(folder, fold.getPlayerId());
        assertEquals(List.of(fold), session.getAcceptedActions());
        assertEquals(review.ownerCards(folder), review.shown().get(folder));
    }

    @Test
    void reviewStartsOnlyBeforeTheFirstAcceptedDecision() {
        SetupSession session = fixedDeal();
        session.submitAction(new ActionRequest(current(session), ActionType.CALL));
        String actor = current(session);
        assertThrows(IllegalStateException.class, () -> session.beginReview(false, false));
        assertEquals(actor, current(session));
        assertEquals(1, session.getAcceptedActions().size());
    }

    @Test
    void royalBoardTiesTheMainAndSidePotsAfterAllLiveHandsAreExposed() {
        List<Card> deck = new ArrayList<>(new Deck().deal(52));
        List<Card> board = List.of(new Card(Rank.TEN, Suit.HEARTS),
                new Card(Rank.JACK, Suit.HEARTS), new Card(Rank.QUEEN, Suit.HEARTS),
                new Card(Rank.KING, Suit.HEARTS), new Card(Rank.ACE, Suit.HEARTS));
        deck.removeAll(board);
        List<PlayerState> seats = new ArrayList<>();
        seats.add(seat(0, 0, 100, PlayerStatus.ALL_IN, deck.subList(0, 2)));
        seats.add(seat(1, 0, 200, PlayerStatus.ALL_IN, deck.subList(2, 4)));
        seats.add(seat(2, 5700, 0, PlayerStatus.ACTIVE, deck.subList(4, 6)));
        for (int i = 3; i < 6; i++) {
            seats.add(seat(i, 0, 0, PlayerStatus.ELIMINATED, List.of()));
        }
        List<Card> runout = List.of(deck.get(6), board.get(0), board.get(1), board.get(2),
                deck.get(7), board.get(3), deck.get(8), board.get(4));
        GameState before = GameState.create(new GameConfig(), "split", GamePhase.PREFLOP,
                seats, new HandPositions(0, 1, 2), Optional.of("p2"), List.of(), runout,
                new BettingProgress(200, 100, Set.of("p2"), Set.of()));
        ReviewedHand review = new ReviewedHand(before, false, false);
        assertTrue(review.shown().isEmpty());
        BettingEngine engine = new BettingEngine();
        BettingResult call = engine.apply(before, new ActionRequest("p2", ActionType.CALL));
        GameState finalHand = call.getState();
        while (finalHand.getPhase() != GamePhase.SHOWDOWN) {
            finalHand = engine.advanceStreet(finalHand);
        }
        review.accepted(call.getAcceptedAction(), finalHand, true, false);
        assertEquals(3, review.shown().size());
        assertFalse(review.immediate());
        assertEquals(Map.of("from", 0, "shown", review.shown()), review.runout());
        assertEquals(true, review.events().get(1).get("allIn"));
        assertThrows(IllegalArgumentException.class, () -> review.decide("p0", false));
        Settlement paid = new PotSettlement().settle(finalHand, review.withdrawnClaims());
        review.paid(paid);
        assertEquals(List.of(Map.of("p0", 100L, "p1", 100L, "p2", 100L),
                Map.of("p1", 100L, "p2", 100L)), paid.getPots());
        assertEquals(Map.of("p0", board, "p1", board, "p2", board), review.winningCards());
        assertEquals(100L, paid.getPayouts().get("p0"));
        assertEquals(200L, paid.getPayouts().get("p1"));
        assertEquals(200L, paid.getPayouts().get("p2"));
        assertEquals(Map.of("p0", "STRAIGHT_FLUSH", "p1", "STRAIGHT_FLUSH",
                "p2", "STRAIGHT_FLUSH"), review.categories());
        assertEquals(List.of(Map.of("p0", "100", "p1", "100", "p2", "100"),
                Map.of("p1", "100", "p2", "100")), lastEvent(review).get("pots"));
        assertEquals(Map.of("p0", "0", "p1", "0", "p2", "0", "p3", "0", "p4", "0", "p5", "0"),
                lastEvent(review).get("refunds"));
        assertEquals(List.of("p0", "p1", "p2"), review.events().get(0).get("dealt"));

        ReviewedHand openedAtShowdown = new ReviewedHand(finalHand, false, false);
        assertEquals(review.shown(), openedAtShowdown.shown());
        assertEquals("SHOWDOWN", lastEvent(openedAtShowdown).get("kind"));
        assertTrue(openedAtShowdown.nextToShow().isEmpty());
    }

    @Test
    void optionalFoldedCardsStayDeadAndPrivateUntilTheChosenTime() {
        SetupSession session = new SetupSession();
        session.start(6);
        ReviewedHand review = session.beginReview(false, false);
        String folder = current(session);
        session.submitAction(new ActionRequest(folder, ActionType.FOLD));
        session.chooseDisclosure(folder, true);
        assertTrue(review.shown().isEmpty());
        var earlier = review.events();
        while (session.getResult().isEmpty()) {
            session.submitAction(new ActionRequest(current(session), ActionType.FOLD));
        }
        assertEquals(review.ownerCards(folder), review.shown().get(folder));
        assertFalse(review.showAfterHand(folder));
        assertTrue(earlier.stream().noneMatch(event -> event.containsKey("cards")));
        Settlement paid = session.settleHand();
        assertEquals(0L, paid.getPayouts().get(folder));
        assertFalse(review.categories().containsKey(folder));
    }

    private static String current(SetupSession session) {
        SetupView view = session.getView().orElseThrow();
        return view.getPlayers().get(view.getCurrentActorSeat()).getId();
    }

    @Test
    void strongerMuckedHandReceivesNothingFromThePot() {
        SetupSession session = fixedDeal();
        ReviewedHand review = session.beginReview(false, false);
        String folder = foldCurrentPlayer(session);
        playToShowdown(session, review);
        assertTrue(session.getResult().isEmpty());
        assertThrows(IllegalStateException.class, session::settleHand);
        String claimant = review.nextToShow().orElseThrow();

        Set<String> mucked = showFirstAndMuckOthers(session, review, claimant);
        List<Card> board = session.getView().orElseThrow().getCommunityCards();
        var evaluator = new PokerHandEvaluator();
        assertTrue(mucked.stream().anyMatch(id -> evaluator.compare(
                holding(board, review.ownerCards(id)), holding(board, review.ownerCards(claimant)))
                > 0), "This deal must have a stronger hand among the mucked hands");
        assertEquals(Set.of(folder), review.folded());
        assertEquals(4, mucked.size());
        assertEquals(List.of(claimant), session.getResult().orElseThrow().getWinnerIds());
        List<Map<String, Object>> closedShowdown = review.events();
        for (String id : mucked) {
            assertThrows(IllegalArgumentException.class, () -> session.chooseDisclosure(id, true));
        }
        assertEquals(closedShowdown, review.events());

        Settlement paid = session.settleHand();
        assertEquals(List.of(Map.of(claimant, FIVE_WAY_POT)), paid.getPots());
        assertEquals(0L, paid.getPayouts().get(folder));
        mucked.forEach(id -> assertEquals(0L, paid.getPayouts().get(id)));
        assertEquals(Set.of(claimant), review.shown().keySet());
        assertEquals(Set.of(claimant), review.categories().keySet());
    }

    @Test
    void voluntaryDisclosureWaitsForSettlementPublication() {
        SetupSession session = fixedDeal();
        ReviewedHand review = session.beginReview(false, false);
        String folder = foldCurrentPlayer(session);
        playToShowdown(session, review);
        String claimant = review.nextToShow().orElseThrow();
        List<Map<String, Object>> beforeChoices = review.events();
        assertThrows(IllegalStateException.class, () -> session.quit(claimant));
        assertFalse(session.hasDeparted(claimant));
        assertEquals(beforeChoices, review.events());

        showFirstAndMuckOthers(session, review, claimant);
        ShowdownResult result = session.getResult().orElseThrow();
        List<Map<String, Object>> beforePayment = review.events();
        PreparedSettlement prepared = session.prepareSettlement();
        assertThrows(IllegalStateException.class, () -> session.chooseDisclosure(folder, true));
        prepared.save();
        assertThrows(IllegalStateException.class, () -> session.chooseDisclosure(folder, true));
        assertEquals(beforePayment, review.events());
        assertTrue(session.getSettlement().isEmpty());

        Settlement paid = session.publishSettlement(prepared);
        assertEquals("AWARD", lastEvent(review).get("kind"));
        assertFalse(review.shown().containsKey(folder));
        session.chooseDisclosure(folder, true);
        assertSame(result, session.getResult().orElseThrow());
        assertSame(paid, session.getSettlement().orElseThrow());
        assertEquals(0L, paid.getPayouts().get(folder));
        assertEquals(Map.of("kind", "VOLUNTARILY_SHOWN", "street", GamePhase.SHOWDOWN,
                "id", folder, "cards", review.ownerCards(folder)), lastEvent(review));
        assertEquals(1, review.events().stream()
                .filter(event -> "AWARD".equals(event.get("kind"))).count());
    }

    @Test
    void historyStartsWithTheConfiguredBlindsAndDealsEachStreetOnce() {
        SetupSession session = fixedDeal();
        ReviewedHand review = session.beginReview(false, false);
        SetupView opening = session.getView().orElseThrow();
        int smallSeat = opening.getPositions().getSmallBlindSeat();
        int bigSeat = opening.getPositions().getBigBlindSeat();
        assertEquals(10L, opening.getSmallBlind());
        assertEquals(20L, opening.getBigBlind());
        List<Map<String, Object>> openingEvents = review.events();
        assertEquals(opening.getPositions().getDealerSeat(), openingEvents.get(0).get("dealer"));
        Map<String, String> stacks = new HashMap<>();
        opening.getPlayers().forEach(p -> stacks.put(p.getId(), "1000"));
        assertEquals(stacks, openingEvents.get(0).get("stacks"));
        assertEquals(Map.of("kind", "SMALL_BLIND", "street", GamePhase.PREFLOP,
                "id", opening.getPlayers().get(smallSeat).getId(), "amount", "10", "allIn", false),
                openingEvents.get(1));
        assertEquals(Map.of("kind", "BIG_BLIND", "street", GamePhase.PREFLOP,
                "id", opening.getPlayers().get(bigSeat).getId(), "amount", "20", "allIn", false),
                openingEvents.get(2));

        String caller = current(session);
        session.submitAction(new ActionRequest(caller, ActionType.CALL));
        assertEquals(Map.of("kind", "CALL", "street", GamePhase.PREFLOP,
                "id", caller, "target", "20", "amount", "20", "allIn", false), lastEvent(review));
        playToShowdown(session, review);
        assertEquals(3, openingEvents.size());
        List<Card> board = session.getView().orElseThrow().getCommunityCards();
        assertEquals(List.of(deal(GamePhase.FLOP, board.subList(0, 3)),
                deal(GamePhase.TURN, board.subList(0, 4)), deal(GamePhase.RIVER, board)),
                review.events().stream().filter(event -> "DEAL".equals(event.get("kind")))
                        .collect(Collectors.toList()));
        assertEquals(Map.of("kind", "SHOWDOWN", "street", GamePhase.SHOWDOWN), lastEvent(review));
        assertTrue(review.events().stream().noneMatch(event -> event.containsKey("cards")));
    }

    @Test
    void lastUnshownClaimantMayRevealTheWinningCategoryAfterPayment() {
        SetupSession session = fixedDeal();
        ReviewedHand review = session.beginReview(false, false);
        playToShowdown(session, review);
        for (int decision = 1; decision < GameConfig.SEAT_COUNT; decision++) {
            session.chooseDisclosure(review.nextToShow().orElseThrow(), false);
        }
        String winner = session.getResult().orElseThrow().getWinnerIds().get(0);
        assertTrue(review.nextToShow().isEmpty());
        assertTrue(review.shown().isEmpty());
        assertEquals(ReviewedHand.Visibility.HIDDEN, review.visibility().get(winner));
        List<Card> board = session.getView().orElseThrow().getCommunityCards();
        List<Card> holeCards = review.ownerCards(winner);
        String category = new PokerHandEvaluator().category(holding(board, holeCards));

        Settlement paid = session.settleHand();
        assertEquals(Map.of(), lastEvent(review).get("categories"));
        session.chooseDisclosure(winner, true);
        assertEquals(Map.of(winner, category), review.categories());
        assertEquals(Map.of("kind", "VOLUNTARILY_SHOWN", "street", GamePhase.SHOWDOWN,
                "id", winner, "cards", holeCards, "category", category), lastEvent(review));
        assertSame(paid, session.getSettlement().orElseThrow());
    }

    @Test
    void automaticRiverShowsEachLiveHandInOrderAndStreetEventsArriveImmediately() {
        SetupSession session = fixedDeal();
        ReviewedHand review = session.beginReview(false, false, true);
        String folded = foldCurrentPlayer(session);
        for (int turn = 0; turn < PASSIVE_ACTION_LIMIT && session.getResult().isEmpty(); turn++) {
            GamePhase previous = phase(session);
            actPassively(session);
            if (phase(session) != previous && phase(session) != GamePhase.SHOWDOWN) {
                assertEquals(deal(phase(session), session.getView().orElseThrow()
                        .getCommunityCards()), lastEvent(review));
            }
        }
        assertTrue(session.getResult().isPresent());
        assertTrue(review.nextToShow().isEmpty());
        assertEquals(5, review.shown().size());
        assertFalse(review.shown().containsKey(folded));
        Settlement paid = session.settleHand();
        assertEquals(0L, paid.getPayouts().get(folded));
        var evaluator = new PokerHandEvaluator();
        List<Card> board = session.getView().orElseThrow().getCommunityCards();
        review.winningCards().forEach((id, best) -> {
            assertEquals(5, best.size());
            assertEquals(evaluator.winningCards(holding(board, review.shown().get(id))), best);
        });
    }

    @Test
    void lastRiverRaiserHasTheFirstShowdownDecision() {
        SetupSession session = fixedDeal();
        ReviewedHand review = session.beginReview(false, false);
        for (int turn = 0; turn < PASSIVE_ACTION_LIMIT && phase(session) != GamePhase.RIVER;
                turn++) {
            actPassively(session);
        }
        assertEquals(GamePhase.RIVER, phase(session));
        session.submitAction(new ActionRequest(current(session), ActionType.BET, 20));
        String raiser = current(session);
        session.submitAction(new ActionRequest(raiser, ActionType.RAISE, 40));
        playToShowdown(session, review);
        assertEquals(Optional.of(raiser), review.nextToShow());
        String other = session.getView().orElseThrow().getPlayers().stream()
                .map(Player::getId).filter(id -> !id.equals(raiser)).findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> session.chooseDisclosure(other, true));
        assertTrue(review.shown().isEmpty());
    }

    @Test
    void muckCancelsADeferredVoluntaryShow() {
        SetupSession session = fixedDeal();
        ReviewedHand review = session.beginReview(false, false);
        String folder = foldCurrentPlayer(session);
        session.chooseDisclosure(folder, true);
        assertTrue(review.showAfterHand(folder));
        session.chooseDisclosure(folder, false);
        assertFalse(review.showAfterHand(folder));
        assertEquals(ReviewedHand.Visibility.MUCKED, review.visibility().get(folder));
        assertEquals(Map.of("kind", "MUCKED", "street", GamePhase.PREFLOP, "id", folder),
                lastEvent(review));
        List<Map<String, Object>> afterMuck = review.events();
        session.chooseDisclosure(folder, false);
        assertEquals(afterMuck, review.events());
        playToShowdown(session, review);
        showFirstAndMuckOthers(session, review, review.nextToShow().orElseThrow());
        assertFalse(review.shown().containsKey(folder));
        session.chooseDisclosure(folder, true);
        assertEquals(review.ownerCards(folder), review.shown().get(folder));
        assertThrows(IllegalArgumentException.class, () -> session.chooseDisclosure(folder, false));
    }

    @Test
    void botPolicyControlsVoluntaryExposureWithoutChangingEligibility() {
        for (boolean show : List.of(false, true)) {
            SetupSession session = new SetupSession();
            session.start(1);
            ReviewedHand review = session.beginReview(false, show);
            while (session.getResult().isEmpty()) {
                session.submitAction(new ActionRequest(current(session), ActionType.FOLD));
                if (session.getResult().isEmpty()) {
                    assertTrue(review.shown().isEmpty());
                }
            }
            assertEquals(show ? 5 : 0, review.shown().size());
            assertEquals(5, review.folded().size());
            Settlement paid = session.settleHand();
            review.folded().forEach(id -> assertEquals(0L, paid.getPayouts().get(id)));
        }
    }

    @ParameterizedTest
    @CsvSource({"QUEEN,false", "KING,false", "ACE,false", "QUEEN,true"})
    void botTablesAWinnerOrTieAndMucksALosingHand(Rank botPair, boolean forceShow) {
        GameState river = headsUpRiver(botPair, PlayerType.HUMAN);
        SetupSession session = new SetupSession(new GameInitializer(seats -> river, hand -> hand));
        session.start(1);
        ReviewedHand review = session.beginReview(false, forceShow);
        actPassively(session);
        actPassively(session);
        assertEquals(Optional.of("p1"), review.nextToShow());
        assertThrows(IllegalArgumentException.class, () -> session.chooseDisclosure("p0", true));
        assertThrows(IllegalArgumentException.class, () -> review.ownerCards("p0"));
        assertThrows(IllegalArgumentException.class, () -> review.showAfterHand("p0"));
        assertThrows(IllegalArgumentException.class, () -> review.ownerCards("p2"));

        session.chooseDisclosure("p1", true);
        boolean botMucked = botPair == Rank.QUEEN && !forceShow;
        assertEquals(botMucked ? ReviewedHand.Visibility.MUCKED
                : ReviewedHand.Visibility.SHOWDOWN_REVEALED, review.visibility().get("p0"));
        assertEquals(botMucked ? "MUCKED" : "SHOWDOWN_REVEALED", lastEvent(review).get("kind"));
        assertEquals("p0", lastEvent(review).get("id"));
        assertEquals(botMucked ? Set.of("p1") : Set.of("p0", "p1"), review.shown().keySet());
        assertEquals(expectedHeadsUpPayout(botPair).keySet(),
                Set.copyOf(session.getResult().orElseThrow().getWinnerIds()));
        assertEquals(botMucked, session.getResult().orElseThrow().isUncontested());
        assertEquals(List.of(expectedHeadsUpPayout(botPair)), session.settleHand().getPots());
    }

    @Test
    void automaticBotMuckPublishesTheUncontestedResultWithTheClosingAction() {
        GameState river = headsUpRiver(Rank.QUEEN, PlayerType.BOT);
        SetupSession session = new SetupSession(new GameInitializer(seats -> river, hand -> hand));
        session.start(1);
        ReviewedHand review = session.beginReview(false, false);
        actPassively(session);
        actPassively(session);
        assertEquals(List.of("p1"), session.getResult().orElseThrow().getWinnerIds());
        assertTrue(session.getResult().orElseThrow().isUncontested());
        assertEquals(ReviewedHand.Visibility.MUCKED, review.visibility().get("p0"));
        assertEquals(Set.of("p1"), review.shown().keySet());
        assertEquals(List.of(Map.of("p1", 200L)), session.settleHand().getPots());
    }

    @Test
    void aFoldedStrongerHandDoesNotMakeABotMuckItsTie() {
        GameState river = headsUpRiver(Rank.KING, PlayerType.HUMAN);
        List<PlayerState> seats = new ArrayList<>(river.getPlayerStates());
        seats.set(2, seat(2, 1, 0, PlayerStatus.FOLDED,
                pair(Rank.ACE, Suit.CLUBS, Suit.DIAMONDS)));
        GameConfig funded = river.getConfig().withTableTotal(river.getTotalChips() + 1);
        GameState withFold = GameState.create(funded, river.getHandId(), GamePhase.RIVER,
                seats, new HandPositions(0, 1, 2), Optional.of("p1"), river.getCommunityCards(),
                List.of(), river.getBettingProgress());
        SetupSession session = new SetupSession(
                new GameInitializer(chairs -> withFold, hand -> hand));
        session.start(1);
        ReviewedHand review = session.beginReview(false, false);
        actPassively(session);
        actPassively(session);
        session.chooseDisclosure("p1", true);

        assertEquals(Set.of("p0", "p1"), review.shown().keySet());
        assertEquals(List.of(Map.of("p0", 100L, "p1", 100L)), session.settleHand().getPots());
    }

    @Test
    void optionalBotExposureShowsTheBotWinnerButKeepsTheHumanFoldPrivate() {
        GameState river = headsUpRiver(Rank.QUEEN, PlayerType.HUMAN);
        SetupSession session = new SetupSession(new GameInitializer(seats -> river, hand -> hand));
        session.start(1);
        ReviewedHand review = session.beginReview(false, true);
        assertEquals("p1", foldCurrentPlayer(session));

        assertEquals(Set.of("p1"), review.folded());
        assertEquals(Set.of("p0"), review.shown().keySet());
        assertEquals(ReviewedHand.Visibility.VOLUNTARILY_SHOWN, review.visibility().get("p0"));
        assertEquals(ReviewedHand.Visibility.HIDDEN, review.visibility().get("p1"));
        assertEquals(List.of(Map.of("p0", 200L)), session.settleHand().getPots());
    }

    @Test
    void foldedBotStaysPrivateAndCannotMakeAHumanDisclosureRequest() {
        GameState river = headsUpRiver(Rank.ACE, PlayerType.HUMAN);
        SetupSession session = new SetupSession(new GameInitializer(seats -> river, hand -> hand));
        session.start(1);
        ReviewedHand review = session.beginReview(false, false);
        actPassively(session);
        assertEquals("p0", foldCurrentPlayer(session));

        assertThrows(IllegalArgumentException.class, () -> session.chooseDisclosure("p0", true));
        assertEquals(ReviewedHand.Visibility.HIDDEN, review.visibility().get("p0"));
        assertTrue(review.shown().isEmpty());
        assertEquals(List.of(Map.of("p1", 200L)), session.settleHand().getPots());
    }

    @Test
    void immediateBotFoldRecordsItsCardsBeforeTheShowdownEvent() {
        GameState river = headsUpRiver(Rank.ACE, PlayerType.HUMAN);
        SetupSession session = new SetupSession(new GameInitializer(seats -> river, hand -> hand));
        session.start(1);
        ReviewedHand review = session.beginReview(true, true);
        actPassively(session);
        int beforeFold = review.events().size();
        assertEquals("p0", foldCurrentPlayer(session));

        List<Map<String, Object>> events = review.events();
        assertEquals(List.of("FOLD", "VOLUNTARILY_SHOWN", "SHOWDOWN"),
                events.subList(beforeFold, events.size()).stream()
                        .map(event -> event.get("kind")).collect(Collectors.toList()));
        assertEquals(river.getPlayerStates().get(0).getHoleCards(), review.shown().get("p0"));
        assertEquals(List.of(Map.of("p1", 200L)), session.settleHand().getPots());
    }

    @Test
    void paidHandWithoutAcceptedActionsCannotStartReview() {
        GameState river = headsUpRiver(Rank.ACE, PlayerType.HUMAN);
        BettingEngine engine = new BettingEngine();
        GameState checked = engine.apply(river, new ActionRequest("p1", ActionType.CHECK))
                .getState();
        GameState closed = engine.apply(checked, new ActionRequest("p0", ActionType.CHECK))
                .getState();
        GameState showdown = engine.advanceStreet(closed);
        SetupSession session = new SetupSession(
                new GameInitializer(seats -> showdown, hand -> hand));
        session.start(1);
        Settlement paid = session.settleHand();

        assertTrue(session.getAcceptedActions().isEmpty());
        assertThrows(IllegalStateException.class, () -> session.beginReview(false, false));
        assertSame(paid, session.getSettlement().orElseThrow());
    }

    @Test
    void uncontestedAllInWinsWithoutATabledHandOrRunout() {
        SetupSession session = fixedDeal();
        ReviewedHand review = session.beginReview(true, false, true);
        String winner = current(session);
        session.submitAction(new ActionRequest(winner, ActionType.RAISE, 1000));
        String folder = foldCurrentPlayer(session);
        session.chooseDisclosure(folder, true);
        while (session.getResult().isEmpty()) {
            foldCurrentPlayer(session);
        }
        assertTrue(review.immediate());
        assertEquals(List.of(winner), session.getResult().orElseThrow().getWinnerIds());
        assertTrue(review.nextToShow().isEmpty());
        assertEquals(Set.of(folder), review.shown().keySet());
        assertTrue(review.runout().isEmpty());
        assertTrue(review.events().stream().noneMatch(event -> "DEAL".equals(event.get("kind"))));
        Settlement paid = session.settleHand();
        assertEquals(List.of(Map.of(winner, 50L)), paid.getPots());
        assertEquals(980L, paid.getRefunds().get(winner));
    }

    @Test
    void riverAllInTablesBothHandsWithoutRestartingTheBoardRunout() {
        GameState river = headsUpRiver(Rank.ACE, PlayerType.HUMAN);
        SetupSession session = new SetupSession(new GameInitializer(seats -> river, hand -> hand));
        session.start(1);
        ReviewedHand review = session.beginReview(false, false);
        session.submitAction(new ActionRequest("p1", ActionType.BET, 2900));
        assertTrue(review.shown().isEmpty());
        session.submitAction(new ActionRequest("p0", ActionType.CALL));
        assertTrue(review.runout().isEmpty());
        assertEquals(Set.of("p0", "p1"), review.shown().keySet());
        assertEquals(3, review.events().stream()
                .filter(event -> "DEAL".equals(event.get("kind"))).count());
        assertEquals(List.of(Map.of("p0", 6000L)), session.settleHand().getPots());
    }

    @Test
    void preflopWinnerCanShowWithoutPublishingAHandCategory() {
        SetupSession session = fixedDeal();
        ReviewedHand review = session.beginReview(false, false);
        while (session.getResult().isEmpty()) {
            foldCurrentPlayer(session);
        }
        String winner = session.getResult().orElseThrow().getWinnerIds().get(0);
        Settlement paid = session.settleHand();
        assertTrue(session.getView().orElseThrow().getCommunityCards().isEmpty());
        assertTrue(review.shown().isEmpty());

        session.chooseDisclosure(winner, true);

        List<Card> cards = review.ownerCards(winner);
        assertEquals(Map.of(winner, cards), review.shown());
        assertTrue(review.categories().isEmpty());
        assertTrue(review.winningCards().isEmpty());
        assertEquals(Map.of("kind", "VOLUNTARILY_SHOWN", "street", GamePhase.SHOWDOWN,
                "id", winner, "cards", cards), lastEvent(review));
        assertSame(paid, session.getSettlement().orElseThrow());
    }

    @Test
    void winningCategoryCanBePublishedWithExactlyFiveKnownCards() {
        SetupSession session = fixedDeal();
        ReviewedHand review = session.beginReview(false, false);
        while (phase(session) == GamePhase.PREFLOP) {
            actPassively(session);
        }
        assertEquals(GamePhase.FLOP, phase(session));
        while (session.getResult().isEmpty()) {
            foldCurrentPlayer(session);
        }
        String winner = session.getResult().orElseThrow().getWinnerIds().get(0);
        session.settleHand();
        session.chooseDisclosure(winner, true);
        List<Card> known = holding(session.getView().orElseThrow().getCommunityCards(),
                review.ownerCards(winner));
        assertEquals(PokerHandEvaluator.HAND_SIZE, known.size());
        String category = new PokerHandEvaluator().category(known);
        assertEquals(Map.of(winner, category), review.categories());
        assertEquals(category, lastEvent(review).get("category"));
    }

    private static Map<String, Long> expectedHeadsUpPayout(Rank botPair) {
        if (botPair == Rank.QUEEN) {
            return Map.of("p1", 200L);
        }
        return botPair == Rank.KING ? Map.of("p0", 100L, "p1", 100L) : Map.of("p0", 200L);
    }

    private static GameState headsUpRiver(Rank botPair, PlayerType opponentType) {
        List<PlayerState> seats = new ArrayList<>();
        seats.add(new PlayerState(new Player("p0", 0, PlayerType.BOT, 2900),
                PlayerStatus.ACTIVE, pair(botPair, Suit.HEARTS, Suit.SPADES), 0, 100));
        seats.add(new PlayerState(new Player("p1", 1, opponentType, 2900),
                PlayerStatus.ACTIVE, pair(Rank.KING, Suit.CLUBS, Suit.DIAMONDS), 0, 100));
        for (int index = 2; index < GameConfig.SEAT_COUNT; index++) {
            seats.add(seat(index, 0, 0, PlayerStatus.ELIMINATED, List.of()));
        }
        List<Card> board = List.of(new Card(Rank.TWO, Suit.HEARTS), new Card(Rank.FOUR, Suit.CLUBS),
                new Card(Rank.SIX, Suit.DIAMONDS), new Card(Rank.EIGHT, Suit.SPADES),
                new Card(Rank.JACK, Suit.HEARTS));
        Set<String> pending = Set.of("p0", "p1");
        return GameState.create(new GameConfig(), "heads-up", GamePhase.RIVER, seats,
                new HandPositions(0, 0, 1), Optional.of("p1"), board, List.of(),
                new BettingProgress(0, 20, pending, pending));
    }

    private static List<Card> pair(Rank rank, Suit left, Suit right) {
        return List.of(new Card(rank, left), new Card(rank, right));
    }

    private static SetupSession fixedDeal() {
        var initializer = new GameInitializer(new GameConfig(), new Random(DEALER_SEED),
                new Random(DECK_SEED));
        SetupSession session = new SetupSession(initializer);
        session.start(GameConfig.SEAT_COUNT);
        return session;
    }

    private static String foldCurrentPlayer(SetupSession session) {
        String folder = current(session);
        session.submitAction(new ActionRequest(folder, ActionType.FOLD));
        return folder;
    }

    private static void playToShowdown(SetupSession session, ReviewedHand review) {
        for (int turn = 0; turn < PASSIVE_ACTION_LIMIT && review.nextToShow().isEmpty(); turn++) {
            actPassively(session);
        }
        assertTrue(review.nextToShow().isPresent(), "The passive hand must reach showdown");
    }

    private static GamePhase phase(SetupSession session) {
        return session.getView().orElseThrow().getPhase();
    }

    private static void actPassively(SetupSession session) {
        ActionType action = session.getAvailableActions().contains(ActionType.CHECK)
                ? ActionType.CHECK : ActionType.CALL;
        session.submitAction(new ActionRequest(current(session), action));
    }

    private static Set<String> showFirstAndMuckOthers(SetupSession session, ReviewedHand review,
            String claimant) {
        session.chooseDisclosure(claimant, true);
        for (int choice = 1; choice < GameConfig.SEAT_COUNT && review.nextToShow().isPresent();
                choice++) {
            session.chooseDisclosure(review.nextToShow().orElseThrow(), false);
        }
        assertTrue(review.nextToShow().isEmpty());
        return review.withdrawnClaims();
    }

    private static List<Card> holding(List<Card> board, List<Card> holeCards) {
        List<Card> holding = new ArrayList<>(board);
        holding.addAll(holeCards);
        return holding;
    }

    private static Map<String, Object> deal(GamePhase street, List<Card> board) {
        return Map.of("kind", "DEAL", "street", street, "board", List.copyOf(board));
    }

    private static Map<String, Object> lastEvent(ReviewedHand review) {
        List<Map<String, Object>> events = review.events();
        return events.get(events.size() - 1);
    }

    private static PlayerState seat(int seat, long stack, long contribution,
            PlayerStatus status, List<Card> cards) {
        return new PlayerState(new Player("p" + seat, seat, PlayerType.HUMAN, stack),
                status, cards, contribution, contribution);
    }
}
