package application;

import domain.model.GameConfig;
import domain.model.GameState;
import domain.model.PlayerType;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class SetupSessionTest {
    @Test
    void rejectsNullInitializer() {
        assertThrows(IllegalArgumentException.class, () -> new SetupSession(null));
    }

    @Test
    void noMatchHasNoDisplayAndHidingIsSafe() {
        SetupSession session = new SetupSession();
        assertTrue(session.getView().isEmpty());
        assertDoesNotThrow(session::hideCards);
        assertTrue(session.getView().isEmpty());
    }

    @Test
    void cannotRevealBeforeStarting() {
        assertThrows(IllegalStateException.class, new SetupSession()::revealCards);
    }

    @Test
    void cannotSelectBeforeStarting() {
        assertThrows(IllegalStateException.class, () -> new SetupSession().selectViewer("any"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 6})
    void startsAtHumanCountBoundariesWithHiddenCards(int humans) {
        SetupSession session = new SetupSession();
        session.start(humans);
        SetupView view = view(session);
        assertEquals(6, view.getPlayers().size());
        assertEquals(humans, view.getPlayers().stream()
                .filter(player -> player.getType() == PlayerType.HUMAN).count());
        assertEquals(0, view.getViewerSeat());
        assertTrue(view.getHoleCards().isEmpty());
        assertEquals(40, view.getRemainingCardCount());
        assertEquals(30, view.getPot());
        assertEquals(6000, view.getPlayers().stream().mapToLong(player -> player.getChipStack()).sum() + view.getPot());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7})
    void invalidInitialStartDoesNotCreateAMatch(int humans) {
        SetupSession session = new SetupSession();
        assertThrows(IllegalArgumentException.class, () -> session.start(humans));
        assertTrue(session.getView().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, 0, 7, Integer.MAX_VALUE})
    void failedRestartKeepsMatchButHidesCards(int humans) {
        SetupSession session = session();
        session.start(2);
        session.revealCards();
        SetupView before = view(session);
        assertEquals(2, before.getHoleCards().size());
        assertThrows(IllegalArgumentException.class, () -> session.start(humans));
        SetupView after = view(session);
        assertEquals(before.getPlayers(), after.getPlayers());
        assertEquals(before.getPot(), after.getPot());
        assertEquals(40, after.getRemainingCardCount());
        assertTrue(after.getHoleCards().isEmpty());
    }

    @Test
    void revealShowsOnlySelectedHumanCardsAndHideClearsThem() {
        SetupSession session = session();
        session.start(2);
        SetupView hidden = view(session);
        session.selectViewer(hidden.getPlayers().get(1).getId());
        session.revealCards();
        SetupView revealed = view(session);
        GameState reference = initializer().start(2);
        assertEquals(reference.getPlayerStates().get(1).getHoleCards(), revealed.getHoleCards());
        assertEquals(1, revealed.getViewerSeat());
        assertTrue(hidden.getHoleCards().isEmpty());
        assertEquals(0, revealed.getPositions().getDealerSeat());
        assertEquals(1, revealed.getPositions().getSmallBlindSeat());
        assertEquals(2, revealed.getPositions().getBigBlindSeat());
        assertEquals(3, revealed.getCurrentActorSeat());
        assertThrows(UnsupportedOperationException.class, () -> revealed.getHoleCards().clear());
        assertThrows(UnsupportedOperationException.class, () -> revealed.getPlayers().clear());
        session.hideCards();
        assertTrue(view(session).getHoleCards().isEmpty());
        assertEquals(2, revealed.getHoleCards().size());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void everyViewerSelectionRequiresANewReveal(int seat) {
        SetupSession session = session();
        session.start(2);
        session.revealCards();
        session.selectViewer(view(session).getPlayers().get(seat).getId());
        assertEquals(seat, view(session).getViewerSeat());
        assertTrue(view(session).getHoleCards().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "blank", "missing", "bot"})
    void rejectsInvalidViewerAndHidesPreviousCards(String value) {
        SetupSession session = session();
        session.start(2);
        session.revealCards();
        SetupView before = view(session);
        String id = value.equals("null") ? null : value.equals("blank") ? " "
                : value.equals("bot") ? before.getPlayers().get(2).getId() : "missing";
        assertThrows(IllegalArgumentException.class, () -> session.selectViewer(id));
        assertEquals(0, view(session).getViewerSeat());
        assertEquals(before.getPlayers(), view(session).getPlayers());
        assertTrue(view(session).getHoleCards().isEmpty());
    }

    @Test
    void newMatchResetsViewerAndHidesOldCards() {
        SetupSession session = session();
        session.start(2);
        session.selectViewer(view(session).getPlayers().get(1).getId());
        session.revealCards();
        SetupView first = view(session);
        session.start(1);
        SetupView second = view(session);
        assertNotEquals(first.getPlayers().get(0).getId(), second.getPlayers().get(0).getId());
        assertEquals(0, second.getViewerSeat());
        assertTrue(second.getHoleCards().isEmpty());
        assertEquals(1, second.getPlayers().stream().filter(player -> player.getType() == PlayerType.HUMAN).count());
        assertEquals(2, first.getHoleCards().size());
    }

    private static SetupView view(SetupSession session) {
        assertTrue(session.getView().isPresent(), "A started match must have display data");
        return session.getView().orElseThrow();
    }

    private static SetupSession session() { return new SetupSession(initializer()); }

    private static GameInitializer initializer() {
        Random dealer = new Random() {
            @Override public int nextInt(int bound) { return 0; }
        };
        return new GameInitializer(new GameConfig(), dealer, new Random(7));
    }
}
