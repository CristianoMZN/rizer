package br.com.rizermarketplaces.core.marketplace.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

// @Component: registra este filtro como componente Spring
// @Order(Ordered.HIGHEST_PRECEDENCE): garante que este filtro execute cedo na cadeia de filtros
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RegionContextFilter extends OncePerRequestFilter {

    // Regex para aceitar segmentos de rota com exatamente 2 letras (ex: BR, US)
    private static final Pattern COUNTRY_PATTERN = Pattern.compile("^[A-Za-z]{2}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        try {
            // Extrai o possível countryCode do começo da URI e seta no RegionContextHolder
            String countryCode = extractCountryFromPath(request.getRequestURI());
            if (countryCode != null) {
                RegionContextHolder.setCountryCode(countryCode.toUpperCase());
            }
            // Continua a cadeia de filtros / processamento da requisição
            filterChain.doFilter(request, response);
        } finally {
            // Garante remoção do contexto para evitar vazamento entre requisições
            RegionContextHolder.clear();
        }
    }

    // Busca o primeiro segmento não vazio da URI e verifica se é um country code válido
    private String extractCountryFromPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        String[] segments = path.split("/");
        for (String segment : segments) {
            if (!segment.isBlank()) {
                return COUNTRY_PATTERN.matcher(segment).matches() ? segment : null;
            }
        }

        return null;
    }
}
