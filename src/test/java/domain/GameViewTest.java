package domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class GameViewTest {

    @Test
    void GV_01() {
        FoundationFixtures.Fixture f=FoundationFixtures.fixture(); f.board(GamePhase.FLOP,3); GameState s=f.build(); GameView v=s.viewFor("p0");
        assertEquals("p0",v.getViewerId()); assertSame(s.getConfig(),v.getConfig()); assertEquals("h1",v.getHandId()); assertEquals(GamePhase.FLOP,v.getPhase());
        assertSame(s.getPositions(),v.getPositions()); assertEquals(s.getCurrentActorId(),v.getCurrentActorId()); assertEquals(s.getCommunityCards(),v.getCommunityCards());
        assertEquals(0,v.getPot()); assertEquals(0,v.getCurrentBet()); assertEquals(20,v.getLastFullRaiseSize());
        assertEquals(FoundationFixtures.allIds(),v.getPlayersToAct()); assertEquals(FoundationFixtures.allIds(),v.getPlayersAllowedToRaise());
        assertEquals(2,v.getPlayers().get(0).getHoleCards().size()); for(int seat=1;seat<6;seat++) assertTrue(v.getPlayers().get(seat).getHoleCards().isEmpty());
        assertEquals(37,s.getRemainingDeck().size());
    }

    @Test
    void GV_02() {
        FoundationFixtures.Fixture f=FoundationFixtures.fixture(); f.deal(); GameState s=f.build(); GameView v=s.viewFor("p4");
        assertEquals(s.getPlayerStates().get(4).getHoleCards(),v.getPlayers().get(4).getHoleCards()); assertTrue(v.getPlayers().get(0).getHoleCards().isEmpty());
    }

    @Test
    void GV_03() {
        assertTrue(FoundationFixtures.fixture().build().viewFor("p0").getPlayers().get(0).getHoleCards().isEmpty());
    }

    @Test
    void GV_04() {
        assertThrows(IllegalArgumentException.class,()->FoundationFixtures.fixture().build().viewFor(null));
    }

    @Test
    void GV_05() {
        assertThrows(IllegalArgumentException.class,()->FoundationFixtures.fixture().build().viewFor(""));
    }

    @Test
    void GV_06() {
        assertThrows(IllegalArgumentException.class,()->FoundationFixtures.fixture().build().viewFor(" "));
    }

    @Test
    void GV_07() {
        assertThrows(IllegalArgumentException.class,()->FoundationFixtures.fixture().build().viewFor("unknown"));
    }

    @Test
    void GV_08() {
        FoundationFixtures.Fixture f=FoundationFixtures.fixture(); f.board(GamePhase.FLOP,3); GameView v=f.build().viewFor("p0");
        assertThrows(UnsupportedOperationException.class,()->v.getPlayers().clear()); assertThrows(UnsupportedOperationException.class,()->v.getCommunityCards().clear());
        assertThrows(UnsupportedOperationException.class,()->v.getPlayersToAct().clear()); assertThrows(UnsupportedOperationException.class,()->v.getPlayersAllowedToRaise().clear());
    }

}
