# PasswordHash BVA

Immutable encoded PBKDF2 credential data. The constructor requires valid Base64
encoding of exactly 16 salt bytes and 32 derived-hash bytes. No raw password is
stored. Both public getters retain the supplied valid strings.

| ID | Public method(s) | Input/state | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| PH-C01–05 | Constructor | Salt null, empty, invalid Base64, 15 bytes or 17 bytes | IllegalArgumentException | Yes - locally passing |
| PH-C06–10 | Constructor | Hash null, empty, invalid Base64, 31 bytes or 33 bytes | IllegalArgumentException | Yes - locally passing |
| PH-C11 | Constructor; both getters | Valid 16-byte salt/32-byte hash | Retain both encoded values | Yes - locally passing |

PasswordHashTest supplies each case ID. The catalog version fixes the algorithm
and work factor; no caller-controlled weak iteration count is accepted.
