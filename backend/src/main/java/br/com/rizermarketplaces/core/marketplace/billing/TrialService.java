package br.com.rizermarketplaces.core.marketplace.billing;

import br.com.rizermarketplaces.core.marketplace.model.Plan;
import br.com.rizermarketplaces.core.marketplace.model.Subscription;
import br.com.rizermarketplaces.core.marketplace.model.SubscriptionSource;
import br.com.rizermarketplaces.core.marketplace.model.SubscriptionStatus;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.repository.SubscriptionRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Trial é por tenant, uma única vez (tenants.had_trial).
 * Ao expirar (TrialExpirationJob) a subscription vai para canceled.
 */
@Service
public class TrialService {

    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanService planService;
    private final SubscriptionService subscriptionService;

    public TrialService(
        TenantRepository tenantRepository,
        SubscriptionRepository subscriptionRepository,
        PlanService planService,
        SubscriptionService subscriptionService
    ) {
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planService = planService;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public Subscription startTrial(UUID tenantId, String planCode) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Tenant"));
        if (tenant.isHadTrial()) {
            throw TenantExceptions.conflict("Este tenant já utilizou o período de trial.");
        }
        Plan plan = planService.get(planCode);
        if (plan.getTrialDays() <= 0) {
            throw TenantExceptions.badRequest("Plano " + plan.getCode() + " não oferece trial.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime end = now.plus(plan.getTrialDays(), ChronoUnit.DAYS);
        Subscription sub = subscriptionService.ensureSubscription(
            tenantId, plan.getCode(), SubscriptionSource.trial,
            now, end, SubscriptionStatus.trialing
        );
        tenant.setHadTrial(true);
        tenantRepository.save(tenant);
        return sub;
    }
}
