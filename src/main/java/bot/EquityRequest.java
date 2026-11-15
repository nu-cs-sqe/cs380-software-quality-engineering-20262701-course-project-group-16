package bot;

import domain.model.Card;
import domain.model.GameView;
import domain.model.GameConfig;
import domain.model.PlayerStatus;
import domain.model.PlayerView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Immutable legitimate information and a bounded trial count for one equity calculation. */
public final class EquityRequest {
    public static final int MAX_SIMULATIONS = 10000;
    private final String heroId;
    private final List<Card> holeCards;
    private final List<Card> board;
    private final List<String> opponents;
    private final List<EquityPot> pots;
    private final int simulations;
    private final long eligiblePotAmount;
    private final long totalPotAmount;

    public EquityRequest(String heroId, List<Card> holeCards, List<Card> board,
            List<String> opponents, List<EquityPot> pots, int simulations) {
        requireBounds(holeCards, board, opponents, pots, simulations);
        requireKnownCards(holeCards, board);
        Set<String> participants = requireParticipants(heroId, opponents);
        requirePots(pots, participants);
        long eligible = requireHeroEligiblePot(heroId, pots);
        totalPotAmount = pots.stream().mapToLong(EquityPot::getAmount).reduce(0L, Math::addExact);
        this.heroId = heroId;
        this.holeCards = List.copyOf(holeCards);
        this.board = List.copyOf(board);
        this.opponents = List.copyOf(opponents);
        this.pots = List.copyOf(pots);
        this.simulations = simulations;
        eligiblePotAmount = eligible;
    }

    private static long requireHeroEligiblePot(String heroId, List<EquityPot> pots) {
        long eligible = pots.stream().filter(pot -> pot.getEligiblePlayers().contains(heroId))
                .mapToLong(EquityPot::getAmount).reduce(0L, Math::addExact);
        if (eligible == 0) {
            throw new IllegalArgumentException("The bot must be eligible for an awardable pot.");
        }
        return eligible;
    }

    private static void requireBounds(List<Card> holes, List<Card> board,
            List<String> opponents, List<EquityPot> pots, int simulations) {
        if (holes.size() != 2 || !Set.of(0, 3, 4, 5).contains(board.size())) {
            throw new IllegalArgumentException("Use two hole cards and a valid Hold'em board.");
        }
        if (opponents.size() >= GameConfig.SEAT_COUNT
                || pots.isEmpty() || pots.size() > GameConfig.SEAT_COUNT
                || simulations < 1 || simulations > MAX_SIMULATIONS) {
            throw new IllegalArgumentException("Player, pot or simulation count is out of range.");
        }
    }

    private static void requireKnownCards(List<Card> holes, List<Card> board) {
        Set<Card> known = new HashSet<>();
        addKnown(holes, known);
        addKnown(board, known);
    }

    private static void addKnown(List<Card> cards, Set<Card> known) {
        for (Card card : cards) {
            if (!known.add(card)) {
                throw new IllegalArgumentException("Known cards must be unique.");
            }
        }
    }

    private static Set<String> requireParticipants(String hero, List<String> opponents) {
        Set<String> players = new HashSet<>();
        addParticipant(players, hero);
        for (String opponent : opponents) {
            addParticipant(players, opponent);
        }
        return players;
    }

    private static void addParticipant(Set<String> players, String id) {
        requireId(id);
        if (!players.add(id)) {
            throw new IllegalArgumentException("Participant IDs must be unique.");
        }
    }

    private static void requirePots(List<EquityPot> pots, Set<String> players) {
        Set<String> ids = new HashSet<>();
        for (EquityPot pot : pots) {
            if (!ids.add(pot.getId())
                    || !players.containsAll(pot.getEligiblePlayers())) {
                throw new IllegalArgumentException("Pots require unique IDs and live eligibility.");
            }
        }
    }

    private static void requireId(String id) {
        if (id.isBlank()) {
            throw new IllegalArgumentException("Participant IDs must be nonblank.");
        }
    }

    /** Uses only the viewer's cards and public information; pots describe a supplied scenario. */
    public static EquityRequest fromView(GameView view, List<EquityPot> pots, int simulations) {
        PlayerView hero = view.getPlayers().stream()
                .filter(player -> player.getId().equals(view.getViewerId())).findFirst()
                .orElseThrow();
        if (!isLive(hero)) {
            throw new IllegalArgumentException("The viewer must still participate in the hand.");
        }
        List<String> opponents = view.getPlayers().stream().filter(EquityRequest::isLive)
                .map(PlayerView::getId).filter(id -> !id.equals(view.getViewerId()))
                .collect(Collectors.toList());
        return new EquityRequest(hero.getId(), hero.getHoleCards(), view.getCommunityCards(),
                opponents, pots, simulations);
    }

    private static boolean isLive(PlayerView player) {
        return player.getStatus() == PlayerStatus.ACTIVE
                || player.getStatus() == PlayerStatus.ALL_IN;
    }

    public String getHeroId() { return heroId; }
    public List<Card> getHoleCards() { return holeCards; }
    public List<Card> getBoard() { return board; }
    public List<String> getOpponents() { return opponents; }
    public List<EquityPot> getPots() { return pots; }
    public int getSimulations() { return simulations; }
    public long getEligiblePotAmount() { return eligiblePotAmount; }
    public long getTotalPotAmount() { return totalPotAmount; }
}
