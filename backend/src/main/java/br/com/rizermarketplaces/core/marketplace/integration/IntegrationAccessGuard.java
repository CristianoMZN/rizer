package br.com.rizermarketplaces.core.marketplace.integration;

import br.com.rizermarketplaces.core.marketplace.billing.PlanService;
import br.com.rizermarketplaces.core.marketplace.billing.SubscriptionService;
import br.com.rizermarketplaces.core.marketplace.billing.SubscriptionStateMachine;
import br.com.rizermarketplaces.core.marketplace.model.IntegrationProvider;
import br.com.rizermarketplaces.core.marketplace.model.Plan;
import br.com.rizermarketplaces.core.marketplace.model.Subscription;
import br.com.rizermarketplaces.core.marketplace.model.TenantIntegration;
import br.com.rizermarketplaces.core.marketplace.repository.TenantIntegrationRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Garante que o tenant tem plano + integration conectada antes de
 * acionar Meta/Google.
 */
@Service
public class IntegrationAccessGuard {

    private final SubscriptionService subscriptionService;
    private final PlanService planService;
    private final TenantIntegrationRepository integrationRepository;
    private final SubscriptionStateMachine stateMachine;

    public IntegrationAccessGuard(
        SubscriptionService subscriptionService,
        PlanService planService,
        TenantIntegrationRepository integrationRepository,
        SubscriptionStateMachine stateMachine
    ) {
        this.subscriptionService = subscriptionService;
        this.planService = planService;
        this.integrationRepository = integrationRepository;
        this.stateMachine = stateMachine;
    }

    public Subscription requireActiveSubscription(UUID tenantId) {
        Subscription sub = subscriptionService.getEntity(tenantId)
            .orElseThrow(() -> TenantExceptions.paymentRequired("Sem assinatura ativa"));
        if (!stateMachine.isActiveLike(sub)) {
            throw TenantExceptions.paymentRequired("Assinatura inativa — regularize para usar integrações.");
        }
        return sub;
    }

    public void requirePlanFeature(UUID tenantId, IntegrationProvider provider) {
        Subscription sub = requireActiveSubscription(tenantId);
        Plan plan = planService.get(sub.getPlanCode());
        switch (provider) {
            case INSTAGRAM -> stateMachine.assertFeatureEnabled(sub, plan, SubscriptionStateMachine.Feature.INSTAGRAM);
            case META_BUSINESS -> stateMachine.assertFeatureEnabled(sub, plan, SubscriptionStateMachine.Feature.META_DPA);
            case GOOGLE_MERCHANT -> stateMachine.assertFeatureEnabled(sub, plan, SubscriptionStateMachine.Feature.GOOGLE_SHOPPING);
        }
    }

    public TenantIntegration requireConnected(UUID tenantId, IntegrationProvider provider) {
        Optional<TenantIntegration> integration = integrationRepository
            .findByTenantIdAndProvider(tenantId, provider);
        TenantIntegration i = integration.orElseThrow(() -> TenantExceptions
            .badRequest("Conecte a integração " + provider + " antes"));
        if (i.getStatus() != br.com.rizermarketplaces.core.marketplace.model.IntegrationStatus.CONNECTED) {
            throw TenantExceptions.badRequest("Integração " + provider + " não está conectada");
        }
        return i;
    }
}
