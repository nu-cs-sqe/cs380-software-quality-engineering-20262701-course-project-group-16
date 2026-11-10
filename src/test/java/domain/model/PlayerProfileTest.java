package domain.model;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerProfileTest {
    @ParameterizedTest(name = "{0}: rejects an invalid profile ID")
    @MethodSource("invalidProfileIds")
    void rejectsInvalidProfileIds(String caseId, String profileId) {
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerProfile(profileId, "Estella"), caseId);
    }

    static Stream<Arguments> invalidProfileIds() {
        return Stream.of(
                Arguments.of("PP-C01", (String) null),
                Arguments.of("PP-C02", ""),
                Arguments.of("PP-C03", " "),
                Arguments.of("PP-C04", "\t"),
                Arguments.of("PP-C05", "\n"),
                Arguments.of("PP-C06", "\u2003"));
    }

    @ParameterizedTest(name = "{0}: rejects an invalid display name")
    @MethodSource("invalidDisplayNames")
    void rejectsInvalidDisplayNames(String caseId, String displayName) {
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerProfile("profile-1", displayName), caseId);
    }

    static Stream<Arguments> invalidDisplayNames() {
        return Stream.of(
                Arguments.of("PP-C07", (String) null),
                Arguments.of("PP-C08", ""),
                Arguments.of("PP-C09", " "),
                Arguments.of("PP-C10", "\t"),
                Arguments.of("PP-C11", "\n"),
                Arguments.of("PP-C12", "\u2003"));
    }

    @ParameterizedTest(name = "{0}: preserves valid profile data")
    @MethodSource("validProfiles")
    void preservesValidProfileData(String caseId, String profileId, String displayName) {
        PlayerProfile profile = new PlayerProfile(profileId, displayName);

        assertEquals(profileId, profile.getProfileId(), caseId);
        assertEquals(displayName, profile.getDisplayName(), caseId);
    }

    static Stream<Arguments> validProfiles() {
        return Stream.of(
                Arguments.of("PP-C13", "p", "A"),
                Arguments.of("PP-C14", "123e4567-e89b-12d3-a456-426614174000", "Estella Xie"),
                Arguments.of("PP-C15", "profile-1", "玩家一"),
                Arguments.of("PP-C16", "profile-1", "Estella 玩家"),
                Arguments.of("PP-C17", "profile-1", "Zoë 🃏"),
                Arguments.of("PP-C18", " profile-1 ", " Estella "),
                Arguments.of("PP-C19", "p".repeat(512), "玩家".repeat(512)));
    }

    @Test
    @DisplayName("PP-C20: separate identities may share a display name")
    void separateIdentitiesMayShareADisplayName() {
        PlayerProfile first = new PlayerProfile("profile-1", "玩家");
        PlayerProfile second = new PlayerProfile("profile-2", "玩家");

        assertEquals("profile-1", first.getProfileId());
        assertEquals("profile-2", second.getProfileId());
        assertEquals("玩家", first.getDisplayName());
        assertEquals("玩家", second.getDisplayName());
    }
}
