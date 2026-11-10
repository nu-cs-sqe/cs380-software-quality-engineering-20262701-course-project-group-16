package persistence;

import domain.model.CompletedStatistics;
import domain.model.Statistic;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Encodes raw completed counters/history without serializing pending game state. */
final class StatisticsProperties {
    CompletedStatistics read(Properties properties, String prefix) {
        Map<Statistic, Long> counts = new EnumMap<>(Statistic.class);
        for (Statistic counter : Statistic.values()) {
            String encoded = properties.getProperty(prefix + ".stats." + counter);
            counts.put(counter, Long.parseLong(encoded));
        }
        int count = Integer.parseInt(properties.getProperty(prefix + ".completedCount"));
        if (count < 0) {
            throw new IllegalArgumentException("Invalid completed-history size.");
        }
        Set<String> ids = new HashSet<>();
        for (int index = 0; index < count; index++) {
            if (!ids.add(properties.getProperty(prefix + ".completed." + index))) {
                throw new IllegalArgumentException("Duplicate completed hand ID.");
            }
        }
        return new CompletedStatistics(counts, ids);
    }

    void write(Properties properties, String prefix, CompletedStatistics statistics) {
        statistics.getCounts().forEach((counter, count) ->
                properties.setProperty(prefix + ".stats." + counter, count.toString()));
        List<String> ids = new ArrayList<>(statistics.getCompletedHandIds());
        properties.setProperty(prefix + ".completedCount", Integer.toString(ids.size()));
        for (int index = 0; index < ids.size(); index++) {
            properties.setProperty(prefix + ".completed." + index, ids.get(index));
        }
    }

    int propertyCount(CompletedStatistics statistics) {
        return Statistic.values().length + 1 + statistics.getCompletedHandIds().size();
    }
}
