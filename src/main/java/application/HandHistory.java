package application;

import domain.model.AcceptedAction;
import domain.model.Card;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.PlayerState;
import domain.model.Settlement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Append-only public facts. A reveal contains exactly the cards made public at that event. */
final class HandHistory {
    private static final int FLOP_SIZE = 3;
    private static final int TURN_SIZE = 4;
    private static final int RIVER_SIZE = 5;
    private final List<Map<String, Object>> events = new ArrayList<>();
    private GamePhase street = GamePhase.PREFLOP;
    private int boardSize;

    HandHistory(GameState opening) {
        recordOpening(opening);
        blind(opening.getPlayerStates().get(opening.getSmallBlindSeat()), "SMALL_BLIND");
        blind(opening.getPlayerStates().get(opening.getBigBlindSeat()), "BIG_BLIND");
    }

    private void recordOpening(GameState opening) {
        Map<String, String> stacks = new LinkedHashMap<>();
        List<String> dealt = new ArrayList<>();
        for (PlayerState seat : opening.getPlayerStates()) {
            long chips = Math.addExact(seat.getPlayer().getChipStack(), seat.getHandContribution());
            stacks.put(seat.getPlayer().getId(), Long.toString(chips));
            if (seat.isDealtSeat()) {
                dealt.add(seat.getPlayer().getId());
            }
        }
        append("OPEN", Map.of("stacks", Map.copyOf(stacks), "dealer", opening.getDealerSeat(),
                "dealt", List.copyOf(dealt)));
    }

    private void blind(PlayerState seat, String kind) {
        append(kind, Map.of("id", seat.getPlayer().getId(),
                "amount", Long.toString(seat.getHandContribution()),
                "allIn", seat.getPlayer().getChipStack() == 0));
    }

    void action(AcceptedAction action) {
        append(action.getType().name(), Map.of("id", action.getPlayerId(),
                "target", Long.toString(action.getRoundContributionAfter()),
                "amount", Long.toString(action.getChipsAdded()), "allIn", action.becameAllIn()));
    }

    void deal(List<Card> dealt) {
        if (boardSize < FLOP_SIZE && dealt.size() >= FLOP_SIZE) {
            boardStreet(GamePhase.FLOP, dealt, FLOP_SIZE);
        }
        if (boardSize < TURN_SIZE && dealt.size() >= TURN_SIZE) {
            boardStreet(GamePhase.TURN, dealt, TURN_SIZE);
        }
        if (boardSize < RIVER_SIZE && dealt.size() >= RIVER_SIZE) {
            boardStreet(GamePhase.RIVER, dealt, RIVER_SIZE);
        }
    }

    private void boardStreet(GamePhase next, List<Card> dealt, int size) {
        street = next;
        boardSize = size;
        append("DEAL", Map.of("board", List.copyOf(dealt.subList(0, size))));
    }

    void showdown() {
        street = GamePhase.SHOWDOWN;
        append("SHOWDOWN", Map.of());
    }

    void muck(String id) {
        append("MUCKED", Map.of("id", id));
    }

    void show(String id, String kind, List<Card> cards, Optional<String> category) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("id", id);
        fields.put("cards", cards);
        category.ifPresent(name -> fields.put("category", name));
        append(kind, fields);
    }

    void award(Settlement paid, Map<String, String> categories) {
        append("AWARD", Map.of("payouts", amounts(paid.getPayouts()),
                "refunds", amounts(paid.getRefunds()), "categories", categories,
                "pots", paid.getPots().stream().map(HandHistory::amounts)
                        .collect(Collectors.toUnmodifiableList())));
    }

    private static Map<String, String> amounts(Map<String, Long> values) {
        Map<String, String> chips = new LinkedHashMap<>();
        values.forEach((id, amount) -> chips.put(id, Long.toString(amount)));
        return Map.copyOf(chips);
    }

    private void append(String kind, Map<String, Object> fields) {
        Map<String, Object> event = new LinkedHashMap<>(fields);
        event.put("kind", kind);
        event.put("street", street);
        events.add(Map.copyOf(event));
    }

    List<Map<String, Object>> events() { return List.copyOf(events); }
}
