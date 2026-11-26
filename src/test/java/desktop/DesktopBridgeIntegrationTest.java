package desktop;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopBridgeIntegrationTest {
    private static final Gson JSON = new Gson();
    @TempDir Path directory;
    private DesktopBridge bridge;

    @BeforeEach
    void connect() {
        bridge = new DesktopBridge(directory);
    }

    @Test
    void identityIsPublicButAccountDataRequiresPasswordAfterLogoutOrRestart() {
        String name = "庄家 \"North\"";
        JsonObject account = register(name);
        String id = account.get("id").getAsString();
        assertEquals("2000", account.get("balance").getAsString());
        assertTrue(account.getAsJsonArray("history").isEmpty());
        assertEquals(0, account.getAsJsonObject("metrics").get("handsPlayed").getAsLong());
        JsonObject identity = call("profiles").getAsJsonArray("data").get(0).getAsJsonObject();
        assertEquals(2, identity.size());
        assertEquals(name, identity.get("name").getAsString());
        ok("logout", "id", id);
        reject("overview", "id", id);
        reject("login", "id", id, "password", "wrong-password");
        ok("login", "id", id, "password", "poker123");
        connect();
        reject("overview", "id", id);
        assertEquals(name, ok("login", "id", id, "password", "poker123").get("name").getAsString());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void soloOwnerCanInspectOnlyTheirSelectedSeatWithoutAnotherPassword(int seat) {
        String owner = register("Solo").get("id").getAsString();
        String outsider = register("Visitor").get("id").getAsString();
        var selection = Map.of("id", owner, "buyIn", "500", "seat", seat);
        reject("start", "seats", List.of(selection, selection));
        JsonObject game = ok("start", "seats", List.of(selection));
        var seats = game.getAsJsonArray("players");
        assertEquals(owner, seats.get(seat).getAsJsonObject().get("accountId").getAsString());
        assertEquals(5, seats.asList().stream().map(value -> value.getAsJsonObject())
                .filter(player -> "BOT".equals(player.get("type").getAsString())).count());
        assertEquals(2, turn(game, "inspect", "id", owner).getAsJsonArray("cards").size());
        rejectTurn(game, "inspect", "id", outsider);
        reject("start", "seats", List.of(selection));
        assertTrue(game.getAsJsonObject("shown").isEmpty());
        assertEquals(3, game.getAsJsonArray("events").size());
    }

    @Test
    void selectedSeatsBindAccountsIrrespectiveOfJoinOrderAndRejectConflicts() {
        String first = register("First").get("id").getAsString();
        String second = register("Second").get("id").getAsString();
        var seatFive = Map.of("id", first, "buyIn", "400", "seat", 5);
        for (int invalid : List.of(-1, 5, 6)) {
            reject("start", "seats", List.of(seatFive,
                    Map.of("id", second, "buyIn", "600", "seat", invalid)));
        }
        JsonObject game = ok("start", "seats", List.of(seatFive,
                Map.of("id", second, "buyIn", "600", "seat", 2)));
        var seats = game.getAsJsonArray("players");
        assertEquals(first, seats.get(5).getAsJsonObject().get("accountId").getAsString());
        assertEquals(second, seats.get(2).getAsJsonObject().get("accountId").getAsString());
        rejectTurn(game, "inspect", "id", first);
        rejectTurn(game, "inspect", "id", second);
    }

    @Test
    void idleSeatExpiresAtThirtyMinutesAndOnlyItsOwnerCanResumeIt() {
        var time = new java.util.concurrent.atomic.AtomicReference<>(java.time.Instant.EPOCH);
        bridge = new DesktopBridge(directory, time::get);
        String owner = register("Sage").get("id").getAsString();
        JsonObject game = ok("start", "seats", List.of(Map.of("id", owner, "buyIn", "400")));
        time.set(time.get().plusSeconds(1799));
        JsonObject privateRead = turn(game, "inspect", "id", owner);
        time.set(time.get().plusSeconds(1800));
        rejectTurn(game, "inspect", "id", owner);
        rejectTurn(game, "switchOwner", "id", owner, "password", "incorrect");
        assertFalse(ok("state").has("privateOwner"));
        assertTrue(ok("state").getAsJsonObject("shown").isEmpty());
        turn(game, "switchOwner", "id", owner, "password", "poker123");
        assertEquals(privateRead, turn(game, "inspect", "id", owner));
    }

    @Test
    void defaultRiverTablesLiveHandsAndPaysOnlyOnce() {
        List<Map<String, String>> seats = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            seats.add(Map.of("id", register("River " + seat).get("id").getAsString(),
                    "buyIn", "400"));
        }
        JsonObject game = ok("start", "seats", seats);
        String folder = actor(game).get("id").getAsString();
        game = humanAction(game, "FOLD");
        for (int action = 0; action < 24 && !game.get("result").isJsonObject(); action++) {
            game = passive(game);
        }
        assertFalse(game.has("showActor"));
        assertEquals(5, game.getAsJsonObject("shown").size());
        assertFalse(game.getAsJsonObject("shown").has(folder));
        JsonObject paid = turn(game, "settle");
        rejectTurn(paid, "settle");
        assertEquals(paid, ok("state"));
        for (var winner : paid.getAsJsonObject("result").getAsJsonArray("winners")) {
            assertEquals(5, paid.getAsJsonObject("winningCards")
                    .getAsJsonArray(winner.getAsString()).size());
        }
    }

    @Test
    void malformedInputReturnsOneSanitizedResponseAndKeepsProcessing() {
        for (String invalid : List.of("[]", "{", "{} garbage", "{\"op\":null}")) {
            JsonObject reply = parse(bridge.respond(invalid));
            assertFalse(reply.get("ok").getAsBoolean());
            assertTrue(reply.get("error").isJsonPrimitive());
        }
        JsonObject failed = call("register", "name", "A", "password", "secret-too-long-xxx");
        assertFalse(failed.get("ok").getAsBoolean());
        assertFalse(failed.toString().contains("secret-too-long-xxx"));
        assertTrue(call("profiles").getAsJsonArray("data").isEmpty());
    }

    @Test
    void onlyCurrentHumanMaySeeCardsOrActAndOldSnapshotsCannotReplay() {
        JsonObject game = fullTable();
        JsonObject actor = actor(game);
        JsonArray players = game.getAsJsonArray("players");
        String id = actor.get("accountId").getAsString();
        int otherSeat = (game.get("actor").getAsInt() + 1) % players.size();
        String other = players.get(otherSeat).getAsJsonObject().get("accountId").getAsString();
        reject("logout", "id", id);
        reject("start", "seats", List.of(Map.of("id", id, "buyIn", "400")));
        rejectTurn(game, "reveal", "id", other);
        JsonObject shown = turn(game, "reveal", "id", id);
        assertEquals(2, shown.getAsJsonArray("hole").size());
        assertEquals(2, shown.getAsJsonArray("hole").get(0).getAsJsonObject().size());
        assertTrue(ok("state").getAsJsonArray("hole").isEmpty());
        assertTrue(ok("hide").getAsJsonArray("hole").isEmpty());
        rejectTurn(game, "act", "id", other, "action", "FOLD");
        assertEquals("", game.get("lastAction").getAsString());
        JsonObject raised = turn(game, "act", "id", id, "action", "RAISE", "target", "300");
        JsonObject last = raised.getAsJsonObject("lastAction");
        assertEquals(actor.get("name"), last.get("name"));
        assertEquals("RAISE", last.get("action").getAsString());
        assertEquals("300", last.get("amount").getAsString());
        assertEquals("300", last.get("target").getAsString());
        JsonObject shortRaise = raised.getAsJsonObject("legal");
        assertEquals("400", shortRaise.get("min").getAsString());
        assertEquals("400", shortRaise.get("max").getAsString());
        String nextId = actor(raised).get("accountId").getAsString();
        JsonObject allIn = turn(raised, "act", "id", nextId, "action", "RAISE", "target", "400");
        assertTrue(allIn.getAsJsonObject("lastAction").get("allIn").getAsBoolean());
        rejectTurn(game, "act", "id", id, "action", "FOLD");
        rejectTurn(game, "reveal", "id", id);
        assertTrue(ok("state").getAsJsonArray("hole").isEmpty());
    }

    @Test
    void foldedOwnerCanInspectPrivatelyAndDeferShowingUntilTheHandEnds() {
        JsonObject game = fullTable();
        JsonObject folder = actor(game);
        String id = folder.get("accountId").getAsString();
        String player = folder.get("id").getAsString();
        game = humanAction(game, "FOLD");
        String next = actor(game).get("accountId").getAsString();
        rejectTurn(game, "inspect", "id", id);
        rejectTurn(game, "inspect", "id", next);
        rejectTurn(game, "switchOwner", "id", next, "password", "incorrect");
        turn(game, "switchOwner", "id", id, "password", "poker123");
        JsonArray own = turn(game, "inspect", "id", id)
                .getAsJsonArray("cards");
        assertEquals(2, own.size());
        game = turn(game, "disclose", "id", id, "choice", "SHOW");
        assertFalse(game.getAsJsonObject("shown").has(player));
        for (int i = 0; i < 4; i++) {
            game = humanAction(game, "FOLD");
        }
        assertEquals(own, game.getAsJsonObject("shown").get(player));
        game = turn(game, "settle");
        assertEquals(0, game.getAsJsonObject("result").getAsJsonObject("payouts")
                .get(player).getAsInt());
        JsonArray events = turn(game, "history").getAsJsonArray("hands").get(0)
                .getAsJsonObject().getAsJsonArray("events");
        assertEquals("OPEN", events.get(0).getAsJsonObject().get("kind").getAsString());
        assertEquals("SMALL_BLIND", events.get(1).getAsJsonObject().get("kind").getAsString());
        assertEquals("BIG_BLIND", events.get(2).getAsJsonObject().get("kind").getAsString());
        int folds = 0;
        for (var item : events) {
            JsonObject event = item.getAsJsonObject();
            String kind = event.get("kind").getAsString();
            if (kind.equals("FOLD")) {
                folds++;
            }
            if (event.has("cards") && kind.equals("VOLUNTARILY_SHOWN")) {
                assertEquals(5, folds);
                assertEquals(own, event.get("cards"));
            }
        }
    }

    @Test
    void seatBadgesRetainEachPlayersLatestActionOnlyOnTheCurrentStreet() {
        JsonObject game = fullTable();
        assertTrue(game.getAsJsonObject("streetActions").isEmpty());
        String first = actor(game).get("id").getAsString();
        game = humanAction(game, "CALL");
        String second = actor(game).get("id").getAsString();
        game = humanAction(game, "RAISE", "target", "40");
        assertEquals("CALL", game.getAsJsonObject("streetActions").get(first).getAsString());
        assertEquals("RAISE", game.getAsJsonObject("streetActions").get(second).getAsString());
        while (!actor(game).get("id").getAsString().equals(first)) {
            game = humanAction(game, "CALL");
        }
        game = humanAction(game, "CALL");
        assertEquals("FLOP", game.get("phase").getAsString());
        assertTrue(game.getAsJsonObject("streetActions").isEmpty());
        String bettor = actor(game).get("id").getAsString();
        game = humanAction(game, "BET", "target", "20");
        assertEquals("BET", ok("state").getAsJsonObject("streetActions").get(bettor).getAsString());
        game = humanAction(game, "CALL");
        assertEquals("BET", game.getAsJsonObject("streetActions").get(bettor).getAsString());
        game = humanAction(game, "RAISE", "target", "60");
        while (!actor(game).get("id").getAsString().equals(bettor)) {
            game = humanAction(game, "CALL");
        }
        game = humanAction(game, "CALL");
        assertEquals("CALL", game.getAsJsonObject("streetActions").get(bettor).getAsString());
    }

    private JsonObject humanAction(JsonObject game, String action, Object... fields) {
        String id = actor(game).get("accountId").getAsString();
        List<Object> args = new ArrayList<>(List.of("id", id, "action", action));
        args.addAll(List.of(fields));
        return turn(game, "act", args.toArray());
    }

    @Test
    void settledLedgerPersistsTopUpsDoNotChangeHandAndNextHandGetsANewId() {
        JsonObject game = fullTable();
        String original = game.get("handId").getAsString();
        rejectTurn(game, "end");
        rejectTurn(game, "next");
        for (int fold = 0; fold < 5; fold++) {
            String id = actor(game).get("accountId").getAsString();
            game = turn(game, "act", "id", id, "action", "FOLD");
        }
        assertEquals(-1, game.get("actor").getAsInt());
        assertTrue(game.getAsJsonObject("result").get("uncontested").getAsBoolean());
        JsonObject paid = turn(game, "settle");
        rejectTurn(game, "settle");
        JsonObject winner = paid.getAsJsonObject("result");
        String winnerId = winner.getAsJsonArray("winners").get(0).getAsString();
        assertEquals("20", winner.getAsJsonObject("payouts").get(winnerId).getAsString());
        assertEquals("10", winner.getAsJsonObject("refunds").get(winnerId).getAsString());
        assertEquals("400", winner.getAsJsonObject("before").get(winnerId).getAsString());
        long total = 0;
        for (var item : paid.getAsJsonArray("players")) {
            JsonObject seat = item.getAsJsonObject();
            String id = seat.get("accountId").getAsString();
            JsonObject details = ok("overview", "id", id);
            total += details.get("balance").getAsLong();
            assertEquals(1, details.getAsJsonArray("history").size());
            assertEquals(1, details.getAsJsonObject("metrics").get("handsPlayed").getAsInt());
        }
        assertEquals(12000, total);
        String id = paid.getAsJsonArray("players").get(0).getAsJsonObject()
                .get("accountId").getAsString();
        JsonObject funded = turn(paid, "topup", "id", id, "amount", "100");
        assertEquals(winner, funded.getAsJsonObject("result"));
        String departing = paid.getAsJsonArray("players").get(1).getAsJsonObject()
                .get("accountId").getAsString();
        JsonObject next = turn(turn(funded, "quit", "id", departing), "next");
        assertNotEquals(original, next.get("handId").getAsString());
        assertEquals("BOT", next.getAsJsonArray("players").get(1).getAsJsonObject()
                .get("type").getAsString());
        assertTrue(next.getAsJsonArray("hole").isEmpty());
    }

    @Test
    void botTakesOneDecisionAndOnlyPaidTablesCanClose() {
        String id = register("Human").get("id").getAsString();
        reject("start", "seats", List.of(Map.of("id", id, "buyIn", "399")));
        reject("start", "seats", List.of(Map.of("id", id, "buyIn", "400.5")));
        JsonObject game = ok("start", "seats", List.of(Map.of("id", id, "buyIn", "400")));
        int bots = 0;
        while (!game.get("result").isJsonObject()) {
            JsonObject seat = actor(game);
            if (seat.get("type").getAsString().equals("HUMAN")) {
                rejectTurn(game, "bot");
                game = turn(game, "act", "id", id, "action", "FOLD");
            } else {
                rejectTurn(game, "reveal", "id", id);
                rejectTurn(game, "act", "id", id, "action", "CALL");
                JsonObject next = turn(game, "bot");
                assertTrue(next.getAsJsonArray("hole").isEmpty());
                assertEquals(seat.get("id"), next.getAsJsonObject("lastAction").get("playerId"));
                if (next.get("phase").equals(game.get("phase"))) {
                    String botId = seat.get("id").getAsString();
                    assertEquals(next.getAsJsonObject("lastAction").get("action"),
                            next.getAsJsonObject("streetActions").get(botId));
                }
                assertEquals(game.get("revision").getAsInt() + 1, next.get("revision").getAsInt());
                game = next;
                bots++;
            }
        }
        assertTrue(bots > 0);
        JsonObject paid = turn(game, "settle");
        JsonObject departed = turn(paid, "quit", "id", id);
        assertFalse(departed.get("canNext").getAsBoolean());
        assertEquals("0", departed.getAsJsonObject("funds").getAsJsonObject(id)
                .get("tableChips").getAsString());
        rejectTurn(departed, "next");
        JsonObject beforeRestart = ok("overview", "id", id);
        turn(departed, "end");
        reject("state");
        ok("logout", "id", id);
        connect();
        JsonObject reopened = ok("login", "id", id, "password", "poker123");
        assertEquals(beforeRestart.get("balance"), reopened.get("balance"));
        assertEquals(beforeRestart.get("history"), reopened.get("history"));
    }

    @Test
    void translatedRecoveryPersistsSingleReward() throws SQLException {
        String id = register("Short Stack").get("id").getAsString();
        setBalance(id, 0);
        JsonObject quiz = ok("recovery", "id", id, "locale", "en");
        JsonObject chinese = ok("recovery", "id", id, "locale", "zh");
        String challenge = quiz.get("challengeId").getAsString();
        assertEquals(challenge, chinese.get("challengeId").getAsString());
        assertNotEquals(quiz.getAsJsonArray("questions").get(0).getAsJsonObject().get("prompt"),
                chinese.getAsJsonArray("questions").get(0).getAsJsonObject().get("prompt"));
        Map<String, Integer> correct = Map.of("blinds", 1, "fold", 2, "private", 0, "board", 1,
                "allIn", 2, "refund", 0, "sidePot", 1, "tie", 2, "quit", 0, "topUp", 1);
        List<Integer> answers = new ArrayList<>();
        quiz.getAsJsonArray("questions").forEach(item -> {
            JsonObject question = item.getAsJsonObject();
            assertEquals(3, question.getAsJsonArray("choices").size());
            answers.add(correct.get(question.get("id").getAsString()));
        });
        reject("claim", "id", id, "challengeId", "wrong", "answers", answers);
        reject("claim", "id", id, "challengeId", challenge, "answers", List.of(0));
        JsonObject claim = ok("claim", "id", id, "challengeId", challenge, "answers", answers);
        assertEquals("AWARDED", claim.get("outcome").getAsString());
        JsonObject account = claim.getAsJsonObject("account");
        assertEquals("200", account.get("balance").getAsString());
        assertEquals("RECOVERY_REWARD", account.getAsJsonArray("history").get(0)
                .getAsJsonObject().get("type").getAsString());
        reject("claim", "id", id, "challengeId", challenge, "answers", answers);
        verifyClaimAfterReadFailure(id, correct);
    }

    private void verifyClaimAfterReadFailure(String id, Map<String, Integer> correct)
            throws SQLException {
        JsonObject quiz = ok("recovery", "id", id, "locale", "en");
        String challenge = quiz.get("challengeId").getAsString();
        List<Integer> answers = new ArrayList<>();
        quiz.getAsJsonArray("questions").forEach(item -> answers.add(
                correct.get(item.getAsJsonObject().get("id").getAsString())));
        String url = "jdbc:sqlite:" + directory.resolve("profiles.db");
        try (var db = DriverManager.getConnection(url); var statement = db.createStatement()) {
            statement.executeUpdate("UPDATE recovery_claims SET claimed_at='unreadable'");
            reject("claim", "id", id, "challengeId", challenge, "answers", answers);
            statement.executeUpdate("UPDATE recovery_claims SET claimed_at='2026-01-01T00:00:00Z'"
                    + " WHERE claimed_at='unreadable'");
        }
        JsonObject retry = ok("claim", "id", id, "challengeId", challenge, "answers", answers);
        assertEquals("ALREADY_AWARDED", retry.get("outcome").getAsString());
        JsonObject account = retry.getAsJsonObject("account");
        assertEquals("400", account.get("balance").getAsString());
        assertEquals(2, account.getAsJsonArray("history").size());
    }

    @Test
    void chipStringsPreserveIntegersAboveJavascriptSafeRange() throws SQLException {
        String id = register("Deep Stack").get("id").getAsString();
        long chips = 9_007_199_254_740_993L;
        setBalance(id, chips);
        reject("start", "seats", List.of(Map.of("id", id, "buyIn", chips)));
        Map<String, String> seat = Map.of("id", id, "buyIn", "9007199254740993");
        JsonObject game = ok("start", "seats", List.of(seat));
        assertEquals("9007199254740993", game.getAsJsonObject("funds").getAsJsonObject(id)
                .get("tableChips").getAsString());
        assertEquals("10", game.get("smallBlind").getAsString());
        assertEquals("20", game.get("bigBlind").getAsString());
    }

    @Test
    void stdioContinuesThroughMalformedLineAndExitsAtEof() throws IOException {
        String input = "{\n{\"op\":\"profiles\"}\n";
        StringWriter sink = new StringWriter();
        try (var reader = new BufferedReader(new StringReader(input));
                var writer = new BufferedWriter(sink)) {
            DesktopMain.serve(bridge, reader, writer);
        }
        String[] replies = sink.toString().split("\\R");
        assertEquals(2, replies.length);
        assertFalse(parse(replies[0]).get("ok").getAsBoolean());
        assertTrue(parse(replies[1]).getAsJsonArray("data").isEmpty());
    }

    private void setBalance(String id, long chips) throws SQLException {
        String url = "jdbc:sqlite:" + directory.resolve("profiles.db");
        try (var connection = DriverManager.getConnection(url);
                var update = connection.prepareStatement(
                        "UPDATE profiles SET chip_balance=? WHERE id=?")) {
            update.setLong(1, chips);
            update.setString(2, id);
            assertEquals(1, update.executeUpdate());
        }
    }

    @Test
    void immediateShowingNeverRestoresAFoldedClaimAndAllInsExposeBeforeTheFlop() {
        JsonObject game = fullTable(true);
        String folded = actor(game).get("id").getAsString();
        String account = actor(game).get("accountId").getAsString();
        game = humanAction(game, "FOLD");
        turn(game, "switchOwner", "id", account, "password", "poker123");
        game = turn(game, "disclose", "id", account, "choice", "SHOW");
        assertEquals(2, game.getAsJsonObject("shown").getAsJsonArray(folded).size());
        game = humanAction(game, "RAISE", "target", "400");
        assertEquals(1, game.getAsJsonObject("shown").size());
        game = humanAction(game, "CALL");
        assertEquals(1, game.getAsJsonObject("shown").size());
        game = humanAction(game, "FOLD");
        game = humanAction(game, "FOLD");
        String closingSeat = actor(game).get("id").getAsString();
        game = humanAction(game, "FOLD", "showFold", true);
        assertTrue(game.getAsJsonObject("runout").getAsJsonObject("shown").has(closingSeat));
        assertEquals(4, game.getAsJsonObject("shown").size());
        assertFalse(game.has("showActor"));
        game = turn(game, "settle");
        assertEquals(0, game.getAsJsonObject("result").getAsJsonObject("payouts")
                .get(folded).getAsInt());
        JsonArray events = turn(game, "history").getAsJsonArray("hands").get(0)
                .getAsJsonObject().getAsJsonArray("events");
        int tabled = 0;
        int deals = 0;
        for (var item : events) {
            String kind = item.getAsJsonObject().get("kind").getAsString();
            if (kind.equals("SHOWDOWN_REVEALED")) {
                tabled++;
            }
            if (kind.equals("DEAL")) {
                assertEquals(2, tabled);
                deals++;
            }
        }
        assertEquals(3, deals);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void riverOrderAndLastLiveUnshownClaim(boolean aggressive) {
        JsonObject game = fullTable();
        while (!game.get("phase").getAsString().equals("RIVER")) {
            game = passive(game);
        }
        String first = actor(game).get("id").getAsString();
        game = humanAction(game, "CHECK");
        if (aggressive) {
            game = humanAction(game, "BET", "target", "20");
            first = actor(game).get("id").getAsString();
            game = humanAction(game, "RAISE", "target", "40");
        }
        while (!game.has("showActor")) {
            game = passive(game);
        }
        assertEquals(first, game.get("showActor").getAsString());
        assertFalse(game.get("result").isJsonObject());
        rejectTurn(game, "settle");
        int mucked = 0;
        while (game.has("showActor")) {
            String player = game.get("showActor").getAsString();
            var seats = game.getAsJsonArray("players");
            String account = seats.asList().stream().map(value -> value.getAsJsonObject())
                    .filter(seat -> seat.get("id").getAsString().equals(player)).findFirst()
                    .orElseThrow().get("accountId").getAsString();
            turn(game, "handoff", "id", account);
            game = turn(game, "disclose", "id", account, "choice", "MUCK");
            mucked++;
        }
        assertEquals(5, mucked);
        assertTrue(game.getAsJsonObject("shown").isEmpty());
        assertTrue(game.getAsJsonObject("result").get("uncontested").getAsBoolean());
        game = turn(game, "settle");
        assertTrue(game.getAsJsonObject("shown").isEmpty());
        JsonObject next = turn(game, "next");
        assertEquals(1, turn(next, "history").getAsJsonArray("hands").size());
    }

    private JsonObject passive(JsonObject game) {
        String action = game.getAsJsonObject("legal").get("call").getAsLong() == 0
                ? "CHECK" : "CALL";
        return humanAction(game, action);
    }

    private JsonObject fullTable() {
        return fullTable(false);
    }

    private JsonObject fullTable(boolean immediate) {
        List<Map<String, String>> seats = new ArrayList<>();
        for (int seat = 0; seat < 6; seat++) {
            String id = register("Seat " + seat).get("id").getAsString();
            seats.add(Map.of("id", id, "buyIn", "400"));
        }
        return ok("start", "seats", seats, "immediateFold", immediate, "manualShowdown", true);
    }

    private JsonObject register(String name) {
        return ok("register", "name", name, "password", "poker123");
    }

    private static JsonObject actor(JsonObject game) {
        return game.getAsJsonArray("players").get(game.get("actor").getAsInt()).getAsJsonObject();
    }

    private JsonObject turn(JsonObject game, String op, Object... args) {
        if (op.equals("act")) {
            ok("reveal", turnArgs(game, new Object[]{"id", actor(game).get("accountId")}));
        }
        return ok(op, turnArgs(game, args));
    }

    private void rejectTurn(JsonObject game, String op, Object... args) {
        reject(op, turnArgs(game, args));
    }

    private static Object[] turnArgs(JsonObject game, Object[] args) {
        List<Object> values = new ArrayList<>(List.of(args));
        values.addAll(List.of("handId", game.get("handId"), "revision", game.get("revision")));
        return values.toArray();
    }

    private void reject(String op, Object... args) {
        assertFalse(call(op, args).get("ok").getAsBoolean(), op);
    }

    private JsonObject ok(String op, Object... args) {
        JsonObject response = call(op, args);
        assertTrue(response.get("ok").getAsBoolean(), response::toString);
        return response.getAsJsonObject("data");
    }

    private JsonObject call(String op, Object... args) {
        JsonObject request = new JsonObject();
        request.addProperty("op", op);
        for (int i = 0; i < args.length; i += 2) {
            request.add((String) args[i], JSON.toJsonTree(args[i + 1]));
        }
        return parse(bridge.respond(request.toString()));
    }

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }
}
