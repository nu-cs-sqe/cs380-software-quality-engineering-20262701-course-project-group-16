package bot.policy;

/** Small documented configuration differences; randomness is supplied when sampling. */
public enum BotPersonality {
    CAUTIOUS(-0.02, 0.8, 0.5),
    BALANCED(0, 1, 0.75),
    AGGRESSIVE(0.02, 1.2, 1);

    private final double equityBias;
    private final double aggressionScale;
    private final double potFraction;

    BotPersonality(double equityBias, double aggressionScale, double potFraction) {
        this.equityBias = equityBias;
        this.aggressionScale = aggressionScale;
        this.potFraction = potFraction;
    }

    double equityBias() { return equityBias; }
    double aggressionScale() { return aggressionScale; }
    double potFraction() { return potFraction; }
}
