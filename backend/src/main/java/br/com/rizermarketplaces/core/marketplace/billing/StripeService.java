package br.com.rizermarketplaces.core.marketplace.billing;

import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.CheckoutResponse;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.PortalResponse;
import br.com.rizermarketplaces.core.marketplace.model.Plan;
import br.com.rizermarketplaces.core.marketplace.model.Subscription;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Integração com Stripe.
 *
 * Modo 1 (default, dev): quando `app.stripe.enabled=false`, gera URLs
 * mockadas para o frontend simular o fluxo (assinatura ativada via
 * `BillingWebhookController.simulate`).
 *
 * Modo 2 (prod): quando `app.stripe.enabled=true`, o `stripe-java` será
 * plugado e os métodos `createCustomer`, `createCheckoutSession` e
 * `createBillingPortal` farão chamadas reais à API.
 *
 * TODO(fase-5-prod): plugar stripe-java SDK, trocar implementações.
 */
@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    private final TenantRepository tenantRepository;
    private final PlanService planService;
    private final SubscriptionService subscriptionService;

    @Value("${app.stripe.enabled:false}")
    private boolean enabled;

    @Value("${app.stripe.api-key:}")
    private String apiKey;

    @Value("${app.stripe.success-url:http://localhost:3000/app/assinatura/checkout}")
    private String successUrl;

    @Value("${app.stripe.cancel-url:http://localhost:3000/app/assinatura}")
    private String cancelUrl;

    public StripeService(
        TenantRepository tenantRepository,
        PlanService planService,
        SubscriptionService subscriptionService
    ) {
        this.tenantRepository = tenantRepository;
        this.planService = planService;
        this.subscriptionService = subscriptionService;
    }

    public boolean isEnabled() { return enabled; }

    @Transactional
    public CheckoutResponse createCheckoutSession(UUID tenantId, String planCode) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Tenant"));
        Plan plan = planService.get(planCode);

        if (enabled) {
            // TODO(fase-5-prod): implementar com stripe-java
            throw TenantExceptions.badRequest("Stripe ainda não está ativado nesta build");
        }

        // Modo dev: gera uma URL mockada que o frontend pode usar para simular o webhook.
        String sessionId = "cs_dev_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String url = successUrl + "?session_id=" + sessionId + "&plan=" + planCode
            + "&tenant=" + tenant.getSlug() + "&simulated=1";
        log.warn("[stripe-mock] Checkout session criado (DEV) tenant={} plan={} sessionId={}",
            tenant.getId(), planCode, sessionId);
        return new CheckoutResponse(url, sessionId, true);
    }

    @Transactional
    public PortalResponse createBillingPortal(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Tenant"));
        if (enabled) {
            throw TenantExceptions.badRequest("Stripe ainda não está ativado nesta build");
        }
        String url = "http://localhost:3000/app/assinatura?portal=dev&tenant=" + tenant.getSlug();
        return new PortalResponse(url);
    }

    /**
     * Chamado pelo BillingWebhookService quando o webhook simulado (ou real)
     * confirma o checkout.
     */
    @Transactional
    public Subscription applyCheckoutCompleted(
        UUID tenantId, String planCode, String sessionId, String customerId
    ) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Tenant"));
        Plan plan = planService.get(planCode);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime end = now.plus(30, ChronoUnit.DAYS);
        Subscription sub = subscriptionService.ensureSubscription(
            tenant.getId(), plan.getCode(),
            br.com.rizermarketplaces.core.marketplace.model.SubscriptionSource.stripe,
            now, end,
            br.com.rizermarketplaces.core.marketplace.model.SubscriptionStatus.active
        );
        if (customerId != null) {
            sub.setStripeCustomerId(customerId);
        }
        if (sessionId != null) {
            sub.setStripeSubscriptionId(sessionId);
        }
        return sub;
    }
}
