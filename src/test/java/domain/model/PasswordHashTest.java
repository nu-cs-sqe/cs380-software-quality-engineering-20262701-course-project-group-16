package domain.model;

import java.util.Base64;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHashTest {
    private static final String SALT = encoded(16);
    private static final String HASH = encoded(32);

    @ParameterizedTest(name = "{0}: rejects malformed credential data")
    @MethodSource("invalidCredentials")
    void rejectsMalformedCredentials(String caseId, String salt, String hash) {
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash(salt, hash), caseId);
    }

    static Stream<Arguments> invalidCredentials() {
        return Stream.of(
                Arguments.of("PH-C01", null, HASH),
                Arguments.of("PH-C02", "", HASH),
                Arguments.of("PH-C03", "not base64!", HASH),
                Arguments.of("PH-C04", encoded(15), HASH),
                Arguments.of("PH-C05", encoded(17), HASH),
                Arguments.of("PH-C06", SALT, null),
                Arguments.of("PH-C07", SALT, ""),
                Arguments.of("PH-C08", SALT, "not base64!"),
                Arguments.of("PH-C09", SALT, encoded(31)),
                Arguments.of("PH-C10", SALT, encoded(33)));
    }

    @Test
    @DisplayName("PH-C11: retains a valid encoded salt and derived hash")
    void retainsValidEncodedValues() {
        PasswordHash hash = new PasswordHash(SALT, HASH);
        assertEquals(SALT, hash.getSaltBase64());
        assertEquals(HASH, hash.getHashBase64());
    }

    private static String encoded(int bytes) {
        return Base64.getEncoder().encodeToString(new byte[bytes]);
    }
}
