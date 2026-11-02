# GameSetup boundary value analysis

Each case maps to the JUnit method with the same ID (hyphens become underscores).
Grouped assertions exercise the public getters at the stated boundary; no mutable state is exposed.

| ID | Public methods | State/input boundary | Expected result | Implemented |
| --- | --- | --- | --- | --- |
| GU-01 | GameSetup(); create | Default settings and two humans | Six seats; 6000 total; 30 pot; dealer within occupied seats | Yes |
| GU-02 | GameSetup(GameConfig,Random) | Null config | IllegalArgumentException | Yes |
| GU-03 | GameSetup(GameConfig,Random) | Null dealer random source | IllegalArgumentException | Yes |
| GU-04 | create | -1 human players | IllegalArgumentException; no randomness consumed | Yes |
| GU-05 | create | 0 human players | IllegalArgumentException; no randomness consumed | Yes |
| GU-06 | create | 7 human players | IllegalArgumentException; no randomness consumed | Yes |
| GU-07 | create | 2147483647 human players | IllegalArgumentException; no randomness consumed | Yes |
| GU-08 | create | 1 humans at valid boundary | Humans occupy first seats; bots fill exactly six; unique IDs and seats | Yes |
| GU-09 | create | 2 humans at valid boundary | Humans occupy first seats; bots fill exactly six; unique IDs and seats | Yes |
| GU-10 | create | 5 humans at valid boundary | Humans occupy first seats; bots fill exactly six; unique IDs and seats | Yes |
| GU-11 | create | 6 humans at valid boundary | Humans occupy first seats; bots fill exactly six; unique IDs and seats | Yes |
| GU-12 | create | Exact shared example: six players; dealer 0; 1000 chips; blinds 10/20 | SB seat1=990; BB seat2=980; actor3; bet/raise20; pot30; total6000 | Yes |
| GU-13 | create | Dealer source selects occupied-seat index 0 | One bounded draw over all six seats; correct wrapped roles | Yes |
| GU-14 | create | Dealer source selects occupied-seat index 1 | One bounded draw over all six seats; correct wrapped roles | Yes |
| GU-15 | create | Dealer source selects occupied-seat index 2 | One bounded draw over all six seats; correct wrapped roles | Yes |
| GU-16 | create | Dealer source selects occupied-seat index 3 | One bounded draw over all six seats; correct wrapped roles | Yes |
| GU-17 | create | Dealer source selects occupied-seat index 4 | One bounded draw over all six seats; correct wrapped roles | Yes |
| GU-18 | create | Dealer source selects occupied-seat index 5 | One bounded draw over all six seats; correct wrapped roles | Yes |
| GU-19 | create | New table before Member A deals | SETUP; empty board/deck/hole cards; live actors include BB option | Yes |
| GU-20 | create | Custom 2000 chips, blinds 20/40, dealer0 | Stacks1980/1960; pot60; total12000 | Yes |
| GU-21 | create | Starting stack equals big blind:20/10/20 | BB becomes all-in, contributes forced20, cannot act/raise; total120 | Yes |
| GU-22 | create | Minimum valid config:2 chips; blinds1/2 | Correct blind deductions and all-in; no negative stacks | Yes |
| GU-23 | create | Largest safe starting stack | Six-stack total conserved after posting blinds | Yes |
| GU-24 | create | Two matches created by same setup object | Distinct hand/player IDs; previous snapshot unchanged | Yes |
| GU-25 | create | Same random seed across independent setup instances | Reproducible dealer sequence without fixing production choice | Yes |
