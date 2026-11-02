package domain.model;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PlayerViewTest {

    @Test
    void PV_01() {
        FoundationFixtures.Fixture f=FoundationFixtures.fixture(); f.player(1,990,PlayerStatus.ACTIVE,10,10); f.betting=new BettingProgress(10,20,FoundationFixtures.allIds(),FoundationFixtures.allIds()); f.deal();
        PlayerView p=f.build().viewFor("p1").getPlayers().get(1);
        assertEquals("p1",p.getId()); assertEquals(1,p.getSeat()); assertEquals(PlayerType.HUMAN,p.getType()); assertEquals(990,p.getChipStack());
        assertEquals(PlayerStatus.ACTIVE,p.getStatus()); assertEquals(10,p.getRoundContribution()); assertEquals(10,p.getHandContribution()); assertEquals(2,p.getHoleCards().size());
    }

    @Test
    void PV_02() {
        FoundationFixtures.Fixture f=FoundationFixtures.fixture(); f.deal(); PlayerView p=f.build().viewFor("p0").getPlayers().get(0);
        assertThrows(UnsupportedOperationException.class,()->p.getHoleCards().clear());
    }

    @Test
    void PV_03() {
        FoundationFixtures.Fixture f=FoundationFixtures.fixture(); f.deal(); PlayerView p=f.build().viewFor("p0").getPlayers().get(5);
        assertEquals(5,p.getSeat()); assertEquals(1000,p.getChipStack()); assertTrue(p.getHoleCards().isEmpty());
    }

}
