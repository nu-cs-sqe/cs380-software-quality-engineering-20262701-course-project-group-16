package bot;

import domain.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class UniformDealSamplerTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 3, 4, 5})
    void usC01DealsUniqueCardsToAllSixPlayersAndCompletesEachStreet(int boardSize) {
        List<Integer> bounds = new ArrayList<>();
        UniformDealSampler sampler = new UniformDealSampler(bound -> { bounds.add(bound); return bound - 1; });
        EquityRequest request = new EquityRequest("hero", EquityFixtures.HOLE,
                EquityFixtures.BOARD.subList(0, boardSize), EquityFixtures.opponents(5),
                List.of(new EquityPot("main", 1, Set.of("hero", "p1"))), 1);
        Map<String, List<Card>> deal = sampler.sample(request);
        assertEquals(6, deal.size());
        assertEquals(EquityFixtures.HOLE, deal.get("hero").subList(0, 2));
        List<Card> board = deal.get("hero").subList(2, 7);
        assertEquals(request.getBoard(), board.subList(0, boardSize));
        Set<Card> unique = new HashSet<>(board);
        assertEquals(5, unique.size());
        for (List<Card> holding : deal.values()) {
            assertEquals(7, holding.size());
            assertEquals(board, holding.subList(2, 7));
            assertTrue(unique.add(holding.get(0)));
            assertTrue(unique.add(holding.get(1)));
            assertThrows(UnsupportedOperationException.class, holding::clear);
        }
        assertEquals(17, unique.size());
        assertEquals(15 - boardSize, bounds.size());
        for (int i = 0; i < bounds.size(); i++) assertEquals(50 - boardSize - i, bounds.get(i));
        assertThrows(UnsupportedOperationException.class, deal::clear);
    }
    @Test
    void usC02FreshIdenticalSeedsReproduceTrialsWithoutMutatingTheRequest() {
        UniformDealSampler first = new UniformDealSampler(new Random(45)::nextInt);
        UniformDealSampler second = new UniformDealSampler(new Random(45)::nextInt);
        EquityRequest request = EquityFixtures.request(2, Rank.TEN, Rank.NINE);
        for (int i = 0; i < 20; i++) assertEquals(first.sample(request), second.sample(request));
        assertEquals(EquityFixtures.HOLE, request.getHoleCards());
        assertEquals(EquityFixtures.BOARD, request.getBoard());
    }
    @Test
    void usC03RejectsInvalidDependenciesRequestsAndRandomIndices() {
        assertThrows(IllegalArgumentException.class, () -> new UniformDealSampler(null));
        assertThrows(IllegalArgumentException.class, () -> new UniformDealSampler().sample(null));
        EquityRequest request = EquityFixtures.request(1, Rank.NINE);
        assertThrows(IllegalArgumentException.class, () -> new UniformDealSampler(b -> -1).sample(request));
        assertThrows(IllegalArgumentException.class, () -> new UniformDealSampler(b -> b).sample(request));
        assertEquals(2, new UniformDealSampler().sample(request).size());
    }
}
