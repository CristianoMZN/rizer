package br.com.rizermarketplaces.core.marketplace.tools;

public final class CnpjValidator {

    private CnpjValidator() {}

    /** Valida CNPJ (com ou sem máscara). Retorna true se for estruturalmente válido. */
    public static boolean isValid(String cnpj) {
        if (cnpj == null) return false;
        String digits = cnpj.replaceAll("\\D", "");
        if (digits.length() != 14) return false;
        if (digits.chars().distinct().count() == 1) return false;

        int dv1 = calculateDigit(digits, 12);
        int dv2 = calculateDigit(digits, 13);
        return digits.charAt(11) - '0' == dv1 && digits.charAt(12) - '0' == dv2;
    }

    private static int calculateDigit(String digits, int length) {
        int[] weights = length == 12
            ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
            : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }

    public static String format(String cnpj) {
        String d = cnpj == null ? "" : cnpj.replaceAll("\\D", "");
        if (d.length() != 14) return cnpj == null ? "" : cnpj;
        return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "/" +
               d.substring(8, 12) + "-" + d.substring(12, 14);
    }
}
