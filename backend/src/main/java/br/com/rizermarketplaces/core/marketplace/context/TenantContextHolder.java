package br.com.rizermarketplaces.core.marketplace.context;

import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import java.util.UUID;

/**
 * Contexto de tenant da requisição atual (ThreadLocal).
 * Populado por filtros na ordem:
 *  1. JWT claim "tenantId" (escolhido no login)
 *  2. Header X-Tenant-Slug (chamadas SSR / internas)
 *  3. Path variable /{countryCode}/public/tenants/{slug}
 */
public final class TenantContextHolder {

    private static final ThreadLocal<TenantRef> CURRENT = new ThreadLocal<>();

    public record TenantRef(UUID id, String slug, String countryCode) {}

    private TenantContextHolder() {}

    public static void set(UUID id, String slug, String countryCode) {
        CURRENT.set(new TenantRef(id, slug, countryCode));
    }

    public static TenantRef get() {
        return CURRENT.get();
    }

    public static UUID getId() {
        TenantRef ref = CURRENT.get();
        return ref != null ? ref.id() : null;
    }

    public static String getSlug() {
        TenantRef ref = CURRENT.get();
        return ref != null ? ref.slug() : null;
    }

    public static UUID requireId() {
        UUID id = getId();
        if (id == null) throw TenantExceptions.forbidden("Selecione um tenant antes de continuar");
        return id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
