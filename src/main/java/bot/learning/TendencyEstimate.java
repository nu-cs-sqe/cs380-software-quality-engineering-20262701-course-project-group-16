package bot.learning;

/** Beta(1,1) belief for one eligible public-decision outcome, separate from PlayStats. */
public final class TendencyEstimate {
    private final long successes;
    private final long opportunities;

    TendencyEstimate(long successes, long opportunities) {
        if (successes < 0 || opportunities < successes) {
            throw new IllegalArgumentException("Successes must be between zero and opportunities.");
        }
        this.successes = successes;
        this.opportunities = opportunities;
    }

    TendencyEstimate withOutcome(boolean success) {
        long next = success ? Math.incrementExact(successes) : successes;
        return new TendencyEstimate(next, Math.incrementExact(opportunities));
    }

    public long getSuccesses() { return successes; }
    public long getOpportunities() { return opportunities; }
    public double getProbability() { return (successes + 1.0) / (opportunities + 2.0); }

    public double getStandardDeviation() {
        double probability = getProbability();
        return Math.sqrt(probability * (1 - probability) / (opportunities + 3.0));
    }

    /** Data weight relative to two prior pseudo-observations; not a confidence interval. */
    public double getEvidenceWeight() { return opportunities / (opportunities + 2.0); }
}
