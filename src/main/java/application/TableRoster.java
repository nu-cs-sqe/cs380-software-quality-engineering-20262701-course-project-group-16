package application;

import domain.model.GameState;
import domain.model.Player;
import domain.model.PlayerType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

/** Funded seats for the next hand; new bots receive separate virtual house funding. */
final class TableRoster {
    static final long DEFAULT_BUY_IN = BuyInLimits.DEFAULT;

    private TableRoster() { }

    static List<Player> opening(GameState initial, Map<String, String> accounts,
            Map<String, Long> buyIns) {
        List<Player> players = new ArrayList<>();
        for (Player player : initial.getPlayers()) {
            long chips = player.getType() == PlayerType.HUMAN
                    ? buyIns.get(accounts.get(player.getId()))
                    : DEFAULT_BUY_IN;
            players.add(new Player(player.getId(), player.getSeat(), player.getType(), chips));
        }
        requireSafeTotal(players);
        return List.copyOf(players);
    }

    private static void requireSafeTotal(List<Player> players) {
        if (totalChips(players).isEmpty()) {
            throw new IllegalArgumentException("Funded table total exceeds the chip range.");
        }
    }

    static OptionalLong totalChips(List<Player> players) {
        long total = 0;
        for (Player player : players) {
            if (player.getChipStack() > Long.MAX_VALUE - total) {
                return OptionalLong.empty();
            }
            total += player.getChipStack();
        }
        return OptionalLong.of(total);
    }

    static List<Player> next(GameState paid, Set<String> departed, Map<String, Long> topUps) {
        return fundedRoster(paid, departed, topUps, true);
    }

    static List<Player> preview(GameState paid, Set<String> departed, Map<String, Long> topUps) {
        return fundedRoster(paid, departed, topUps, false);
    }

    private static List<Player> fundedRoster(GameState paid, Set<String> departed,
            Map<String, Long> topUps, boolean newIdentities) {
        List<Player> players = new ArrayList<>();
        for (Player player : paid.getPlayers()) {
            if (departed.contains(player.getId())
                    || (player.getType() == PlayerType.BOT && player.getChipStack() == 0)) {
                String id = newIdentities ? UUID.randomUUID().toString() : player.getId();
                players.add(new Player(id, player.getSeat(), PlayerType.BOT, DEFAULT_BUY_IN));
            } else {
                long chips = Math.addExact(player.getChipStack(),
                        topUps.getOrDefault(player.getId(), 0L));
                players.add(new Player(player.getId(), player.getSeat(), player.getType(), chips));
            }
        }
        return List.copyOf(players);
    }

    static Map<String, Long> startingStacks(GameState hand) {
        Map<String, Long> stacks = new java.util.LinkedHashMap<>();
        hand.getPlayerStates().forEach(player -> stacks.put(player.getPlayer().getId(),
                Math.addExact(player.getPlayer().getChipStack(), player.getHandContribution())));
        return Map.copyOf(stacks);
    }
}
