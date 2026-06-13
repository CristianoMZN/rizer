package br.com.rizermarketplaces.core.marketplace.context;

import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolve o tenant atual a partir de (em ordem):
 *  1. Header X-Tenant-Id (UUID)
 *  2. Header X-Tenant-Slug (resolve por slug + country do context)
 *  3. Path /{countryCode}/public/tenants/{slug}
 *  4. Host header (slug.motorise.com.br ou custom domain via CNAME)
 *
 * Não exige autenticação. Para requests em que o tenant é apenas
 * referencial (ex.: página pública de parceiro), funciona sem JWT.
 */
@Component
@Order(20)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Pattern PUBLIC_TENANT_PATH = Pattern.compile(
        "^/?([A-Za-z]{2})/public/tenants/([A-Za-z0-9\\-_.]+)(/.*)?$"
    );

    private final TenantRepository tenantRepository;

    public TenantContextFilter(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String tenantIdHeader = request.getHeader("X-Tenant-Id");
            String tenantSlugHeader = request.getHeader("X-Tenant-Slug");
            String countryCode = CountryContextHolder.get();

            if (tenantIdHeader != null && !tenantIdHeader.isBlank()) {
                try {
                    UUID id = UUID.fromString(tenantIdHeader);
                    tenantRepository.findById(id).ifPresent(t ->
                        TenantContextHolder.set(t.getId(), t.getSlug(), t.getCountryCode())
                    );
                } catch (IllegalArgumentException ignored) { /* header inválido */ }
            } else if (tenantSlugHeader != null && !tenantSlugHeader.isBlank()) {
                tenantRepository.findBySlugAndCountryCodeAndDeletedAtIsNull(tenantSlugHeader, countryCode)
                    .ifPresent(t -> TenantContextHolder.set(t.getId(), t.getSlug(), t.getCountryCode()));
            } else {
                Matcher m = PUBLIC_TENANT_PATH.matcher(request.getRequestURI());
                if (m.matches()) {
                    String cc = m.group(1).toUpperCase();
                    String slug = m.group(2);
                    CountryContextHolder.set(cc);
                    tenantRepository.findBySlugAndCountryCodeAndDeletedAtIsNull(slug, cc)
                        .ifPresent(t -> TenantContextHolder.set(t.getId(), t.getSlug(), cc));
                } else {
                    String host = request.getHeader("Host");
                    if (host != null) {
                        String hostOnly = host.split(":")[0].toLowerCase();
                        tenantRepository.findByCustomDomainIgnoreCaseAndDeletedAtIsNull(hostOnly)
                            .ifPresent(t -> TenantContextHolder.set(t.getId(), t.getSlug(), t.getCountryCode()));
                    }
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
