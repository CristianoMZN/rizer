package br.com.rizermarketplaces.core.marketplace.tools;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugGenerator {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern DASHES = Pattern.compile("-+");

    private SlugGenerator() {}

    public static String from(String input) {
        if (input == null) return "";
        String s = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
            .toLowerCase(Locale.ROOT)
            .trim();
        s = WHITESPACE.matcher(s).replaceAll("-");
        s = NON_LATIN.matcher(s).replaceAll("");
        s = DASHES.matcher(s).replaceAll("-");
        s = s.replaceAll("^-|-$", "");
        if (s.length() > 80) s = s.substring(0, 80).replaceAll("-$", "");
        return s;
    }
}
