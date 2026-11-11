# ActionPrediction BVA

| ID | Methods | State/input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| AP-C01 | constructor/getters | Three legal choices, no observations | Uniform add-one prior, zero evidence, nonzero uncertainty, immutable counts/maps | Yes |
| AP-C02 | withAction/getters | Three folds and one call | Counts 3/1/0; probabilities 4/7, 2/7, 1/7; detached prior | Yes |
| AP-C03 | constructor/withAction/getters | One legal choice, null context/action, unavailable action | Probability one and zero uncertainty; reject invalid changes | Yes |
