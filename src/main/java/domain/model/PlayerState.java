package domain.model;

import java.util.List;
import java.util.HashSet;

/** Immutable per-hand state; hand contribution includes the current round contribution. */
public final class PlayerState {
    private final Player player;
    private final PlayerStatus status;
    private final List<Card> holeCards;
    private final long roundContribution;
    private final long handContribution;

    /** @throws IllegalArgumentException for inconsistent status, cards or chip amounts */
    public PlayerState(Player player, PlayerStatus status, List<Card> cards, long round, long hand) {
        if (player == null || status == null || round < 0 || hand < round
                || player.getChipStack() > Long.MAX_VALUE - hand) {
            throw new IllegalArgumentException("Invalid player, status or contribution.");
        }
        if (cards == null || (cards.size() != 0 && cards.size() != 2)
                || cards.stream().anyMatch(card -> card == null)
                || new HashSet<>(cards).size() != cards.size()) {
            throw new IllegalArgumentException("Hole cards must be empty or two distinct cards.");
        }
        validateStatus(player.getChipStack(), status, hand, cards);
        this.player = player;
        this.status = status;
        this.holeCards = List.copyOf(cards);
        this.roundContribution = round;
        this.handContribution = hand;
    }

    private static void validateStatus(long chips, PlayerStatus status, long hand, List<Card> cards) {
        if ((status == PlayerStatus.ACTIVE || status == PlayerStatus.FOLDED) && chips == 0) {
            throw new IllegalArgumentException("A zero-stack live player must be marked all-in.");
        }
        if (status == PlayerStatus.ALL_IN && (chips != 0 || hand == 0)) {
            throw new IllegalArgumentException("An all-in player has zero remaining chips and a commitment.");
        }
        if (status == PlayerStatus.ELIMINATED && (chips != 0 || hand != 0 || !cards.isEmpty())) {
            throw new IllegalArgumentException("An eliminated seat has no chips, commitments or cards.");
        }
    }

    public Player getPlayer() { return player; }
    public PlayerStatus getStatus() { return status; }
    public List<Card> getHoleCards() { return holeCards; }
    public long getRoundContribution() { return roundContribution; }
    public long getHandContribution() { return handContribution; }
    public boolean canAct() { return status == PlayerStatus.ACTIVE; }
    public boolean isParticipating() { return status == PlayerStatus.ACTIVE || status == PlayerStatus.ALL_IN; }
}
