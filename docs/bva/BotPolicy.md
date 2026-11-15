# BotPolicy BVA

E05 is a documented probability-based heuristic, not a GTO or positive-profit
guarantee. PolicyRequest supplies a paired filtered snapshot, legal options and
call scenario. The policy does not read GameState or produce accepted actions.

| ID | Methods | Input/state | Expected | Status |
| --- | --- | --- | --- | --- |
| BP01 | distribution/sample | Equity 0 or hopeless call; free check | Fold paid hopeless calls, never fold a free check | Implemented, GREEN |
| BP02 | distribution/sample | Equity 1, short all-in CALL | Never fold certain equity; capped call stays CALL | Implemented, GREEN |
| BP03 | distribution | Break-even call, closed raise rights | Nonzero fold/call probabilities; no RAISE | Implemented, GREEN |
| BP04 | distribution/sample | Three personalities, four streets, five equity values, 101 quantiles | Only legal actions; all targets accepted by Wager.allows | Implemented, GREEN |
| BP05 | distribution/sample/engine apply | Existing SB contribution; stack 25/35/40/50 around minimum 40 | Raise-to semantics, correct deductions and all-in classification | Implemented, GREEN |
| BP06 | distribution | Eligible pot reduced to 20; refundable new call | Minimum clamp; correct side-pot odds; no fold when no new call chips are at risk | Implemented, GREEN |
| BP07 | distribution | Three personalities with equity .9 | Modest ordered aggression; pot-fraction sizing 45/58/70 | Implemented, GREEN |
| BP08 | distribution | In/out of position; deep/short stack | Position and commitment pressure affect preferences | Implemented, GREEN |
| BP09 | distribution | Controlled passive/aggressive opponents; prior; unseen street | Confidence-weighted adaptation; prior and other streets do not fabricate evidence | Implemented, GREEN |
| BP10 | distribution/sample | 10,000 draws, fixed duplicated seed | Reproducible mixed actions; frequencies near published probabilities | Implemented, GREEN |

Tests map to BotPolicyTest. Real engine action acceptance in BP02/BP05 is integration
evidence; other cases test pure policy behavior on immutable values. No win-rate
or live-learning claim. BotPersonality is an enum and uses the rubric exemption.

## Later integration and quality regressions

| ID | Methods | Input/state | Expected | Status |
| --- | --- | --- | --- | --- |
| BP11 | distribution | Same paid decision, different public position | In-position call preference increases | Implemented |
| BP12 | distribution | Multiway versus heads-up fair shares | Avoid treating heads-up .4 equity as value | Implemented |
| BP13 | distribution | Full-stack versus deep-stack commitment, equity 1 | Lower aggressive weight for larger exposure | Implemented |
| BP14 | distribution/sample | Same whole-hand contributions and risk, round targets offset by 10 | Same probabilities; raise-to target offset only | Implemented |
| BP15 | distribution | Break-even paid decision, three personalities | Ordered call preferences | Implemented |
| BP16 | distribution | Certain equity at the accepted pot/cost boundary | No folds for every personality | Implemented |
| BP17 | distribution | Fair-share and thin multiway .25 equity | No value bet at fair share; cautious/loose behavior differs modestly | Implemented |
| BP18 | distribution | Learned flop aggression; balanced strong preflop hand | Learning reduces thin bets; balanced raises stay in calibrated .2–.5 range | Implemented |
| BI01 | estimate/distribution/engine apply | Four streets, real Monte Carlo, all personalities and quantiles | Engine accepts actions and conserves 6,000 chips | Implemented |
| BI02 | estimate/distribution/sample | Changed hidden opponent cards and reversed internal deck | Same seeded estimate, distribution and actions | Implemented |
| BI03 | distribution/engine/settlement | Short blind; 10 new call chips refunded, plus 5 older committed chips | CALL; settlement refund 15; new call cost 0; chips conserved | Implemented |

BP11–18 and BI01–03 were added after the first feature GREEN; BP15–18 strengthen
the first mutation findings. These are hardening/integration evidence, not extra
feature RED cycles. BI cases map to BotPolicyIntegrationTest.
