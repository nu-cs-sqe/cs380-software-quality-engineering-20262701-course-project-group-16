# Pbkdf2PasswordHasher BVA

PasswordHasher is the application hashing port. Pbkdf2PasswordHasher implements
PBKDF2WithHmacSHA256 with 600,000 iterations, a 16-byte SecureRandom salt and
32 derived bytes. Hashing requires 15–128 valid Unicode code points, without
trimming or normalization. Verification returns false for invalid/wrong password
input; a null credential is an invalid caller dependency. JCA failures propagate
as GeneralSecurityException, not as successful authentication.

Caller char arrays remain caller-owned. PBEKeySpec password copies are cleared;
temporary derived bytes are cleared after encoding/comparison. Verification uses
MessageDigest.isEqual. The default constructor uses SecureRandom and JCA. A salt-source constructor
controls deterministic algorithm tests; the salt-source/PasswordDeriver
constructor isolates provider failure and cleanup. Null dependencies are rejected.

| ID | Public method(s) | Input/state | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| PW-C01 | injected constructor; hash; matches | Fixed salt, independent Python hashlib reference vector | Matching encoded hash; correct succeeds/wrong fails; caller input unchanged | Yes - locally passing |
| PW-C02-0/14/129 | hash; matches | Length below minimum or above maximum | Hash rejects; verification false | Yes - locally passing |
| PW-C03-15/16/127/128 | hash; matches | Boundary/adjacent valid lengths | Successful hash and verification | Yes - locally passing |
| PW-C04 | hash; matches | Supplementary Unicode, added spaces, malformed surrogate | Valid Unicode succeeds unchanged; different password fails; malformed rejected | Yes - locally passing |
| PW-C05 | default constructor; hash; matches | Same password hashed twice | Independent salts/hashes; both verify | Yes - locally passing |
| PW-C06 | both constructors; hash; matches | Null salt source, password or credential | IllegalArgumentException as contracted | Yes - locally passing |

Pbkdf2PasswordHasherIntegrationTest runs the real JCA provider; ProfileServiceTest
uses an injected fake hasher to isolate orchestration and failure behavior.

References: [OWASP password storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html),
[OWASP authentication](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html).

| ID | Method(s) | State | Expected result | Implemented? |
| --- | --- | --- | --- | --- |
| PB-C05 | matches | Null, too-short/long or malformed Unicode password | Return false before invoking salt/provider sources | Yes - locally passing |

PasswordBufferTest additionally maps PB-C01–04 from
[the derivation contract](PasswordDeriver.md), covering specification and derived-
buffer cleanup on successful hashing/verification and provider failure. PB-C05
was added after mutation testing exposed the missing provider-rejection assertion.
