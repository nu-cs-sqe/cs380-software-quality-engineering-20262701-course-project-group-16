package bot.learning;

import domain.model.*;
import java.util.Set;

final class LearningFixtures {
    static final StatsContext NONE = new StatsContext(PreflopRaiseType.NONE, false, false, false);
    static DecisionContext context(BettingStreet street, boolean position, ResolvedActionType... choices) {
        return new DecisionContext(street, Set.of(choices), position);
    }
    static AcceptedAction action(String player, BettingStreet street, ResolvedActionType type, StatsContext stats) {
        long added = type == ResolvedActionType.CHECK || type == ResolvedActionType.FOLD ? 0 : 20;
        return new AcceptedAction("hand", player, street, type, added, added, false, stats);
    }
    static PublicDecision decision(long sequence, String player, BettingStreet street,
            ResolvedActionType type, StatsContext stats, ResolvedActionType... choices) {
        AcceptedAction accepted = action(player, street, type, stats);
        AcceptedAction numbered = new AcceptedAction("hand-" + sequence, player, street, type,
                accepted.getChipsAdded(), accepted.getRoundContributionAfter(), false, stats);
        return new PublicDecision("match", sequence, numbered,
                context(street, false, choices), DecisionOrigin.PLAYER_CHOICE);
    }
}
