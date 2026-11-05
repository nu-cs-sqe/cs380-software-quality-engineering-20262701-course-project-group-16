package integration;

import application.GameInitializer;
import application.SetupSession;
import application.SetupView;
import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.Card;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.PlayerStatus;
import domain.model.Rank;
import domain.model.ShowdownResult;
import domain.model.Suit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PlayedHandIntegrationTest {
    @ParameterizedTest(name = "SS-INT-{0}: real played hand, unique winner = {0}")
    @ValueSource(booleans = {false, true})
    void actionsAndStreetProgressionReachTheRealEvaluator(boolean uniqueWinner) {
        SetupSession session = new SetupSession(new GameInitializer(new GameConfig(),
                new FirstSeatRandom(), new ScriptedDeckRandom(uniqueWinner)));
        session.start(2);
        SetupView opening = session.getView().orElseThrow();
        assertEquals(3, opening.getCurrentActorSeat());
        assertEquals(40, opening.getRemainingCardCount());
        assertTrue(session.getResult().isEmpty());
        int[] order = {3, 4, 5, 0, 1, 2};
        ActionType[] decisions = {ActionType.CALL, ActionType.FOLD, ActionType.CALL,
                ActionType.CALL, ActionType.CALL, ActionType.CHECK};
        for (int index = 0; index < order.length; index++) {
            assertEquals(order[index], session.getView().orElseThrow().getCurrentActorSeat());
            act(session, decisions[index]);
        }
        for (GamePhase street : new GamePhase[]{GamePhase.FLOP, GamePhase.TURN, GamePhase.RIVER}) {
            assertEquals(street, session.getView().orElseThrow().getPhase());
            for (int seat : new int[]{1, 2, 3, 5, 0}) {
                assertEquals(seat, session.getView().orElseThrow().getCurrentActorSeat());
                act(session, ActionType.CHECK);
            }
        }
        SetupView finalView = session.getView().orElseThrow();
        ShowdownResult result = session.getResult().orElseThrow();
        List<String> expectedWinners = new ArrayList<>();
        for (int seat : uniqueWinner ? new int[]{5} : new int[]{0, 1, 2, 3, 5}) {
            expectedWinners.add(opening.getPlayers().get(seat).getId());
        }
        assertEquals(expectedWinners, result.getWinnerIds());
        assertFalse(result.isUncontested());
        assertEquals(ShowdownResult.Status.AWAITING_SETTLEMENT, result.getStatus());
        assertEquals(opening.getHandId(), result.getHandId());
        assertEquals(GamePhase.SHOWDOWN, finalView.getPhase());
        assertEquals(-1, finalView.getCurrentActorSeat());
        assertEquals(5, finalView.getCommunityCards().size());
        assertEquals(32, finalView.getRemainingCardCount());
        assertEquals(100, finalView.getPot());
        assertEquals(5900, finalView.getPlayers().stream().mapToLong(player -> player.getChipStack()).sum());
        assertEquals(21, session.getAcceptedActions().size());
        assertEquals(PlayerStatus.FOLDED, finalView.getStatuses().get(4));
        assertEquals(1000, finalView.getPlayers().get(4).getChipStack());
        assertTrue(finalView.getHoleCards().isEmpty());
        assertTrue(session.getAvailableActions().isEmpty());
        assertSame(result, session.getResult().orElseThrow());
        assertEquals(30, opening.getPot());
        assertEquals(GamePhase.PREFLOP, opening.getPhase());
    }

    private static void act(SetupSession session, ActionType type) {
        SetupView before = session.getView().orElseThrow();
        session.submitAction(new ActionRequest(before.getPlayers().get(before.getCurrentActorSeat()).getId(), type));
        SetupView after = session.getView().orElseThrow();
        assertEquals(6000, after.getPot() + after.getPlayers().stream().mapToLong(player -> player.getChipStack()).sum());
    }

    private static final class FirstSeatRandom extends Random {
        private static final long serialVersionUID = 1L;
        @Override public int nextInt(int bound) { return 0; }
    }

    /** Controls real Fisher-Yates shuffle choices; no game component is replaced. */
    private static final class ScriptedDeckRandom extends Random {
        private static final long serialVersionUID = 1L;
        private final int[] choices = new int[51];
        private int next;

        ScriptedDeckRandom(boolean uniqueWinner) {
            List<Card> current = new ArrayList<>();
            for (Suit suit : Suit.values()) {
                for (Rank rank : Rank.values()) current.add(new Card(rank, suit));
            }
            List<Card> desired = new ArrayList<>(current);
            // Hole cards remain clubs. Board is a shared diamond flush (tie), or
            // 2D 3D 4D 5H 9S: seat5's 6C then makes the sole six-high straight.
            if (uniqueWinner) {
                Collections.swap(desired, 17, desired.indexOf(new Card(Rank.FIVE, Suit.HEARTS)));
                Collections.swap(desired, 19, desired.indexOf(new Card(Rank.NINE, Suit.SPADES)));
            }
            for (int bound = 52; bound > 1; bound--) {
                int selected = current.indexOf(desired.get(bound - 1));
                choices[52 - bound] = selected;
                Collections.swap(current, bound - 1, selected);
            }
        }

        @Override public int nextInt(int bound) {
            assertEquals(52 - next, bound);
            return choices[next++];
        }
    }
}
