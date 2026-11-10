# EquityRequest BVA

Only own cards and public scenario information are accepted. IDs remain opaque and exact.

| ID | Methods | State/input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| ER-C01 | constructor; all getters | Board sizes 0/3/4/5; two own cards; one opponent/pot/trial | Exact immutable values, eligible and total amount 100 | Yes |
| ER-C02 | constructor | Board sizes 1/2/6 | IllegalArgumentException | Yes |
| ER-C03 | constructor | Own-card sizes 0/1/3; duplicates within/across known cards | IllegalArgumentException | Yes |
| ER-C04 | constructor | Missing collections; null card/pot members | IllegalArgumentException | Yes |
| ER-C05 | constructor, getOpponents | Blank/null hero; repeated/self/blank/null opponent; 0/5/6 opponents | Accept 0–5 distinct opponents, reject invalid identities and sixth opponent | Yes |
| ER-C06 | constructor | Trials −1/0/10001 | IllegalArgumentException | Yes |
| ER-C07 | constructor; collection/count getters | 10000 trials; mutate input lists and attempt output mutation | Upper bound accepted; copied immutable lists | Yes |
| ER-C08 | constructor, getEligiblePotAmount/getTotalPotAmount | Empty/duplicate/unknown-eligibility pots; hero ineligible; mixed pots; eligible sum overflow | Reject invalid scenarios; count eligible amount separately; checked overflow | Yes |
| ER-C09 | constructor, getTotalPotAmount | 6/7 unique pots; total overflow from hero-ineligible side pot | Accept six, reject seven; ArithmeticException for overflow | Yes |
| ER-C10 | constructor | Blank/null opponent with an otherwise valid hero-only pot | IllegalArgumentException independently of pot-membership checks | Yes |
| ER-C11 | constructor | Unknown eligible player in a second pot while hero has a valid first pot | IllegalArgumentException independently of hero eligibility | Yes |
| EV-C01 | fromView | Real six-seat initialization and filtered viewer | Own cards plus ordered five public opponent IDs; no hidden holdings | Yes |
| EV-C02 | fromView | ACTIVE/ALL_IN/FOLDED/ELIMINATED viewers/opponents; null view | Retain active/all-in participants, reject inactive viewer/missing view | Yes |
| EV-C03 | fromView | Same public state with different hidden holes and reversed physical deck | Identical request data and seeded real-evaluator results | Yes |

ER cases map to EquityRequestTest; EV cases map to EquityViewIntegrationTest.
The 1–10000 simulation cap is an initial resource policy, not a course requirement.
A supplied pot scenario may represent a prospective legal call; it need not equal
the currently displayed pot. Constructing authoritative scenarios belongs to application integration.

Local TDD observed 15 expected constructor failures after EquityPot was implemented,
then GREEN. EV-C01–03 produced three expected failures before fromView.
ER-C09 is boundary hardening; ER-C10–11 are mutation-driven regressions, not
additional claimed feature RED cycles.
