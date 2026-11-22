package desktop;

import application.AuthenticatedProfile;
import application.BotComputer;
import application.BotTurn;
import application.ProfileService;
import application.SetupSession;
import application.SetupView;
import application.TableAccountStore;
import com.google.gson.JsonElement;
import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.Player;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Single-owner table. Every state-changing request identifies its observed hand and revision. */
final class DesktopTable {
    private static final int BOT_SIMULATIONS = 512;
    private final SetupSession session = new SetupSession();
    private final BotComputer bots = new BotComputer();
    private final ProfileService profiles;
    private final TableAccountStore store;
    private final DesktopAccounts accounts;
    private boolean open;
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
        List<AuthenticatedProfile> humans = new ArrayList<>();
        Map<String, Long> buyIns = new LinkedHashMap<>();
        for (JsonElement value : request.array("seats")) {
            DesktopRequest seat = new DesktopRequest(value);
            String id = seat.text("id");
            humans.add(accounts.require(id));
            buyIns.put(id, seat.chips("buyIn"));
        }
        session.startTable(profiles, humans, store, buyIns);
        open = true;
        handNumber = 0;
        beginHand();
        return changed();
    }

    private void beginHand() {
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
        return new DesktopSnapshot(session, accounts).render(revision, handNumber, before);
    }

    Map<String, Object> handle(String operation, DesktopRequest request) {
        requireCurrent(request);
        switch (operation) {
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
                session.nextHand();
                beginHand();
                break;
            case "topup":
                String id = request.text("id");
                playerFor(id);
                session.addTableChips(id, request.chips("amount"));
                break;
            case "end":
                session.endTable();
                open = false;
                return Map.of();
            default:
                throw new IllegalArgumentException("Unknown desktop operation.");
        }
        return changed();
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
        try {
            return state();
        } finally {
            session.hideCards();
        }
    }

    private void act(DesktopRequest request) {
        String id = humanActor(request);
        String action = request.text("action");
        ActionType type = session.getAvailableActions().stream()
                .filter(legal -> legal.name().equals(action)).findFirst().orElseThrow(() ->
                        new IllegalArgumentException("Choose a legal action."));
        ActionRequest actionRequest = request.has("target")
                ? new ActionRequest(id, type, request.chips("target"))
                : new ActionRequest(id, type);
        session.submitAction(actionRequest);
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
