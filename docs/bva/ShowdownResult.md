# ShowdownResult contract cases

This immutable result is created by ShowdownResolver, not by external callers.
Its package-private constructor copies the winner IDs. The nested Status enum
is exempt from separate BVA. These public-method checks reuse meaningful
resolver tests instead of adding duplicate trivial getter tests.

| ID | Method | System state | Expected output | Implemented / mapping |
| --- | --- | --- | --- | --- |
| RESULT-01 | getHandId | Result for `result-hand` | Same source hand ID | Yes, GREEN: ShowdownResolverTest.SR_V01 |
| RESULT-02 | getWinnerIds | Single strongest eligible player | One ID; modification throws UnsupportedOperationException | Yes, GREEN: SR_V01 |
| RESULT-03 | getWinnerIds | Two tied players / all six tied | Only tied contenders, in seat order | Yes, GREEN: SR_V04 / SR_V03 |
| RESULT-04 | isUncontested | Multiple eligible players compared | false | Yes, GREEN: SR_V01 / SR_V03 |
| RESULT-05 | isUncontested | One eligible player at any dealt phase | true; evaluator not invoked | Yes, GREEN: SR_U01 phase cases |
| RESULT-06 | getStatus | Contested or uncontested result | Always AWAITING_SETTLEMENT; pot and stacks unchanged | Yes, GREEN: SR_V01 / SR_U01 and integration fixtures |

Created in the resolver RED checkpoint `29442e4`; behavior passes in GREEN
`0d5172e`. Winner IDs describe comparison strength, not side-pot shares or
payout amounts. See [the result contract](../design/showdown.md).
