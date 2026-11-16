package application;

import bot.EquityRequest;
import bot.learning.OpponentBeliefs;
import bot.policy.BotPersonality;
import domain.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionBotIntegrationTest {
    @Test
    void sb01CapturesOnlyFilteredImmutableBotDataAndBoundedWork() {
        SetupSession empty = new SetupSession();
        assertTrue(empty.getBotTurn(64).isEmpty());
        for (int count : new int[] {0, -1, EquityRequest.MAX_SIMULATIONS + 1}) {
            assertThrows(IllegalArgumentException.class, () -> empty.getBotTurn(count));
        }
        SetupSession session = BotFixtures.started(1);
        session.revealCards();
        BotTurn turn = session.getBotTurn(64).orElseThrow();
        String actor = BotFixtures.actor(session);
        assertEquals(actor, turn.getView().getViewerId());
        assertEquals(0, turn.getRevision());
        assertFalse(turn.getMatchId().isBlank());
        assertEquals(2, turn.getView().getPlayers().get(3).getHoleCards().size());
        assertTrue(turn.getView().getPlayers().get(0).getHoleCards().isEmpty());
        assertEquals(2, session.getView().orElseThrow().getHoleCards().size());
        assertEquals(64, turn.getScenario().getSimulations());
        assertEquals(5, turn.getScenario().getOpponents().size());
        assertEquals(50, turn.getScenario().getEligiblePotAmount());
        assertEquals(2, turn.getScenario().getPots().size());
        assertEquals(0, turn.getCallRefund());
        assertEquals(20, turn.getLegalActions().getCallCost());
        assertEquals(BotPersonality.CAUTIOUS, turn.getPersonality());
        assertEquals(5, turn.getBeliefs().size());
        assertThrows(UnsupportedOperationException.class, () -> turn.getBeliefs().clear());
        assertEquals(1, session.getBotTurn(1).orElseThrow().getScenario().getSimulations());
        assertEquals(10000, session.getBotTurn(10000).orElseThrow().getScenario().getSimulations());
        assertTrue(session.getAcceptedActions().isEmpty());
    }

    @Test
    void sb02AppliesOnceAndPublishesLearningOnlyAfterAcceptance() {
        SetupSession session = BotFixtures.started(1);
        BotTurn first = session.getBotTurn(64).orElseThrow();
        String actor = first.getView().getViewerId();
        String observer = session.getView().orElseThrow().getPlayers().get(4).getId();
        Map<String, OpponentBeliefs> before = session.getBotBeliefs(observer);
        assertThrows(IllegalArgumentException.class, () -> session.applyBotDecision(first,
                new ActionRequest(actor, ActionType.CHECK)));
        assertThrows(IllegalArgumentException.class, () -> session.applyBotDecision(first,
                new ActionRequest(observer, ActionType.CALL)));
        assertEquals(0, session.getBotBeliefs(observer).get(actor).getObservedDecisions());
        assertTrue(session.applyBotDecision(first, new ActionRequest(actor, ActionType.CALL)));
        assertEquals(1, session.getBotBeliefs(observer).get(actor).getObservedDecisions());
        assertEquals(0, before.get(actor).getObservedDecisions());
        assertEquals(1, session.getAcceptedActions().size());
        long pot = session.getView().orElseThrow().getPot();
        assertFalse(session.applyBotDecision(first, new ActionRequest(actor, ActionType.CALL)));
        assertEquals(pot, session.getView().orElseThrow().getPot());
        assertEquals(1, session.getAcceptedActions().size());
        assertEquals(1, session.getBotBeliefs(observer).get(actor).getObservedDecisions());
        assertThrows(UnsupportedOperationException.class,
                () -> session.getBotBeliefs(observer).clear());
        assertThrows(IllegalArgumentException.class, () -> session.getBotBeliefs("unknown"));
    }

    @Test
    void sb03CancellationInvalidatesWithoutChangingTheHandOrHumanPrivacy() {
        SetupSession session = BotFixtures.started(1);
        session.revealCards();
        BotTurn old = session.getBotTurn(64).orElseThrow();
        SetupView before = session.getView().orElseThrow();
        session.invalidateBotTurns();
        BotTurn current = session.getBotTurn(64).orElseThrow();
        assertTrue(current.getRevision() > old.getRevision());
        assertFalse(session.applyBotDecision(old, BotFixtures.passive(session)));
        assertEquals(before.getPot(), session.getView().orElseThrow().getPot());
        assertEquals(before.getHoleCards(), session.getView().orElseThrow().getHoleCards());
        assertTrue(session.getAcceptedActions().isEmpty());
        assertTrue(session.applyBotDecision(current, BotFixtures.passive(session)));
        assertTrue(session.getView().orElseThrow().getHoleCards().isEmpty());
    }

    @Test
    void sb04RestartIdentityRejectsOldResultsEvenWithIdenticalPlayersAndHandId() {
        GameState fixed = BotFixtures.initializer().start(1);
        SetupSession session = new SetupSession(
                new GameInitializer(count -> fixed, state -> state));
        session.start(1);
        BotTurn first = session.getBotTurn(64).orElseThrow();
        session.start(1);
        BotTurn second = session.getBotTurn(64).orElseThrow();
        assertEquals(first.getView().getHandId(), second.getView().getHandId());
        assertEquals(first.getRevision(), second.getRevision());
        assertNotEquals(first.getMatchId(), second.getMatchId());
        assertFalse(session.applyBotDecision(first, BotFixtures.passive(session)));
        assertTrue(session.applyBotDecision(second, BotFixtures.passive(session)));
    }

    @Test
    void sb05HumanAndFinishedTurnsNeedNoJobAndNextHandRetainsLearning() {
        SetupSession humans = BotFixtures.started(6);
        assertTrue(humans.getBotTurn(64).isEmpty());
        SetupSession session = BotFixtures.started(1);
        BotTurn old = session.getBotTurn(64).orElseThrow();
        String observer = old.getView().getViewerId();
        String human = session.getView().orElseThrow().getPlayers().get(0).getId();
        BotFixtures.finish(session);
        assertFalse(session.isCurrentBotTurn(old));
        assertTrue(session.getBotTurn(64).isEmpty());
        long learned = session.getBotBeliefs(observer).get(human).getObservedDecisions();
        assertTrue(learned > 0);
        session.settleHand();
        assertTrue(session.getBotTurn(64).isEmpty());
        session.nextHand();
        BotTurn next = session.getBotTurn(64).orElseThrow();
        assertEquals(old.getMatchId(), next.getMatchId());
        assertNotEquals(old.getView().getHandId(), next.getView().getHandId());
        assertEquals(learned, session.getBotBeliefs(observer).get(human).getObservedDecisions());
        assertFalse(session.applyBotDecision(old, BotFixtures.passive(session)));
    }

    @Test
    void sb06DepartureFoldsNeverTeachPreferencesAndInvalidatePendingWork() {
        SetupSession session = BotFixtures.started(4);
        String quitter = BotFixtures.actor(session);
        String observer = session.getView().orElseThrow().getPlayers().get(4).getId();
        session.quit(quitter);
        assertEquals(1, session.getAcceptedActions().size());
        assertEquals(0, session.getBotBeliefs(observer).get(quitter).getObservedDecisions());
        SetupSession queued = BotFixtures.started(1);
        BotTurn old = queued.getBotTurn(64).orElseThrow();
        String human = queued.getView().orElseThrow().getPlayers().get(0).getId();
        queued.quit(human);
        assertEquals(1, queued.getAcceptedActions().size());
        assertEquals(human, queued.getAcceptedActions().get(0).getPlayerId());
        assertEquals(PlayerStatus.FOLDED, queued.getView().orElseThrow().getStatuses().get(0));
        assertFalse(queued.applyBotDecision(old, BotFixtures.passive(queued)));
        for (int index = 0; index < 3; index++) queued.submitAction(BotFixtures.passive(queued));
        assertTrue(queued.playDepartureTurns().isEmpty());
        assertTrue(queued.playDepartureTurns().isEmpty());
        String bot = queued.getView().orElseThrow().getPlayers().get(1).getId();
        assertEquals(0, queued.getBotBeliefs(bot).get(human).getObservedDecisions());
        assertTrue(new SetupSession().playDepartureTurns().isEmpty());
    }

    @Test
    void sb07OldResultsAreIgnoredBeforeStartAndFromAnotherMatch() {
        SetupSession first = BotFixtures.started(1);
        BotTurn ticket = first.getBotTurn(64).orElseThrow();
        ActionRequest action = BotFixtures.passive(first);
        assertFalse(new SetupSession().applyBotDecision(ticket, action));
        assertFalse(BotFixtures.started(1).applyBotDecision(ticket, action));
        assertThrows(IllegalStateException.class, () -> new SetupSession().getBotBeliefs("bot"));
    }
    @Test
    void sb08ShortBlindPreviewSeparatesOldRefundFromNewCallingCost() {
        GameState base = BotFixtures.initializer().start(1);
        List<PlayerState> players = new ArrayList<>();
        for (PlayerState old : base.getPlayerStates()) {
            int seat = old.getPlayer().getSeat();
            long chips = seat == 3 ? 5985 : 0;
            Player player = new Player(old.getPlayer().getId(), seat,
                    old.getPlayer().getType(), chips);
            PlayerStatus status = seat == 0 ? PlayerStatus.ALL_IN
                    : seat == 3 ? PlayerStatus.ACTIVE : PlayerStatus.ELIMINATED;
            long contribution = seat == 0 ? 5 : seat == 3 ? 10 : 0;
            players.add(new PlayerState(player, status,
                    seat == 0 || seat == 3 ? old.getHoleCards() : List.of(),
                    contribution, contribution));
        }
        String actor = players.get(3).getPlayer().getId();
        GameState shortBlind = GameState.create(new GameConfig(), "short-blind",
                GamePhase.PREFLOP, players, new HandPositions(3, 3, 0), Optional.of(actor),
                List.of(), base.getRemainingDeck(),
                new BettingProgress(20, 20, Set.of(actor), Set.of()));
        SetupSession session = new SetupSession(
                new GameInitializer(count -> shortBlind, state -> state));
        session.start(1);
        BotTurn turn = session.getBotTurn(32).orElseThrow();
        assertEquals(10, turn.getLegalActions().getCallCost());
        assertEquals(10, turn.getCallRefund());
        assertEquals(10, turn.getScenario().getEligiblePotAmount());
        assertEquals(1, turn.getScenario().getOpponents().size());
        BotComputer computer = new BotComputer((request, stop) ->
                bot.BotEstimateValues.fixed(request, 0), () -> 0.5);
        ActionRequest action = computer.decide(turn, () -> false);
        assertEquals(ActionType.CALL, action.getType());
        assertTrue(session.applyBotDecision(turn, action));
        assertTrue(session.getResult().isPresent());
        session.settleHand();
        assertEquals(15, session.getSettlement().orElseThrow().getRefunds().get(actor));
        assertEquals(6000, session.getView().orElseThrow().getPlayers().stream()
                .mapToLong(Player::getChipStack).sum());
    }

    @Test
    void sb09RejectsHandAndActorMismatchEvenAtTheSameRevision() {
        SetupSession session = BotFixtures.started(1);
        BotTurn current = session.getBotTurn(1).orElseThrow();
        GameState other = BotFixtures.initializer().start(1);
        BotTurn wrongHand = copyTurn(current, other.viewFor(other.getPlayers().get(3).getId()));
        assertFalse(session.isCurrentBotTurn(wrongHand));
        GameState same = GameState.create(other.getConfig(), current.getView().getHandId(),
                other.getPhase(), other.getPlayerStates(), other.getPositions(),
                other.getCurrentActorId(), other.getCommunityCards(), other.getRemainingDeck(),
                other.getBettingProgress());
        assertFalse(session.isCurrentBotTurn(copyTurn(current,
                same.viewFor(same.getPlayers().get(4).getId()))));
        assertTrue(session.isCurrentBotTurn(current));
        assertTrue(session.getAcceptedActions().isEmpty());
    }

    @Test
    void sb10ClosedInitialRoundHasNoBotJobOrFabricatedActor() {
        GameState source = BotFixtures.initializer().start(1);
        GameState closed = GameState.create(source.getConfig(), source.getHandId(),
                source.getPhase(), source.getPlayerStates(), source.getPositions(),
                Optional.empty(), source.getCommunityCards(), source.getRemainingDeck(),
                new BettingProgress(20, 20, Set.of(), Set.of()));
        SetupSession session = new SetupSession(
                new GameInitializer(count -> closed, state -> state));
        session.start(1);
        assertTrue(session.getBotTurn(1).isEmpty());
        assertTrue(session.getAcceptedActions().isEmpty());
    }

    @Test
    void sb11LearningUsesPreActionLegalChoicesAndStreetResponsePosition() {
        SetupSession session = BotFixtures.started(1);
        String observer = session.getView().orElseThrow().getPlayers().get(5).getId();
        while (session.getResult().isEmpty()) {
            SetupView before = session.getView().orElseThrow();
            String actor = BotFixtures.actor(session);
            BettingStreet street = BettingStreet.valueOf(before.getPhase().name());
            Set<ResolvedActionType> choices = new HashSet<>();
            session.getAvailableActions().forEach(type ->
                    choices.add(ResolvedActionType.valueOf(type.name())));
            boolean position = before.getCurrentActorSeat()
                    == (street == BettingStreet.PREFLOP ? 2 : 0);
            bot.learning.DecisionContext context = new bot.learning.DecisionContext(
                    street, choices, position);
            long observations = actor.equals(observer) ? 0
                    : session.getBotBeliefs(observer).get(actor)
                    .getPrediction(context).getObservations();
            if (street != BettingStreet.PREFLOP && before.getCurrentActorSeat() == 1) {
                BotTurn free = session.getBotTurn(1).orElseThrow();
                assertEquals(0, free.getLegalActions().getCallCost());
                assertEquals(before.getPot(), free.getScenario().getEligiblePotAmount());
                assertEquals(0, free.getCallRefund());
            }
            session.submitAction(BotFixtures.passive(session));
            if (!actor.equals(observer)) {
                assertEquals(observations + 1, session.getBotBeliefs(observer).get(actor)
                        .getPrediction(context).getObservations());
            }
        }
        assertTrue(session.getBotTurn(1).isEmpty());
        assertTrue(session.playDepartureTurns().isEmpty());
    }

    @Test
    void sb12LegacyPassiveHelperAlsoExcludesDeparturePreferences() {
        SetupSession session = BotFixtures.started(1);
        String human = session.getView().orElseThrow().getPlayers().get(0).getId();
        String observer = session.getView().orElseThrow().getPlayers().get(3).getId();
        session.quit(human);
        session.playBotTurns();
        assertTrue(session.getResult().isPresent());
        assertEquals(0, session.getBotBeliefs(observer).get(human).getObservedDecisions());
    }

    @Test
    void sb13DeparturePumpSelectsTheActingHumanWithoutRevealingCards() {
        SetupSession session = BotFixtures.started(6);
        assertEquals(0, session.getView().orElseThrow().getViewerSeat());
        session.revealCards();
        assertTrue(session.playDepartureTurns().isEmpty());
        assertEquals(3, session.getView().orElseThrow().getViewerSeat());
        assertTrue(session.getView().orElseThrow().getHoleCards().isEmpty());
    }

    @Test
    void sb14RecorderFailureDoesNotPublishLearningOrConsumeTheTurn() {
        java.util.concurrent.atomic.AtomicBoolean reject =
                new java.util.concurrent.atomic.AtomicBoolean(true);
        SetupSession session = new SetupSession(BotFixtures.initializer(), new BettingEngine(),
                new ShowdownResolver(), state -> action -> {
                    if (reject.get()) throw new IllegalStateException("Recorder rejected action");
                });
        session.start(1);
        BotTurn turn = session.getBotTurn(1).orElseThrow();
        String actor = turn.getView().getViewerId();
        String observer = turn.getView().getPlayers().get(4).getId();
        ActionRequest call = new ActionRequest(actor, ActionType.CALL);
        assertThrows(IllegalStateException.class, () -> session.applyBotDecision(turn, call));
        assertTrue(session.isCurrentBotTurn(turn));
        assertEquals(30, session.getView().orElseThrow().getPot());
        assertTrue(session.getAcceptedActions().isEmpty());
        assertEquals(0, session.getBotBeliefs(observer).get(actor).getObservedDecisions());
        reject.set(false);
        assertTrue(session.applyBotDecision(turn, call));
        assertEquals(1, session.getBotBeliefs(observer).get(actor).getObservedDecisions());
        assertFalse(session.applyBotDecision(turn, call));
    }

    private static BotTurn copyTurn(BotTurn turn, GameView view) {
        return new BotTurn(turn.getMatchId(), turn.getRevision(), view, turn.getLegalActions(),
                turn.getScenario(), turn.getCallRefund(), turn.getBeliefs(), turn.getPersonality());
    }

}
