# EquityEstimate BVA

Constructed only within the bot package after aggregation.

| ID | Methods | State/input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| MC-C01–05 | getSimulations, getPotShares, getEquity, getExpectedPayout | Loss, win, fractional ties, mixed pots and repeated trials | Unrounded numeric estimates; correct trial count; immutable per-pot fractions | Yes |

Equity is expected payout divided by hero-eligible awardable chips. Expected
payout excludes uncalled refunds and is neither net profit nor complete action EV.
No display rounding or integer odd-chip allocation is performed here.
