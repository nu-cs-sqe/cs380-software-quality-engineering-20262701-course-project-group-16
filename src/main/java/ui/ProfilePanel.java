package ui;

import application.AuthenticatedProfile;
import application.ProfileService;
import application.SetupSession;
import application.StatisticsView;
import domain.model.PlayerProfile;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import localization.Localizations;

/** Local-account forms and next-match roster. All Swing access stays on the event thread. */
final class ProfilePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final Model model;
    private final JTextField name = named(new JTextField(18), "profileName");
    private final JPasswordField createPassword = named(new JPasswordField(18), "createPassword");
    private final JPasswordField loginPassword = named(new JPasswordField(18), "loginPassword");
    private final JComboBox<String> saved = named(new JComboBox<>(), "savedProfiles");
    private final DefaultListModel<String> roster = new DefaultListModel<>();
    private final JList<String> joined = named(new JList<>(roster), "joinedProfiles");
    private final JButton create = named(new JButton(), "createProfile");
    private final JButton login = named(new JButton(), "loginProfile");
    private final JButton reload = named(new JButton(), "reloadProfiles");
    private final JButton remove = named(new JButton(), "removeProfile");
    private final JButton refreshStats = named(new JButton(), "refreshLifetime");
    private final JLabel intro = new JLabel();
    private final JLabel nameLabel = new JLabel();
    private final JLabel createLabel = new JLabel();
    private final JLabel loginLabel = new JLabel();
    private final JLabel savedLabel = new JLabel();
    private final JTextArea policy = named(new JTextArea(2, 0), "passwordHint");
    private final JLabel selectionNote = new JLabel();
    private final JLabel error = named(new JLabel(" "), "profileError");
    private final JLabel status = named(new JLabel(" "), "profileStatus");
    private final JPanel registration = new JPanel(new GridLayout(0, 1, 0, 7));
    private final JPanel signIn = new JPanel(new GridLayout(0, 1, 0, 7));
    private final JPanel selection = new JPanel(new BorderLayout(8, 8));
    private final StatisticsPanel lifetime = new StatisticsPanel("lifetimeTable");
    private boolean rendering;

    ProfilePanel(ProfileService profiles, Localizations messages, Consumer<Boolean> busy,
            Runnable changed) {
        model = new Model(profiles, messages, busy, changed);
        build();
        create.addActionListener(event -> createProfile());
        createPassword.addActionListener(event -> createProfile());
        login.addActionListener(event -> loginProfile());
        loginPassword.addActionListener(event -> loginProfile());
        reload.addActionListener(event -> loadProfiles());
        remove.addActionListener(event -> removeProfile());
        refreshStats.addActionListener(event -> refreshLifetime());
        saved.addActionListener(event -> {
            if (!rendering) {
                model.selectedSaved = selectedSavedId();
                renderButtons();
            }
        });
        joined.addListSelectionListener(event -> {
            if (!rendering && !event.getValueIsAdjusting() && !model.busy) {
                refreshLifetime();
            }
        });
        render();
    }

    private void build() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(buildForms(), BorderLayout.NORTH);
        add(buildProfileViews(), BorderLayout.CENTER);
        add(buildNotices(), BorderLayout.SOUTH);
    }

    private JPanel buildForms() {
        buildRegistration();
        buildLogin();
        JPanel forms = new JPanel(new GridLayout(1, 2, 16, 0));
        forms.add(registration);
        forms.add(signIn);
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.add(intro, BorderLayout.NORTH);
        top.add(forms, BorderLayout.CENTER);
        return top;
    }

    private void buildRegistration() {
        policy.setEditable(false);
        policy.setFocusable(false);
        policy.setOpaque(false);
        policy.setLineWrap(true);
        policy.setWrapStyleWord(true);
        policy.setFont(nameLabel.getFont());
        registration.add(nameLabel);
        registration.add(name);
        registration.add(createLabel);
        registration.add(createPassword);
        registration.add(policy);
        registration.add(create);
    }

    private void buildLogin() {
        signIn.add(savedLabel);
        signIn.add(saved);
        signIn.add(loginLabel);
        signIn.add(loginPassword);
        signIn.add(reload);
        signIn.add(login);
    }

    private JPanel buildProfileViews() {
        joined.setVisibleRowCount(6);
        selection.setPreferredSize(new Dimension(235, 210));
        selection.add(new JScrollPane(joined), BorderLayout.CENTER);
        JPanel rosterFooter = new JPanel(new BorderLayout(0, 8));
        rosterFooter.add(selectionNote, BorderLayout.NORTH);
        rosterFooter.add(remove, BorderLayout.CENTER);
        selection.add(rosterFooter, BorderLayout.SOUTH);
        JPanel statistics = new JPanel(new BorderLayout(0, 6));
        statistics.add(lifetime, BorderLayout.CENTER);
        statistics.add(refreshStats, BorderLayout.SOUTH);
        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.add(selection, BorderLayout.WEST);
        bottom.add(statistics, BorderLayout.CENTER);
        return bottom;
    }

    private JPanel buildNotices() {
        JPanel notices = new JPanel(new GridLayout(2, 1, 0, 4));
        error.setForeground(new Color(159, 34, 34));
        status.setForeground(new Color(24, 76, 63));
        notices.add(error);
        notices.add(status);
        return notices;
    }

    void language(String language) {
        if (!model.language.equals(language)) {
            clearPasswords();
        }
        model.language = language;
        render();
    }

    void clearPasswords() {
        createPassword.setText("");
        loginPassword.setText("");
    }

    int humanCount() { return model.humans.size(); }

    void startMatch(SetupSession session) {
        if (model.busy) {
            throw new IllegalStateException("Account operation is still running.");
        }
        session.startProfiles(model.profiles, List.copyOf(model.humans));
    }

    Map<String, String> profileNames() {
        Map<String, String> names = new LinkedHashMap<>();
        model.humans.forEach(human -> names.put(human.getProfile().getProfileId(),
                human.getProfile().getDisplayName()));
        return Map.copyOf(names);
    }

    void loadProfiles() {
        if (model.busy) {
            return;
        }
        String selected = selectedSavedId();
        run(model.profiles::list, profiles -> {
            model.saved = List.copyOf(profiles);
            model.selectedSaved = selected;
            model.message = " ";
        }, () -> { });
    }

    private void createProfile() {
        if (model.busy) {
            return;
        }
        String displayName = name.getText();
        char[] password = createPassword.getPassword();
        clearPasswords();
        run(() -> model.profiles.create(displayName, password), profile -> {
            List<PlayerProfile> profiles = new ArrayList<>(model.saved);
            profiles.add(profile);
            model.saved = List.copyOf(profiles);
            model.selectedSaved = profile.getProfileId();
            model.message = "profile.created";
            name.setText("");
            loginPassword.requestFocusInWindow();
        }, () -> Arrays.fill(password, '\0'));
    }

    private void loginProfile() {
        if (model.busy || saved.getSelectedIndex() < 0 || model.humans.size() >= 6) {
            return;
        }
        PlayerProfile selected = model.saved.get(saved.getSelectedIndex());
        if (isJoined(selected.getProfileId())) {
            return;
        }
        char[] password = loginPassword.getPassword();
        clearPasswords();
        run(() -> loginData(selected, password), result -> {
            if (result.isEmpty()) {
                model.error = "profile.wrongPassword";
                return;
            }
            AccountData data = result.orElseThrow();
            model.humans.add(data.account);
            model.statistics.put(selected.getProfileId(), data.statistics);
            model.selectedJoined = model.humans.size() - 1;
            model.message = "profile.ready";
            model.changed.run();
        }, () -> Arrays.fill(password, '\0'));
    }

    private Optional<AccountData> loginData(PlayerProfile selected, char[] password)
            throws IOException, GeneralSecurityException {
        Optional<AuthenticatedProfile> account =
                model.profiles.login(selected.getProfileId(), password);
        if (account.isEmpty()) {
            return Optional.empty();
        }
        AuthenticatedProfile verified = account.orElseThrow();
        return Optional.of(new AccountData(verified,
                model.profiles.getLifetimeStatistics(verified)));
    }

    private void removeProfile() {
        if (model.busy || joined.getSelectedIndex() < 0) {
            return;
        }
        AuthenticatedProfile removed = model.humans.remove(joined.getSelectedIndex());
        model.statistics.remove(removed.getProfile().getProfileId());
        model.selectedJoined = Math.min(joined.getSelectedIndex(), model.humans.size() - 1);
        clearPasswords();
        model.message = " ";
        model.error = " ";
        render();
        model.changed.run();
    }

    void refreshLifetime() {
        int index = joined.getSelectedIndex();
        if (model.busy || index < 0) {
            return;
        }
        model.selectedJoined = index;
        AuthenticatedProfile account = model.humans.get(index);
        run(() -> model.profiles.getLifetimeStatistics(account), stats ->
                model.statistics.put(account.getProfile().getProfileId(), stats), () -> { });
    }

    private <T> void run(ProfileWork<T> work, Consumer<T> success, Runnable cleanup) {
        model.error = " ";
        model.message = " ";
        busy(true);
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws IOException, GeneralSecurityException {
                try {
                    return work.run();
                } finally {
                    cleanup.run();
                }
            }
            @Override protected void done() {
                try {
                    success.accept(get());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    model.error = "profile.operationError";
                } catch (ExecutionException failed) {
                    showFailure(failed.getCause());
                } finally {
                    busy(false);
                }
            }
        }.execute();
    }

    private void showFailure(Throwable failure) {
        if (failure instanceof IOException) {
            model.error = "profile.storageError";
        } else if (failure instanceof IllegalArgumentException) {
            model.error = "profile.invalidInput";
        } else if (failure instanceof GeneralSecurityException) {
            model.error = "profile.securityError";
        } else {
            model.error = "profile.operationError";
        }
    }

    private void busy(boolean busy) {
        model.busy = busy;
        render();
        model.busyChanged.accept(busy);
    }

    private boolean isJoined(String id) {
        return model.humans.stream().anyMatch(human ->
                human.getProfile().getProfileId().equals(id));
    }

    private String selectedSavedId() {
        int index = saved.getSelectedIndex();
        return index < 0 || index >= model.saved.size() ? ""
                : model.saved.get(index).getProfileId();
    }

    private void render() {
        rendering = true;
        try {
            String selected = saved.getSelectedIndex() >= 0
                    ? selectedSavedId() : model.selectedSaved;
            if (!model.selectedSaved.isEmpty()) {
                selected = model.selectedSaved;
            }
            saved.removeAllItems();
            for (int index = 0; index < model.saved.size(); index++) {
                saved.addItem(labelText(model.messages.text(model.language, "profile.option",
                        index + 1, model.saved.get(index).getDisplayName())));
            }
            for (int index = 0; index < model.saved.size(); index++) {
                if (model.saved.get(index).getProfileId().equals(selected)) {
                    saved.setSelectedIndex(index);
                }
            }
            model.selectedSaved = selectedSavedId();
            roster.clear();
            for (int index = 0; index < model.humans.size(); index++) {
                roster.addElement(labelText(model.messages.text(model.language, "profile.option",
                        index + 1, model.humans.get(index).getProfile().getDisplayName())));
            }
            joined.setSelectedIndex(model.selectedJoined);
            renderText();
            renderLifetime();
            renderButtons();
        } finally {
            rendering = false;
        }
        revalidate();
        repaint();
    }

    private void renderText() {
        setLocale(model.messages.locale(model.language));
        registration.setBorder(BorderFactory.createTitledBorder(text("profile.createTitle")));
        signIn.setBorder(BorderFactory.createTitledBorder(text("profile.loginTitle")));
        selection.setBorder(BorderFactory.createTitledBorder(text("profile.nextMatch")));
        intro.setText(text("profile.intro"));
        nameLabel.setText(text("profile.name"));
        createLabel.setText(text("profile.password"));
        loginLabel.setText(text("profile.password"));
        savedLabel.setText(text("profile.saved"));
        policy.setText(text("profile.policy"));
        selectionNote.setText("<html>" + text("profile.selectionNote") + "</html>");
        create.setText(text("profile.create"));
        login.setText(text("profile.login"));
        reload.setText(text("profile.reload"));
        remove.setText(text("profile.remove"));
        refreshStats.setText(text("profile.refreshStats"));
        error.setText(model.error.equals(" ") ? " " : text(model.error));
        status.setText(model.busy ? text("profile.busy")
                : model.message.equals(" ") ? " " : text(model.message));
        name.getAccessibleContext().setAccessibleName(text("profile.name"));
        createPassword.getAccessibleContext().setAccessibleName(text("profile.password"));
        loginPassword.getAccessibleContext().setAccessibleName(text("profile.password"));
        saved.getAccessibleContext().setAccessibleName(text("profile.saved"));
        joined.getAccessibleContext().setAccessibleName(text("profile.nextMatch"));
    }

    private void renderLifetime() {
        int index = joined.getSelectedIndex();
        List<PlayerProfile> profiles = index < 0 ? List.of()
                : List.of(model.humans.get(index).getProfile());
        lifetime.renderProfiles(profiles, model.statistics, model.messages, model.language);
    }

    private void renderButtons() {
        boolean ready = !model.busy;
        name.setEnabled(ready);
        createPassword.setEnabled(ready);
        loginPassword.setEnabled(ready);
        saved.setEnabled(ready);
        joined.setEnabled(ready);
        create.setEnabled(ready);
        reload.setEnabled(ready);
        login.setEnabled(ready && saved.getSelectedIndex() >= 0 && model.humans.size() < 6
                && !isJoined(selectedSavedId()));
        remove.setEnabled(ready && joined.getSelectedIndex() >= 0);
        refreshStats.setEnabled(ready && joined.getSelectedIndex() >= 0);
    }

    private String text(String key) { return model.messages.text(model.language, key); }

    static String labelText(String value) {
        return value.contains("<") ? "<html>" + escape(value) + "</html>" : value;
    }

    static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static <T extends javax.swing.JComponent> T named(T component, String name) {
        component.setName(name);
        return component;
    }

    @FunctionalInterface
    private interface ProfileWork<T> {
        T run() throws IOException, GeneralSecurityException;
    }

    private static final class AccountData {
        private final AuthenticatedProfile account;
        private final StatisticsView statistics;
        private AccountData(AuthenticatedProfile account, StatisticsView statistics) {
            this.account = account;
            this.statistics = statistics;
        }
    }

    // Live view state and callbacks; Swing screens are never serialized or persisted.
    private static final class Model {
        private final ProfileService profiles;
        private final Localizations messages;
        private final Consumer<Boolean> busyChanged;
        private final Runnable changed;
        private final List<AuthenticatedProfile> humans = new ArrayList<>();
        private List<PlayerProfile> saved = List.of();
        private final Map<String, StatisticsView> statistics = new LinkedHashMap<>();
        private String language;
        private String selectedSaved = "";
        private int selectedJoined = -1;
        private String error = " ";
        private String message = " ";
        private boolean busy;
        private Model(ProfileService profiles, Localizations messages, Consumer<Boolean> busy,
                Runnable changed) {
            this.profiles = profiles;
            this.messages = messages;
            this.busyChanged = busy;
            this.changed = changed;
            language = messages.defaultLanguage();
        }
    }
}
