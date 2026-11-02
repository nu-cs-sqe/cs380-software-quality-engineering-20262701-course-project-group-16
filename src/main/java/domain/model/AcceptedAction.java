package domain.model;
/**
 * Immutable accepted engine result, with resolved all-in meaning and explicit stats context.
 * Construct only after engine legality checks; construction validates structural consistency.
 */
public final class AcceptedAction {
    private final String handId;
    private final String playerId;
    private final long roundContributionAfter;
    private final boolean becameAllIn;
    private final StatsAction observation;

    /**
     * @param chipsAdded actual new chips, never the requested raise-to total
     * @param roundContributionAfter resulting total contribution for this betting round
     * @throws IllegalArgumentException for missing fields or inconsistent action information
     */
    public AcceptedAction(String handId, String playerId, BettingStreet street, ResolvedActionType type,
            long chipsAdded, long roundContributionAfter, boolean becameAllIn, StatsContext context) {
        if (handId == null || handId.isBlank() || playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("Hand and player IDs are required.");
        }
        if (roundContributionAfter < 0 || chipsAdded > roundContributionAfter
                || (becameAllIn && (type == ResolvedActionType.CHECK || type == ResolvedActionType.FOLD))) {
            throw new IllegalArgumentException("Inconsistent resulting contribution or all-in flag.");
        }
        this.observation = new StatsAction(true, street, type, chipsAdded, context);
        this.handId = handId;
        this.playerId = playerId;
        this.roundContributionAfter = roundContributionAfter;
        this.becameAllIn = becameAllIn;
    }

    public String getHandId() { return handId; }
    public String getPlayerId() { return playerId; }
    public BettingStreet getStreet() { return observation.getStreet(); }
    public ResolvedActionType getType() { return observation.getType(); }
    public long getChipsAdded() { return observation.getChipsAdded(); }
    public long getRoundContributionAfter() { return roundContributionAfter; }
    public boolean becameAllIn() { return becameAllIn; }
    public StatsContext getStatsContext() { return observation.getContext(); }
    public StatsAction toStatsAction() { return observation; }
}
