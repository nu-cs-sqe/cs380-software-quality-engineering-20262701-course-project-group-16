package application;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;

/** Atomically checks eligibility and records one successful recovery claim. */
public interface RecoveryStore {
    RecoveryOutcome grantRecovery(String profileId, String challengeId,
            LocalDate date, Instant timestamp) throws IOException;
}
