package bot.policy;

import bot.learning.OpponentBeliefs;
import domain.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class PolicyRequestTest {
    @ParameterizedTest
    @EnumSource(value = GamePhase.class, names = {"PREFLOP", "FLOP", "TURN", "RIVER"})
    void pr01RetainsFilteredValuesAndCopiesBeliefs(GamePhase phase) {
        PolicyFixtures.Fixture fixture = PolicyFixtures.table();
        if (phase != GamePhase.PREFLOP) fixture.street(phase);
        GameView view = fixture.state().viewFor("p3");
        LegalActions legal = fixture.legal();
        CallValue call = new CallValue(0.5, view.getPot() + legal.getCallCost(), 0);
        Map<String, OpponentBeliefs> beliefs = new HashMap<>();
        OpponentBeliefs prior = new OpponentBeliefs();
        beliefs.put("p1", prior);
        PolicyRequest request = new PolicyRequest(view, legal, call, beliefs);
        beliefs.clear();
        assertSame(view, request.getView());
        assertSame(legal, request.getLegalActions());
        assertSame(call, request.getCallValue());
        assertEquals(BettingStreet.valueOf(phase.name()), request.getStreet());
        assertEquals("p3", request.getHero().getId());
        assertSame(prior, request.getOpponents().get("p1"));
        assertThrows(UnsupportedOperationException.class, () -> request.getOpponents().clear());
        assertEquals(6, request.getParticipantCount());
        assertEquals(5, request.getActiveOpponents().size());
        assertEquals(2, request.getHero().getHoleCards().size());
        assertTrue(view.getPlayers().get(1).getHoleCards().isEmpty());
    }

    @Test
    void pr02RejectsHumanNonActorInactiveAndUncontestedViews() {
        PolicyFixtures.Fixture fixture = PolicyFixtures.table();
        GameView view = fixture.state().viewFor("p0");
        assertThrows(IllegalArgumentException.class, () -> create(view, fixture.legal(), 50));
        fixture.hero = 4;
        assertThrows(IllegalArgumentException.class, () -> fixture.request(0.5));
        fixture.statuses[4] = PlayerStatus.FOLDED;
        assertThrows(IllegalArgumentException.class, () -> fixture.request(0.5));
        fixture.hero = 3;
        for (int seat : new int[] {0, 1, 2, 4, 5}) {
            fixture.statuses[seat] = PlayerStatus.FOLDED;
        }
        assertThrows(IllegalArgumentException.class, () -> fixture.request(0.5));
    }

    @Test
    void pr03RejectsSetupEmptyOptionsAndMismatchedCallingCosts() {
        PolicyFixtures.Fixture fixture = PolicyFixtures.table();
        GameView view = fixture.state().viewFor("p3");
        assertThrows(IllegalArgumentException.class,
                () -> create(view, LegalActions.none(), 50));
        assertThrows(IllegalArgumentException.class,
                () -> create(view, PolicyLegalFixtures.passive(19), 50));
        assertThrows(IllegalArgumentException.class,
                () -> create(view, PolicyLegalFixtures.passive(21), 51));
        fixture.phase = GamePhase.SETUP;
        assertThrows(IllegalArgumentException.class, () -> fixture.request(0.5));
    }

    @Test
    void pr04RejectsImpossibleScenarioAndUnknownBeliefs() {
        PolicyFixtures.Fixture fixture = PolicyFixtures.table();
        GameView view = fixture.state().viewFor("p3");
        assertThrows(IllegalArgumentException.class, () -> create(view, fixture.legal(), 51));
        assertThrows(IllegalArgumentException.class, () -> create(view, fixture.legal(), 19));
        assertThrows(IllegalArgumentException.class, () -> new PolicyRequest(view, fixture.legal(),
                new CallValue(0.5, 50, 21), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new PolicyRequest(view, fixture.legal(),
                new CallValue(0.5, 50, 1), Map.of()));
        for (String id : List.of("p3", "unknown")) {
            assertThrows(IllegalArgumentException.class, () -> new PolicyRequest(view,
                    fixture.legal(), new CallValue(0.5, 50, 0), Map.of(id, new OpponentBeliefs())));
        }
        PolicyRequest sidePot = create(view, fixture.legal(), 20);
        assertEquals(1, sidePot.getCallValue().breakEvenEquity(20).orElseThrow());
        PolicyRequest refund = new PolicyRequest(view, fixture.legal(),
                new CallValue(0.5, 40, 10), Map.of());
        assertEquals(0.25, refund.getCallValue().breakEvenEquity(20).orElseThrow());
    }

    @Test
    void pr05RejectsMismatchedWagerAndAcceptsClosedRaiseRights() {
        GameView view = PolicyFixtures.table().state().viewFor("p3");
        for (LegalActions legal : List.of(
                PolicyLegalFixtures.wager(20, ActionType.RAISE, 20, 20, 999),
                PolicyLegalFixtures.wager(20, ActionType.BET, 0, 20, 1000))) {
            assertThrows(IllegalArgumentException.class, () -> create(view, legal, 50));
        }
        PolicyFixtures.Fixture closed = PolicyFixtures.table();
        closed.raiseRights = false;
        assertThrows(IllegalArgumentException.class, () -> create(closed.state().viewFor("p3"),
                PolicyLegalFixtures.wager(20, ActionType.RAISE, 20, 20, 1000), 50));
        PolicyRequest passive = create(view, PolicyLegalFixtures.passive(20), 50);
        assertTrue(passive.getLegalActions().getWager().isEmpty());
    }

    @Test
    void pr06UsesPublicStreetOrderAndSkipsFoldedOrAllInOpponents() {
        PolicyFixtures.Fixture fixture = PolicyFixtures.table();
        fixture.hero = 2;
        fixture.actor = 2;
        assertTrue(fixture.request(0.5).isInPosition());
        fixture.street(GamePhase.FLOP);
        assertFalse(fixture.request(0.5).isInPosition());
        fixture.dealer = 2;
        assertTrue(fixture.request(0.5).isInPosition());
        for (int seat : new int[] {0, 1, 3, 4, 5}) {
            fixture.statuses[seat] = PlayerStatus.FOLDED;
        }
        fixture.statuses[3] = PlayerStatus.ALL_IN;
        fixture.stacks[3] = 0;
        PolicyRequest request = fixture.request(0.5);
        assertEquals(2, request.getParticipantCount());
        assertTrue(request.getActiveOpponents().isEmpty());
        assertFalse(request.isInPosition());
    }

    private static PolicyRequest create(GameView view, LegalActions legal, long pot) {
        return new PolicyRequest(view, legal, new CallValue(0.5, pot, 0), Map.of());
    }
}
