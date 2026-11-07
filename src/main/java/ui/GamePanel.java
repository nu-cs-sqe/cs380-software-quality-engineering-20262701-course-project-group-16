package ui;

import application.SetupSession;
import application.SetupView;
import domain.model.Card;
import domain.model.Player;
import domain.model.PlayerType;
import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.ShowdownResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;
import localization.Localizations;

/** Shared-computer played-hand screen. Construct and use on the Swing event thread. */
public final class GamePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color INK = new Color(24, 39, 47);
    private static final Color FELT = new Color(24, 76, 63);
    private static final Color GOLD = new Color(224, 184, 92);
    private final SetupSession session;
    private final Localizations messages = Localizations.load();
    private final JComboBox<String> language = named(new JComboBox<>(), "language");
    private String selectedLanguage = messages.defaultLanguage();
    private enum ErrorMessage { NONE, INVALID_COUNT, INVALID_ACTION }

    private ErrorMessage errorMessage = ErrorMessage.NONE;
    private final JTextField humanCount = named(new JTextField("1"), "humanCount");
    private final JComboBox<String> viewers = named(new JComboBox<>(), "viewers");
    private final JButton start = named(new JButton(), "start");
    private final JButton reveal = named(new JButton(), "reveal");
    private final JButton hide = named(new JButton(), "hide");
    private final JButton fold = named(new JButton(), "fold");
    private final JButton check = named(new JButton(), "check");
    private final JButton call = named(new JButton(), "call");
    private final JButton settle = named(new JButton(), "settle");
    private final JButton nextHand = named(new JButton(), "nextHand");
    private final JTabbedPane sections = named(new JTabbedPane(), "sections");
    private final StatisticsPanel statistics = new StatisticsPanel();
    private final JLabel error = named(new JLabel(" "), "error");
    private final JLabel status = named(new JLabel(), "status");
    private final JLabel pot = named(new JLabel(), "pot");
    private final JLabel deck = named(new JLabel(), "deck");
    private final JLabel handoff = named(new JLabel(), "handoff");
    private final JLabel community = named(new JLabel(), "community");
    private final JLabel phase = named(new JLabel(), "phase");
    private final JLabel result = named(new JLabel(" "), "result");
    private final JLabel[] holes = new JLabel[2];
    private final JPanel seats = new JPanel(new GridLayout(2, 3, 12, 12));
    private final List<Player> humanViewers = new ArrayList<>();
    private boolean rendering;

    public GamePanel(SetupSession session) {
        this.session = java.util.Objects.requireNonNull(session, "session");
        for (String tag : messages.languages()) {
            language.addItem(messages.languageName(tag));
        }
        language.setSelectedIndex(messages.languages().indexOf(selectedLanguage));
        language.addActionListener(event -> {
            if (!rendering && language.getSelectedIndex() >= 0) {
                selectedLanguage = messages.languages().get(language.getSelectedIndex());
                session.hideCards();
                buildScreen();
            }
        });
        start.addActionListener(event -> startGame());
        humanCount.addActionListener(event -> startGame());
        viewers.addActionListener(event -> {
            if (!rendering && viewers.getSelectedIndex() >= 0) {
                session.selectViewer(humanViewers.get(viewers.getSelectedIndex()).getId());
                refresh();
            }
        });
        reveal.addActionListener(event -> { session.revealCards(); refresh(); });
        hide.addActionListener(event -> hidePrivateCards());
        fold.addActionListener(event -> submitAction(ActionType.FOLD));
        check.addActionListener(event -> submitAction(ActionType.CHECK));
        call.addActionListener(event -> submitAction(ActionType.CALL));
        settle.addActionListener(event -> advanceHand(session::settleHand));
        nextHand.addActionListener(event -> advanceHand(this::startNextHand));
        sections.addChangeListener(event -> {
            if (!rendering) {
                hidePrivateCards();
            }
        });
        setPreferredSize(new Dimension(1100, 740));
        buildScreen();
    }

    /** Conceals the selected hand, including when the containing window loses focus. */
    public void hidePrivateCards() {
        session.hideCards();
        refresh();
    }

    private void buildScreen() {
        rendering = true;
        try {
            removeAll();
            setLocale(messages.locale(selectedLanguage));
            putClientProperty("windowTitle", text("title"));
            setLayout(new BorderLayout(18, 18));
            setBackground(INK);
            setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
            add(buildHeader(), BorderLayout.NORTH);
            add(buildSidebar(), BorderLayout.WEST);
            sections.removeAll();
            sections.addTab(text("table.title"), buildTable());
            sections.addTab(text("stats.title"), statistics);
            add(sections, BorderLayout.CENTER);
        } finally {
            rendering = false;
        }
        refresh();
    }

    private JPanel buildHeader() {
        JPanel header = transparent(new BorderLayout());
        JLabel title = label(text("title"), 27, Color.WHITE);
        header.add(title, BorderLayout.WEST);
        JPanel languageChoice = transparent(new FlowLayout(FlowLayout.RIGHT));
        languageChoice.add(label(text("language"), 14, Color.WHITE));
        language.getAccessibleContext().setAccessibleName(text("language"));
        languageChoice.add(language);
        header.add(languageChoice, BorderLayout.EAST);
        return header;

    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(244, 246, 244));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 18, 20, 18));
        sidebar.setPreferredSize(new Dimension(285, 600));
        addSetupControls(sidebar);
        sidebar.add(Box.createVerticalStrut(20));
        addPrivateControls(sidebar);
        sidebar.add(Box.createVerticalStrut(10));
        addActionControls(sidebar);
        sidebar.add(Box.createVerticalGlue());
        addRow(sidebar, new JLabel(wrapped(text("privacyNote"))));
        return sidebar;
    }

    private void addSetupControls(JPanel sidebar) {
        addRow(sidebar, label(text("setup"), 21, INK));
        addRow(sidebar, new JLabel(text("humanCount")));
        humanCount.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        humanCount.getAccessibleContext().setAccessibleName(text("humanCount"));
        addRow(sidebar, humanCount);
        addRow(sidebar, new JLabel(text("botsFill")));
        start.setText(text("start"));
        start.getAccessibleContext().setAccessibleName(text("start"));
        addRow(sidebar, start);
        error.setForeground(new Color(159, 34, 34));
        error.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        addRow(sidebar, error);
    }

    private void addPrivateControls(JPanel sidebar) {
        addRow(sidebar, label(text("privateHand"), 21, INK));
        viewers.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        viewers.getAccessibleContext().setAccessibleName(text("privateHand"));
        addRow(sidebar, viewers);
        addRow(sidebar, handoff);
        addRow(sidebar, buildPrivateCards());
        JPanel privacy = transparent(new FlowLayout(FlowLayout.LEFT, 5, 0));
        reveal.setText(text("reveal"));
        hide.setText(text("hide"));
        privacy.add(reveal); privacy.add(hide);
        addRow(sidebar, privacy);
    }

    private JPanel buildPrivateCards() {
        JPanel hand = transparent(new FlowLayout(FlowLayout.LEFT, 8, 8));
        for (int i = 0; i < 2; i++) {
            holes[i] = named(new JLabel("--", JLabel.CENTER), "hole" + i);
            holes[i].setOpaque(true);
            holes[i].setBackground(Color.WHITE);
            holes[i].setFont(new Font(Font.DIALOG, Font.BOLD, 25));
            holes[i].setPreferredSize(new Dimension(98, 115));
            holes[i].setBorder(BorderFactory.createLineBorder(new Color(190, 200, 192)));
            hand.add(holes[i]);
        }
        return hand;
    }

    private void addActionControls(JPanel sidebar) {
        addRow(sidebar, label(text("actions"), 18, INK));
        fold.setText(text("fold"));
        check.setText(text("check"));
        call.setText(text("call", 0));
        JPanel actions = transparent(new GridLayout(1, 3, 5, 0));
        for (JButton action : List.of(fold, check, call)) {
            action.setMargin(new Insets(2, 4, 2, 4));
        }
        actions.add(fold); actions.add(check); actions.add(call);
        addRow(sidebar, actions);
    }

    private JPanel buildTable() {
        JPanel table = new JPanel(new BorderLayout(14, 20));
        table.setBackground(FELT);
        table.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        table.add(buildSummary(), BorderLayout.NORTH);
        seats.setOpaque(false);
        table.add(seats, BorderLayout.CENTER);
        table.add(buildFooter(), BorderLayout.SOUTH);
        return table;
    }

    private JPanel buildSummary() {
        JPanel summary = transparent(new GridLayout(3, 1, 0, 8));
        status.setForeground(Color.WHITE);
        status.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        summary.add(status);
        pot.setForeground(GOLD); pot.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        summary.add(pot);
        deck.setForeground(Color.WHITE); summary.add(deck);
        return summary;
    }

    private JPanel buildFooter() {
        JPanel footer = transparent(new GridLayout(5, 1, 0, 8));
        community.setForeground(Color.WHITE);
        community.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        phase.setForeground(Color.WHITE);
        result.setForeground(GOLD);
        footer.add(community);
        footer.add(phase);
        footer.add(result);
        footer.add(buildProgressionControls());
        footer.add(label(text("preview"), 13, new Color(224, 232, 226)));
        return footer;
    }

    private JPanel buildProgressionControls() {
        JPanel controls = transparent(new FlowLayout(FlowLayout.LEFT, 10, 0));
        settle.setText(text("settle"));
        nextHand.setText(text("nextHand"));
        controls.add(settle);
        controls.add(nextHand);
        return controls;
    }

    private void advanceHand(Runnable operation) {
        session.hideCards();
        try {
            operation.run();
            errorMessage = ErrorMessage.NONE;
        } catch (IllegalArgumentException | IllegalStateException invalid) {
            errorMessage = ErrorMessage.INVALID_ACTION;
        }
        refresh();
    }

    private void startNextHand() {
        session.nextHand();
        session.playBotTurns();
    }

    private void startGame() {
        session.hideCards();
        try {
            session.start(Integer.parseInt(humanCount.getText().trim()));
            session.playBotTurns();
            errorMessage = ErrorMessage.NONE;
        } catch (IllegalArgumentException invalid) {
            errorMessage = ErrorMessage.INVALID_COUNT;
        }
        refresh();
    }

    private void submitAction(ActionType type) {
        Optional<SetupView> display = session.getView();
        if (display.isEmpty() || display.orElseThrow().getCurrentActorSeat() < 0) {
            return;
        }
        SetupView view = display.orElseThrow();
        try {
            String actor = view.getPlayers().get(view.getCurrentActorSeat()).getId();
            session.submitAction(new ActionRequest(actor, type));
            session.playBotTurns();
            errorMessage = ErrorMessage.NONE;
        } catch (IllegalArgumentException | IllegalStateException
                | UnsupportedOperationException invalid) {
            errorMessage = ErrorMessage.INVALID_ACTION;
        }
        refresh();
    }

    private void refresh() {
        rendering = true;
        try {
            error.setText(errorText());
            Optional<SetupView> view = session.getView();
            seats.removeAll();
            viewers.removeAllItems();
            humanViewers.clear();
            for (int seat = 0; seat < 6; seat++) {
                seats.add(seatPanel(seat, view));
            }
            viewers.setEnabled(view.isPresent());
            reveal.setEnabled(view.isPresent());
            if (view.isPresent()) {
                showMatch(view.orElseThrow());
            } else {
                showWaitingTable();
            }
            renderActions(view);
            settle.setEnabled(session.getResult().isPresent() && session.getSettlement().isEmpty());
            nextHand.setEnabled(session.getSettlement().isPresent()
                    && session.getMatchWinner().isEmpty());
            statistics.render(view.map(SetupView::getPlayers).orElseGet(List::of),
                    session.getStatistics(), messages, selectedLanguage);
            handoff.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    handoff.getPreferredSize().height));
        } finally {
            rendering = false;
        }
        revalidate();
        repaint();
    }

    private String errorText() {
        switch (errorMessage) {
            case INVALID_COUNT:
                return text("invalidCount");
            case INVALID_ACTION:
                return text("invalidAction");
            default:
                return " ";
        }
    }

    private void showMatch(SetupView view) {
        for (Player player : view.getPlayers()) {
            if (player.getType() == PlayerType.HUMAN) {
                humanViewers.add(player);
                viewers.addItem(playerName(player));
            }
        }
        for (int i = 0; i < humanViewers.size(); i++) {
            if (humanViewers.get(i).getSeat() == view.getViewerSeat()) {
                viewers.setSelectedIndex(i);
            }
        }
        hide.setEnabled(!view.getHoleCards().isEmpty());
        int actor = view.getCurrentActorSeat();
        status.setText(actor >= 0 ? text("currentActor", playerName(view.getPlayers().get(actor)))
                : completionText());
        pot.setText(text("pot", view.getPot()));
        deck.setText(text("deck", view.getRemainingCardCount()));
        handoff.setText(wrapped(text("handoff",
                playerName(view.getPlayers().get(view.getViewerSeat())))));
        showHoleCards(view.getHoleCards());
        showBoardAndResult(view);
    }

    private void showBoardAndResult(SetupView view) {
        community.setText(view.getCommunityCards().isEmpty() ? text("community")
                : text("communityDealt", view.getCommunityCards().stream()
                        .map(GamePanel::cardText).collect(Collectors.joining("   "))));
        phase.setText(text("phase." + view.getPhase().name()));
        result.setText(session.getResult()
                .map(comparison -> resultText(comparison, view)).orElse(" "));
    }

    private void showWaitingTable() {
        hide.setEnabled(false);
        status.setText(text("waiting"));
        pot.setText(text("pot", 0));
        deck.setText(text("notDealt"));
        handoff.setText(wrapped(text("startHint")));
        community.setText(text("community"));
        phase.setText(" ");
        result.setText(" ");
        showHoleCards(List.of());
    }

    private void renderActions(Optional<SetupView> display) {
        int actor = display.map(SetupView::getCurrentActorSeat).orElse(-1);
        boolean mayAct = display.filter(view -> actor >= 0 && view.getViewerSeat() == actor
                && view.getPlayers().get(actor).getType() == PlayerType.HUMAN).isPresent();
        Set<ActionType> available = mayAct ? session.getAvailableActions() : Set.of();
        long toCall = display.filter(view -> actor >= 0)
                .map(this::amountToCall).orElse(0L);
        fold.setText(text("fold"));
        check.setText(text("check"));
        call.setText(text("call", toCall));
        fold.setEnabled(available.contains(ActionType.FOLD));
        check.setEnabled(available.contains(ActionType.CHECK));
        call.setEnabled(available.contains(ActionType.CALL));
    }

    private long amountToCall(SetupView view) {
        int actor = view.getCurrentActorSeat();
        return Math.min(view.getCurrentBet() - view.getRoundContribution(actor),
                view.getPlayers().get(actor).getChipStack());
    }

    private String resultText(ShowdownResult comparison, SetupView view) {
        String winners = comparison.getWinnerIds().stream()
                .map(id -> view.getPlayers().stream()
                        .filter(player -> player.getId().equals(id)).findFirst().orElseThrow())
                .map(this::playerName).collect(Collectors.joining(", "));
        return text(comparison.isUncontested()
                        ? "result.inline.uncontested" : "result.inline.contested",
                winners, completionText());
    }

    private String completionText() {
        if (session.getMatchWinner().isPresent()) {
            return text("match.winner", playerName(session.getMatchWinner().orElseThrow()));
        }
        return text(session.getSettlement().isPresent() ? "result.paid"
                : "result.awaitingSettlement");
    }

    private JPanel seatPanel(int seat, Optional<SetupView> view) {
        Optional<Player> player = view.map(match -> match.getPlayers().get(seat));
        boolean acting = view.map(match -> seat == match.getCurrentActorSeat()).orElse(false);
        JPanel panel = new JPanel(new GridLayout(5, 1, 0, 6));
        panel.setBackground(new Color(237, 242, 236));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(acting ? GOLD : new Color(120, 153, 136),
                        acting ? 3 : 1),
                BorderFactory.createEmptyBorder(12, 10, 12, 10)));
        String name = player.map(this::playerName).orElseGet(() -> text("seat", seat + 1));
        String stack = player.map(person -> text("chips", person.getChipStack())).orElse("--");
        panel.add(named(label(name, 17, INK), "seatName" + seat));
        panel.add(named(new JLabel(stack), "seatStack" + seat));
        panel.add(new JLabel(view.map(match -> seatRole(seat, match)).orElse(" ")));
        String playerStatus = view.map(match -> text("status." + match.getStatuses().get(seat)
                .name())).orElse("--");
        panel.add(named(new JLabel(playerStatus), "seatStatus" + seat));
        panel.add(named(label(player.isPresent() ? "▧  ▧" : "--", 25, FELT), "seatCards" + seat));
        return panel;
    }

    private void showHoleCards(List<Card> cards) {
        for (int i = 0; i < holes.length; i++) {
            if (cards.isEmpty()) {
                holes[i].setText("--");
                holes[i].setForeground(INK);
            } else {
                Card card = cards.get(i);
                holes[i].setText(cardText(card));
                boolean red = card.getSuit() == domain.model.Suit.HEARTS
                        || card.getSuit() == domain.model.Suit.DIAMONDS;
                holes[i].setForeground(red ? new Color(175, 40, 40) : INK);
            }
        }
    }

    private String seatRole(int seat, SetupView view) {
        if (seat == view.getPositions().getBigBlindSeat()) {
            return text("bigBlind");
        }
        if (seat == view.getPositions().getSmallBlindSeat()) {
            return text("smallBlind");
        }
        if (seat == view.getPositions().getDealerSeat()) {
            return text("dealer");
        }
        return " ";
    }

    private String playerName(Player player) {
        return text(player.getType() == PlayerType.HUMAN ? "human" : "bot", player.getSeat() + 1);
    }

    private String text(String key, Object... arguments) {
        return messages.text(selectedLanguage, key, arguments);
    }

    private static String cardText(Card card) {
        String rank;
        switch (card.getRank()) {
            case ACE: rank = "A"; break;
            case KING: rank = "K"; break;
            case QUEEN: rank = "Q"; break;
            case JACK: rank = "J"; break;
            default: rank = Integer.toString(card.getRank().ordinal() + 2);
        }
        String suit;
        switch (card.getSuit()) {
            case HEARTS: suit = "♥"; break;
            case DIAMONDS: suit = "♦"; break;
            case CLUBS: suit = "♣"; break;
            default: suit = "♠";
        }
        return rank + " " + suit;
    }

    private static String wrapped(String text) {
        return "<html><div style=\"width: 180px\">" + text + "</div></html>";
    }

    private static void addRow(JPanel parent, JComponent child) {
        child.setAlignmentX(LEFT_ALIGNMENT);
        child.setMaximumSize(new Dimension(Integer.MAX_VALUE, child.getPreferredSize().height));
        parent.add(child);
        parent.add(Box.createVerticalStrut(10));
    }
    private static JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.DIALOG, Font.BOLD, size));
        label.setForeground(color);
        return label;
    }
    private static JPanel transparent(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout); panel.setOpaque(false); return panel;
    }
    private static <T extends JComponent> T named(T component, String name) {
        component.setName(name); return component;
    }
}
