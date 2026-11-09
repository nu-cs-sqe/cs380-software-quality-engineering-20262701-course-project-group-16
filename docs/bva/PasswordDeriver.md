# PasswordDeriver contract and buffer BVA

PasswordDeriver supplies derived bytes for a PBEKeySpec and can propagate
GeneralSecurityException. The production implementation delegates to JCA; an
injected implementation permits isolated security-lifecycle tests.

| ID | Method(s) | State | Expected result | Implemented? |
| --- | --- | --- | --- | --- |
| PB-C01 | hash; derive | Supplied specification and nonzero derived buffer | Encoded output retained; specification and temporary bytes cleared | Yes - locally passing |
| PB-C02 | matches; derive | Stored salt and matching derived bytes | Verification succeeds; stored salt used; sensitive buffers cleared | Yes - locally passing |
| PB-C03 | hash; derive | Provider failure | Failure propagates; specification still cleared | Yes - locally passing |
| PB-C04 | hash; derive | Incorrect output length | Credential rejected; returned bytes still cleared | Yes - locally passing |

PasswordBufferTest is isolated from the JCA provider. The corresponding real
algorithm checks remain integration tests.

| ID | Method(s) | State | Expected result | Implemented? |
| --- | --- | --- | --- | --- |
| PB-C05 | matches | Null, too-short/long or malformed Unicode password | Return false before invoking salt/provider sources | Yes - locally passing |
