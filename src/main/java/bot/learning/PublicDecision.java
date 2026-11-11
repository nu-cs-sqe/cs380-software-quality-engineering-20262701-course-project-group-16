package bot.learning;

import domain.model.AcceptedAction;
import domain.model.ResolvedActionType;
import domain.model.StatsContext;
import java.util.Optional;

/** Accepted public event and match-wide sequence supplied by the authoritative application. */
public final class PublicDecision {
    private final String matchId;
    private final long sequence;
    private final AcceptedAction action;
    private final Optional<DecisionContext> context;
    private final DecisionOrigin origin;

    public PublicDecision(String matchId, long sequence, AcceptedAction action,
            DecisionContext context, DecisionOrigin origin) {
        this(matchId, sequence, action, Optional.ofNullable(context), origin);
    }

    /** Automatic events need no legal-choice context and do not teach preferences. */
    public PublicDecision(String matchId, long sequence, AcceptedAction action,
            DecisionOrigin origin) {
        this(matchId, sequence, action, Optional.empty(), origin);
    }

    private PublicDecision(String matchId, long sequence, AcceptedAction action,
            Optional<DecisionContext> context, DecisionOrigin origin) {
        requireFields(matchId, sequence, action, origin);
        requireChoice(action, context, origin);
        this.matchId = matchId;
        this.sequence = sequence;
        this.action = action;
        this.context = context;
        this.origin = origin;
    }

    private static void requireFields(String match, long sequence, AcceptedAction action,
            DecisionOrigin origin) {
        if (match == null || match.isBlank() || sequence < 1 || action == null || origin == null) {
            throw new IllegalArgumentException("Match, sequence and accepted origin are required.");
        }
    }

    private static void requireChoice(AcceptedAction action, Optional<DecisionContext> supplied,
            DecisionOrigin origin) {
        if (origin == DecisionOrigin.DEPARTURE && action.getType() != ResolvedActionType.FOLD) {
            throw new IllegalArgumentException("An automatic departure event must be a fold.");
        }
        if (!isStrategic(action, origin)) {
            return;
        }
        DecisionContext context = supplied.orElseThrow(() ->
                new IllegalArgumentException("A player's choice requires its public context."));
        if (action.getStreet() != context.getStreet()
                || !context.getLegalActions().contains(action.getType())) {
            throw new IllegalArgumentException("The choice must match its public legal context.");
        }
        requireOpportunities(action.getStatsContext(), context);
    }

    private static void requireOpportunities(StatsContext stats, DecisionContext context) {
        if ((stats.hasThreeBetOpportunity()
                && !context.getLegalActions().contains(ResolvedActionType.RAISE))
                || (stats.hasFlopCBetOpportunity()
                && !context.getLegalActions().contains(ResolvedActionType.BET))) {
            throw new IllegalArgumentException(
                    "Opportunity flags require legal aggressive choices.");
        }
    }

    private static boolean isStrategic(AcceptedAction action, DecisionOrigin origin) {
        return origin == DecisionOrigin.PLAYER_CHOICE
                && action.getType() != ResolvedActionType.FORCED_POST;
    }

    public String getMatchId() { return matchId; }
    public long getSequence() { return sequence; }
    public AcceptedAction getAction() { return action; }
    public Optional<DecisionContext> getContext() { return context; }
    public boolean isStrategic() { return isStrategic(action, origin); }
}
