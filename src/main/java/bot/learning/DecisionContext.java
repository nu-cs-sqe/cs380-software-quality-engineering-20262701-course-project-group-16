package bot.learning;

import domain.model.BettingStreet;
import domain.model.ResolvedActionType;
import java.util.Set;

/** Public engine-supplied situation; legal choices are not reconstructed here. */
public final class DecisionContext {
    private final BettingStreet street;
    private final Set<ResolvedActionType> legalActions;
    private final boolean inPosition;

    public DecisionContext(BettingStreet street, Set<ResolvedActionType> legalActions,
            boolean inPosition) {
        requireChoices(street, legalActions);
        this.street = street;
        this.legalActions = Set.copyOf(legalActions);
        this.inPosition = inPosition;
    }

    private static void requireChoices(BettingStreet street, Set<ResolvedActionType> choices) {
        if (street == null || choices == null || choices.isEmpty()
                || choices.stream().anyMatch(action -> action == null
                || action == ResolvedActionType.FORCED_POST)) {
            throw new IllegalArgumentException("A street and legal decision choices are required.");
        }
    }

    public BettingStreet getStreet() { return street; }
    public Set<ResolvedActionType> getLegalActions() { return legalActions; }
    public boolean isInPosition() { return inPosition; }

    // Explicit situation matching; this descriptor is not used as a mutable/hash-map key.
    boolean matches(DecisionContext other) {
        return street == other.street && inPosition == other.inPosition
                && legalActions.equals(other.legalActions);
    }
}
