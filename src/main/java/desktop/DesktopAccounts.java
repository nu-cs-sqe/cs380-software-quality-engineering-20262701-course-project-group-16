package desktop;

import application.AuthenticatedProfile;
import application.ChipHistoryEntry;
import application.ProfileOverview;
import application.ProfileService;
import application.StatisticsView;
import domain.model.PlayerProfile;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Owns password-verified handles for this child process only. */
final class DesktopAccounts {
    private final ProfileService profiles;
    private final Map<String, AuthenticatedProfile> signedIn = new HashMap<>();

    DesktopAccounts(ProfileService profiles) {
        this.profiles = profiles;
    }

    List<Map<String, String>> list() throws IOException {
        return profiles.list().stream().map(DesktopAccounts::identity)
                .collect(Collectors.toList());
    }

    Map<String, Object> authenticate(DesktopRequest request, boolean register)
            throws IOException, GeneralSecurityException {
        char[] password = request.text("password").toCharArray();
        try {
            String id = register ? profiles.create(request.text("name"), password).getProfileId()
                    : request.text("id");
            AuthenticatedProfile account = profiles.login(id, password).orElseThrow(() ->
                    new IllegalArgumentException("The account or password is incorrect."));
            Map<String, Object> details = overview(account);
            signedIn.put(id, account);
            return details;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    void logout(String id) {
        require(id);
        signedIn.remove(id);
    }

    AuthenticatedProfile require(String id) {
        return Optional.ofNullable(signedIn.get(id)).orElseThrow(() ->
                new IllegalArgumentException("Sign in to this account first."));
    }

    Map<String, Object> overview(AuthenticatedProfile account) throws IOException {
        ProfileOverview saved = profiles.getOverview(account);
        Map<String, Object> details = new LinkedHashMap<>(identity(account.getProfile()));
        details.put("balance", Long.toString(saved.getBalance()));
        details.put("history", saved.getHistory().stream().map(DesktopAccounts::entry)
                .collect(Collectors.toList()));
        details.put("metrics", metrics(profiles.getLifetimeStatistics(account)));
        return details;
    }

    static Map<String, Object> metrics(StatisticsView stats) {
        Map<String, Double> rates = new LinkedHashMap<>();
        for (int index = 0; index < StatisticsView.METRICS.size(); index++) {
            String name = StatisticsView.METRICS.get(index);
            stats.getRates().get(index).ifPresent(value -> rates.put(name, value));
        }
        return Map.of("handsPlayed", stats.getHandsPlayed(), "rates", rates);
    }

    private static Map<String, String> identity(PlayerProfile profile) {
        return Map.of("id", profile.getProfileId(), "name", profile.getDisplayName());
    }

    private static Map<String, Object> entry(ChipHistoryEntry entry) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("type", entry.getType().name());
        fields.put("timestamp", entry.getTimestamp().toString());
        fields.put("change", Long.toString(entry.getChange()));
        fields.put("balance", Long.toString(entry.getBalance()));
        entry.getHandId().ifPresent(id -> fields.put("handId", id));
        entry.getSessionId().ifPresent(id -> fields.put("sessionId", id));
        entry.getSessionNet().ifPresent(net -> fields.put("sessionNet", Long.toString(net)));
        return fields;
    }
}
