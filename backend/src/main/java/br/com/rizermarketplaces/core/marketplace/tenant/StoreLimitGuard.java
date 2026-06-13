package br.com.rizermarketplaces.core.marketplace.tenant;

import br.com.rizermarketplaces.core.marketplace.billing.PlanService;
import br.com.rizermarketplaces.core.marketplace.repository.PhysicalStoreRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Guard que enforça o limite de lojas físicas por tenant de acordo
 * com o plano (lido pelo PlanService).
 */
@Service
public class StoreLimitGuard {

    private final PhysicalStoreRepository physicalStoreRepository;
    private final PlanService planService;

    public StoreLimitGuard(PhysicalStoreRepository physicalStoreRepository, PlanService planService) {
        this.physicalStoreRepository = physicalStoreRepository;
        this.planService = planService;
    }

    public void assertCanCreate(UUID tenantId) {
        long active = physicalStoreRepository.countByTenantIdAndIsActiveTrueAndDeletedAtIsNull(tenantId);
        planService.requireStoreSlot(tenantId, active);
    }

    public long currentActive(UUID tenantId) {
        return physicalStoreRepository.countByTenantIdAndIsActiveTrueAndDeletedAtIsNull(tenantId);
    }
}
