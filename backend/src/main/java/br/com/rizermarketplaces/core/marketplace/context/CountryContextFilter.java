package br.com.rizermarketplaces.core.marketplace.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Popula CountryContextHolder a partir de /{countryCode}/...
 * ou do header X-Country-Code. Fallback: "BR".
 */
@Component
@Order(10)
public class CountryContextFilter extends OncePerRequestFilter {

    private static final Pattern COUNTRY_PATH = Pattern.compile("^/?([A-Za-z]{2})(/.*)?$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("X-Country-Code");
            if (header != null && !header.isBlank()) {
                CountryContextHolder.set(header);
            } else {
                String uri = request.getRequestURI();
                if (uri != null && !uri.startsWith("/docs") && !uri.startsWith("/openapi")) {
                    Matcher m = COUNTRY_PATH.matcher(uri);
                    if (m.matches() && !isReserved(m.group(1))) {
                        CountryContextHolder.set(m.group(1));
                    }
                }
            }
            chain.doFilter(request, response);
        } finally {
            CountryContextHolder.clear();
        }
    }

    private boolean isReserved(String code) {
        // Evita capturar /auth, /media, /admin, /tenant, /billing, /legal, /oauth2, /login como países.
        return switch (code.toLowerCase()) {
            case "auth", "media", "admin", "tenant", "billing", "legal", "oauth2", "login", "favicon.ico" -> true;
            default -> false;
        };
    }
}
