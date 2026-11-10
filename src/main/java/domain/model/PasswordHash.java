package domain.model;

import java.util.Base64;

/** Immutable encoded salt and PBKDF2 output; contains no raw password. */
public final class PasswordHash {
    private final String saltBase64;
    private final String hashBase64;

    /** Requires a 16-byte salt and 32-byte output encoded as Base64. */
    public PasswordHash(String saltBase64, String hashBase64) {
        this.saltBase64 = requireEncodedBytes(saltBase64, 16);
        this.hashBase64 = requireEncodedBytes(hashBase64, 32);
    }

    private static String requireEncodedBytes(String encoded, int expectedBytes) {
        if (encoded == null || Base64.getDecoder().decode(encoded).length != expectedBytes) {
            throw new IllegalArgumentException("Credential encoding or length is invalid.");
        }
        return encoded;
    }

    public String getSaltBase64() { return saltBase64; }
    public String getHashBase64() { return hashBase64; }
}
