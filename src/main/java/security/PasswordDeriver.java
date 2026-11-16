package security;

import java.security.GeneralSecurityException;
import javax.crypto.spec.PBEKeySpec;

/** Derivation seam for provider-failure and sensitive-buffer lifecycle tests. */
@FunctionalInterface
public interface PasswordDeriver {
    byte[] derive(PBEKeySpec specification) throws GeneralSecurityException;
}
