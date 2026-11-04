# PokerHandEvaluator BVA

## Current checkpoint

Week 6 starts with input validation for the existing HandEvaluator API. The
concrete class is currently a compile-only scaffold: compare returns zero and
is not connected to the application. Validation and ranking are not implemented.

## Method: compare(List<Card> firstCards, List<Card> secondCards)

Each valid holding must contain exactly seven non-null, distinct card values.
Overlap between the two holdings is allowed for shared community cards. Every
rejection case also checks that both lists retain their original contents/order;
that preservation assertion will run after exception validation is implemented.
No-argument construction has no input boundary and is exercised by every case.

| ID | Method | System state / input | Expected output | Test implemented / status |
| --- | --- | --- | --- | --- |
| PHE-N01 | compare | Null first list; other list has seven valid cards | NullPointerException; both lists unchanged | Yes, RED: rejectsNullList |
| PHE-N02 | compare | Null second list; other list has seven valid cards | NullPointerException; both lists unchanged | Yes, RED: rejectsNullList |
| PHE-N03 | compare | Seven entries per list; first contains null at index 0 | NullPointerException; both lists unchanged | Yes, RED: rejectsNullCard |
| PHE-N04 | compare | Seven entries per list; first contains null at index 3 | NullPointerException; both lists unchanged | Yes, RED: rejectsNullCard |
| PHE-N05 | compare | Seven entries per list; first contains null at index 6 | NullPointerException; both lists unchanged | Yes, RED: rejectsNullCard |
| PHE-N06 | compare | Seven entries per list; second contains null at index 0 | NullPointerException; both lists unchanged | Yes, RED: rejectsNullCard |
| PHE-N07 | compare | Seven entries per list; second contains null at index 3 | NullPointerException; both lists unchanged | Yes, RED: rejectsNullCard |
| PHE-N08 | compare | Seven entries per list; second contains null at index 6 | NullPointerException; both lists unchanged | Yes, RED: rejectsNullCard |
| PHE-S01 | compare | First list has 0 cards; other list has seven valid cards | IllegalArgumentException; both lists unchanged | Yes, RED: rejectsCardCountOutsideSeven |
| PHE-S02 | compare | First list has 6 cards; other list has seven valid cards | IllegalArgumentException; both lists unchanged | Yes, RED: rejectsCardCountOutsideSeven |
| PHE-S03 | compare | First list has 8 cards; other list has seven valid cards | IllegalArgumentException; both lists unchanged | Yes, RED: rejectsCardCountOutsideSeven |
| PHE-S04 | compare | Second list has 0 cards; other list has seven valid cards | IllegalArgumentException; both lists unchanged | Yes, RED: rejectsCardCountOutsideSeven |
| PHE-S05 | compare | Second list has 6 cards; other list has seven valid cards | IllegalArgumentException; both lists unchanged | Yes, RED: rejectsCardCountOutsideSeven |
| PHE-S06 | compare | Second list has 8 cards; other list has seven valid cards | IllegalArgumentException; both lists unchanged | Yes, RED: rejectsCardCountOutsideSeven |
| PHE-D01 | compare | Seven entries per list; first repeats a card using a separate equal-value Card | IllegalArgumentException; both lists unchanged | Yes, RED: rejectsDuplicateCardValues |
| PHE-D02 | compare | Seven entries per list; second repeats a card using a separate equal-value Card | IllegalArgumentException; both lists unchanged | Yes, RED: rejectsDuplicateCardValues |

## Verification

Before this checkpoint, the clean build passed all 601 existing tests. The full
suite now runs 617 cases: 601 pass and these 16 fail with expected assertion
failures because exceptions are not thrown. There are zero test errors or skips.
The JUnit display names match the IDs above. Tests use real Cards and the real
scaffold, with no mocks, stubs or random input.

Next checkpoint: implement input validation and make these cases GREEN. Valid
seven-card comparisons, immutability of successful comparisons, all hand
categories, kickers, ace-low straights and ties will follow in separate TDD
checkpoints. They are not claimed implemented or tested here.
