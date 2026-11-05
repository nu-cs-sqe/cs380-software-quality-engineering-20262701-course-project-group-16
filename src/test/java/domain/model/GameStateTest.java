package domain.model;


import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class GameStateTest {
    private static FoundationFixtures.Fixture fixture() { return FoundationFixtures.fixture(); }
    private static Set<String> ids() { return FoundationFixtures.allIds(); }
    @Test
    void GS_01() {
        FoundationFixtures.Fixture f = fixture(); GameState s = f.build();
        assertSame(f.config, s.getConfig()); assertEquals("h1", s.getHandId()); assertEquals(GamePhase.SETUP, s.getPhase());
        assertEquals(6, s.getPlayers().size()); assertEquals(6, s.getPlayerStates().size());
        assertEquals(0, s.getDealerSeat()); assertEquals(1, s.getSmallBlindSeat()); assertEquals(2, s.getBigBlindSeat());
        assertEquals("p3", s.getCurrentActorId().orElseThrow()); assertEquals(3, s.getCurrentActorSeat().orElseThrow());
        assertSame(f.positions, s.getPositions()); assertSame(f.betting, s.getBettingProgress());
        assertEquals(0, s.getCurrentBet()); assertEquals(20, s.getLastFullRaiseSize());
        assertEquals(0, s.getPot()); assertEquals(6000, s.getTotalChips());
        assertTrue(s.getCommunityCards().isEmpty()); assertTrue(s.getRemainingDeck().isEmpty());
    }

    @Test
    void GS_02() {
        FoundationFixtures.Fixture f = fixture(); f.player(1,990,PlayerStatus.ACTIVE,10,10); f.player(2,980,PlayerStatus.ACTIVE,20,20);
        f.betting = new BettingProgress(20,20,ids(),ids()); GameState s=f.build();
        assertEquals(30,s.getPot()); assertEquals(6000,s.getTotalChips()); assertEquals(10,s.getCommittedChips(1)); assertEquals(20,s.getCommittedChips(2));
    }

    @Test
    void GS_03() {
        FoundationFixtures.Fixture f=fixture(); f.player(0,970,PlayerStatus.ACTIVE,10,30);
        f.betting=new BettingProgress(10,20,ids(),ids()); GameState s=f.build(); assertEquals(30,s.getPot()); assertEquals(6000,s.getTotalChips());
    }

    @Test
    void GS_04() {
        FoundationFixtures.Fixture f=fixture(); f.config=null; assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_05() {
        FoundationFixtures.Fixture f=fixture(); f.hand=null; assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_06() {
        FoundationFixtures.Fixture f=fixture(); f.hand=""; assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_07() {
        FoundationFixtures.Fixture f=fixture(); f.hand=" "; assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_08() {
        FoundationFixtures.Fixture f=fixture(); f.phase=null; assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_09() {
        FoundationFixtures.Fixture f=fixture(); f.players=null; assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_10() {
        FoundationFixtures.Fixture f=fixture(); f.positions=null; assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_11() {
        FoundationFixtures.Fixture f=fixture(); f.community=null; assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_12() {
        FoundationFixtures.Fixture f=fixture(); f.deck=null; assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_13() {
        FoundationFixtures.Fixture f=fixture(); f.betting=null; assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_14() {
        FoundationFixtures.Fixture f=fixture(); f.players.remove(5); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_15() {
        FoundationFixtures.Fixture f=fixture(); f.players.add(f.players.get(0)); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_16() {
        FoundationFixtures.Fixture f=fixture(); f.players.set(0,null); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_17() {
        FoundationFixtures.Fixture f=fixture(); f.players.set(1,f.players.get(0)); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_18() {
        FoundationFixtures.Fixture f=fixture(); f.players.set(1,new PlayerState(new Player("p0",1,PlayerType.HUMAN,1000),PlayerStatus.ACTIVE,List.of(),0,0)); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_19() {
        FoundationFixtures.Fixture f=fixture(); f.positions=new HandPositions(0,2,1); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_20() {
        FoundationFixtures.Fixture f=fixture(); f.player(0,999,PlayerStatus.ACTIVE,0,0); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_21() {
        FoundationFixtures.Fixture f=fixture(); f.player(0,1001,PlayerStatus.ACTIVE,0,0); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_22() {
        FoundationFixtures.Fixture f=fixture(); f.player(0,980,PlayerStatus.ACTIVE,20,20); f.betting=new BettingProgress(19,20,ids(),ids()); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_23() {
        FoundationFixtures.Fixture f=fixture(); f.betting=new BettingProgress(1,20,ids(),ids()); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_24() {
        FoundationFixtures.Fixture f=fixture(); f.betting=new BettingProgress(0,19,ids(),ids()); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_25() {
        FoundationFixtures.Fixture f=fixture(); f.betting=new BettingProgress(0,20,Set.of("unknown"),Set.of()); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_26() {
        FoundationFixtures.Fixture f=fixture(); f.betting=new BettingProgress(0,20,Set.of("p3"),Set.of("unknown")); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_27() {
        FoundationFixtures.Fixture f=fixture(); f.player(0,1000,PlayerStatus.FOLDED,0,0); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_28() {
        FoundationFixtures.Fixture f=fixture(); f.player(0,0,PlayerStatus.ALL_IN,1000,1000); f.betting=new BettingProgress(1000,20,ids(),Set.of("p3")); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_29() {
        FoundationFixtures.Fixture f=fixture(); f.player(0,1000,PlayerStatus.FOLDED,0,0); f.betting=new BettingProgress(0,20,Set.of("p3"),Set.of("p0")); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_30() {
        FoundationFixtures.Fixture f=fixture(); f.actor="unknown"; assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_31() {
        FoundationFixtures.Fixture f=fixture(); f.actor=" "; assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_32() {
        FoundationFixtures.Fixture f=fixture(); f.actor=null; assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_33() {
        FoundationFixtures.Fixture f=fixture(); f.betting=new BettingProgress(0,20,Set.of("p0"),ids()); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_34() {
        FoundationFixtures.Fixture f=fixture(); f.betting=new BettingProgress(0,20,Set.of(),Set.of()); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_35() {
        FoundationFixtures.Fixture f=fixture(); Collections.reverse(f.players); GameState s=f.build(); f.players.clear();
        assertEquals(0,s.getPlayerStates().get(0).getPlayer().getSeat()); assertEquals(5,s.getPlayers().get(5).getSeat()); assertEquals(6,s.getPlayerStates().size());
    }

    @Test
    void GS_36() {
        GameState s=fixture().build(); assertThrows(UnsupportedOperationException.class,()->s.getPlayers().clear()); assertThrows(UnsupportedOperationException.class,()->s.getPlayerStates().clear());
    }

    @Test
    void GS_37() {
        FoundationFixtures.Fixture f=fixture(); f.actor=null; f.betting=new BettingProgress(0,20,Set.of(),Set.of()); GameState s=f.build();
        assertTrue(s.getCurrentActorId().isEmpty()); assertTrue(s.getCurrentActorSeat().isEmpty());
    }

    @Test
    void GS_38() {
        FoundationFixtures.Fixture f=fixture();
        for(int seat: new int[]{1,2,3,5}) f.player(seat,0,PlayerStatus.ELIMINATED,0,0);
        f.player(0,3000,PlayerStatus.ACTIVE,0,0); f.player(4,3000,PlayerStatus.ACTIVE,0,0);
        f.positions=new HandPositions(0,0,4); f.actor="p0"; f.betting=new BettingProgress(0,20,Set.of("p0","p4"),Set.of("p0","p4"));
        GameState s=f.build(); assertEquals(6,s.getPlayers().size()); assertEquals(0,s.getSmallBlindSeat()); assertEquals(4,s.getBigBlindSeat()); assertEquals(4,s.getPlayers().get(4).getSeat());
    }

    @Test
    void GS_39() {
        FoundationFixtures.Fixture f=fixture(); f.config=new GameConfig(Long.MAX_VALUE/6,10,20);
        f.player(0,Long.MAX_VALUE,PlayerStatus.ACTIVE,0,0); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_40() {
        FoundationFixtures.Fixture f=fixture(); long stack=Long.MAX_VALUE/6; f.config=new GameConfig(stack,10,20);
        for(int seat=0;seat<6;seat++) f.player(seat,stack,PlayerStatus.ACTIVE,0,0);
        assertEquals(stack*6,f.build().getTotalChips());
    }

    @Test
    void GS_41() {
        assertThrows(IllegalArgumentException.class,()->fixture().build().getCommittedChips(-1));
    }

    @Test
    void GS_42() {
        assertThrows(IllegalArgumentException.class,()->fixture().build().getCommittedChips(6));
    }

    @Test
    void GS_43() {
        GameState s=fixture().build(); assertEquals(0,s.getCommittedChips(0)); assertEquals(0,s.getCommittedChips(5));
    }

    @Test
    void GS_44() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.PREFLOP,0); GameState s=f.build();
        assertEquals(GamePhase.PREFLOP,s.getPhase()); assertEquals(0,s.getCommunityCards().size()); assertEquals(40,s.getRemainingDeck().size());
    }

    @Test
    void GS_45() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.FLOP,3); GameState s=f.build();
        assertEquals(GamePhase.FLOP,s.getPhase()); assertEquals(3,s.getCommunityCards().size()); assertEquals(37,s.getRemainingDeck().size());
    }

    @Test
    void GS_46() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.TURN,4); GameState s=f.build();
        assertEquals(GamePhase.TURN,s.getPhase()); assertEquals(4,s.getCommunityCards().size()); assertEquals(36,s.getRemainingDeck().size());
    }

    @Test
    void GS_47() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.RIVER,5); GameState s=f.build();
        assertEquals(GamePhase.RIVER,s.getPhase()); assertEquals(5,s.getCommunityCards().size()); assertEquals(35,s.getRemainingDeck().size());
    }

    @Test
    void GS_48() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.SHOWDOWN,5); GameState s=f.build();
        assertEquals(GamePhase.SHOWDOWN,s.getPhase()); assertEquals(5,s.getCommunityCards().size()); assertEquals(35,s.getRemainingDeck().size());
    }

    @Test
    void GS_49() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.SETUP,3); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_50() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.PREFLOP,3); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_51() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.FLOP,2); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_52() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.FLOP,4); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_53() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.TURN,3); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_54() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.TURN,5); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_55() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.RIVER,4); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_56() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.RIVER,6); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_57() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.SHOWDOWN,4); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_58() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.COMPLETION,2); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_59() {
        FoundationFixtures.Fixture f=fixture(); f.deal(); PlayerState p=f.players.get(1); f.players.set(1,new PlayerState(p.getPlayer(),p.getStatus(),f.players.get(0).getHoleCards(),0,0)); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_60() {
        FoundationFixtures.Fixture f=fixture(); f.deal(); f.deck.add(new Card(Rank.TWO, Suit.CLUBS)); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_61() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.FLOP,3); f.deck.add(f.community.get(0)); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_62() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.FLOP,3); f.community.set(0,new Card(Rank.TWO, Suit.CLUBS)); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_63() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.FLOP,3); f.community.set(1,f.community.get(0)); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_64() {
        FoundationFixtures.Fixture f=fixture(); f.deck=new ArrayList<>(List.of(new Card(Rank.ACE, Suit.SPADES),new Card(Rank.ACE, Suit.SPADES))); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_65() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.FLOP,3); f.community.set(0,null); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_66() {
        FoundationFixtures.Fixture f=fixture(); f.deck=new ArrayList<>(Arrays.asList((Card)null)); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_67() {
        FoundationFixtures.Fixture f=fixture(); f.deal(); f.players.set(0,new PlayerState(f.players.get(0).getPlayer(),PlayerStatus.ACTIVE,List.of(),0,0)); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_68() {
        FoundationFixtures.Fixture f=fixture(); f.deal(); f.phase=GamePhase.SETUP; assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_69() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.SHOWDOWN,5); f.actor="p3"; f.betting=new BettingProgress(0,20,Set.of("p3"),Set.of()); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_70() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.COMPLETION,5); f.player(0,990,PlayerStatus.ACTIVE,0,10); assertThrows(IllegalArgumentException.class,f::build);
    }

    @Test
    void GS_71() {
        FoundationFixtures.Fixture f=fixture(); f.phase=GamePhase.COMPLETION; f.actor=null;
        for(int seat=0;seat<6;seat++) f.player(seat,seat==4?6000:0,seat==4?PlayerStatus.ACTIVE:PlayerStatus.ELIMINATED,0,0);
        f.betting=new BettingProgress(0,20,Set.of(),Set.of()); GameState s=f.build(); assertEquals(0,s.getPot()); assertEquals(6000,s.getPlayers().get(4).getChipStack());
    }

    @Test
    void GS_72() {
        FoundationFixtures.Fixture f=fixture(); f.board(GamePhase.FLOP,3); GameState s=f.build(); f.community.clear(); f.deck.clear();
        assertEquals(3,s.getCommunityCards().size()); assertEquals(37,s.getRemainingDeck().size());
        assertThrows(UnsupportedOperationException.class,()->s.getCommunityCards().clear()); assertThrows(UnsupportedOperationException.class,()->s.getRemainingDeck().clear());
    }

    @Test
    void GS_73() {
        FoundationFixtures.Fixture f=fixture(); f.phase=GamePhase.COMPLETION; f.actor=null;
        f.player(0,0,PlayerStatus.ALL_IN,0,Long.MAX_VALUE);
        f.player(1,0,PlayerStatus.ALL_IN,0,Long.MAX_VALUE);
        f.betting=new BettingProgress(0,20,Set.of(),Set.of());
        assertThrows(IllegalArgumentException.class,f::build);
    }
}
