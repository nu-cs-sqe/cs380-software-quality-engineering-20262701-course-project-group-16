package desktop;

import application.AuthenticatedProfile;
import application.BotComputer;
import application.BotTurn;
import application.ProfileService;
import application.ReviewedHand;
import application.SetupSession;
import application.SetupView;
import application.TableAccountStore;
import com.google.gson.JsonElement;
import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.Player;
import domain.model.GameConfig;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Single-owner table. Every state-changing request identifies its observed hand and revision. */
final class DesktopTable {
    private static final int BOT_SIMULATIONS = 512;
    private final SetupSession session = new SetupSession();
    private final BotComputer bots = new BotComputer();
    private final ProfileService profiles;
    private final TableAccountStore store;
    private final DesktopAccounts accounts;
    private boolean open;
    private boolean immediate;
    private boolean tableLiveHands;
    private boolean shared;
    private Optional<String> owner = Optional.empty();
    private boolean showBots;
    private Optional<ReviewedHand> review = Optional.empty();
    private final List<Map<String, Object>> pastHands = new ArrayList<>();
    private long revision;
    private long handNumber;
    private Map<String, String> before = Map.of();

    DesktopTable(ProfileService profiles, TableAccountStore store, DesktopAccounts accounts) {
        this.profiles = profiles;
        this.store = store;
        this.accounts = accounts;
    }

    Map<String, Object> start(DesktopRequest request) {
        if (open) {
            throw new IllegalStateException("End the current table first.");
        }
        immediate = request.flag("immediateFold");
        showBots = request.flag("showBots");
        tableLiveHands = !request.flag("manualShowdown");
        seatAccounts(request);
        pastHands.clear();
        open = true;
        handNumber = 0;
        beginHand();
        return changed();
    }

    private void seatAccounts(DesktopRequest request) {
        Map<Integer, AuthenticatedProfile> humans = new LinkedHashMap<>();
        Map<String, Long> buyIns = new LinkedHashMap<>();
        for (JsonElement value : request.array("seats")) {
            DesktopRequest seat = new DesktopRequest(value);
            String id = seat.text("id");
            int index = availableSeat(seat, humans);
            humans.put(index, accounts.require(id));
            buyIns.put(id, seat.chips("buyIn"));
        }
        session.startSeatedTable(profiles, humans, store, buyIns);
        shared = humans.size() > 1;
        owner = shared ? Optional.empty() : buyIns.keySet().stream().findFirst();
    }

    private static int availableSeat(DesktopRequest seat,
            Map<Integer, AuthenticatedProfile> seats) {
        long index = seat.has("seat") ? seat.number("seat") : seats.size();
        if (index < 0 || index >= GameConfig.SEAT_COUNT || seats.containsKey((int) index)) {
            throw new IllegalArgumentException("Choose a different available seat.");
        }
        return (int) index;
    }

    private void beginHand() {
        if (shared) {
            owner = Optional.empty();
        }
        review = Optional.of(session.beginReview(immediate, showBots, tableLiveHands));
        handNumber++;
        SetupView view = view();
        Map<String, String> stacks = new LinkedHashMap<>();
        for (Player player : view.getPlayers()) {
            long invested = view.getHandContribution(player.getSeat());
            long opening = Math.addExact(player.getChipStack(), invested);
            stacks.put(player.getId(), Long.toString(opening));
        }
        before = Map.copyOf(stacks);
    }

    Map<String, Object> state() {
        requireOpen();
        Map<String, Object> data = new DesktopSnapshot(session, accounts)
                .render(revision, handNumber, before);
        addReviewFields(data, review.orElseThrow());
        return data;
    }

    Map<String, Object> hide() {
        if (shared) {
            owner = Optional.empty();
        }
        return state();
    }

    private void addReviewFields(Map<String, Object> data, ReviewedHand hand) {
        data.put("visibility", hand.visibility());
        data.put("shown", hand.shown());
        data.put("events", hand.events());
        data.put("winningCards", hand.winningCards());
        owner.filter(accounts::active).ifPresent(id -> data.put("privateOwner", id));
        data.put("categories", hand.categories());
        data.put("folded", hand.folded());
        hand.nextToShow().ifPresent(id -> data.put("showActor", id));
        data.put("immediateFold", immediate);
        if (!hand.runout().isEmpty()) {
            data.put("runout", hand.runout());
        }
        data.put("pastHands", pastHands.size());
    }

    Map<String, Object> handle(String operation, DesktopRequest request)
            throws IOException, GeneralSecurityException {
        requireCurrent(request);
        Optional<String> previous = expectedHuman();
        switch (operation) {
            case "history":
                return histories();
            case "switchOwner":
                switchOwner(request);
                return state();
            case "handoff":
                showdownHandoff(request);
                return state();
            case "inspect":
                return inspect(request);
            case "disclose":
                disclose(request);
                break;
            case "reveal":
                return reveal(request);
            case "act":
                act(request);
                break;
            case "bot":
                playBot();
                break;
            case "quit":
                session.quit(playerFor(request.text("id")));
                break;
            case "settle":
                session.settleHand();
                break;
            case "next":
                startNextHand();
                break;
            case "topup":
                String id = request.text("id");
                playerFor(id);
                session.addTableChips(id, request.chips("amount"));
                break;
            case "end":
                closeTable();
                return Map.of();
            default:
                throw new IllegalArgumentException("Unknown desktop operation.");
        }
        lockPreviousOwnerOnHandoff(previous);
        return changed();
    }

    private Optional<String> expectedHuman() {
        if (!shared) {
            return Optional.empty();
        }
        Map<String, String> bindings = session.getProfileIds();
        Optional<String> showing = review.flatMap(ReviewedHand::nextToShow);
        if (showing.isPresent()) {
            return showing.map(bindings::get);
        }
        SetupView current = view();
        int seat = current.getCurrentActorSeat();
        return seat < 0 ? Optional.empty()
                : Optional.of(current.getPlayers().get(seat).getId())
                        .filter(bindings::containsKey).map(bindings::get);
    }

    private void lockPreviousOwnerOnHandoff(Optional<String> before) {
        Optional<String> next = expectedHuman();
        if (!before.equals(next) && next.isPresent()) {
            owner = owner.filter(next.orElseThrow()::equals);
        }
    }

    private void startNextHand() {
        if (session.getProfileIds().values().stream().noneMatch(accounts::active)) {
            throw new IllegalStateException("Sign in to deal the next hand.");
        }
        Map<String, Object> previous = publicHistory();
        session.nextHand();
        pastHands.add(previous);
        beginHand();
    }

    private void closeTable() {
        session.endTable();
        pastHands.clear();
        review = Optional.empty();
        owner = Optional.empty();
        open = false;
    }

    private static boolean showChoice(DesktopRequest request) {
        String choice = request.text("choice");
        if (!choice.equals("SHOW") && !choice.equals("MUCK")) {
            throw new IllegalArgumentException("Choose show or muck.");
        }
        return choice.equals("SHOW");
    }

    private void switchOwner(DesktopRequest request) throws IOException, GeneralSecurityException {
        String account = request.text("id");
        if (!binds(account)) {
            throw new IllegalArgumentException("Choose a seated human account.");
        }
        accounts.authenticate(request, false);
        owner = Optional.of(account);
    }

    private void showdownHandoff(DesktopRequest request) {
        String account = request.text("id");
        String player = playerFor(account);
        if (!review.orElseThrow().nextToShow().filter(player::equals).isPresent()) {
            throw new IllegalStateException("Wait for this seat's showdown turn.");
        }
        owner = Optional.of(account);
    }

    private String authorizedPlayer(DesktopRequest request) {
        String account = request.text("id");
        requirePrivateOwner(account);
        return playerFor(account);
    }

    private void requirePrivateOwner(String account) {
        if (owner.filter(account::equals).isEmpty()) {
            throw new IllegalArgumentException("Switch to this seat before viewing its hand.");
        }
    }

    private Map<String, Object> inspect(DesktopRequest request) {
        String player = authorizedPlayer(request);
        ReviewedHand hand = review.orElseThrow();
        return Map.of("cards", hand.ownerCards(player), "deferred", hand.showAfterHand(player),
                "validForMillis", accounts.idleLimitMillis());
    }

    private void disclose(DesktopRequest request) {
        session.chooseDisclosure(authorizedPlayer(request), showChoice(request));
    }

    private Map<String, Object> histories() {
        List<Map<String, Object>> history = new ArrayList<>(pastHands);
        if (session.getSettlement().isPresent()) {
            history.add(publicHistory());
        }
        return Map.of("hands", history);
    }

    private Map<String, Object> publicHistory() {
        List<Map<String, Object>> seats = new ArrayList<>();
        for (Player player : view().getPlayers()) {
            String id = player.getId();
            String name = Optional.ofNullable(session.getProfileIds().get(id))
                    .map(accounts::displayName)
                    .orElseGet(() -> "Bot " + (player.getSeat() + 1));
            seats.add(Map.of("id", id, "name", name, "seat", player.getSeat(),
                    "type", player.getType()));
        }
        return Map.of("number", handNumber, "handId", view().getHandId(),
                "players", seats, "events", review.orElseThrow().events());
    }

    private Map<String, Object> changed() {
        revision++;
        return state();
    }

    private void requireCurrent(DesktopRequest request) {
        requireOpen();
        if (!view().getHandId().equals(request.text("handId"))
                || revision != request.number("revision")) {
            throw new IllegalStateException("This table view is out of date.");
        }
    }

    private void requireOpen() {
        if (!open) {
            throw new IllegalStateException("Start a table first.");
        }
    }

    private SetupView view() {
        return session.getView().orElseThrow();
    }

    private String playerFor(String accountId) {
        accounts.require(accountId);
        return session.getProfileIds().entrySet().stream()
                .filter(binding -> binding.getValue().equals(accountId)).map(Map.Entry::getKey)
                .findFirst().orElseThrow(() ->
                        new IllegalArgumentException("Choose a human at this table."));
    }

    private String humanActor(DesktopRequest request) {
        String playerId = playerFor(request.text("id"));
        SetupView view = view();
        int actor = view.getCurrentActorSeat();
        if (session.getResult().isPresent() || actor < 0
                || !view.getPlayers().get(actor).getId().equals(playerId)) {
            throw new IllegalArgumentException("Only the current human may take this turn.");
        }
        return playerId;
    }

    private Map<String, Object> reveal(DesktopRequest request) {
        session.selectViewer(humanActor(request));
        session.revealCards();
        owner = Optional.of(request.text("id"));
        try {
            return state();
        } finally {
            session.hideCards();
        }
    }

    private void act(DesktopRequest request) {
        String id = humanActor(request);
        requirePrivateOwner(request.text("id"));
        ActionRequest action = bettingIntent(request, id);
        if (request.flag("showFold")) {
            if (action.getType() != ActionType.FOLD) {
                throw new IllegalArgumentException("Only a fold can request optional disclosure.");
            }
            session.foldAndShow(id);
        } else {
            session.submitAction(action);
        }
    }

    private ActionRequest bettingIntent(DesktopRequest request, String id) {
        String action = request.text("action");
        ActionType type = session.getAvailableActions().stream()
                .filter(legal -> legal.name().equals(action)).findFirst().orElseThrow(() ->
                        new IllegalArgumentException("Choose a legal action."));
        return request.has("target")
                ? new ActionRequest(id, type, request.chips("target"))
                : new ActionRequest(id, type);
    }

    private void playBot() {
        BotTurn turn = session.getBotTurn(BOT_SIMULATIONS).orElseThrow(() ->
                new IllegalStateException("Wait for a bot turn."));
        ActionRequest decision = bots.decide(turn, Thread.currentThread()::isInterrupted);
        if (!session.applyBotDecision(turn, decision)) {
            throw new IllegalStateException("The bot turn is out of date.");
        }
    }

    boolean binds(String accountId) {
        return open && session.getProfileIds().containsValue(accountId);
    }

    void requireRecoverable(String accountId) {
        if (open && !session.canRecoverAccount(accountId)) {
            throw new IllegalStateException("Settle this hand before claiming recovery chips.");
        }
    }

    void credit(String id, long balance) {
        if (binds(id)) {
            session.applyRecoveredBalance(id, balance);
            revision++;
        }
    }
}
