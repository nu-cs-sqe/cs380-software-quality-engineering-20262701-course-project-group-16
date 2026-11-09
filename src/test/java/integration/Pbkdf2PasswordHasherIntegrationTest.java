package integration;

import domain.model.PasswordHash;
import java.security.GeneralSecurityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import security.Pbkdf2PasswordHasher;

import static org.junit.jupiter.api.Assertions.*;

class Pbkdf2PasswordHasherIntegrationTest {
    private static final String SALT = "AAECAwQFBgcICQoLDA0ODw==";
    private static final String HASH = "7xdxRO7JQgy8EJPSqLNEqSvFBtDU7JwCjdGfgyTYweY=";

    @Test
    @DisplayName("PW-C01: matches an independently computed PBKDF2 reference vector")
    void matchesIndependentReferenceVectorAndPreservesCallerPassword() throws GeneralSecurityException {
        Pbkdf2PasswordHasher hasher = fixedSalt();
        char[] password = "correct horse battery staple".toCharArray();
        char[] original = password.clone();
        PasswordHash hash = hasher.hash(password);

        assertEquals(SALT, hash.getSaltBase64());
        assertEquals(HASH, hash.getHashBase64());
        assertTrue(hasher.matches(password, hash));
        assertFalse(hasher.matches("an incorrect long password".toCharArray(), hash));
        assertFalse(hasher.matches(null, hash));
        assertFalse(hasher.matches(new char[0], hash));
        assertArrayEquals(original, password);
    }

    @ParameterizedTest(name = "PW-C02-{0}: enforces password length")
    @ValueSource(ints = {0, 14, 129})
    void rejectsLengthsOutsideThePolicy(int length) throws GeneralSecurityException {
        Pbkdf2PasswordHasher hasher = fixedSalt();
        char[] password = "a".repeat(length).toCharArray();
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(password));
        assertFalse(hasher.matches(password, new PasswordHash(SALT, HASH)));
    }

    @ParameterizedTest(name = "PW-C03-{0}: accepts policy boundary lengths")
    @ValueSource(ints = {15, 16, 127, 128})
    void acceptsBoundaryLengths(int length) throws GeneralSecurityException {
        Pbkdf2PasswordHasher hasher = fixedSalt();
        char[] password = "a".repeat(length).toCharArray();
        PasswordHash hash = hasher.hash(password);
        assertTrue(hasher.matches(password, hash));
    }

    @Test
    @DisplayName("PW-C04: supports valid Unicode and rejects malformed surrogate input")
    void supportsUnicodeWithoutNormalization() throws GeneralSecurityException {
        Pbkdf2PasswordHasher hasher = fixedSalt();
        char[] password = "😀".repeat(15).toCharArray();
        PasswordHash hash = hasher.hash(password);
        assertEquals("tsG5qDH+l3gq6Br0xaE5js45ai3r6vMqXt4T9svqKUs=", hash.getHashBase64());
        assertTrue(hasher.matches(password, hash));
        assertFalse(hasher.matches((" " + "😀".repeat(15)).toCharArray(), hash));
        char[] malformed = "a".repeat(15).toCharArray();
        malformed[0] = '\uD800';
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(malformed));
        assertFalse(hasher.matches(malformed, hash));
    }

    @Test
    @DisplayName("PW-C05: identical passwords receive independent random salts")
    void usesIndependentSaltsWithTheProductionSource() throws GeneralSecurityException {
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        char[] password = "correct horse battery staple".toCharArray();
        PasswordHash first = hasher.hash(password);
        PasswordHash second = hasher.hash(password);
        assertNotEquals(first.getSaltBase64(), second.getSaltBase64());
        assertNotEquals(first.getHashBase64(), second.getHashBase64());
        assertTrue(hasher.matches(password, first));
        assertTrue(hasher.matches(password, second));
    }

    @Test
    @DisplayName("PW-C06: validates null input and credential dependencies")
    void validatesNullInputs() {
        Pbkdf2PasswordHasher hasher = fixedSalt();
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(null));
        assertThrows(IllegalArgumentException.class,
                () -> hasher.matches("a".repeat(15).toCharArray(), null));
        assertThrows(IllegalArgumentException.class, () -> new Pbkdf2PasswordHasher(null));
    }

    private static Pbkdf2PasswordHasher fixedSalt() {
        return new Pbkdf2PasswordHasher(salt -> {
            for (int index = 0; index < salt.length; index++) {
                salt[index] = (byte) index;
            }
        });
    }
}
