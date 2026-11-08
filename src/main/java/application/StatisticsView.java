package application;

import domain.model.PlayStats;
import java.util.List;
import java.util.OptionalDouble;

/** Immutable completed-hand totals; rates follow METRICS order and reveal no cards. */
public final class StatisticsView {
    public static final List<String> METRICS = List.of("vpip", "pfr", "gap", "threeBet",
            "af", "afq", "cBet", "foldCBet", "wtsd", "wsd");
    private final long handsPlayed;
    private final List<OptionalDouble> rates;

    StatisticsView(PlayStats stats) {
        handsPlayed = stats.getHandsPlayed();
        rates = List.of(stats.getVpipPercent(), stats.getPfrPercent(),
                stats.getVpipPfrGapPercentagePoints(), stats.getThreeBetPercent(),
                stats.getAggressionFactor(), stats.getAggressionFrequencyPercent(),
                stats.getFlopCBetPercent(), stats.getFoldToFlopCBetPercent(),
                stats.getWtsdPercent(), stats.getWonAtShowdownPercent());
    }

    public long getHandsPlayed() { return handsPlayed; }
    public List<OptionalDouble> getRates() { return rates; }
}
