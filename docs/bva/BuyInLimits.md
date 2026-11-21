# BuyInLimits BVA

| ID | Methods | Input/state | Expected | Status / evidence |
| --- | --- | --- | --- | --- |
| BI01 | defaultAmount | 399 / 400 / 401 / 999 / 1,000 / 1,001 | Reject 399; use full available amount through 999; otherwise default 1,000 | GREEN, BuyInLimitsTest |
| BI02 | requireAmount | -1 / 0 / 399; amount above balance | Reject without funding or saving | GREEN, BuyInLimitsTest / invalidSelectedAmountsPreserveTheCurrentHand |
| BI03 | requireAmount | 400 / 401 / 2,001 / large representable amount | No fixed cap; amount must fit balance | GREEN, BuyInLimitsTest / selectedBuyInsCanExceedTwoThousandWithoutSpendingTheOutsideBalance |

Public constants expose the accepted 400 minimum and 1,000 default; actual game
funding also passes the shared [numeric capacity](FundingCapacity.md) checks.
