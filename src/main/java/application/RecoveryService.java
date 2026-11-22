package application;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Grades one challenge and saves a reward only for an authenticated account. */
public final class RecoveryService {
    public static final long REWARD = 200;
    public static final int DAILY_LIMIT = 3;
    private final ProfileService profiles;
    private final RecoveryStore store;
    private final Clock clock;
    private final Random random;

    public RecoveryService(ProfileService profiles, RecoveryStore store, Clock clock,
            long seed) {
        this.profiles = profiles;
        this.store = store;
        this.clock = clock;
        random = new Random(seed);
    }

    public RecoveryChallenge begin() {
        List<RecoveryQuestion> shuffled = new ArrayList<>(RecoveryQuestion.BANK);
        Collections.shuffle(shuffled, random);
        return new RecoveryChallenge(shuffled.subList(0, RecoveryChallenge.QUESTION_COUNT));
    }

    public RecoveryOutcome claim(AuthenticatedProfile profile, RecoveryChallenge challenge,
            List<Integer> answers) throws IOException {
        profiles.requireAccount(profile);
        if (challenge.score(answers) < RecoveryChallenge.PASS_SCORE) {
            return RecoveryOutcome.WRONG_ANSWERS;
        }
        Instant now = clock.instant();
        return store.grantRecovery(profile.getProfile().getProfileId(), challenge.getId(),
                now.atZone(clock.getZone()).toLocalDate(), now);
    }
}
