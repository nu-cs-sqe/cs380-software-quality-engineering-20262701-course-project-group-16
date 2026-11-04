package localization;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class LocalizationsTest {
    @Test
    void bundledLanguagesUseExplicitDefaultAndFormatSupportedMessages() {
        Localizations messages = assertDoesNotThrow(() -> { return Localizations.load(); });
        assertEquals(java.util.List.of("en", "zh-CN"), messages.languages());
        assertEquals("en", messages.defaultLanguage());
        assertEquals("English", messages.languageName("en"));
        assertEquals("简体中文", messages.languageName("zh-CN"));
        assertEquals("zh-CN", messages.locale("zh-CN").toLanguageTag());
        assertEquals("First turn: Human 2", messages.text("en", "firstActor", "Human 2"));
        assertEquals("首先行动：玩家 2", messages.text("zh-CN", "firstActor", "玩家 2"));
        assertEquals("Result awaiting settlement", messages.text("en", "result.awaitingSettlement"));
        assertEquals("结果等待结算", messages.text("zh-CN", "result.awaitingSettlement"));
        assertThrows(UnsupportedOperationException.class, () -> messages.languages().add("fr-FR"));
    }

    @Test
    void thirdLanguageIsDiscoveredAndNumbersUseItsLocaleFromResourcesOnly() {
        Map<String, String> files = fixture();
        files.put("ui/languages.properties", "languages=en,zh-CN,de-DE\ndefault=de-DE\nbundle.en=en.properties\nbundle.zh-CN=zh.properties\nbundle.de-DE=de.properties\n");
        files.put("de.properties", "languageName=Deutsch\ngreeting=Hallo {0}\namount=Betrag: {0,number}\n");
        Localizations messages = load(files);
        assertEquals(java.util.List.of("en", "zh-CN", "de-DE"), messages.languages());
        assertEquals("de-DE", messages.defaultLanguage());
        assertEquals("Deutsch", messages.languageName("de-DE"));
        assertEquals("Hallo Ada", messages.text("de-DE", "greeting", "Ada"));
        assertEquals("Amount: 1,234.5", messages.text("en", "amount", 1234.5));
        assertEquals("Betrag: 1.234,5", messages.text("de-DE", "amount", 1234.5));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "languages=\ndefault=en", "languages=en,en\ndefault=en\nbundle.en=en.properties",
        "languages=en,\ndefault=en\nbundle.en=en.properties",
        "languages=en\ndefault=zh-CN\nbundle.en=en.properties",
        "languages=bad_tag\ndefault=bad_tag\nbundle.bad_tag=en.properties",
        "languages=en\ndefault=en", "languages=en\nbundle.en=en.properties",
        "languages=en\ndefault=en\nbundle.en=", "languages=en\ndefault=en\nbundle.en=missing.properties",
        "languages=en\ndefault=en\nbundle.en=en.properties\nunexpected=value",
        "languages=und\ndefault=und\nbundle.und=en.properties"
    })
    void malformedCatalogIsRejectedBeforeDisplayingTheScreen(String config) {
        Map<String, String> files = fixture();
        files.put("ui/languages.properties", config);
        assertThrows(IllegalArgumentException.class, () -> Localizations.load(resources(files)));
    }

    @Test
    void missingCatalogIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Localizations.load(resources(Map.of())));
    }

    @Test
    void resourceReadFailureRetainsCauseAndClosesTheStream() {
        IOException readFailure = new IOException("Resource read failed");
        AtomicBoolean closed = new AtomicBoolean();
        ClassLoader loader = failingResources(readFailure, closed);
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> Localizations.load(loader));
        assertSame(readFailure, failure.getCause());
        assertTrue(closed.get(), "A failed resource read must still close its stream");
    }

    @Test
    void nullResourceLoaderIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Localizations.load(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "languageName=中文\ngreeting=你好 {0}\n",
        "languageName=中文\ngreeting=你好 {0}\namount={0,number}\nextra=多余",
        "languageName=中文\ngreeting=你好 {0}\namount=",
        "languageName=\ngreeting=你好 {0}\namount={0,number}",
        "languageName=中文\ngreeting=你好 {0}\namount={0,number",
        "languageName=中文\ngreeting=你好 {1}\namount={0,number}",
        "languageName=中文\ngreeting=你好 {0}\namount={0}",
        "languageName=中文\ngreeting=你好 {0}\namount={0,date}"
    })
    void missingExtraBlankOrIncompatibleTranslationIsRejected(String translation) {
        Map<String, String> files = fixture();
        files.put("zh.properties", translation);
        assertThrows(IllegalArgumentException.class, () -> Localizations.load(resources(files)));
    }

    @Test
    void malformedDefaultMessageIsRejected() {
        Map<String, String> files = fixture();
        files.put("en.properties", "languageName=English\ngreeting=Hello {0\namount={0,number}");
        assertThrows(IllegalArgumentException.class, () -> Localizations.load(resources(files)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"fr-FR", "en-US", " "})
    void unconfiguredLanguageIsRejectedInsteadOfFallingBack(String language) {
        Localizations messages = load(fixture());
        assertThrows(IllegalArgumentException.class, () -> messages.text(language, "greeting", "Ada"));
        assertThrows(IllegalArgumentException.class, () -> messages.locale(language));
        assertThrows(IllegalArgumentException.class, () -> messages.languageName(language));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"missing"})
    void unknownMessageKeyIsRejected(String key) {
        Localizations messages = load(fixture());
        assertThrows(IllegalArgumentException.class, () -> messages.text("en", key));
    }

    @Test
    void wrongArgumentCountOrTypeIsRejectedInsteadOfLeavingPlaceholders() {
        Localizations messages = load(fixture());
        assertThrows(IllegalArgumentException.class, () -> messages.text("en", "greeting"));
        assertThrows(IllegalArgumentException.class, () -> messages.text("en", "greeting", "Ada", "Extra"));
        assertThrows(IllegalArgumentException.class, () -> messages.text("en", "greeting", (Object[]) null));
        assertThrows(IllegalArgumentException.class, () -> messages.text("en", "amount", "not a number"));
    }

    private static Localizations load(Map<String, String> files) {
        return assertDoesNotThrow(() -> Localizations.load(resources(files)));
    }

    private static Map<String, String> fixture() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("ui/languages.properties", "languages=en,zh-CN\ndefault=en\nbundle.en=en.properties\nbundle.zh-CN=zh.properties\n");
        files.put("en.properties", "languageName=English\ngreeting=Hello {0}\namount=Amount: {0,number}\n");
        files.put("zh.properties", "languageName=中文\ngreeting=你好 {0}\namount=金额：{0,number}\n");
        return files;
    }

    private static ClassLoader failingResources(IOException readFailure, AtomicBoolean closed) {
        InputStream failingStream = new InputStream() {
            @Override public int read() throws IOException {
                throw readFailure;
            }
            @Override public void close() {
                closed.set(true);
            }
        };
        return new ClassLoader(null) {
            @Override public InputStream getResourceAsStream(String name) {
                return "ui/languages.properties".equals(name) ? failingStream : null;
            }
        };
    }

    private static ClassLoader resources(Map<String, String> files) {
        return new ClassLoader(null) {
            @Override public InputStream getResourceAsStream(String name) {
                String content = files.get(name);
                return content == null ? null : new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            }
        };
    }
}
