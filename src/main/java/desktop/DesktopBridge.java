package desktop;

import application.AuthenticatedProfile;
import application.ProfileService;
import application.RecoveryOutcome;
import application.RecoveryService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import persistence.SqliteProfileRepository;

/** One process-local session, accessed serially through the desktop shell's private pipes. */
final class DesktopBridge {
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private final DesktopAccounts accounts;
    private final DesktopRecovery recovery;
    private final DesktopTable table;
    private final ProfileService profiles;

    DesktopBridge(Path directory) {
        this(directory, Instant::now);
    }

    DesktopBridge(Path directory, Supplier<Instant> now) {
        SqliteProfileRepository store = new SqliteProfileRepository(directory);
        profiles = new ProfileService(store);
        accounts = new DesktopAccounts(profiles, now);
        recovery = new DesktopRecovery(new RecoveryService(profiles, store,
                Clock.systemDefaultZone(), ThreadLocalRandom.current().nextLong()));
        table = new DesktopTable(profiles, store, accounts);
    }

    String respond(String line) {
        try {
            Object data = dispatch(parse(line));
            return JSON.toJson(Map.of("ok", true, "data", data));
        } catch (JsonParseException malformed) {
            return error("Enter a valid JSON request.");
        } catch (IOException | UncheckedIOException | GeneralSecurityException unavailable) {
            return error("Could not complete the local account operation.");
        } catch (ArithmeticException outsideRange) {
            return error("The amount is outside the supported range.");
        } catch (IllegalArgumentException | IllegalStateException invalid) {
            return error(Optional.ofNullable(invalid.getMessage()).orElse("Invalid request."));
        } catch (RuntimeException unexpected) {
            return error("Could not complete the desktop operation.");
        }
    }

    private static DesktopRequest parse(String line) {
        JsonElement parsed = Optional.ofNullable(JSON.fromJson(line, JsonElement.class))
                .orElseThrow(() -> new IllegalArgumentException("A request is required."));
        return new DesktopRequest(parsed);
    }

    private static String error(String message) {
        return JSON.toJson(Map.of("ok", false, "error", message));
    }

    private Object dispatch(DesktopRequest request) throws IOException, GeneralSecurityException {
        String op = request.text("op");
        switch (op) {
            case "profiles":
                return accounts.list();
            case "register":
                return accounts.authenticate(request, true);
            case "login":
                return accounts.authenticate(request, false);
            case "overview":
                return accounts.overview(accounts.require(request.text("id")));
            case "logout":
                return logout(request.text("id"));
            case "recovery":
                return recovery.begin(accounts.require(request.text("id")), request.text("locale"));
            case "claim":
                return claim(request);
            case "start":
                return table.start(request);
            case "state":
                return table.state();
            case "hide":
                return table.hide();
            default:
                return table.handle(op, request);
        }
    }

    private Map<String, Object> logout(String id) {
        if (table.binds(id)) {
            throw new IllegalStateException("End the table before signing out a seated account.");
        }
        accounts.logout(id);
        recovery.forget(id);
        return Map.of();
    }

    private Map<String, Object> claim(DesktopRequest request) throws IOException {
        String id = request.text("id");
        AuthenticatedProfile account = accounts.require(id);
        table.requireRecoverable(id);
        RecoveryOutcome outcome = recovery.claim(account, request);
        Map<String, Object> overview = accounts.overview(account);
        if (outcome == RecoveryOutcome.AWARDED || outcome == RecoveryOutcome.ALREADY_AWARDED) {
            table.credit(id, profiles.getChipBalance(account));
        }
        if (outcome != RecoveryOutcome.WRONG_ANSWERS) {
            recovery.forget(id);
        }
        return Map.of("outcome", outcome.name(), "account", overview);
    }
}
