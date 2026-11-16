package bot;

import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable estimate values for application-port unit tests; no simulation is run. */
public final class BotEstimateValues {
    private BotEstimateValues() { }

    public static EquityEstimate fixed(EquityRequest request, double equity) {
        Map<String, Double> shares = new LinkedHashMap<>();
        request.getPots().forEach(pot -> shares.put(pot.getId(),
                pot.getEligiblePlayers().contains(request.getHeroId()) ? equity : 0));
        return new EquityEstimate(request.getSimulations(), shares, equity,
                request.getEligiblePotAmount() * equity);
    }
}
