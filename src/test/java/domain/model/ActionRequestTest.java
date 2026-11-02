package domain.model;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ActionRequestTest {

    @Test
    void AR_01() {
        assertThrows(IllegalArgumentException.class, () -> new ActionRequest(null, ActionType.CALL));
    }

    @Test
    void AR_02() {
        assertThrows(IllegalArgumentException.class, () -> new ActionRequest("", ActionType.CALL));
    }

    @Test
    void AR_03() {
        assertThrows(IllegalArgumentException.class, () -> new ActionRequest(" ", ActionType.CALL));
    }

    @Test
    void AR_04() {
        assertThrows(IllegalArgumentException.class, () -> new ActionRequest("\t\n", ActionType.CALL));
    }

    @Test
    void AR_05() {
        assertThrows(IllegalArgumentException.class, () -> new ActionRequest("p", null));
    }

    @Test
    void AR_06() {
        ActionRequest request = new ActionRequest("p", ActionType.CHECK);
        assertEquals("p", request.getPlayerId()); assertEquals(ActionType.CHECK, request.getType());
        assertTrue(request.getTargetAmount().isEmpty());
    }

    @Test
    void AR_07() {
        ActionRequest request = new ActionRequest("p", ActionType.CALL);
        assertEquals("p", request.getPlayerId()); assertEquals(ActionType.CALL, request.getType());
        assertTrue(request.getTargetAmount().isEmpty());
    }

    @Test
    void AR_08() {
        ActionRequest request = new ActionRequest("p", ActionType.FOLD);
        assertEquals("p", request.getPlayerId()); assertEquals(ActionType.FOLD, request.getType());
        assertTrue(request.getTargetAmount().isEmpty());
    }

    @Test
    void AR_09() {
        assertThrows(IllegalArgumentException.class, () -> new ActionRequest("p", ActionType.BET));
    }

    @Test
    void AR_10() {
        assertThrows(IllegalArgumentException.class, () -> new ActionRequest("p", ActionType.BET, -1));
    }

    @Test
    void AR_11() {
        assertThrows(IllegalArgumentException.class, () -> new ActionRequest("p", ActionType.BET, 0));
    }

    @Test
    void AR_12() {
        assertEquals(1, new ActionRequest("p", ActionType.BET, 1).getTargetAmount().orElseThrow());
    }

    @Test
    void AR_13() {
        assertEquals(60, new ActionRequest("p", ActionType.BET, 60).getTargetAmount().orElseThrow());
    }

    @Test
    void AR_14() {
        assertEquals(Long.MAX_VALUE, new ActionRequest("p", ActionType.BET, Long.MAX_VALUE).getTargetAmount().orElseThrow());
    }

    @Test
    void AR_15() {
        assertThrows(IllegalArgumentException.class, () -> new ActionRequest("p", ActionType.RAISE));
    }

    @Test
    void AR_16() {
        assertThrows(IllegalArgumentException.class, () -> new ActionRequest("p", ActionType.RAISE, -1));
    }

    @Test
    void AR_17() {
        assertThrows(IllegalArgumentException.class, () -> new ActionRequest("p", ActionType.RAISE, 0));
    }

    @Test
    void AR_18() {
        assertEquals(1, new ActionRequest("p", ActionType.RAISE, 1).getTargetAmount().orElseThrow());
    }

    @Test
    void AR_19() {
        assertEquals(60, new ActionRequest("p", ActionType.RAISE, 60).getTargetAmount().orElseThrow());
    }

    @Test
    void AR_20() {
        assertEquals(Long.MAX_VALUE, new ActionRequest("p", ActionType.RAISE, Long.MAX_VALUE).getTargetAmount().orElseThrow());
    }

    @Test
    void AR_21() {
        assertThrows(IllegalArgumentException.class, () -> new ActionRequest("p", ActionType.CHECK, 20));
    }

    @Test
    void AR_22() {
        assertThrows(IllegalArgumentException.class, () -> new ActionRequest("p", ActionType.CALL, 20));
    }

    @Test
    void AR_23() {
        assertThrows(IllegalArgumentException.class, () -> new ActionRequest("p", ActionType.FOLD, 20));
    }

    @Test
    void AR_24() {
        assertEquals(" p ", new ActionRequest(" p ", ActionType.CALL).getPlayerId());
    }

}
