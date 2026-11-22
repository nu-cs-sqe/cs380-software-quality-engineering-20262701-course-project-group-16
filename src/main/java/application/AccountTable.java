package application;

import domain.model.CompletedStatistics;
import domain.model.GameState;
import domain.model.GamePhase;
import domain.model.Player;
import domain.model.PlayerType;
import domain.model.Settlement;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Owner-thread account-table bookkeeping. The store owns durable transactions and rewards. */
final class AccountTable {
    private final TableAccountStore store;
    private final String sessionId;
    private final Map<String, String> accounts;
    private final Map<String, Long> balances;
    private final Map<String, Long> netChanges;
    private final Map<String, Long> startingStacks;
    private final Map<String, Long> topUps = new LinkedHashMap<>();

    private AccountTable(TableAccountStore store, String sessionId, Map<String, String> accounts,
            Map<String, Long> balances, Map<String, Long> netChanges, GameState hand) {
        this.store = store;
        this.sessionId = sessionId;
        Set<String> current = hand.getPlayers().stream().map(Player::getId)
                .collect(Collectors.toSet());
        Map<String, String> seated = new LinkedHashMap<>();
        accounts.forEach((id, account) -> {
            if (current.contains(id)) {
                seated.put(id, account);
            }
        });
        this.accounts = Map.copyOf(seated);
        this.balances = Map.copyOf(balances);
        this.netChanges = Map.copyOf(netChanges);
        this.startingStacks = TableRoster.startingStacks(hand);
    }

    static AccountTable open(TableAccountStore store, Map<String, String> accounts,
            GameState initial) {
        Map<String, Long> balances;
        try {
            balances = Map.copyOf(store.getBalances(Set.copyOf(accounts.values())));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        return open(store, accounts, initial, balances);
    }

    AccountTable withRecoveredBalance(GameState paid, Set<String> departed,
            String accountId, long balance) {
        Map<String, Long> loaded = new LinkedHashMap<>();
        for (String id : accounts.values()) {
            loaded.put(id, balances.get(id));
        }
        loaded.put(accountId, balance);
        return refreshWithBalances(paid, departed, loaded);
    }

    static AccountTable open(TableAccountStore store, Map<String, String> accounts,
            GameState initial, Map<String, Long> balances) {
        if (!balances.keySet().equals(Set.copyOf(accounts.values()))
                || balances.values().stream().anyMatch(chips -> chips < BuyInLimits.MINIMUM)) {
            throw new IllegalArgumentException("Selected accounts require at least 400 chips.");
        }
        return new AccountTable(store, UUID.randomUUID().toString(), accounts, balances,
                Map.of(), initial);
    }

    AccountTable refresh(GameState paid, Set<String> departed) {
        Map<String, Long> loaded;
        try {
            loaded = Map.copyOf(store.getBalances(Set.copyOf(accounts.values())));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        return refreshWithBalances(paid, departed, loaded);
    }

    private AccountTable refreshWithBalances(GameState paid, Set<String> departed,
            Map<String, Long> loaded) {
        if (!loaded.keySet().equals(Set.copyOf(accounts.values()))) {
            throw new IllegalArgumentException("Every seated account requires a saved balance.");
        }
        Map<String, TableFunds> funds = displayedFunds(paid, departed, false);
        loaded.forEach((id, balance) -> {
            if (balance < funds.get(id).getTableChips()) {
                throw new IllegalArgumentException("Saved balance is below reserved table chips.");
            }
        });
        AccountTable next = new AccountTable(store, sessionId, accounts, loaded, netChanges, paid);
        next.topUps.putAll(topUps);
        next.requireCapacity(TableRoster.preview(paid, departed, topUps));
        return next;
    }

    Map<String, Long> sessionNetChanges() { return netChanges; }

    Map<String, Long> defaultBuyIns() {
        Map<String, Long> choices = new LinkedHashMap<>();
        accounts.values().forEach(id ->
                choices.put(id, BuyInLimits.defaultAmount(balances.get(id))));
        return Map.copyOf(choices);
    }

    List<Player> openingRoster(GameState initial, Map<String, Long> buyIns) {
        if (!buyIns.keySet().equals(Set.copyOf(accounts.values()))) {
            throw new IllegalArgumentException("Choose a buy-in for each selected account only.");
        }
        buyIns.forEach((id, chips) -> BuyInLimits.requireAmount(chips, balances.get(id)));
        List<Player> roster = TableRoster.opening(initial, accounts, buyIns);
        requireCapacity(roster);
        return roster;
    }

    AccountTable begin(GameState hand) {
        return new AccountTable(store, sessionId, accounts, balances, netChanges, hand);
    }

    Map<String, String> profileIds() { return accounts; }

    Map<String, TableFunds> displayedFunds(GameState hand, Set<String> departed, boolean stopped) {
        Map<String, TableFunds> result = new LinkedHashMap<>();
        for (Player player : hand.getPlayers()) {
            if (accounts.containsKey(player.getId())) {
                result.put(accounts.get(player.getId()), snapshot(hand, departed, stopped, player));
            }
        }
        return Map.copyOf(result);
    }

    private TableFunds snapshot(GameState hand, Set<String> departed, boolean stopped,
            Player player) {
        String id = player.getId();
        String account = accounts.get(id);
        boolean released = stopped || (hand.getPhase() == GamePhase.COMPLETION
                && departed.contains(id));
        long reserved = released ? 0 : Math.addExact(Math.addExact(player.getChipStack(),
                hand.getCommittedChips(player.getSeat())), topUps.getOrDefault(id, 0L));
        long capacity = released || hand.getPhase() != GamePhase.COMPLETION ? 0
                : FundingCapacity.maximumAddition(TableRoster.preview(hand, departed, topUps),
                        accounts, balances, id);
        return new TableFunds(balances.get(account), reserved,
                netChanges.getOrDefault(account, 0L), capacity);
    }

    void addChips(GameState paid, Set<String> departed, String account, long chips) {
        Player player = paid.getPlayers().stream().filter(seat -> accounts.containsKey(seat.getId())
                && account.equals(accounts.get(seat.getId()))).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown account."));
        if (departed.contains(player.getId())) {
            throw new IllegalArgumentException("Choose an account that has not left.");
        }
        TableFunds funds = snapshot(paid, departed, false, player);
        if (chips < funds.getMinimumTopUp() || chips > funds.getMaximumTopUp()) {
            throw new IllegalArgumentException("Choose an amount within the funding range.");
        }
        topUps.put(player.getId(), Math.addExact(topUps.getOrDefault(player.getId(), 0L), chips));
    }

    private void requireCapacity(List<Player> roster) {
        if (!FundingCapacity.accepts(roster, accounts, balances)) {
            throw new IllegalArgumentException("Funding needs room for winnings and replacements.");
        }
    }

    List<Player> nextRoster(GameState paid, Set<String> departed) {
        return TableRoster.next(paid, departed, topUps);
    }

    boolean canContinue(GameState paid, Set<String> departed) {
        List<Player> roster = TableRoster.preview(paid, departed, topUps);
        return roster.stream().filter(player -> player.getChipStack() > 0).count() >= 2
                && FundingCapacity.accepts(roster, accounts, balances);
    }

    HandCompletion completion(GameState paid, Map<String, CompletedStatistics> statistics) {
        Map<String, HandCompletion.AccountChange> changes = new LinkedHashMap<>();
        Map<String, Long> bots = new LinkedHashMap<>();
        for (Player player : paid.getPlayers()) {
            String id = player.getId();
            long starting = startingStacks.get(id);
            if (player.getType() == PlayerType.BOT) {
                bots.put(id, Math.subtractExact(player.getChipStack(), starting));
            } else {
                String account = accounts.get(id);
                changes.put(account, new HandCompletion.AccountChange(account,
                        balances.get(account), starting, player.getChipStack(),
                        statistics.get(id)));
            }
        }
        return new HandCompletion(sessionId, paid.getHandId(), changes, bots);
    }

    AccountTable afterPayment(HandCompletion hand, Settlement paid) {
        Map<String, Long> nextBalances = new LinkedHashMap<>(balances);
        Map<String, Long> nextNet = new LinkedHashMap<>(netChanges);
        hand.getAccounts().forEach((id, change) -> {
            nextBalances.put(id, change.getBalanceAfter());
            nextNet.put(id, Math.addExact(nextNet.getOrDefault(id, 0L), change.getNetChange()));
        });
        AccountTable next = new AccountTable(store, sessionId, accounts, nextBalances,
                nextNet, paid.getState());
        return next;
    }

    void save(HandCompletion hand) {
        try {
            store.save(hand);
        } catch (IOException failure) {
            throw new UncheckedIOException("Account hand could not be saved.", failure);
        }
    }
}
