package bot;

/** Pure equity-calculation boundary; implementations use only legitimate request data. */
@FunctionalInterface
public interface EquityEstimator {
    EquityEstimate estimate(EquityRequest request);
}
