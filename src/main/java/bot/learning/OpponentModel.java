package bot.learning;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

/** Match-scoped learning on one serialized owner thread; snapshots are immutable. */
public final class OpponentModel {
    private final String matchId;
    private final String observerId;
    private final Map<String, OpponentBeliefs> beliefs;
    private final BiFunction<OpponentBeliefs, PublicDecision, OpponentBeliefs> update;
    private long lastSequence;

    public OpponentModel(String matchId, String observerId, Set<String> opponents) {
        this(matchId, observerId, emptyBeliefs(opponents), OpponentBeliefs::withDecision);
    }

    OpponentModel(String matchId, String observerId, Map<String, OpponentBeliefs> initial,
            BiFunction<OpponentBeliefs, PublicDecision, OpponentBeliefs> update) {
        requireRegistration(matchId, observerId, initial, update);
        this.matchId = matchId;
        this.observerId = observerId;
        beliefs = new HashMap<>(initial);
        this.update = update;
    }

    private static Map<String, OpponentBeliefs> emptyBeliefs(Set<String> opponents) {
        if (opponents == null) {
            throw new IllegalArgumentException("A set of opponent identities is required.");
        }
        Map<String, OpponentBeliefs> result = new HashMap<>();
        opponents.forEach(id -> result.put(id, new OpponentBeliefs()));
        return result;
    }

    private static void requireRegistration(String match, String observer,
            Map<String, OpponentBeliefs> initial,
            BiFunction<OpponentBeliefs, PublicDecision, OpponentBeliefs> update) {
        requireId(match);
        requireId(observer);
        if (initial == null || update == null || initial.size() > 5
                || initial.values().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "At most five opponents and an updater are required.");
        }
        for (String id : initial.keySet()) {
            requireId(id);
            if (id.equals(observer)) {
                throw new IllegalArgumentException(
                        "Register opponents separately from the observer.");
            }
        }
    }

    private static void requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Match and participant IDs must be nonblank.");
        }
    }

    /**
     * Consumes committed events in match-wide increasing order, including skipped events.
     * Replayed/stale events return false. A failed update consumes nothing and may be retried.
     */
    public boolean observe(PublicDecision decision) {
        requireKnownEvent(decision);
        if (decision.getSequence() <= lastSequence) {
            return false;
        }
        String player = decision.getAction().getPlayerId();
        boolean learned = decision.isStrategic() && !observerId.equals(player);
        if (learned) {
            OpponentBeliefs next = Objects.requireNonNull(
                    update.apply(beliefs.get(player), decision));
            beliefs.put(player, next);
        }
        lastSequence = decision.getSequence();
        return learned;
    }

    private void requireKnownEvent(PublicDecision decision) {
        if (decision == null || !matchId.equals(decision.getMatchId())) {
            throw new IllegalArgumentException("The accepted event must belong to this match.");
        }
        String player = decision.getAction().getPlayerId();
        if (!observerId.equals(player) && !beliefs.containsKey(player)) {
            throw new IllegalArgumentException("Only registered participants can be observed.");
        }
    }

    public OpponentBeliefs snapshot(String opponentId) {
        OpponentBeliefs result = beliefs.get(opponentId);
        if (result == null) {
            throw new IllegalArgumentException("A registered opponent is required.");
        }
        return result;
    }
}
