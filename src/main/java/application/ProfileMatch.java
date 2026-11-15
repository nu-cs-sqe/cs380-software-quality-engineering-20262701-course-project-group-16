package application;

import domain.model.GameState;
import domain.model.Player;
import domain.model.PlayerType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Stable match-player to authenticated-profile bindings; bots are never persisted. */
final class ProfileMatch {
    private final ProfileService profiles;
    private final Map<String, AuthenticatedProfile> accounts;

    static void requireAccounts(ProfileService profiles, List<AuthenticatedProfile> humans) {
        if (profiles == null || humans == null || humans.isEmpty() || humans.size() > 6) {
            throw new IllegalArgumentException("Choose one to six authenticated human accounts.");
        }
        Set<String> ids = new HashSet<>();
        for (AuthenticatedProfile human : humans) {
            profiles.requireAccount(human);
            if (!ids.add(human.getProfile().getProfileId())) {
                throw new IllegalArgumentException("A profile may occupy only one seat.");
            }
        }
    }

    ProfileMatch(ProfileService profiles, List<AuthenticatedProfile> humans, GameState state) {
        this.profiles = profiles;
        List<Player> players = state.getPlayers().stream()
                .filter(player -> player.getType() == PlayerType.HUMAN)
                .collect(Collectors.toList());
        if (players.size() != humans.size()) {
            throw new IllegalArgumentException(
                    "Match human count differs from authenticated seats.");
        }
        Map<String, AuthenticatedProfile> bindings = new LinkedHashMap<>();
        for (int index = 0; index < humans.size(); index++) {
            bindings.put(players.get(index).getId(), humans.get(index));
        }
        accounts = Map.copyOf(bindings);
    }

    Map<String, String> profileIds() {
        Map<String, String> ids = new LinkedHashMap<>();
        accounts.forEach((player, account) -> ids.put(player, account.getProfile().getProfileId()));
        return Map.copyOf(ids);
    }

    Map<String, StatisticsView> snapshot() throws IOException {
        Map<String, StatisticsView> views = new LinkedHashMap<>();
        for (Map.Entry<String, AuthenticatedProfile> binding : accounts.entrySet()) {
            views.put(binding.getKey(), profiles.getLifetimeStatistics(binding.getValue()));
        }
        return Map.copyOf(views);
    }

    void save(MatchStatistics hand) {
        try {
            profiles.saveCompleted(accounts, hand.completedSnapshots());
        } catch (IOException failure) {
            throw new UncheckedIOException(
                    "Completed profile statistics could not be saved.", failure);
        }
    }
}
