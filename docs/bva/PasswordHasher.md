# PasswordHasher contract and PBKDF2 BVA

PasswordHasher is the application hashing port. Pbkdf2PasswordHasher implements
PBKDF2WithHmacSHA256 with 600,000 iterations, a 16-byte SecureRandom salt and
32 derived bytes. New hashing requires 6–15 valid characters, without
trimming or normalization. Verification returns false for invalid/wrong password
input; a null credential is an invalid caller dependency. JCA failures propagate
as GeneralSecurityException, not as successful authentication.

Caller char arrays remain caller-owned. PBEKeySpec password copies are cleared;
temporary derived bytes are cleared after encoding/comparison. Verification uses
MessageDigest.isEqual. The injected salt source is for controlled tests; the
default constructor uses SecureRandom.

| ID | Public method(s) | Input/state | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| PW-C01 | injected constructor; hash; matches | Fixed salt, independent Python hashlib reference vector | Matching encoded hash; correct succeeds/wrong fails; caller input unchanged | Yes - locally passing |
| PW-C02-0/5/16/129 | hash; matches | Registration length below 6 or above 15 | Hash rejects; comparison with the fixture credential is false | Yes - locally passing |
| PW-C03-6/7/14/15 | hash; matches | Boundary/adjacent valid lengths | Successful hash and verification | Yes - locally passing |
| PW-C04 | hash; matches | Supplementary Unicode, added spaces, malformed surrogate | Valid Unicode succeeds unchanged; different password fails; malformed rejected | Yes - locally passing |
| PW-C05 | default constructor; hash; matches | Same password hashed twice | Independent salts/hashes; both verify | Yes - locally passing |
| PW-C06 | both constructors; hash; matches | Null salt source, password or credential | IllegalArgumentException as contracted | Yes - locally passing |

Pbkdf2PasswordHasherIntegrationTest runs the real JCA provider; ProfileServiceTest
uses an injected fake hasher to isolate orchestration and failure behavior.

References: [OWASP password storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html),
[OWASP authentication](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html).

Verification retains 6–128 characters for existing account compatibility; new
registrations enforce 6–15. PW-C07 verifies an existing 28-character password and
rejects it for new registration; PW-C08 verifies independent 127/128-character
reference credentials. No password/catalog migration or algorithm change occurs.
