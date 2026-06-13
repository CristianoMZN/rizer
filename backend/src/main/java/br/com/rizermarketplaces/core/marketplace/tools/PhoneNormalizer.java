package br.com.rizermarketplaces.core.marketplace.tools;

public final class PhoneNormalizer {

    private PhoneNormalizer() {}

    public static String onlyDigits(String input) {
        return input == null ? "" : input.replaceAll("\\D", "");
    }
}
