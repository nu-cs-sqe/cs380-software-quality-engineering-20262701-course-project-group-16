package domain.model;

/** Compares eligible players without mutating state or paying the pot. */
public final class ShowdownResolver {
    public ShowdownResolver() { }
    public ShowdownResolver(HandEvaluator evaluator) { }
    public ShowdownResult resolve(GameState state) { return null; }
}
