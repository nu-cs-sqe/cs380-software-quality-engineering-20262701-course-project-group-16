# PotLayout BVA

Shared public contribution-layer structure used by settlement and prospective
call previews. Hole cards and the deck are not retained by the layout.

| ID | Methods | Input/state | Expected | Status |
| --- | --- | --- | --- | --- |
| PL01 | from/getters | Contributions 10/20/30 | Pots 30/20; refund 10; immutable data; conserve total | Implemented, GREEN |
| PL02 | from | Folded contributor; folded unique top contributor | Dead money retained; no folded eligibility; correct refund owner | Implemented, GREEN |
| PL03 | from | All zero; all one; six distinct levels | Empty, one pot of 6, then five pots and one refund | Implemented, GREEN |
| PL04 | from | Contested layer without a live eligible player | Reject | Implemented, GREEN |
| PL05 | settlement integration | Tied 10/20/30 layers | Payouts 10/20/20, refund 10, 6,000 conserved | Implemented, GREEN |

Tests map to PotLayoutTest. The nested Layer is immutable public data.
