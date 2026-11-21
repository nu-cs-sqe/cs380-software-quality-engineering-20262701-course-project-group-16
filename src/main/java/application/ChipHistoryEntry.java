package application;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

/** One saved balance change, with the running net for its play session. */
public final class ChipHistoryEntry {
    public enum Type { HAND_SETTLEMENT, RECOVERY_REWARD }

    private final Optional<String> handId;
    private final Optional<String> sessionId;
    private final Instant timestamp;
    private final Type type;
    private final long change;
    private final long balance;
    private final OptionalLong sessionNet;

    public ChipHistoryEntry(String handId, String sessionId, Instant timestamp,
            long change, long balance, long sessionNet) {
        this(Optional.of(handId), Optional.of(sessionId), timestamp, Type.HAND_SETTLEMENT,
                change, balance, OptionalLong.of(sessionNet));
    }

    public ChipHistoryEntry(Instant timestamp, long change, long balance) {
        this(Optional.empty(), Optional.empty(), timestamp, Type.RECOVERY_REWARD,
                change, balance, OptionalLong.empty());
    }

    private ChipHistoryEntry(Optional<String> handId, Optional<String> sessionId,
            Instant timestamp, Type type, long change, long balance, OptionalLong sessionNet) {
        this.handId = handId;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.type = type;
        this.change = change;
        this.balance = balance;
        this.sessionNet = sessionNet;
    }

    public Optional<String> getHandId() { return handId; }
    public Optional<String> getSessionId() { return sessionId; }
    public Instant getTimestamp() { return timestamp; }
    public Type getType() { return type; }
    public long getChange() { return change; }
    public long getBalance() { return balance; }
    public OptionalLong getSessionNet() { return sessionNet; }
}
