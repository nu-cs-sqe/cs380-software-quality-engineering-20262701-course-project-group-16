package application;

import domain.model.PasswordHash;
import java.security.GeneralSecurityException;

/** Credential hashing port; raw passwords are caller-owned and are never persisted. */
public interface PasswordHasher {
    PasswordHash hash(char[] password) throws GeneralSecurityException;

    boolean matches(char[] password, PasswordHash credential) throws GeneralSecurityException;
}
