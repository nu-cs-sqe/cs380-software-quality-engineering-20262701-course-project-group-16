# EquityPot BVA

Public scenario value; eligibility comes from the engine/application. Refunds are excluded.

| ID | Methods | State/input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| EP-C01 | constructor | ID null, empty, spaces or tab | IllegalArgumentException | Yes |
| EP-C02 | constructor | Amount −1 or 0 | IllegalArgumentException | Yes |
| EP-C03 | constructor | Missing/empty eligibility, null/blank member | IllegalArgumentException | Yes |
| EP-C04 | constructor, getId/getAmount/getEligiblePlayers | Exact opaque ID, Long.MAX_VALUE, mutable source set | Preserved values and detached immutable set | Yes |



Local TDD observed eight expected failures against the compiling constructor stub,
then eight passes after implementation.
