package br.com.rizermarketplaces.core.marketplace.billing;

import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Stub do PlanService para a Fase 2. Substituído por leitura real de
 * `plans`/`subscriptions` na Fase 5.
 *
 * Regra atual: PRO = até 3 lojas (configurável). Basicamente um teto fixo
 * por enquanto, suficiente para a Fase 2 demonstrar o guard.
 */
@Service
public class PlanService {

    @Value("${app.tenant.default-max-stores:3}")
    private int defaultMaxStores;

    public int getMaxStoresFor(java.util.UUID tenantId) {
        // TODO(fase-5): ler de subscriptions.plan_code → plans.max_physical_stores
        return defaultMaxStores;
    }

    public boolean isUnlimited(java.util.UUID tenantId) {
        return getMaxStoresFor(tenantId) <= 0;
    }

    public void requireStoreSlot(java.util.UUID tenantId, long currentActive) {
        if (isUnlimited(tenantId)) return;
        int max = getMaxStoresFor(tenantId);
        if (currentActive >= max) {
            throw TenantExceptions.paymentRequired(
                "Limite de " + max + " lojas ativas atingido. Faça upgrade do plano."
            );
        }
    }
}
