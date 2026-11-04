package domain.model;

import java.util.List;

/** Immutable comparison result; it does not award chips or settle side pots. */
public final class ShowdownResult {
    public enum Status {
        AWAITING_SETTLEMENT
    }

    private final String handId;
    private final List<String> winnerIds;
    private final boolean uncontested;

    ShowdownResult(String handId, List<String> winners, boolean uncontested) {
        this.handId = handId;
        this.winnerIds = List.copyOf(winners);
        this.uncontested = uncontested;
    }

    public String getHandId() { return handId; }
    public List<String> getWinnerIds() { return winnerIds; }
    public boolean isUncontested() { return uncontested; }
    public Status getStatus() { return Status.AWAITING_SETTLEMENT; }
}
