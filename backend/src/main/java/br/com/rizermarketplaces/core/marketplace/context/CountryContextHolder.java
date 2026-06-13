package br.com.rizermarketplaces.core.marketplace.context;

/**
 * Contexto da requisição atual (ThreadLocal).
 * Populado por filtros a partir de:
 *  1. Path variable /{countryCode}/...
 *  2. Header X-Country-Code
 *  3. Tenant resolvido do subdomínio/domínio custom
 *  4. Fallback: "BR"
 */
public final class CountryContextHolder {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CountryContextHolder() {}

    public static void set(String countryCode) {
        CURRENT.set(countryCode == null ? null : countryCode.toUpperCase());
    }

    public static String get() {
        String c = CURRENT.get();
        return c != null ? c : "BR";
    }

    public static void clear() {
        CURRENT.remove();
    }
}
