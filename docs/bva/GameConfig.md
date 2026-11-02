# GameConfig boundary value analysis

Each case maps to the JUnit method with the same ID (hyphens become underscores).
Grouped assertions exercise the public getters at the stated boundary; no mutable state is exposed.

| ID | Public methods | State/input boundary | Expected result | Implemented |
| --- | --- | --- | --- | --- |
| GC-01 | GameConfig(); getStartingChips(); getSmallBlind(); getBigBlind(); getSeatCount(); getAnte(); isBlindIncreaseEnabled(); getInitialTotalChips() | Default constructor | 1000 chips; 10/20; six seats; no ante/increases; 6000 total | Yes |
| GC-02 | GameConfig(long,long,long); getters | Smallest integer settings: 2/1/2 | Accept; initial total 12 | Yes |
| GC-03 | GameConfig(long,long,long) | Starting chips -1; blinds 1/2 | IllegalArgumentException | Yes |
| GC-04 | GameConfig(long,long,long) | Starting chips 0; blinds 1/2 | IllegalArgumentException | Yes |
| GC-05 | GameConfig(long,long,long) | Starting chips 1; blinds 1/2 | IllegalArgumentException | Yes |
| GC-06 | GameConfig(long,long,long); getInitialTotalChips | Starting chips Long.MAX_VALUE / 6 | Accept without overflow | Yes |
| GC-07 | GameConfig(long,long,long) | One above maximum six-player starting stack | IllegalArgumentException | Yes |
| GC-08 | GameConfig(long,long,long) | Long.MAX_VALUE starting stack | IllegalArgumentException | Yes |
| GC-09 | GameConfig(long,long,long) | Small blind -1; big blind 20 | IllegalArgumentException | Yes |
| GC-10 | GameConfig(long,long,long) | Small blind 0; big blind 20 | IllegalArgumentException | Yes |
| GC-11 | GameConfig(long,long,long) | Small blind 20; big blind 20 | IllegalArgumentException | Yes |
| GC-12 | GameConfig(long,long,long) | Small blind 21; big blind 20 | IllegalArgumentException | Yes |
| GC-13 | GameConfig(long,long,long); getSmallBlind | Small blind one below big blind: 19/20 | Accept | Yes |
| GC-14 | GameConfig(long,long,long) | Big blind -1; small blind 10; stack 1000 | IllegalArgumentException | Yes |
| GC-15 | GameConfig(long,long,long) | Big blind 0; small blind 10; stack 1000 | IllegalArgumentException | Yes |
| GC-16 | GameConfig(long,long,long) | Big blind 9; small blind 10; stack 1000 | IllegalArgumentException | Yes |
| GC-17 | GameConfig(long,long,long) | Big blind 10; small blind 10; stack 1000 | IllegalArgumentException | Yes |
| GC-18 | GameConfig(long,long,long) | Big blind 1001; small blind 10; stack 1000 | IllegalArgumentException | Yes |
| GC-19 | GameConfig(long,long,long); getBigBlind | Big blind one above small blind: 10/11 | Accept | Yes |
| GC-20 | GameConfig(long,long,long); getBigBlind | Big blind equals starting stack | Accept | Yes |
| GC-21 | GameConfig(long,long,long); getters | Custom config does not alter another default config | Independent immutable values | Yes |
