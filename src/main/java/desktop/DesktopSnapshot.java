package desktop;

import application.SetupSession;
import application.SetupView;
import application.TableFunds;
import domain.model.AcceptedAction;
import domain.model.Card;
import domain.model.HandPositions;
import domain.model.LegalActions;
import domain.model.Player;
import domain.model.Settlement;
import domain.model.ShowdownResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Explicit renderer data projection. Its only card source is the privacy-filtered view. */
final class DesktopSnapshot {
    private final SetupSession session;
    private final SetupView view;
    private final DesktopAccounts accounts;
    private final Map<String, String> bindings;

    DesktopSnapshot(SetupSession session, DesktopAccounts accounts) {
        this.session = session;
        this.accounts = accounts;
        view = session.getView().orElseThrow();
        bindings = session.getProfileIds();
    }

    Map<String, Object> render(long revision, long handNumber, Map<String, String> before) {
        Map<String, Object> game = handFields(revision, handNumber);
        game.put("players", view.getPlayers().stream().map(this::player)
                .collect(Collectors.toList()));
        game.put("legal", legal(session.getLegalActions()));
        List<AcceptedAction> actions = session.getAcceptedActions();
        game.put("lastAction", lastAction(actions));
        game.put("streetActions", latestActionsOnStreet(actions));
        game.put("result", session.getResult().<Object>map(result -> result(result, before))
                .orElse(""));
        game.put("metrics", project(session.getStatistics(), DesktopAccounts::metrics));
        game.put("funds", project(session.getTableFunds(), DesktopSnapshot::funds));
        return game;
    }

    private Map<String, Object> handFields(long revision, long handNumber) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("handId", view.getHandId());
        fields.put("revision", revision);
        fields.put("handNumber", handNumber);
        fields.put("smallBlind", Long.toString(view.getSmallBlind()));
        fields.put("bigBlind", Long.toString(view.getBigBlind()));
        fields.put("phase", view.getPhase());
        fields.put("pot", Long.toString(view.getPot()));
        fields.put("board", cards(view.getCommunityCards()));
        fields.put("hole", cards(view.getHoleCards()));
        fields.put("actor", session.getResult().isPresent() ? -1 : view.getCurrentActorSeat());
        fields.put("paid", session.getSettlement().isPresent());
        fields.put("canNext", session.canStartNextHand());
        return fields;
    }

    private Map<String, Object> player(Player player) {
        int seat = player.getSeat();
        String id = player.getId();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("id", id);
        fields.put("seat", seat);
        fields.put("name", name(player));
        Optional.ofNullable(bindings.get(id)).ifPresent(accountId ->
                fields.put("accountId", accountId));
        fields.put("type", player.getType());
        fields.put("role", role(seat, view.getPositions()));
        fields.put("status", session.hasDeparted(id) ? "DEPARTED" : view.getStatuses().get(seat));
        fields.put("stack", Long.toString(player.getChipStack()));
        fields.put("round", Long.toString(view.getRoundContribution(seat)));
        return fields;
    }

    private String name(Player player) {
        return Optional.ofNullable(bindings.get(player.getId()))
                .map(accounts::displayName)
                .orElseGet(() -> "Bot " + (player.getSeat() + 1));
    }

    private Map<String, String> latestActionsOnStreet(List<AcceptedAction> actions) {
        Map<String, String> latest = new LinkedHashMap<>();
        for (AcceptedAction action : actions) {
            if (action.getStreet().name().equals(view.getPhase().name())) {
                latest.put(action.getPlayerId(), action.getType().name());
            }
        }
        return latest;
    }

    private Object lastAction(List<AcceptedAction> actions) {
        if (actions.isEmpty()) {
            return "";
        }
        AcceptedAction action = actions.get(actions.size() - 1);
        Player actor = view.getPlayers().stream()
                .filter(player -> player.getId().equals(action.getPlayerId()))
                .findFirst().orElseThrow();
        return Map.of("playerId", actor.getId(), "name", name(actor), "action", action.getType(),
                "amount", Long.toString(action.getChipsAdded()), "allIn", action.becameAllIn(),
                "target", Long.toString(action.getRoundContributionAfter()));
    }

    private static String role(int seat, HandPositions positions) {
        List<String> roles = new ArrayList<>();
        if (seat == positions.getDealerSeat()) {
            roles.add("BTN");
        }
        if (seat == positions.getSmallBlindSeat()) {
            roles.add("SB");
        }
        if (seat == positions.getBigBlindSeat()) {
            roles.add("BB");
        }
        return String.join(",", roles);
    }

    private static List<Map<String, String>> cards(List<Card> cards) {
        return cards.stream().map(card -> Map.of("rank", card.getRank().name(),
                "suit", card.getSuit().name())).collect(Collectors.toList());
    }

    private static Map<String, Object> legal(LegalActions legal) {
        Optional<LegalActions.Wager> wager = legal.getWager();
        long min = wager.map(value -> value.getShortAllInTarget()
                .orElse(value.getMinimumFullTarget().orElse(0))).orElse(0L);
        long max = wager.map(LegalActions.Wager::getMaximumTarget).orElse(0L);
        return Map.of("actions", legal.getActions(), "call", Long.toString(legal.getCallCost()),
                "min", Long.toString(min), "max", Long.toString(max));
    }

    private Map<String, Object> result(ShowdownResult result, Map<String, String> before) {
        Optional<Settlement> paid = session.getSettlement();
        List<String> winners = paid.map(value -> value.getPayouts().entrySet().stream()
                .filter(entry -> entry.getValue() > 0).map(Map.Entry::getKey)
                .collect(Collectors.toList())).orElse(result.getWinnerIds());
        return Map.of("winners", winners, "uncontested", result.isUncontested(),
                "payouts", paid.map(value -> amounts(value.getPayouts())).orElse(Map.of()),
                "pots", paid.map(value -> value.getPots().stream().map(DesktopSnapshot::amounts)
                        .collect(Collectors.toList())).orElse(List.of()),
                "refunds", paid.map(value -> amounts(value.getRefunds())).orElse(Map.of()),
                "before", before, "after", paid.map(value -> finalStacks(value)).orElse(Map.of()));
    }

    private static Map<String, String> finalStacks(Settlement paid) {
        return paid.getState().getPlayers().stream().collect(Collectors.toMap(Player::getId,
                player -> Long.toString(player.getChipStack())));
    }

    private static Map<String, String> amounts(Map<String, Long> amounts) {
        return project(amounts, value -> Long.toString(value));
    }

    private static Map<String, Object> funds(TableFunds funds) {
        return Map.of("balance", Long.toString(funds.getBalance()),
                "tableChips", Long.toString(funds.getTableChips()),
                "available", Long.toString(funds.getAvailableChips()),
                "net", Long.toString(funds.getSessionNetChange()),
                "min", Long.toString(funds.getMinimumTopUp()),
                "max", Long.toString(funds.getMaximumTopUp()),
                "needsTopUp", funds.needsTopUp(), "needsRecovery", funds.needsRecovery());
    }

    private static <T, R> Map<String, R> project(Map<String, T> items, Function<T, R> convert) {
        Map<String, R> result = new LinkedHashMap<>();
        items.forEach((key, item) -> result.put(key, convert.apply(item)));
        return result;
    }
}
