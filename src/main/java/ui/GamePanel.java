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
    private String errorKey;
    private final JTextField humanCount = named(new JTextField("1"), "humanCount");
    private final JComboBox<String> viewers = named(new JComboBox<>(), "viewers");
    private final JButton start = named(new JButton(), "start");
    private final JButton reveal = named(new JButton(), "reveal");
    private final JButton hide = named(new JButton(), "hide");
    private final JButton fold = named(new JButton(), "fold");
    private final JButton check = named(new JButton(), "check");
    private final JButton call = named(new JButton(), "call");
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
        setPreferredSize(new Dimension(1100, 740));
        buildScreen();
    }

    /** Conceals the selected hand, including when the containing window loses focus. */
    public void hidePrivateCards() {
        session.hideCards();
        refresh();
    }

    private void buildScreen() {
        removeAll();
        setLocale(messages.locale(selectedLanguage));
        putClientProperty("windowTitle", text("title"));
        setLayout(new BorderLayout(18, 18));
        setBackground(INK);
        setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        JPanel header = transparent(new BorderLayout());
        JLabel title = label(text("title"), 27, Color.WHITE);
        header.add(title, BorderLayout.WEST);
        JPanel languageChoice = transparent(new FlowLayout(FlowLayout.RIGHT));
        languageChoice.add(label(text("language"), 14, Color.WHITE));
        language.getAccessibleContext().setAccessibleName(text("language"));
        languageChoice.add(language);
        header.add(languageChoice, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(244, 246, 244));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 18, 20, 18));
        sidebar.setPreferredSize(new Dimension(285, 600));
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
        sidebar.add(Box.createVerticalStrut(20));
        addRow(sidebar, label(text("privateHand"), 21, INK));
        viewers.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        viewers.getAccessibleContext().setAccessibleName(text("privateHand"));
        addRow(sidebar, viewers);
        addRow(sidebar, handoff);
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
        addRow(sidebar, hand);
        JPanel privacy = transparent(new FlowLayout(FlowLayout.LEFT, 5, 0));
        reveal.setText(text("reveal"));
        hide.setText(text("hide"));
        privacy.add(reveal); privacy.add(hide);
        addRow(sidebar, privacy);
        sidebar.add(Box.createVerticalStrut(10));
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
        sidebar.add(Box.createVerticalGlue());
        addRow(sidebar, new JLabel(wrapped(text("privacyNote"))));
        add(sidebar, BorderLayout.WEST);

        JPanel table = new JPanel(new BorderLayout(14, 20));
        table.setBackground(FELT);
        table.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel summary = transparent(new GridLayout(3, 1, 0, 8));
        status.setForeground(Color.WHITE);
        status.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        summary.add(status);
        pot.setForeground(GOLD); pot.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        summary.add(pot);
        deck.setForeground(Color.WHITE); summary.add(deck);
        table.add(summary, BorderLayout.NORTH);
        seats.setOpaque(false);
        table.add(seats, BorderLayout.CENTER);
        JPanel footer = transparent(new GridLayout(4, 1, 0, 8));
        community.setForeground(Color.WHITE);
        community.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        phase.setForeground(Color.WHITE);
        result.setForeground(GOLD);
        footer.add(community);
        footer.add(phase);
        footer.add(result);
        footer.add(label(text("preview"), 13, new Color(224, 232, 226)));
        table.add(footer, BorderLayout.SOUTH);
        add(table, BorderLayout.CENTER);
        refresh();
    }

    private void startGame() {
        session.hideCards();
        try {
            session.start(Integer.parseInt(humanCount.getText().trim()));
            session.playBotTurns();
            errorKey = null;
        } catch (IllegalArgumentException invalid) {
            errorKey = "invalidCount";
        }
        refresh();
    }

    private void submitAction(ActionType type) {
        SetupView view = session.getView().orElse(null);
        if (view == null || view.getCurrentActorSeat() < 0) {
            return;
        }
        try {
            session.submitAction(new ActionRequest(view.getPlayers().get(view.getCurrentActorSeat()).getId(), type));
            session.playBotTurns();
            errorKey = null;
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException invalid) {
            errorKey = "invalidAction";
        }
        refresh();
    }

    private void refresh() {
        rendering = true;
        try {
            error.setText(errorKey == null ? " " : text(errorKey));
            SetupView view = session.getView().orElse(null);
            seats.removeAll(); viewers.removeAllItems(); humanViewers.clear();
            for (int seat = 0; seat < 6; seat++) {
                Player player = view == null ? null : view.getPlayers().get(seat);
                seats.add(seatPanel(seat, player, view));
                if (player != null && player.getType() == PlayerType.HUMAN) {
                    humanViewers.add(player); viewers.addItem(playerName(player));
                }
            }
            boolean started = view != null;
            viewers.setEnabled(started); reveal.setEnabled(started);
            hide.setEnabled(started && !view.getHoleCards().isEmpty());
            if (started) {
                for (int i = 0; i < humanViewers.size(); i++) {
                    if (humanViewers.get(i).getSeat() == view.getViewerSeat()) {
                        viewers.setSelectedIndex(i);
                    }
                }
            }
            int actor = started ? view.getCurrentActorSeat() : -1;
            status.setText(actor >= 0 ? text("currentActor", playerName(view.getPlayers().get(actor)))
                    : started ? text("result.awaitingSettlement") : text("waiting"));
            pot.setText(started ? text("pot", view.getPot()) : text("pot", 0));
            deck.setText(started ? text("deck", view.getRemainingCardCount()) : text("notDealt"));
            handoff.setText(wrapped(started ? text("handoff", playerName(view.getPlayers().get(view.getViewerSeat()))) : text("startHint")));
            handoff.setMaximumSize(new Dimension(Integer.MAX_VALUE, handoff.getPreferredSize().height));
            renderActions(view);
            community.setText(!started || view.getCommunityCards().isEmpty() ? text("community")
                    : text("communityDealt", view.getCommunityCards().stream().map(GamePanel::cardText)
                    .collect(Collectors.joining("   "))));
            phase.setText(started ? text("phase." + view.getPhase().name()) : " ");
            result.setText(session.getResult().map(comparison -> resultText(comparison, view)).orElse(" "));
            for (int i = 0; i < 2; i++) {
                Card card = started && !view.getHoleCards().isEmpty() ? view.getHoleCards().get(i) : null;
                holes[i].setText(card == null ? "--" : cardText(card));
                holes[i].setForeground(card != null && (card.getSuit() == domain.model.Suit.HEARTS || card.getSuit() == domain.model.Suit.DIAMONDS) ? new Color(175, 40, 40) : INK);
            }
        } finally {
            rendering = false;
        }
        revalidate(); repaint();
    }

    private void renderActions(SetupView view) {
        int actor = view == null ? -1 : view.getCurrentActorSeat();
        boolean mayAct = actor >= 0 && view.getViewerSeat() == actor
                && view.getPlayers().get(actor).getType() == PlayerType.HUMAN;
        Set<ActionType> available = mayAct ? session.getAvailableActions() : Set.of();
        fold.setText(text("fold"));
        check.setText(text("check"));
        long toCall = actor < 0 ? 0 : Math.min(view.getCurrentBet() - view.getRoundContribution(actor),
                view.getPlayers().get(actor).getChipStack());
        call.setText(text("call", toCall));
        fold.setEnabled(available.contains(ActionType.FOLD));
        check.setEnabled(available.contains(ActionType.CHECK));
        call.setEnabled(available.contains(ActionType.CALL));
    }

    private String resultText(ShowdownResult comparison, SetupView view) {
        String winners = comparison.getWinnerIds().stream().map(id -> view.getPlayers().stream()
                .filter(player -> player.getId().equals(id)).findFirst().orElseThrow())
                .map(this::playerName).collect(Collectors.joining(", "));
        return text(comparison.isUncontested() ? "result.uncontested" : "result.contested",
                winners, text("result.awaitingSettlement"));
    }

    private JPanel seatPanel(int seat, Player player, SetupView view) {
        JPanel panel = new JPanel(new GridLayout(5, 1, 0, 6));
        panel.setBackground(new Color(237, 242, 236));
        boolean acting = view != null && seat == view.getCurrentActorSeat();
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(acting ? GOLD : new Color(120, 153, 136), acting ? 3 : 1), BorderFactory.createEmptyBorder(12, 10, 12, 10)));
        panel.add(named(label(player == null ? text("seat", seat + 1) : playerName(player), 17, INK), "seatName" + seat));
        panel.add(named(new JLabel(player == null ? "--" : text("chips", player.getChipStack())), "seatStack" + seat));
        String role = " ";
        if (view != null) {
            if (seat == view.getPositions().getDealerSeat()) {
                role = text("dealer");
            }
            if (seat == view.getPositions().getSmallBlindSeat()) {
                role = text("smallBlind");
            }
            if (seat == view.getPositions().getBigBlindSeat()) {
                role = text("bigBlind");
            }
        }
        panel.add(new JLabel(role));
        panel.add(named(new JLabel(view == null ? "--" : text("status." + view.getStatuses().get(seat).name())),
                "seatStatus" + seat));
        panel.add(named(label(player == null ? "--" : "▧  ▧", 25, FELT), "seatCards" + seat));
        return panel;
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
        JLabel label = new JLabel(text); label.setFont(new Font(Font.DIALOG, Font.BOLD, size)); label.setForeground(color); return label;
    }
    private static JPanel transparent(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout); panel.setOpaque(false); return panel;
    }
    private static <T extends JComponent> T named(T component, String name) {
        component.setName(name); return component;
    }
}
