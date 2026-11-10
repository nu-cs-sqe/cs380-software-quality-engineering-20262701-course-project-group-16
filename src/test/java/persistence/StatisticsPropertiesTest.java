package persistence;

import domain.model.CompletedStatistics;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatisticsPropertiesTest {
    @Test
    void spC01DuplicateNonparticipantIdsAreRejectedEvenWhenAllCountersAreZero() {
        StatisticsProperties codec = new StatisticsProperties();
        CompletedStatistics nonparticipants = new CompletedStatistics(
                new CompletedStatistics().getCounts(), Set.of("a", "b"));
        Properties data = new Properties();
        codec.write(data, "p", nonparticipants);
        assertEquals(19, codec.propertyCount(nonparticipants));
        assertEquals(Set.of("a", "b"), codec.read(data, "p").getCompletedHandIds());
        data.setProperty("p.completed.1", data.getProperty("p.completed.0"));
        assertThrows(IllegalArgumentException.class, () -> codec.read(data, "p"));
    }
}
