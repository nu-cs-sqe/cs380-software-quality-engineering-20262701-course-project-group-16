package domain;
import java.util.OptionalLong;

/** Immutable intent. BET/RAISE amounts mean the target total for this betting round. */
public final class ActionRequest {
    private final String playerId;
    private final ActionType type;
    private final OptionalLong targetAmount;

    /** Creates CHECK, CALL or FOLD intent. No caller-supplied chip deduction is accepted. */
    public ActionRequest(String playerId, ActionType type) {
        this(playerId, type, OptionalLong.empty());
    }

    /** Creates BET or RAISE intent; the engine validates limits and calculates chips added. */
    public ActionRequest(String playerId, ActionType type, long targetAmount) {
        this(playerId, type, OptionalLong.of(targetAmount));
    }

    private ActionRequest(String playerId, ActionType type, OptionalLong targetAmount) {
        if (playerId == null || playerId.isBlank() || type == null) {
            throw new IllegalArgumentException("A player ID and action type are required.");
        }
        boolean needsAmount = type == ActionType.BET || type == ActionType.RAISE;
        if (needsAmount != targetAmount.isPresent()
                || (targetAmount.isPresent() && targetAmount.getAsLong() <= 0)) {
            throw new IllegalArgumentException("Only bets and raises require a positive round target.");
        }
        this.playerId = playerId;
        this.type = type;
        this.targetAmount = targetAmount;
    }

    public String getPlayerId() { return playerId; }
    public ActionType getType() { return type; }
    public OptionalLong getTargetAmount() { return targetAmount; }
}
