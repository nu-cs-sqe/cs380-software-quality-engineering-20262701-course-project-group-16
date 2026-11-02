# GameInitializer BVA

`application.GameInitializer` coordinates the existing table setup and deck.
`start(int humanCount)` starts a new match, creates a fresh shuffled 52-card deck,
deals two rounds clockwise after the dealer, and returns an immutable PREFLOP
snapshot. Existing chip, blind, status and betting data must be retained.
`GameSetup.create` remains the lower-level SETUP operation.

## Public methods and cases

| ID | Method(s) | Input / state | Expected result | Implementation status |
| --- | --- | --- | --- | --- |
| INIT-C01 | GameInitializer(); start | Default settings; two humans | Dealt PREFLOP; six players; 6,000 conserved chips; all 52 cards split into 12 hole cards and 40 remaining | Test written: `defaultInitializerStartsADealtMatch`; implementation pending |
| INIT-C02 | GameInitializer(config, dealerRandom, deckRandom) | Null config | IllegalArgumentException at construction | Test written: `rejectsNullDependencies(config)`; implementation pending |
| INIT-C03 | Same constructor | Null dealer source | IllegalArgumentException at construction | Test written: `rejectsNullDependencies(dealer)`; implementation pending |
| INIT-C04 | Same constructor | Null deck source | IllegalArgumentException at construction | Test written: `rejectsNullDependencies(deck)`; implementation pending |
| INIT-V01 | Constructor; start | 1 human, dealer 0 | Two-round deal, SB 1, BB 2, actor 3 | Test written: `startsDealtMatchClockwiseAfterDealer`; implementation pending |
| INIT-V02 | Constructor; start | 2 humans, dealer 1 | Two-round deal, SB 2, BB 3, actor 4 | Same parameterized test; implementation pending |
| INIT-V03 | Constructor; start | 5 humans, dealer 2 | Two-round deal, SB 3, BB 4, actor 5 | Same parameterized test; implementation pending |
| INIT-V04 | Constructor; start | 6 humans, dealer 3 | Two-round deal, SB 4, BB 5, actor 0 | Same parameterized test; implementation pending |
| INIT-V05 | Constructor; start | 2 humans, dealer 4 | Two-round deal, SB 5, BB 0, actor 1 | Same parameterized test; implementation pending |
| INIT-V06 | Constructor; start | 2 humans, dealer 5 | Two-round deal, SB 0, BB 1, actor 2 | Same parameterized test; implementation pending |
| INIT-I01 | start | Integer.MIN_VALUE humans | IllegalArgumentException before randomness is consumed | Test written: `rejectsInvalidHumanCountBeforeUsingRandomness`; forwarding validation already passes |
| INIT-I02 | start | 0 humans (below minimum) | Same rejection | Same parameterized test; forwarding validation already passes |
| INIT-I03 | start | 7 humans (above maximum) | Same rejection | Same parameterized test; forwarding validation already passes |
| INIT-I04 | start | Integer.MAX_VALUE humans | Same rejection | Same parameterized test; forwarding validation already passes |
| INIT-S01 | Constructor; start | Starting stack 2, blinds 1/2, dealer 0 | BB has two cards, zero chips and ALL_IN status; excluded from pending actors; pot 3, total 12 | Test written: `dealsToAnAllInBlindWithoutChangingItsStatus`; implementation pending |
| INIT-S02 | start | Start one-human match, reject seven humans, then start six-human match on same initializer | Fresh full deck and identities per match; correct continuing shuffle sequence; previous state unchanged | Test written: `repeatedStartsUseFreshDecksAndPreserveEarlierMatches`; implementation pending |

All six valid dealer fixtures also check human/bot assignment, two unique cards
per seat, 40 remaining, exact clockwise dealing order, 10/20 blind contributions,
6,000 conserved chips, an empty board, preserved actor/raise sets and private
views for every seat. Real setup/deck/state components run; only the dealer's
random choice is stubbed. Deck shuffling uses seeded real Random instances.

This RED checkpoint introduces a forwarding-only scaffold so assertions can run.
The expected failures are missing dealing/PREFLOP behavior and missing constructor
validation. No UI, betting execution or bot actions are implemented here.
