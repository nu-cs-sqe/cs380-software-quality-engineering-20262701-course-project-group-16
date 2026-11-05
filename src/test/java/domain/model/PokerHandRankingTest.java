package domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static domain.model.PokerTestCards.cards;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PokerHandRankingTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("comparisons")
    void comparesBestFiveAndPreservesInputs(String id, String firstText,
            String secondText, int expectedSign) {
        HandEvaluator evaluator = new PokerHandEvaluator();
        List<Card> first = cards(firstText);
        List<Card> second = cards(secondText);
        List<Card> beforeFirst = new ArrayList<>(first);
        List<Card> beforeSecond = new ArrayList<>(second);
        assertEquals(expectedSign, Integer.signum(evaluator.compare(first, second)), id);
        assertEquals(-expectedSign, Integer.signum(evaluator.compare(second, first)), id + " reversed");
        assertEquals(beforeFirst, first);
        assertEquals(beforeSecond, second);
        Collections.reverse(first);
        Collections.rotate(second, 2);
        assertEquals(expectedSign, Integer.signum(evaluator.compare(
                List.copyOf(first), List.copyOf(second))), id + " input order");
    }

    private static Stream<Arguments> comparisons() {
        return Stream.of(
            Arguments.of("RANK-C01 pair beats high card", "AH AD QC JS 9H 4D 2C", "AH KD QC JS 9H 4D 2C", 1),
            Arguments.of("RANK-C02 two pair beats pair", "AH AD QC QS 9H 4D 2C", "AH AD QC JS 9H 4D 2C", 1),
            Arguments.of("RANK-C03 three of a kind beats two pair", "AH AD AC JS 9H 4D 2C", "AH AD QC QS 9H 4D 2C", 1),
            Arguments.of("RANK-C04 straight beats three of a kind", "9H TD JC QS KH 4D 2C", "AH AD AC JS 9H 4D 2C", 1),
            Arguments.of("RANK-C05 flush beats straight", "AH JH 9H 6H 3H KD 2C", "9H TD JC QS KH 4D 2C", 1),
            Arguments.of("RANK-C06 full house beats flush", "AH AD AC KS KH 4D 2C", "AH JH 9H 6H 3H KD 2C", 1),
            Arguments.of("RANK-C07 four of a kind beats full house", "AH AD AC AS KH 4D 2C", "AH AD AC KS KH 4D 2C", 1),
            Arguments.of("RANK-C08 straight flush beats four of a kind", "9H TH JH QH KH 4D 2C", "AH AD AC AS KH 4D 2C", 1),
            Arguments.of("RANK-K01 high-card fifth kicker", "AH KD QC JS 9H 4D 2C", "AS KC QD JH 8C 4H 2D", 1),
            Arguments.of("RANK-K02 pair rank", "AH AD QC JS 9H 4D 2C", "KH KD AC QS 9H 4D 2C", 1),
            Arguments.of("RANK-K03 pair first kicker", "9H 9D AH KC QS 4D 2C", "9S 9C KH QD JS 4H 2D", 1),
            Arguments.of("RANK-K04 pair second kicker", "9H 9D AH QC JS 4D 2C", "9S 9C AC JD TH 4H 2D", 1),
            Arguments.of("RANK-K05 pair third kicker", "9H 9D AH KC QS 4D 2C", "9S 9C AC KD JH 4H 2D", 1),
            Arguments.of("RANK-K06 two-pair high pair", "AH AD QC QS 9H 4D 2C", "KH KD QD QH 9S 4H 2D", 1),
            Arguments.of("RANK-K07 two-pair low pair", "AH AD QC QS 9H 4D 2C", "AS AC JD JH 9S 4H 2D", 1),
            Arguments.of("RANK-K08 two-pair kicker", "AH AD QC QS KH 4D 2C", "AS AC QD QH JS 4H 2D", 1),
            Arguments.of("RANK-K09 triplet rank", "AH AD AC JS 9H 4D 2C", "KH KD KC AS 9H 4D 2C", 1),
            Arguments.of("RANK-K10 triplet first kicker", "AH AD AC KS QH 4D 2C", "AS AH AC QS JH 4D 2C", 1),
            Arguments.of("RANK-K11 triplet second kicker", "AH AD AC KS QH 4D 2C", "AS AH AC KS JH 4D 2C", 1),
            Arguments.of("RANK-K12 straight high card", "9H TD JC QS KH 4D 2C", "8H 9D TC JS QH 4D 2C", 1),
            Arguments.of("RANK-K13 flush fifth card", "AH KH QH JH 9H 4D 2C", "AS KS QS JS 8S 4H 2D", 1),
            Arguments.of("RANK-K14 full-house triplet", "AH AD AC KS KH 4D 2C", "KH KD KC AS AH 4D 2C", 1),
            Arguments.of("RANK-K15 full-house pair", "AH AD AC KS KH 4D 2C", "AS AH AC QS QH 4D 2C", 1),
            Arguments.of("RANK-K16 quads rank", "AH AD AC AS KH 4D 2C", "KH KD KC KS AH 4D 2C", 1),
            Arguments.of("RANK-K17 quads kicker", "AH AD AC AS KH QD JC", "AH AD AC AS QH JD TC", 1),
            Arguments.of("RANK-K18 royal over king straight flush", "TH JH QH KH AH 4D 2C", "9H TH JH QH KH 4D 2C", 1),
            Arguments.of("RANK-E01 wheel below six-high", "AH 2D 3C 4S 5H 9D JC", "2H 3D 4C 5S 6H 9C JD", -1),
            Arguments.of("RANK-E02 no ace wraparound", "AH 2D 3C 4S 5H 9D JC", "QH KD AC 2S 3H 7D 9C", 1),
            Arguments.of("RANK-E03 wheel straight flush", "AH 2H 3H 4H 5H 9D JC", "2S 3S 4S 5S 6S 9C JD", -1),
            Arguments.of("RANK-E04 two triplets choose higher", "AH AD AC KS KH KD 2C", "QH QD QC JS JH JD 2C", 1),
            Arguments.of("RANK-E05 three pairs choose best two", "AH AD QC QS KH KD 2C", "AS AC JD JH KS QD 2D", 1),
            Arguments.of("RANK-E06 seven suited choose highest five", "AH KH QH 9H 8H 4H 2H", "AS KS QS 9S 7S 4S 2S", 1),
            Arguments.of("RANK-E07 longest straight uses top end", "2C 3D 4H 5S 6C 7D 8H", "2D 3H 4S 5C 6D 7H 9S", 1),
            Arguments.of("RANK-E08 separate flush and straight not straight flush", "2H 4H 6H 8H TH 3D 5S", "9C 9D 9S AH AD 4S 2D", -1),
            Arguments.of("RANK-T01 play the shared board", "AC KC QC JC TC 2D 3H", "AC KC QC JC TC 4D 5H", 0),
            Arguments.of("RANK-T02 suits and unused cards do not break ties", "AH KD QS JC 9H 4D 2C", "AS KC QD JH 9C 5D 3H", 0),
            Arguments.of("RANK-T03 identical holdings are allowed", "AH AD QC JS 9H 4D 2C", "AH AD QC JS 9H 4D 2C", 0));
    }
}
