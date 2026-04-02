package br.com.rizermarketplaces.core.marketplace.context;

/*
 * Holder de contexto de região baseado em ThreadLocal.
 *
 * ThreadLocal permite armazenar dados por thread (por requisição, quando cada requisição é tratada em uma thread),
 * útil para compartilhar contexto (ex: código do país) entre filtros e componentes sem passar como parâmetro.
 */
public final class RegionContextHolder {

    // Armazena o código do país (ISO-2) para a thread atual.
    private static final ThreadLocal<String> COUNTRY_CODE = new ThreadLocal<>();

    private RegionContextHolder() {
    }

    // Define o countryCode para a thread atual
    public static void setCountryCode(String countryCode) {
        COUNTRY_CODE.set(countryCode);
    }

    // Retorna o countryCode armazenado para a thread atual
    public static String getCountryCode() {
        return COUNTRY_CODE.get();
    }

    // Remove o valor quando o processamento terminar para evitar vazamento de memória entre requisições
    public static void clear() {
        COUNTRY_CODE.remove();
    }
}
