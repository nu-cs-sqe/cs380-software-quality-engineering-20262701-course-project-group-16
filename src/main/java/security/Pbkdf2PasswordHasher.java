package security;

import application.PasswordHasher;
import domain.model.PasswordHash;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Consumer;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2-HMAC-SHA256: 600,000 iterations, independent 16-byte salts and 256-bit output. */
public final class Pbkdf2PasswordHasher implements PasswordHasher {
    private final Consumer<byte[]> saltSource;
    private final PasswordDeriver deriver;

    public Pbkdf2PasswordHasher() {
        this(new SecureRandom()::nextBytes);
    }

    public Pbkdf2PasswordHasher(Consumer<byte[]> saltSource) {
        this(saltSource, Pbkdf2PasswordHasher::deriveWithJca);
    }

    /** Controls salt/provider behavior for isolated lifecycle tests. */
    public Pbkdf2PasswordHasher(Consumer<byte[]> saltSource, PasswordDeriver deriver) {
        requireSources(saltSource, deriver);
        this.saltSource = saltSource;
        this.deriver = deriver;
    }

    private static void requireSources(Consumer<byte[]> saltSource, PasswordDeriver deriver) {
        if (saltSource == null || deriver == null) {
            throw new IllegalArgumentException("Salt and derivation sources are required.");
        }
    }

    @Override
    public PasswordHash hash(char[] password) throws GeneralSecurityException {
        if (!validPassword(password, 15)) {
            throw new IllegalArgumentException("Use 6 to 15 characters for a new password.");
        }
        byte[] salt = new byte[16];
        saltSource.accept(salt);
        byte[] derived = derive(password, salt);
        try {
            return new PasswordHash(Base64.getEncoder().encodeToString(salt),
                    Base64.getEncoder().encodeToString(derived));
        } finally {
            Arrays.fill(derived, (byte) 0);
        }
    }

    @Override
    public boolean matches(char[] password, PasswordHash credential)
            throws GeneralSecurityException {
        if (credential == null) {
            throw new IllegalArgumentException("A password credential is required.");
        }
        // Continue accepting credentials registered under the former longer-password policy.
        if (!validPassword(password, 128)) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(credential.getSaltBase64());
        byte[] derived = derive(password, salt);
        try {
            return MessageDigest.isEqual(derived,
                    Base64.getDecoder().decode(credential.getHashBase64()));
        } finally {
            Arrays.fill(derived, (byte) 0);
        }
    }

    private static boolean validPassword(char[] password, int maximumLength) {
        if (password == null) {
            return false;
        }
        int length = Character.codePointCount(password, 0, password.length);
        return length >= 6 && length <= maximumLength
                && StandardCharsets.UTF_8.newEncoder().canEncode(CharBuffer.wrap(password));
    }

    private byte[] derive(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec specification = new PBEKeySpec(password, salt, 600000, 256);
        try {
            return deriver.derive(specification);
        } finally {
            specification.clearPassword();
        }
    }

    private static byte[] deriveWithJca(PBEKeySpec specification) throws GeneralSecurityException {
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(specification).getEncoded();
    }
}
