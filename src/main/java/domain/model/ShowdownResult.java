package domain.model;

import java.util.List;

/** Immutable comparison result; it does not award chips or settle side pots. */
public final class ShowdownResult {
    public enum Status { AWAITING_SETTLEMENT }

    ShowdownResult(String handId, List<String> winners, boolean uncontested) { }

    public String getHandId() { return null; }
    public List<String> getWinnerIds() { return null; }
    public boolean isUncontested() { return false; }
    public Status getStatus() { return null; }
}
