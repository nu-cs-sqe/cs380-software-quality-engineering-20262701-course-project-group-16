package bot.learning;

import application.GameInitializer;
import domain.model.*;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static bot.learning.LearningFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class OpponentLearningIntegrationTest {
    @Test
    void olC01RealEngineRejectionProducesNoObservationAndAcceptedCallLearnsOnce() {
        GameState state = new GameInitializer(new GameConfig(), new Random(1), new Random(2)).start(6);
        String actor = state.getCurrentActorId().orElseThrow();
        String observer = state.getPlayers().stream().map(Player::getId).filter(id -> !id.equals(actor)).findFirst().orElseThrow();
        Set<String> opponents = state.getPlayers().stream().map(Player::getId).filter(id -> !id.equals(observer)).collect(Collectors.toSet());
        OpponentModel model = new OpponentModel("match", observer, opponents);
        BettingEngine engine = new BettingEngine();
        assertThrows(IllegalArgumentException.class, () -> engine.apply(state, new ActionRequest(actor, ActionType.CHECK)));
        assertEquals(0, model.snapshot(actor).getObservedDecisions());
        BettingResult result = engine.apply(state, new ActionRequest(actor, ActionType.CALL));
        DecisionContext context = context(BettingStreet.PREFLOP, false, ResolvedActionType.FOLD, ResolvedActionType.CALL);
        PublicDecision event = new PublicDecision("match", 1, result.getAcceptedAction(), context, DecisionOrigin.PLAYER_CHOICE);
        PlayStats exact = new PlayStats();
        exact.beginHand(state.getHandId(), true);
        exact.recordAction(result.getAcceptedAction().toStatsAction());
        assertTrue(model.observe(event));
        assertFalse(model.observe(event));
        assertEquals(1, model.snapshot(actor).getObservedDecisions());
        assertEquals(Map.of(ResolvedActionType.FOLD, 1.0 / 3, ResolvedActionType.CALL, 2.0 / 3),
                model.snapshot(actor).getPrediction(context).getProbabilities());
        assertEquals(0, exact.getHandsPlayed());
        assertEquals(6000, result.getState().getTotalChips());
        assertEquals(6000, state.getTotalChips());
    }
    @Test
    void olC02ModelsAreOpponentSpecificObserverSpecificAndMatchScoped() {
        OpponentModel first = new OpponentModel("match", "hero", Set.of("p1", "p2"));
        OpponentModel second = new OpponentModel("match", "p2", Set.of("p1", "hero"));
        OpponentBeliefs initial = first.snapshot("p1");
        PublicDecision common = decision(1, "p1", BettingStreet.TURN, ResolvedActionType.CALL, NONE,
                ResolvedActionType.FOLD, ResolvedActionType.CALL);
        assertTrue(first.observe(common)); assertTrue(second.observe(common));
        PublicDecision own = decision(2, "hero", BettingStreet.TURN, ResolvedActionType.FOLD, NONE,
                ResolvedActionType.FOLD, ResolvedActionType.CALL);
        assertFalse(first.observe(own)); assertTrue(second.observe(own));
        PublicDecision other = decision(3, "p2", BettingStreet.TURN, ResolvedActionType.FOLD, NONE,
                ResolvedActionType.FOLD, ResolvedActionType.CALL);
        assertTrue(first.observe(other)); assertFalse(second.observe(other));
        assertEquals(1, first.snapshot("p1").getObservedDecisions());
        assertEquals(1, first.snapshot("p2").getObservedDecisions());
        assertEquals(1, second.snapshot("hero").getObservedDecisions());
        assertEquals(0, initial.getObservedDecisions());
        OpponentModel fresh = new OpponentModel("new-match", "hero", Set.of("p1", "p2"));
        assertThrows(IllegalArgumentException.class, () -> fresh.observe(common));
        assertEquals(0, fresh.snapshot("p1").getObservedDecisions());
        assertTrue(fresh.observe(new PublicDecision("new-match", 1, common.getAction(),
                common.getContext().orElseThrow(), DecisionOrigin.PLAYER_CHOICE)));
    }
    @Test
    void olC03AutomaticDepartureCannotTeachAnOpponentToFoldToCBets() {
        OpponentModel model = new OpponentModel("match", "hero", Set.of("p1"));
        StatsContext faced = new StatsContext(PreflopRaiseType.NONE, false, false, true);
        AcceptedAction fold = action("p1", BettingStreet.FLOP, ResolvedActionType.FOLD, faced);
        DecisionContext context = context(BettingStreet.FLOP, false, ResolvedActionType.FOLD, ResolvedActionType.CALL);
        assertFalse(model.observe(new PublicDecision("match", 1, fold, DecisionOrigin.DEPARTURE)));
        assertEquals(0, model.snapshot("p1").getTendency(OpponentTendency.FOLD_TO_FLOP_CBET).getOpportunities());
        assertFalse(model.observe(new PublicDecision("match", 1, fold, context, DecisionOrigin.PLAYER_CHOICE)));
        assertTrue(model.observe(new PublicDecision("match", 2, fold, context, DecisionOrigin.PLAYER_CHOICE)));
        TendencyEstimate tendency = model.snapshot("p1").getTendency(OpponentTendency.FOLD_TO_FLOP_CBET);
        assertEquals(1, tendency.getOpportunities());
        assertEquals(1, tendency.getSuccesses());
        assertEquals(2.0 / 3, tendency.getProbability());
    }
}
