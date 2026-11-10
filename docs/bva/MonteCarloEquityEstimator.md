# MonteCarloEquityEstimator BVA

Samples are shared across pots. Comparisons are cached per trial and reused for eligibility subsets.

| ID | Methods | State/input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| MC-C01 | injected constructor, estimate(request) | Two trials: win/tie/loss | Shares 1/0.5/0; weighted payout, count and immutable output | Yes |
| MC-C02 | estimate | Three-way tie; tied opponent followed by stronger contender | Fraction 1/3; stronger contender produces zero | Yes |
| MC-C03 | estimate | Different eligible subsets and amounts; hero-ineligible side pot | Correct per-pot shares; eligible-weighted equity 1/3 and payout 100; comparisons reused | Yes |
| MC-C04 | estimate | Win/tie/loss sequence over three trials | Mean 0.5; exactly three samples | Yes |
| MC-C05 | estimate | Hero-only uncontested pot | Share 1 without evaluator calls | Yes |
| MC-C06 | constructors, both estimate overloads | Null collaborator/request/cancellation source | IllegalArgumentException | Yes |
| MC-C07 | estimate(request,cancelled) | Cancel before or after first trial, then retry | CancellationException; no partial result; retry uses fresh totals | Yes |
| MC-C08 | estimate(request,cancelled) | Thread interruption before calculation | CancellationException; interrupt status preserved | Yes |
| MC-C09 | estimate | Injected sampler/evaluator failure | Original exception propagated; no published result | Yes |
| MC-C10 | estimate | Hero beats everyone but is ineligible for a side pot | Zero share of that pot regardless of hand strength | Yes |
| EI-C01 | default constructor, estimate | Real quads against five opponents | Equity 1, payout 120 | Yes |
| EI-C02 | estimate | Royal flush on board; six participants | Equity 1/6, payout 20 | Yes |
| EI-C03 | estimate | 990 exact river combinations vs 2048 trials for seeds 7/23/91 | Each absolute error ≤0.06; record observed error/latency in standard test output | Yes |
| EI-C04 | estimate | Six-player preflop; 1000 trials | Finite bounded result; observational latency recorded without flaky deadline | Yes |

MC cases are isolated unit tests using controlled collaborators. EI/EV cases use
real components. The fixed-seed error threshold is regression coverage, not a
general accuracy guarantee. Cancellation is checked between bounded trials;
async workers and stale-state rejection remain E06.

The sampler/estimator unit phase observed 17 expected failures (6 sampler and
11 estimator invocations) before implementation, then GREEN. MC-C10 is a later
mutation-driven regression. EI cases were added after core implementation and
are integration evidence, not claimed test-first failures.
