package application;

import java.time.Instant;

/** One saved balance change, with the running net for its play session. */
public final class ChipHistoryEntry {
    public enum Type { HAND_SETTLEMENT }

    private final String handId;
    private final String sessionId;
    private final Instant timestamp;
    private final Type type;
    private final long change;
    private final long balance;
    private final long sessionNet;

    public ChipHistoryEntry(String handId, String sessionId, Instant timestamp, Type type,
            long change, long balance, long sessionNet) {
        this.handId = handId;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.type = type;
        this.change = change;
        this.balance = balance;
        this.sessionNet = sessionNet;
    }

    public String getHandId() { return handId; }
    public String getSessionId() { return sessionId; }
    public Instant getTimestamp() { return timestamp; }
    public Type getType() { return type; }
    public long getChange() { return change; }
    public long getBalance() { return balance; }
    public long getSessionNet() { return sessionNet; }
}
