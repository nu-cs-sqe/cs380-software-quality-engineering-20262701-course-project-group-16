package desktop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Optional;

/** Validates untrusted JSON types before passing values to the application. */
final class DesktopRequest {
    private final JsonObject fields;

    DesktopRequest(JsonElement value) {
        if (!value.isJsonObject()) {
            throw new IllegalArgumentException("A JSON request object is required.");
        }
        fields = value.getAsJsonObject();
    }

    String text(String name) {
        JsonElement field = field(name);
        if (!field.isJsonPrimitive() || !field.getAsJsonPrimitive().isString()) {
            throw invalid(name);
        }
        return field.getAsString();
    }

    long chips(String name) {
        String value = text(name);
        if (!value.matches("[0-9]+")) {
            throw invalid(name);
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException outsideRange) {
            throw invalid(name);
        }
    }

    long number(String name) {
        return integer(field(name), name);
    }

    static long integer(JsonElement value, String name) {
        if (!value.isJsonPrimitive()) {
            throw invalid(name);
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (!primitive.isNumber()) {
            throw invalid(name);
        }
        try {
            return Long.parseLong(primitive.getAsString());
        } catch (NumberFormatException outsideRange) {
            throw invalid(name);
        }
    }

    JsonArray array(String name) {
        JsonElement field = field(name);
        if (!field.isJsonArray()) {
            throw invalid(name);
        }
        return field.getAsJsonArray();
    }

    boolean has(String name) {
        return fields.has(name);
    }

    private JsonElement field(String name) {
        return Optional.ofNullable(fields.get(name)).orElseThrow(() -> invalid(name));
    }

    private static IllegalArgumentException invalid(String name) {
        return new IllegalArgumentException("Invalid or missing field: " + name + ".");
    }
}
