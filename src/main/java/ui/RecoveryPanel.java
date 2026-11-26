package ui;

import application.RecoveryChallenge;
import application.RecoveryQuestion;
import application.RecoveryService;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import localization.Localizations;

/** Displays a localizable quiz and keeps choice indices stable during language changes. */
final class RecoveryPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JPanel questions = new JPanel(new GridLayout(0, 1, 0, 4));
    private final JTextArea notice = new JTextArea(2, 0);
    private final JButton begin = new JButton();
    private final JButton submit = new JButton();
    private final List<JComboBox<String>> choices = new ArrayList<>();

    RecoveryPanel(Runnable start, Runnable grade) {
        super(new BorderLayout(0, 4));
        begin.setName("beginRecovery");
        submit.setName("submitRecovery");
        notice.setName("recoveryNotice");
        notice.setEditable(false);
        notice.setOpaque(false);
        notice.setLineWrap(true);
        notice.setWrapStyleWord(true);
        begin.addActionListener(event -> start.run());
        submit.addActionListener(event -> grade.run());
        JPanel buttons = new JPanel();
        buttons.add(begin);
        buttons.add(submit);
        add(notice, BorderLayout.NORTH);
        add(new JScrollPane(questions), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    List<Integer> answers() {
        List<Integer> answers = new ArrayList<>();
        choices.forEach(choice -> answers.add(choice.getSelectedIndex()));
        return List.copyOf(answers);
    }

    void clearQuestions() {
        choices.clear();
        questions.removeAll();
    }

    void render(Optional<RecoveryChallenge> challenge, Localizations messages, String language) {
        begin.setText(messages.text(language, "recovery.begin"));
        submit.setText(messages.text(language, "recovery.submit"));
        notice.setText(messages.text(language, "recovery.instructions",
                RecoveryChallenge.PASS_SCORE, RecoveryChallenge.QUESTION_COUNT,
                RecoveryService.REWARD, RecoveryService.DAILY_LIMIT));
        List<Integer> previous = answers();
        clearQuestions();
        challenge.ifPresent(quiz -> addQuestions(quiz, previous, messages, language));
        revalidate();
        repaint();
    }

    void setReady(boolean ready) {
        begin.setEnabled(ready);
        submit.setEnabled(ready && !choices.isEmpty());
        choices.forEach(choice -> choice.setEnabled(ready));
    }

    private void addQuestions(RecoveryChallenge challenge, List<Integer> previous,
            Localizations messages, String language) {
        for (RecoveryQuestion question : challenge.getQuestions()) {
            int index = choices.size();
            int selected = index < previous.size() ? previous.get(index) : -1;
            addQuestion(question, selected, messages, language);
        }
    }

    private void addQuestion(RecoveryQuestion question, int selected,
            Localizations messages, String language) {
        String prefix = "recovery.question." + question.getId();
        JLabel prompt = new JLabel(messages.text(language, prefix));
        JComboBox<String> options = new JComboBox<>();
        options.setName("recoveryChoice" + choices.size());
        for (int index = 0; index < RecoveryQuestion.CHOICES; index++) {
            options.addItem(messages.text(language, prefix + "." + index));
        }
        options.setSelectedIndex(selected);
        prompt.setLabelFor(options);
        choices.add(options);
        JPanel row = new JPanel(new BorderLayout(0, 2));
        row.add(prompt, BorderLayout.NORTH);
        row.add(options, BorderLayout.CENTER);
        questions.add(row);
    }
}
