package localization;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Format;
import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Validated, resource-backed language catalog. No JVM-default locale fallback is used. */
public final class Localizations {
    private final List<String> languages;
    private final String defaultLanguage;
    private final Map<String, Locale> locales;
    private final Map<String, Properties> bundles;

    private Localizations(List<String> languages, String defaultLanguage,
            Map<String, Locale> locales, Map<String, Properties> bundles) {
        this.languages = List.copyOf(languages);
        this.defaultLanguage = defaultLanguage;
        this.locales = Map.copyOf(locales);
        this.bundles = Map.copyOf(bundles);
    }

    /** Loads the bundled catalog; malformed or missing resources cause IllegalArgumentException. */
    public static Localizations load() {
        return load(Localizations.class.getClassLoader());
    }

    /**
     * Loads UTF-8 resources from a supplied application resource loader.
     * @throws IllegalArgumentException for null loaders, invalid configuration,
     *     or incomplete messages
     */
    public static Localizations load(ClassLoader loader) {
        if (loader == null) {
            throw new IllegalArgumentException("A resource loader is required.");
        }
        Properties config = read(loader, "ui/languages.properties");
        List<String> languages = new ArrayList<>();
        Map<String, Locale> locales = new LinkedHashMap<>();
        Map<String, Properties> bundles = new LinkedHashMap<>();
        Set<String> expectedConfig = new HashSet<>(Set.of("languages", "default"));
        for (String entry : required(config, "languages").split(",", -1)) {
            String language = entry.trim();
            Locale locale = parseLocale(language);
            if (locale.getLanguage().isEmpty() || locales.containsValue(locale)) {
                throw new IllegalArgumentException("Empty or duplicate language: " + language);
            }
            languages.add(language);
            locales.put(language, locale);
            String bundleKey = "bundle." + language;
            expectedConfig.add(bundleKey);
            bundles.put(language, read(loader, required(config, bundleKey)));
        }
        String defaultLanguage = required(config, "default");
        if (!bundles.containsKey(defaultLanguage)
                || !config.stringPropertyNames().equals(expectedConfig)) {
            throw new IllegalArgumentException(
                "The default and bundle entries must match configured languages.");
        }
        validateTranslations(languages, defaultLanguage, locales, bundles);
        return new Localizations(languages, defaultLanguage, locales, bundles);
    }

    private static Locale parseLocale(String language) {
        try {
            return new Locale.Builder().setLanguageTag(language).build();
        } catch (java.util.IllformedLocaleException invalid) {
            throw new IllegalArgumentException("Invalid language tag: " + language, invalid);
        }
    }

    private static void validateTranslations(List<String> languages, String defaultLanguage,
            Map<String, Locale> locales, Map<String, Properties> bundles) {
        Properties reference = bundles.get(defaultLanguage);
        required(reference, "languageName");
        for (String language : languages) {
            Properties messages = bundles.get(language);
            if (!messages.stringPropertyNames().equals(reference.stringPropertyNames())) {
                throw new IllegalArgumentException("Translation keys differ for " + language);
            }
            validateMessageArguments(reference, locales.get(defaultLanguage), messages,
                    locales.get(language), language);
        }
    }

    private static void validateMessageArguments(Properties reference, Locale referenceLocale,
            Properties messages, Locale locale, String language) {
        for (String key : reference.stringPropertyNames()) {
            Map<Integer, Set<String>> expected = argumentTypes(required(reference, key),
                    referenceLocale);
            Map<Integer, Set<String>> actual = argumentTypes(required(messages, key), locale);
            if (!expected.equals(actual)) {
                throw new IllegalArgumentException(
                        "Incompatible message arguments: " + key + " in " + language);
            }
        }
    }

    /** Configured language tags in selector order; the returned list is immutable. */
    public List<String> languages() {
        return languages;
    }

    /** Explicit startup language from configuration, independent of the host's language. */
    public String defaultLanguage() {
        return defaultLanguage;
    }

    /** Native display name for a configured language. */
    public String languageName(String language) {
        return bundle(language).getProperty("languageName");
    }

    /** Locale used to format a configured language; unsupported tags are rejected. */
    public Locale locale(String language) {
        bundle(language);
        return locales.get(language);
    }

    /**
     * Formats one message with its selected locale, for setup and future result consumers.
     * @throws IllegalArgumentException for unknown languages/keys or incorrect message arguments
     */
    public String text(String language, String key, Object... arguments) {
        Properties messages = bundle(language);
        if (key == null || !messages.containsKey(key)) {
            throw new IllegalArgumentException("Unknown message key: " + key);
        }
        String pattern = messages.getProperty(key);
        Locale locale = locales.get(language);
        int argumentCount = 0;
        for (int index : argumentTypes(pattern, locale).keySet()) {
            argumentCount = Math.max(argumentCount, index + 1);
        }
        MessageFormat format = new MessageFormat(pattern, locale);
        if (arguments == null || arguments.length != argumentCount) {
            throw new IllegalArgumentException("Incorrect argument count for " + key);
        }
        return format.format(arguments);
    }

    private Properties bundle(String language) {
        if (language == null || !bundles.containsKey(language)) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        return bundles.get(language);
    }

    private static Properties read(ClassLoader loader, String path) {
        try (InputStream input = loader.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing localization resource: " + path);
            }
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                Properties properties = new Properties();
                properties.load(reader);
                return properties;
            }
        } catch (IOException failure) {
            throw new IllegalArgumentException(
                "Cannot read localization resource: " + path, failure);
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing or blank localization value: " + key);
        }
        return value;
    }

    private static Map<Integer, Set<String>> argumentTypes(String pattern, Locale locale) {
        Map<Integer, Set<String>> types = new LinkedHashMap<>();
        collectArgumentTypes(pattern, locale, types);
        return types;
    }

    private static void collectArgumentTypes(String pattern, Locale locale,
            Map<Integer, Set<String>> types) {
        Format[] formats = new MessageFormat(pattern, locale).getFormats();
        List<String> elements = formatElements(pattern);
        for (int occurrence = 0; occurrence < elements.size(); occurrence++) {
            recordArgumentType(elements.get(occurrence), types);
            collectChoiceArgumentTypes(formats[occurrence], locale, types);
        }
    }

    /** Quoted braces are literals; doubled apostrophes toggle the quote state twice. */
    private static List<String> formatElements(String pattern) {
        List<String> elements = new ArrayList<>();
        boolean quoted = false;
        int depth = 0;
        int start = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char character = pattern.charAt(i);
            if (character == '\'') {
                quoted = !quoted;
                continue;
            }
            if (quoted) {
                continue;
            }
            if (character == '{') {
                if (depth == 0) {
                    start = i + 1;
                }
                depth++;
            } else if (character == '}' && depth > 0) {
                depth--;
                if (depth == 0) {
                    elements.add(pattern.substring(start, i));
                }
            }
        }
        return elements;
    }

    private static void recordArgumentType(String element, Map<Integer, Set<String>> types) {
        String[] parts = element.split(",", 3);
        int index = Integer.parseInt(parts[0]);
        String type = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "";
        types.computeIfAbsent(index, ignored -> new HashSet<>()).add(type);
    }

    private static void collectChoiceArgumentTypes(Format format, Locale locale,
            Map<Integer, Set<String>> types) {
        if (!(format instanceof ChoiceFormat)) {
            return;
        }
        for (Object branch : ((ChoiceFormat) format).getFormats()) {
            String nested = (String) branch;
            // MessageFormat reparses a choice output only if it contains an opening brace.
            if (nested.indexOf('{') >= 0) {
                collectArgumentTypes(nested, locale, types);
            }
        }
    }
}
