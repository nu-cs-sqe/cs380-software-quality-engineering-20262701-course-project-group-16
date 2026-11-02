package ui;

import application.SetupSession;
import application.SetupView;
import domain.model.Card;
import domain.model.Player;
import domain.model.PlayerType;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Shared-computer setup screen. Construct and use on the Swing event thread. */
public final class GamePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color INK = new Color(24, 39, 47);
    private static final Color FELT = new Color(24, 76, 63);
    private static final Color GOLD = new Color(224, 184, 92);
    private final SetupSession session;
    private final JTextField humanCount = named(new JTextField("1"), "humanCount");
    private final JComboBox<String> language = named(new JComboBox<>(new String[]{"English", "中文"}), "language");
    private final JComboBox<String> viewers = named(new JComboBox<>(), "viewers");
    private final JButton start = named(new JButton(), "start");
    private final JButton reveal = named(new JButton(), "reveal");
    private final JButton hide = named(new JButton(), "hide");
    private final JLabel error = named(new JLabel(" "), "error");
    private final JLabel status = named(new JLabel(), "status");
    private final JLabel pot = named(new JLabel(), "pot");
    private final JLabel deck = named(new JLabel(), "deck");
    private final JLabel handoff = named(new JLabel(), "handoff");
    private final JLabel[] holes = new JLabel[2];
    private final JPanel seats = new JPanel(new GridLayout(2, 3, 12, 12));
    private final List<Player> humanViewers = new ArrayList<>();
    private ResourceBundle messages;
    private Locale locale;
    private boolean rendering;

    public GamePanel(SetupSession session, Locale locale) {
        this.session = java.util.Objects.requireNonNull(session, "session");
        this.locale = java.util.Objects.requireNonNull(locale, "locale");
        language.setSelectedIndex("zh".equals(locale.getLanguage()) ? 1 : 0);
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
        language.addActionListener(event -> {
            session.hideCards();
            this.locale = language.getSelectedIndex() == 1 ? Locale.SIMPLIFIED_CHINESE : Locale.ENGLISH;
            buildScreen();
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
        messages = ResourceBundle.getBundle("ui.messages", locale);
        removeAll();
        setLayout(new BorderLayout(18, 18));
        setBackground(INK);
        setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        JPanel header = transparent(new BorderLayout());
        JLabel title = label(text("title"), 27, Color.WHITE);
        header.add(title, BorderLayout.WEST);
        header.add(language, BorderLayout.EAST);
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
        error.setText(" ");
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
        JPanel footer = transparent(new GridLayout(2, 1, 0, 12));
        footer.add(label(text("community"), 15, Color.WHITE));
        footer.add(label(text("preview"), 13, new Color(224, 232, 226)));
        table.add(footer, BorderLayout.SOUTH);
        add(table, BorderLayout.CENTER);
        refresh();
    }

    private void startGame() {
        session.hideCards();
        try {
            session.start(Integer.parseInt(humanCount.getText().trim()));
            error.setText(" ");
        } catch (IllegalArgumentException invalid) {
            error.setText(text("invalidCount"));
        }
        refresh();
    }

    private void refresh() {
        rendering = true;
        try {
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
                    if (humanViewers.get(i).getSeat() == view.getViewerSeat()) viewers.setSelectedIndex(i);
                }
            }
            status.setText(started ? text("firstActor", playerName(view.getPlayers().get(view.getCurrentActorSeat()))) : text("waiting"));
            pot.setText(started ? text("pot", view.getPot()) : text("pot", 0));
            deck.setText(started ? text("deck", view.getRemainingCardCount()) : text("notDealt"));
            handoff.setText(wrapped(started ? text("handoff", playerName(view.getPlayers().get(view.getViewerSeat()))) : text("startHint")));
            handoff.setMaximumSize(new Dimension(Integer.MAX_VALUE, handoff.getPreferredSize().height));
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

    private JPanel seatPanel(int seat, Player player, SetupView view) {
        JPanel panel = new JPanel(new GridLayout(4, 1, 0, 6));
        panel.setBackground(new Color(237, 242, 236));
        boolean acting = view != null && seat == view.getCurrentActorSeat();
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(acting ? GOLD : new Color(120, 153, 136), acting ? 3 : 1), BorderFactory.createEmptyBorder(12, 10, 12, 10)));
        panel.add(named(label(player == null ? text("seat", seat + 1) : playerName(player), 17, INK), "seatName" + seat));
        panel.add(named(new JLabel(player == null ? "--" : text("chips", player.getChipStack())), "seatStack" + seat));
        String role = " ";
        if (view != null) {
            if (seat == view.getPositions().getDealerSeat()) role = text("dealer");
            if (seat == view.getPositions().getSmallBlindSeat()) role = text("smallBlind");
            if (seat == view.getPositions().getBigBlindSeat()) role = text("bigBlind");
        }
        panel.add(new JLabel(role));
        panel.add(named(label(player == null ? "--" : "▧  ▧", 25, FELT), "seatCards" + seat));
        return panel;
    }

    private String playerName(Player player) {
        return text(player.getType() == PlayerType.HUMAN ? "human" : "bot", player.getSeat() + 1);
    }

    private String text(String key, Object... arguments) {
        return new MessageFormat(messages.getString(key), locale).format(arguments);
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
