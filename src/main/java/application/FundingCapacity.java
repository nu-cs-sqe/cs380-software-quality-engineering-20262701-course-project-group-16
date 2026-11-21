package application;

import domain.model.GameConfig;
import domain.model.Player;
import domain.model.PlayerType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/** Numeric headroom for payable winnings and replacement seats, separate from buy-in policy. */
final class FundingCapacity {
    private static final long REPLACEMENT_ALLOWANCE =
            BuyInLimits.DEFAULT * (GameConfig.SEAT_COUNT - 1);

    private FundingCapacity() { }

    static boolean accepts(List<Player> roster, Map<String, String> accounts,
            Map<String, Long> balances) {
        OptionalLong total = TableRoster.totalChips(roster);
        return total.isPresent() && total.orElseThrow()
                <= ceiling(roster, accounts, balances, Optional.empty());
    }

    static long maximumAddition(List<Player> roster, Map<String, String> accounts,
            Map<String, Long> balances, String selectedId) {
        OptionalLong total = TableRoster.totalChips(roster);
        if (total.isEmpty()) {
            return 0;
        }
        Player selected = roster.stream().filter(player -> player.getId().equals(selectedId))
                .findFirst().orElseThrow();
        long balance = balances.get(accounts.get(selectedId));
        long others = total.orElseThrow() - selected.getChipStack();
        if (balance > Long.MAX_VALUE - others - REPLACEMENT_ALLOWANCE) {
            return 0;
        }
        long limit = ceiling(roster, accounts, balances, Optional.of(selectedId));
        return total.orElseThrow() > limit ? 0 : Math.min(balance - selected.getChipStack(),
                limit - total.orElseThrow());
    }

    private static long ceiling(List<Player> roster, Map<String, String> accounts,
            Map<String, Long> balances, Optional<String> selected) {
        long limit = Long.MAX_VALUE - REPLACEMENT_ALLOWANCE;
        for (Player player : roster) {
            if (player.getType() == PlayerType.HUMAN && player.getChipStack() > 0
                    && selected.filter(player.getId()::equals).isEmpty()) {
                long outside = balances.get(accounts.get(player.getId())) - player.getChipStack();
                limit = Math.min(limit, Long.MAX_VALUE - outside - REPLACEMENT_ALLOWANCE);
            }
        }
        return limit;
    }
}
