# UniformDealSampler BVA

Uniform sampling without replacement; one RNG source per worker.

| ID | Methods | State/input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| US-C01 | injected constructor, sample | 0/3/4/5 public cards; five opponents including pot-ineligible players; largest legal index | Complete shared board; 17 distinct physical cards across six holdings; correct decreasing draw bounds; immutable map/lists | Yes |
| US-C02 | injected constructor, sample | Fresh Random sources with seed 45; 20 successive samples | Reproducible sequence; unchanged request | Yes |
| US-C03 | default/injected constructors, sample | Default RNG; null source/request; indices −1 and bound | Valid default deal; IllegalArgumentException for invalid source/index | Yes |

Hole cards are the first two entries, followed by the common five-card board.
Unseen folded cards and burns are marginalized under the uniform unknown-card model;
the actual deck and opponents’ actual cards never enter sampling.
