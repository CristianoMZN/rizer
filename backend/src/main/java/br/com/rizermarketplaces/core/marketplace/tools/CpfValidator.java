package br.com.rizermarketplaces.core.marketplace.tools;

public final class CpfValidator {

    private CpfValidator() {}

    /**
     * Valida CPF (com ou sem máscara). Algoritmo oficial da Receita Federal:
     *  - 11 dígitos, sem sequências repetidas.
     *  - DV1: soma dos 9 primeiros × pesos [10,9,8,7,6,5,4,3,2]; dv1 = (resto*10) % 11.
     *  - DV2: soma dos 10 primeiros × pesos [11,10,9,8,7,6,5,4,3,2]; dv2 = (resto*10) % 11.
     *  - DV1 ocupa a posição 9 (índice 9, contando de 0).
     *  - DV2 ocupa a posição 10 (índice 10, contando de 0).
     */
    public static boolean isValid(String cpf) {
        if (cpf == null) return false;
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11) return false;
        if (digits.chars().distinct().count() == 1) return false;

        int dv1 = calculateDigit(digits, 9, new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2});
        int dv2 = calculateDigit(digits, 10, new int[]{11, 10, 9, 8, 7, 6, 5, 4, 3, 2});
        return digits.charAt(9) - '0' == dv1 && digits.charAt(10) - '0' == dv2;
    }

    private static int calculateDigit(String digits, int length, int[] weights) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }

    public static String format(String cpf) {
        String d = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (d.length() != 11) return cpf == null ? "" : cpf;
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9, 11);
    }

    public static String normalize(String cpf) {
        if (cpf == null) return null;
        String d = cpf.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }
}
