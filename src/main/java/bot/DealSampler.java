package bot;

import domain.model.Card;
import java.util.List;
import java.util.Map;

/** Supplies complete seven-card hypothetical holdings with shared board and unique hole cards. */
@FunctionalInterface
public interface DealSampler {
    Map<String, List<Card>> sample(EquityRequest request);
}
