package security;

import domain.model.PasswordHash;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.spec.PBEKeySpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordBufferTest {
    @Test
    void pbC01HashClearsItsSpecificationAndDerivedBuffer() throws GeneralSecurityException {
        byte[] derived = filled(32);
        String expected = Base64.getEncoder().encodeToString(derived);
        AtomicReference<PBEKeySpec> captured = new AtomicReference<>();
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(salt -> Arrays.fill(salt, (byte) 0),
                specification -> { captured.set(specification); return derived; });

        PasswordHash hash = hasher.hash("PokerPlayer123".toCharArray());

        assertEquals(expected, hash.getHashBase64());
        assertArrayEquals(new byte[32], derived);
        PBEKeySpec specification = Objects.requireNonNull(captured.get());
        assertThrows(IllegalStateException.class, specification::getPassword);
        assertEquals(600000, specification.getIterationCount());
        assertEquals(256, specification.getKeyLength());
    }

    @Test
    void pbC02VerificationUsesStoredSaltAndClearsSensitiveBuffers() throws GeneralSecurityException {
        byte[] derived = filled(32);
        AtomicReference<PBEKeySpec> captured = new AtomicReference<>();
        PasswordHash hash = new PasswordHash(Base64.getEncoder().encodeToString(new byte[16]),
                Base64.getEncoder().encodeToString(derived));
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(
                salt -> { throw new AssertionError("Verification must use the stored salt"); },
                specification -> { captured.set(specification); return derived; });

        assertTrue(hasher.matches("PokerPlayer123".toCharArray(), hash));
        assertArrayEquals(new byte[32], derived);
        PBEKeySpec specification = Objects.requireNonNull(captured.get());
        assertArrayEquals(new byte[16], specification.getSalt());
        assertThrows(IllegalStateException.class, specification::getPassword);
    }

    @Test
    void pbC03ProviderFailureStillClearsThePasswordSpecification() {
        AtomicReference<PBEKeySpec> captured = new AtomicReference<>();
        GeneralSecurityException failure = new GeneralSecurityException("Provider unavailable");
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(
                salt -> Arrays.fill(salt, (byte) 0), specification -> {
                    captured.set(specification);
                    throw failure;
                });

        assertSame(failure, assertThrows(GeneralSecurityException.class,
                () -> hasher.hash("PokerPlayer123".toCharArray())));
        PBEKeySpec specification = Objects.requireNonNull(captured.get());
        assertThrows(IllegalStateException.class, specification::getPassword);
        assertThrows(IllegalArgumentException.class,
                () -> new Pbkdf2PasswordHasher(salt -> { }, null));
    }

    @Test
    void pbC04MalformedProviderOutputIsRejectedAndCleared() {
        byte[] derived = filled(31);
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(
                salt -> Arrays.fill(salt, (byte) 0), specification -> derived);
        assertThrows(IllegalArgumentException.class,
                () -> hasher.hash("PokerPlayer123".toCharArray()));
        assertArrayEquals(new byte[31], derived);
    }

    @Test
    void pbC05InvalidVerificationInputNeverInvokesTheProvider()
            throws GeneralSecurityException {
        PasswordHash hash = new PasswordHash(Base64.getEncoder().encodeToString(new byte[16]),
                Base64.getEncoder().encodeToString(new byte[32]));
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(
                salt -> { throw new AssertionError("Verification must not generate a salt"); },
                specification -> { throw new AssertionError("Invalid input must not be derived"); });

        assertFalse(hasher.matches(null, hash));
        assertFalse(hasher.matches("short".toCharArray(), hash));
        assertFalse(hasher.matches("a".repeat(129).toCharArray(), hash));
        char[] malformed = "a".repeat(15).toCharArray();
        malformed[0] = '\uD800';
        assertFalse(hasher.matches(malformed, hash));
    }

    private static byte[] filled(int count) {
        byte[] bytes = new byte[count];
        Arrays.fill(bytes, (byte) 7);
        return bytes;
    }
}
