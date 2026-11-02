package domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class GameSetupTest {
    private static final class DealerRandom extends Random {
        private final int selected;
        int calls;
        int bound;
        DealerRandom(int selected) { this.selected = selected; }
        @Override public int nextInt(int bound) { calls++; this.bound = bound; return selected; }
    }
    private static GameSetup setup(int dealer) { return new GameSetup(new GameConfig(), new DealerRandom(dealer)); }
    @Test
    void GU_01() {
        GameState s=new GameSetup().create(2);
        assertEquals(6,s.getPlayers().size()); assertEquals(6000,s.getTotalChips()); assertEquals(30,s.getPot());
        assertTrue(s.getDealerSeat()>=0 && s.getDealerSeat()<6);
    }

    @Test
    void GU_02() {
        assertThrows(IllegalArgumentException.class,()->new GameSetup(null,new Random(1)));
    }

    @Test
    void GU_03() {
        assertThrows(IllegalArgumentException.class,()->new GameSetup(new GameConfig(),null));
    }

    @Test
    void GU_04() {
        DealerRandom random=new DealerRandom(0); GameSetup setup=new GameSetup(new GameConfig(),random);
        assertThrows(IllegalArgumentException.class,()->setup.create(-1)); assertEquals(0,random.calls);
    }

    @Test
    void GU_05() {
        DealerRandom random=new DealerRandom(0); GameSetup setup=new GameSetup(new GameConfig(),random);
        assertThrows(IllegalArgumentException.class,()->setup.create(0)); assertEquals(0,random.calls);
    }

    @Test
    void GU_06() {
        DealerRandom random=new DealerRandom(0); GameSetup setup=new GameSetup(new GameConfig(),random);
        assertThrows(IllegalArgumentException.class,()->setup.create(7)); assertEquals(0,random.calls);
    }

    @Test
    void GU_07() {
        DealerRandom random=new DealerRandom(0); GameSetup setup=new GameSetup(new GameConfig(),random);
        assertThrows(IllegalArgumentException.class,()->setup.create(2147483647)); assertEquals(0,random.calls);
    }

    @Test
    void GU_08() {
        GameState s=setup(0).create(1); assertEquals(6,s.getPlayers().size());
        assertEquals(1,s.getPlayers().stream().filter(p->p.getType()==PlayerType.HUMAN).count());
        assertEquals(5,s.getPlayers().stream().filter(p->p.getType()==PlayerType.BOT).count());
        assertEquals(6,s.getPlayers().stream().map(Player::getId).distinct().count());
        for(int seat=0;seat<6;seat++) { Player p=s.getPlayers().get(seat); assertEquals(seat,p.getSeat());
            assertEquals(seat<1?PlayerType.HUMAN:PlayerType.BOT,p.getType()); }
    }

    @Test
    void GU_09() {
        GameState s=setup(0).create(2); assertEquals(6,s.getPlayers().size());
        assertEquals(2,s.getPlayers().stream().filter(p->p.getType()==PlayerType.HUMAN).count());
        assertEquals(4,s.getPlayers().stream().filter(p->p.getType()==PlayerType.BOT).count());
        assertEquals(6,s.getPlayers().stream().map(Player::getId).distinct().count());
        for(int seat=0;seat<6;seat++) { Player p=s.getPlayers().get(seat); assertEquals(seat,p.getSeat());
            assertEquals(seat<2?PlayerType.HUMAN:PlayerType.BOT,p.getType()); }
    }

    @Test
    void GU_10() {
        GameState s=setup(0).create(5); assertEquals(6,s.getPlayers().size());
        assertEquals(5,s.getPlayers().stream().filter(p->p.getType()==PlayerType.HUMAN).count());
        assertEquals(1,s.getPlayers().stream().filter(p->p.getType()==PlayerType.BOT).count());
        assertEquals(6,s.getPlayers().stream().map(Player::getId).distinct().count());
        for(int seat=0;seat<6;seat++) { Player p=s.getPlayers().get(seat); assertEquals(seat,p.getSeat());
            assertEquals(seat<5?PlayerType.HUMAN:PlayerType.BOT,p.getType()); }
    }

    @Test
    void GU_11() {
        GameState s=setup(0).create(6); assertEquals(6,s.getPlayers().size());
        assertEquals(6,s.getPlayers().stream().filter(p->p.getType()==PlayerType.HUMAN).count());
        assertEquals(0,s.getPlayers().stream().filter(p->p.getType()==PlayerType.BOT).count());
        assertEquals(6,s.getPlayers().stream().map(Player::getId).distinct().count());
        for(int seat=0;seat<6;seat++) { Player p=s.getPlayers().get(seat); assertEquals(seat,p.getSeat());
            assertEquals(seat<6?PlayerType.HUMAN:PlayerType.BOT,p.getType()); }
    }

    @Test
    void GU_12() {
        GameState s=setup(0).create(2);
        assertEquals(0,s.getDealerSeat()); assertEquals(1,s.getSmallBlindSeat()); assertEquals(2,s.getBigBlindSeat());
        assertEquals(990,s.getPlayers().get(1).getChipStack()); assertEquals(980,s.getPlayers().get(2).getChipStack());
        for(int seat:new int[]{0,3,4,5}) assertEquals(1000,s.getPlayers().get(seat).getChipStack());
        assertEquals(3,s.getCurrentActorSeat().orElseThrow()); assertEquals(s.getPlayers().get(3).getId(),s.getCurrentActorId().orElseThrow());
        assertEquals(20,s.getCurrentBet()); assertEquals(20,s.getLastFullRaiseSize()); assertEquals(30,s.getPot());
        assertEquals(10,s.getPlayerStates().get(1).getRoundContribution()); assertEquals(10,s.getPlayerStates().get(1).getHandContribution());
        assertEquals(20,s.getCommittedChips(2)); assertEquals(6000,s.getTotalChips());
    }

    @Test
    void GU_13() {
        DealerRandom random=new DealerRandom(0); GameState s=new GameSetup(new GameConfig(),random).create(2);
        assertEquals(1,random.calls); assertEquals(6,random.bound); assertEquals(0,s.getDealerSeat());
        assertEquals(1,s.getSmallBlindSeat()); assertEquals(2,s.getBigBlindSeat()); assertEquals(3,s.getCurrentActorSeat().orElseThrow());
    }

    @Test
    void GU_14() {
        DealerRandom random=new DealerRandom(1); GameState s=new GameSetup(new GameConfig(),random).create(2);
        assertEquals(1,random.calls); assertEquals(6,random.bound); assertEquals(1,s.getDealerSeat());
        assertEquals(2,s.getSmallBlindSeat()); assertEquals(3,s.getBigBlindSeat()); assertEquals(4,s.getCurrentActorSeat().orElseThrow());
    }

    @Test
    void GU_15() {
        DealerRandom random=new DealerRandom(2); GameState s=new GameSetup(new GameConfig(),random).create(2);
        assertEquals(1,random.calls); assertEquals(6,random.bound); assertEquals(2,s.getDealerSeat());
        assertEquals(3,s.getSmallBlindSeat()); assertEquals(4,s.getBigBlindSeat()); assertEquals(5,s.getCurrentActorSeat().orElseThrow());
    }

    @Test
    void GU_16() {
        DealerRandom random=new DealerRandom(3); GameState s=new GameSetup(new GameConfig(),random).create(2);
        assertEquals(1,random.calls); assertEquals(6,random.bound); assertEquals(3,s.getDealerSeat());
        assertEquals(4,s.getSmallBlindSeat()); assertEquals(5,s.getBigBlindSeat()); assertEquals(0,s.getCurrentActorSeat().orElseThrow());
    }

    @Test
    void GU_17() {
        DealerRandom random=new DealerRandom(4); GameState s=new GameSetup(new GameConfig(),random).create(2);
        assertEquals(1,random.calls); assertEquals(6,random.bound); assertEquals(4,s.getDealerSeat());
        assertEquals(5,s.getSmallBlindSeat()); assertEquals(0,s.getBigBlindSeat()); assertEquals(1,s.getCurrentActorSeat().orElseThrow());
    }

    @Test
    void GU_18() {
        DealerRandom random=new DealerRandom(5); GameState s=new GameSetup(new GameConfig(),random).create(2);
        assertEquals(1,random.calls); assertEquals(6,random.bound); assertEquals(5,s.getDealerSeat());
        assertEquals(0,s.getSmallBlindSeat()); assertEquals(1,s.getBigBlindSeat()); assertEquals(2,s.getCurrentActorSeat().orElseThrow());
    }

    @Test
    void GU_19() {
        GameState s=setup(0).create(2); assertEquals(GamePhase.SETUP,s.getPhase()); assertTrue(s.getCommunityCards().isEmpty()); assertTrue(s.getRemainingDeck().isEmpty());
        Set<String> ids=new HashSet<>(); for(PlayerState p:s.getPlayerStates()) { assertEquals(PlayerStatus.ACTIVE,p.getStatus()); assertTrue(p.getHoleCards().isEmpty()); ids.add(p.getPlayer().getId()); }
        assertEquals(ids,s.getBettingProgress().getPlayersToAct()); assertEquals(ids,s.getBettingProgress().getPlayersAllowedToRaise());
    }

    @Test
    void GU_20() {
        GameState s=new GameSetup(new GameConfig(2000,20,40),new DealerRandom(0)).create(2);
        assertEquals(1980,s.getPlayers().get(1).getChipStack()); assertEquals(1960,s.getPlayers().get(2).getChipStack());
        assertEquals(60,s.getPot()); assertEquals(12000,s.getTotalChips()); assertEquals(40,s.getCurrentBet()); assertEquals(40,s.getLastFullRaiseSize());
    }

    @Test
    void GU_21() {
        GameState s=new GameSetup(new GameConfig(20,10,20),new DealerRandom(0)).create(1); PlayerState bb=s.getPlayerStates().get(2);
        assertEquals(0,bb.getPlayer().getChipStack()); assertEquals(PlayerStatus.ALL_IN,bb.getStatus()); assertEquals(20,bb.getHandContribution());
        assertFalse(s.getBettingProgress().getPlayersToAct().contains(bb.getPlayer().getId())); assertFalse(s.getBettingProgress().getPlayersAllowedToRaise().contains(bb.getPlayer().getId())); assertEquals(120,s.getTotalChips());
    }

    @Test
    void GU_22() {
        GameState s=new GameSetup(new GameConfig(2,1,2),new DealerRandom(0)).create(1);
        assertEquals(1,s.getPlayers().get(1).getChipStack()); assertEquals(0,s.getPlayers().get(2).getChipStack()); assertEquals(3,s.getPot()); assertEquals(12,s.getTotalChips());
    }

    @Test
    void GU_23() {
        long stack=Long.MAX_VALUE/6; GameState s=new GameSetup(new GameConfig(stack,1,2),new DealerRandom(0)).create(6);
        assertEquals(stack*6,s.getTotalChips()); assertEquals(stack-1,s.getPlayers().get(1).getChipStack()); assertEquals(stack-2,s.getPlayers().get(2).getChipStack());
    }

    @Test
    void GU_24() {
        DealerRandom random=new DealerRandom(0); GameSetup setup=new GameSetup(new GameConfig(),random);
        GameState first=setup.create(2); GameState second=setup.create(6); assertNotEquals(first.getHandId(),second.getHandId());
        Set<String> ids=new HashSet<>(); first.getPlayers().forEach(p->ids.add(p.getId())); second.getPlayers().forEach(p->assertFalse(ids.contains(p.getId())));
        assertEquals(2,first.getPlayers().stream().filter(p->p.getType()==PlayerType.HUMAN).count()); assertEquals(6000,first.getTotalChips()); assertEquals(2,random.calls);
    }

    @Test
    void GU_25() {
        GameSetup a=new GameSetup(new GameConfig(),new Random(2026)); GameSetup b=new GameSetup(new GameConfig(),new Random(2026));
        for(int match=0;match<12;match++) assertEquals(a.create(2).getDealerSeat(),b.create(2).getDealerSeat());
    }

}
