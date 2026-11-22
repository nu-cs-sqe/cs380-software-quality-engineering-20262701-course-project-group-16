package desktop;

import application.AuthenticatedProfile;
import application.RecoveryChallenge;
import application.RecoveryOutcome;
import application.RecoveryQuestion;
import application.RecoveryService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import localization.Localizations;

/** Keeps translated prompts tied to the same account and server-side answer key. */
final class DesktopRecovery {
    private final RecoveryService service;
    private final Localizations messages = Localizations.load();
    private final Map<String, RecoveryChallenge> challenges = new HashMap<>();

    DesktopRecovery(RecoveryService service) {
        this.service = service;
    }

    Map<String, Object> begin(AuthenticatedProfile account, String locale) {
        String language = locale.equals("zh") ? "zh-CN" : locale;
        if (!messages.languages().contains(language)) {
            throw new IllegalArgumentException("Choose a supported language.");
        }
        String id = account.getProfile().getProfileId();
        RecoveryChallenge challenge = challenges.computeIfAbsent(id, ignored -> service.begin());
        List<Map<String, Object>> prompts = challenge.getQuestions().stream()
                .map(question -> question(language, question)).collect(Collectors.toList());
        return Map.of("challengeId", challenge.getId(), "questions", prompts);
    }

    private Map<String, Object> question(String language, RecoveryQuestion question) {
        String prefix = "recovery.question." + question.getId();
        List<String> choices = new ArrayList<>();
        for (int i = 0; i < RecoveryQuestion.CHOICES; i++) {
            choices.add(messages.text(language, prefix + "." + i));
        }
        return Map.of("id", question.getId(), "prompt", messages.text(language, prefix),
                "choices", choices);
    }

    RecoveryOutcome claim(AuthenticatedProfile account, DesktopRequest request) throws IOException {
        String id = account.getProfile().getProfileId();
        String challengeId = request.text("challengeId");
        RecoveryChallenge challenge = Optional.ofNullable(challenges.get(id))
                .filter(current -> current.getId().equals(challengeId))
                .orElseThrow(() -> new IllegalArgumentException("Choose the current challenge."));
        List<Integer> answers = new ArrayList<>();
        request.array("answers").forEach(value -> answers.add(
                Math.toIntExact(DesktopRequest.integer(value, "answers"))));
        return service.claim(account, challenge, answers);
    }

    void forget(String id) {
        challenges.remove(id);
    }
}
