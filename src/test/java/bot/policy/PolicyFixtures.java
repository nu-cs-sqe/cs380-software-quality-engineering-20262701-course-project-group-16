package bot.policy;

import bot.learning.OpponentBeliefs;
import domain.model.*;
import java.util.*;
import java.util.stream.Collectors;

final class PolicyFixtures {
    private PolicyFixtures() { }

    static Fixture table() { return new Fixture(); }

    static final class Fixture {
        int hero = 3;
        int actor = 3;
        int dealer = 0;
        boolean raiseRights = true;
        GamePhase phase = GamePhase.PREFLOP;
        long[] round = {0, 10, 20, 0, 0, 0};
        long[] hand = round.clone();
        long[] stacks = {1000, 990, 980, 1000, 1000, 1000};
        PlayerStatus[] statuses = new PlayerStatus[6];

        Fixture() { Arrays.fill(statuses, PlayerStatus.ACTIVE); }

        Fixture street(GamePhase next) {
            phase = next;
            Arrays.fill(round, 0);
            Arrays.fill(hand, 40);
            Arrays.fill(stacks, 960);
            return this;
        }

        Fixture eliminated(int seat) {
            round[seat] = 0;
            hand[seat] = 0;
            stacks[seat] = 0;
            statuses[seat] = PlayerStatus.ELIMINATED;
            return this;
        }

        GameState state() {
            List<Card> cards = cards();
            List<PlayerState> players = players(cards);
            long current = Arrays.stream(round).max().orElseThrow();
            Set<String> pending = players.stream().filter(PlayerState::canAct)
                    .map(player -> player.getPlayer().getId()).collect(Collectors.toSet());
            List<Card> community = cards.subList(12, 12 + boardSize());
            return GameState.create(new GameConfig(), "policy", phase, players,
                    new SeatOrder(players).positionsFor(dealer), Optional.of("p" + actor),
                    community, cards.subList(12 + boardSize(), cards.size()),
                    new BettingProgress(current, 20, pending, raiseRights ? pending : Set.of()));
        }

        private List<PlayerState> players(List<Card> cards) {
            List<PlayerState> players = new ArrayList<>();
            long total = Arrays.stream(stacks).sum() + Arrays.stream(hand).sum();
            for (int seat = 0; seat < 6; seat++) {
                long stack = stacks[seat] + (seat == 0 ? 6000 - total : 0);
                Player player = new Player("p" + seat, seat,
                        seat == 0 ? PlayerType.HUMAN : PlayerType.BOT, stack);
                List<Card> holes = statuses[seat] == PlayerStatus.ELIMINATED
                        || phase == GamePhase.SETUP ? List.of()
                        : cards.subList(seat * 2, seat * 2 + 2);
                players.add(new PlayerState(player, statuses[seat], holes,
                        round[seat], hand[seat]));
            }
            return players;
        }

        private int boardSize() {
            return phase == GamePhase.FLOP ? 3 : phase == GamePhase.TURN ? 4
                    : phase == GamePhase.RIVER ? 5 : 0;
        }

        LegalActions legal() { return new BettingEngine().getLegalActions(state()); }

        PolicyRequest request(double equity) {
            return request(equity, Map.of());
        }

        PolicyRequest request(double equity, Map<String, OpponentBeliefs> beliefs) {
            GameState state = state();
            LegalActions legal = new BettingEngine().getLegalActions(state);
            return new PolicyRequest(state.viewFor("p" + hero), legal,
                    new CallValue(equity, state.getPot() + legal.getCallCost(), 0), beliefs);
        }
    }

    private static List<Card> cards() {
        return Arrays.stream(Suit.values()).flatMap(suit -> Arrays.stream(Rank.values())
                .map(rank -> new Card(rank, suit))).collect(Collectors.toList());
    }
}
