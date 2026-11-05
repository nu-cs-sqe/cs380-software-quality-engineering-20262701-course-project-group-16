package ui;

import domain.model.BettingProgress;
import domain.model.Card;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.GameView;
import domain.model.Player;
import domain.model.PlayerState;
import domain.model.PlayerStatus;
import domain.model.PlayerType;
import domain.model.Rank;
import domain.model.SeatOrder;
import domain.model.ShowdownResolver;
import domain.model.ShowdownResult;
import domain.model.Suit;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ShowdownPanelTest {
    @Test
    void startsHiddenWithoutAStaleWinner() throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            ShowdownPanel panel = new ShowdownPanel();
            assertFalse(panel.isVisible());
            assertEquals("", summary(panel));
            assertEquals("", label(panel, "resultStatus"));
        });
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("outcomes")
    void rendersRealResultWithLocalizedPublicNames(String language, Kind kind,
            String expected, String title, String pending) throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            GameState state = state("hand-a", "p", kind);
            GameView view = state.viewFor("p0");
            ShowdownResult result = new ShowdownResolver().resolve(state);
            assertEquals(2, view.getPlayers().get(0).getHoleCards().size());
            ShowdownPanel panel = new ShowdownPanel();
            panel.showResult(view, result, language);
            assertTrue(panel.isVisible());
            assertEquals(title, label(panel, "resultTitle"));
            assertEquals(expected, summary(panel));
            assertEquals(pending, label(panel, "resultStatus"));
            assertFalse(find(panel, "resultSummary", JTextArea.class).isEditable());
            assertEquals(6000, state.getTotalChips());
            assertEquals(60, state.getPot());
        });
    }

    private static Stream<Arguments> outcomes() {
        return Stream.of(
            Arguments.of("en", Kind.SINGLE, "Winner: Human 1", "Hand result", "Result awaiting settlement"),
            Arguments.of("zh-CN", Kind.SINGLE, "获胜者：玩家 1", "本手结果", "结果等待结算"),
            Arguments.of("en", Kind.TWO_WAY, "Tie: Human 1, Human 2", "Hand result", "Result awaiting settlement"),
            Arguments.of("zh-CN", Kind.TWO_WAY, "平局：玩家 1、玩家 2", "本手结果", "结果等待结算"),
            Arguments.of("en", Kind.SIX_WAY, "Tie: Human 1, Human 2, Bot 3, Bot 4, Bot 5, Bot 6", "Hand result", "Result awaiting settlement"),
            Arguments.of("zh-CN", Kind.SIX_WAY, "平局：玩家 1、玩家 2、电脑 3、电脑 4、电脑 5、电脑 6", "本手结果", "结果等待结算"),
            Arguments.of("en", Kind.UNCONTESTED, "Uncontested winner: Bot 6", "Hand result", "Result awaiting settlement"),
            Arguments.of("zh-CN", Kind.UNCONTESTED, "其他玩家均弃牌，获胜者：电脑 6", "本手结果", "结果等待结算"));
    }

    @Test
    void rerenderingInAnotherLanguageReplacesEveryResultMessage() throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            GameState state = state("hand-a", "p", Kind.TWO_WAY);
            GameView view = state.viewFor("p0");
            ShowdownResult result = new ShowdownResolver().resolve(state);
            ShowdownPanel panel = new ShowdownPanel();
            panel.showResult(view, result, "en");
            assertEquals("Tie: Human 1, Human 2", summary(panel));
            panel.showResult(view, result, "zh-CN");
            assertEquals("平局：玩家 1、玩家 2", summary(panel));
            assertEquals("本手结果", label(panel, "resultTitle"));
            assertEquals("结果等待结算", label(panel, "resultStatus"));
        });
    }

    @Test
    void clearingAndShowingAnotherHandRemovesThePreviousWinner() throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            GameState first = state("hand-a", "p", Kind.SINGLE);
            GameState next = state("hand-b", "p", Kind.UNCONTESTED);
            ShowdownPanel panel = new ShowdownPanel();
            show(panel, first);
            assertEquals("Winner: Human 1", summary(panel));
            panel.clearResult();
            assertFalse(panel.isVisible());
            assertEquals("", summary(panel));
            assertEquals("", label(panel, "resultTitle"));
            assertEquals("", label(panel, "resultStatus"));
            show(panel, next);
            assertTrue(panel.isVisible());
            assertEquals("Uncontested winner: Bot 6", summary(panel));
        });
    }

    @Test
    void rejectsResultFromAnotherHandWithoutReplacingTheDisplay() throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            GameState current = state("hand-a", "p", Kind.SINGLE);
            GameState other = state("hand-b", "p", Kind.TWO_WAY);
            ShowdownPanel panel = new ShowdownPanel();
            show(panel, current);
            assertThrows(IllegalArgumentException.class, () -> panel.showResult(
                    current.viewFor("p0"), new ShowdownResolver().resolve(other), "en"));
            assertEquals("Winner: Human 1", summary(panel));
        });
    }

    @Test
    void rejectsWinnerIdsMissingFromTheSuppliedPublicView() throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            GameState current = state("hand-a", "p", Kind.SINGLE);
            GameState unrelatedPlayers = state("hand-a", "other", Kind.SINGLE);
            ShowdownPanel panel = new ShowdownPanel();
            show(panel, current);
            assertThrows(IllegalArgumentException.class, () -> panel.showResult(
                    unrelatedPlayers.viewFor("other0"), new ShowdownResolver().resolve(current), "en"));
            assertEquals("Winner: Human 1", summary(panel));
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rejectsNullViewOrResult(boolean nullView) throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            GameState state = state("hand-a", "p", Kind.SINGLE);
            ShowdownPanel panel = new ShowdownPanel();
            show(panel, state);
            assertThrows(NullPointerException.class, () -> panel.showResult(
                    nullView ? null : state.viewFor("p0"),
                    nullView ? new ShowdownResolver().resolve(state) : null, "en"));
            assertEquals("Winner: Human 1", summary(panel));
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "fr-FR"})
    void rejectsUnsupportedLanguageWithoutReplacingTheDisplay(String language) throws InterruptedException, InvocationTargetException {
        onEdt(() -> {
            GameState state = state("hand-a", "p", Kind.SINGLE);
            ShowdownPanel panel = new ShowdownPanel();
            show(panel, state);
            assertThrows(IllegalArgumentException.class, () -> panel.showResult(
                    state.viewFor("p0"), new ShowdownResolver().resolve(state), language));
            assertEquals("Winner: Human 1", summary(panel));
        });
    }

    private static void show(ShowdownPanel panel, GameState state) {
        panel.showResult(state.viewFor("p0"), new ShowdownResolver().resolve(state), "en");
    }

    private static GameState state(String handId, String prefix, Kind kind) {
        String[] hands = {"AH AD", "KH KD", "QH QD", "JH JD", "TH TD", "6H 6D"};
        if (kind == Kind.TWO_WAY) hands[1] = "AS AC";
        List<PlayerState> players = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            PlayerStatus status = kind == Kind.UNCONTESTED && seat != 5 ? PlayerStatus.FOLDED : PlayerStatus.ACTIVE;
            PlayerType type = seat < 2 ? PlayerType.HUMAN : PlayerType.BOT;
            players.add(new PlayerState(new Player(prefix + seat, seat, type, 990),
                    status, cards(hands[seat]), 0, 10));
        }
        List<Card> board = kind == Kind.UNCONTESTED ? List.of()
                : cards(kind == Kind.SIX_WAY ? "AC KC QC JC TC" : "2C 3D 4H 8S 9C");
        GamePhase phase = kind == Kind.UNCONTESTED ? GamePhase.PREFLOP : GamePhase.SHOWDOWN;
        return new GameState(new GameConfig(), handId, phase, players,
                new SeatOrder(players).positionsFor(0), null, board, List.of(),
                new BettingProgress(0, 20, Set.of(), Set.of()));
    }

    private static List<Card> cards(String text) {
        List<Card> cards = new ArrayList<>();
        for (String token : text.split(" ")) {
            cards.add(new Card(Rank.values()["23456789TJQKA".indexOf(token.charAt(0))],
                    Suit.values()["CDHS".indexOf(token.charAt(1))]));
        }
        return List.copyOf(cards);
    }

    private static String summary(ShowdownPanel panel) { return find(panel, "resultSummary", JTextArea.class).getText(); }
    private static String label(ShowdownPanel panel, String name) { return find(panel, name, JLabel.class).getText(); }

    private static <T extends Component> T find(Container root, String name, Class<T> type) {
        for (Component component : root.getComponents()) {
            if (name.equals(component.getName())) return type.cast(component);
            if (component instanceof Container) {
                T nested = find((Container) component, name, type);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static void onEdt(Runnable action) throws InterruptedException, InvocationTargetException {
        try { SwingUtilities.invokeAndWait(action); }
        catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof AssertionError) throw (AssertionError) cause;
            throw failure;
        }
    }

    private enum Kind { SINGLE, TWO_WAY, SIX_WAY, UNCONTESTED }
}
