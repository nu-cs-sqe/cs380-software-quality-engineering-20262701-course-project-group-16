package localization;

import java.util.List;
import java.util.Locale;

/** Resource-backed language catalog and message formatting. */
public final class Localizations {
    private Localizations() { }

    public static Localizations load() { throw new UnsupportedOperationException("Not implemented"); }
    public static Localizations load(ClassLoader loader) { throw new UnsupportedOperationException("Not implemented"); }
    public List<String> languages() { throw new UnsupportedOperationException("Not implemented"); }
    public String defaultLanguage() { throw new UnsupportedOperationException("Not implemented"); }
    public String languageName(String language) { throw new UnsupportedOperationException("Not implemented"); }
    public Locale locale(String language) { throw new UnsupportedOperationException("Not implemented"); }
    public String text(String language, String key, Object... arguments) { throw new UnsupportedOperationException("Not implemented"); }
}
