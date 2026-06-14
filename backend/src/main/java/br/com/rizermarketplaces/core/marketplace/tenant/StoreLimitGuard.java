package br.com.rizermarketplaces.core.marketplace.tenant;

import br.com.rizermarketplaces.core.marketplace.billing.PlanService;
import br.com.rizermarketplaces.core.marketplace.billing.SubscriptionService;
import br.com.rizermarketplaces.core.marketplace.model.Plan;
import br.com.rizermarketplaces.core.marketplace.repository.PhysicalStoreRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Enforça o limite de lojas físicas por tenant de acordo com a subscription.
 * Se o tenant não tem subscription, libera 1 loja (fallback BASIC).
 */
@Service
public class StoreLimitGuard {

    private final PhysicalStoreRepository physicalStoreRepository;
    private final PlanService planService;
    private final SubscriptionService subscriptionService;

    public StoreLimitGuard(
        PhysicalStoreRepository physicalStoreRepository,
        PlanService planService,
        SubscriptionService subscriptionService
    ) {
        this.physicalStoreRepository = physicalStoreRepository;
        this.planService = planService;
        this.subscriptionService = subscriptionService;
    }

    public void assertCanCreate(UUID tenantId) {
        long active = physicalStoreRepository.countByTenantIdAndIsActiveTrueAndDeletedAtIsNull(tenantId);
        var sub = subscriptionService.getEntity(tenantId).orElse(null);
        Plan plan = sub != null
            ? planService.get(sub.getPlanCode())
            : planService.get("BASIC");
        planService.requireStoreSlot(plan, active);
    }

}
