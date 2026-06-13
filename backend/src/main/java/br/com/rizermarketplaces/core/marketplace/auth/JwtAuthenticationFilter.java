package br.com.rizermarketplaces.core.marketplace.auth;

import br.com.rizermarketplaces.core.marketplace.context.TenantContextHolder;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "motorise_access";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && jwtTokenProvider.isValid(token)) {
            try {
                Claims claims = jwtTokenProvider.parse(token);
                String typ = claims.get("typ", String.class);
                if ("access".equals(typ)) {
                    UUID userId = UUID.fromString(claims.getSubject());
                    String email = claims.get("email", String.class);
                    String role = claims.get("role", String.class);
                    String tenantIdClaim = claims.get("tenantId", String.class);

                    AuthenticatedUser user = new AuthenticatedUser(
                        userId, email, email, parseRole(role), true
                    );
                    if (tenantIdClaim != null && !tenantIdClaim.isBlank()) {
                        UUID tid = UUID.fromString(tenantIdClaim);
                        user.setCurrentTenantId(tid);
                        if (TenantContextHolder.get() == null) {
                            TenantContextHolder.set(tid, null, null);
                        }
                    }

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities()
                    );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (COOKIE_NAME.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    private br.com.rizermarketplaces.core.marketplace.model.SystemRole parseRole(String role) {
        if (role == null) return br.com.rizermarketplaces.core.marketplace.model.SystemRole.user;
        try {
            return br.com.rizermarketplaces.core.marketplace.model.SystemRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            return br.com.rizermarketplaces.core.marketplace.model.SystemRole.user;
        }
    }
}
