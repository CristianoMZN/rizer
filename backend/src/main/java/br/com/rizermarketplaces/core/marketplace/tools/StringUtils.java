package br.com.rizermarketplaces.core.marketplace.tools;

public final class StringUtils {

    private StringUtils() {}

    public static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
    }
}
