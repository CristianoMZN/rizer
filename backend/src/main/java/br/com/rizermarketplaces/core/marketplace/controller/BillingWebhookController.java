package br.com.rizermarketplaces.core.marketplace.controller;

import br.com.rizermarketplaces.core.marketplace.billing.BillingWebhookService;
import br.com.rizermarketplaces.core.marketplace.billing.SubscriptionService;
import br.com.rizermarketplaces.core.marketplace.billing.StripeService;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.CheckoutResponse;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.SubscriptionView;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Endpoints de webhook do Stripe + simulador de dev.
 *
 * Em prod: /billing/webhooks/stripe recebe os eventos do Stripe
 * (validados via stripe.webhook.constructEvent).
 *
 * Em dev: /billing/webhooks/simulate aceita um JSON no formato de evento
 * do Stripe para fins de teste E2E.
 */
@RestController
@RequestMapping("/billing/webhooks")
@Tag(name = "Billing · Webhooks", description = "Recebe eventos do Stripe (e simulador de dev)")
public class BillingWebhookController {

    private static final Logger log = LoggerFactory.getLogger(BillingWebhookController.class);

    private final BillingWebhookService webhookService;
    private final StripeService stripeService;
    private final SubscriptionService subscriptionService;

    @Value("${app.stripe.webhook-secret:}")
    private String webhookSecret;

    public BillingWebhookController(
        BillingWebhookService webhookService,
        StripeService stripeService,
        SubscriptionService subscriptionService
    ) {
        this.webhookService = webhookService;
        this.stripeService = stripeService;
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Map<String, String>> stripe(@RequestBody Map<String, Object> event) {
        // TODO(fase-5-prod): validar assinatura com stripe.webhook.constructEvent(payload, sig, secret)
        if (webhookSecret.isBlank()) {
            log.warn("[stripe-webhook] recebido evento SEM verificação de assinatura (webhook-secret não configurado).");
        }
        webhookService.handle(event);
        return ResponseEntity.ok(Map.of("status", "received"));
    }

    /**
     * Simula o checkout completo de um tenant. Útil em dev para
     * acionar manualmente o que o Stripe faria após o pagamento.
     */
    @PostMapping("/simulate")
    public SubscriptionView simulate(@RequestBody SimulateCheckoutRequest body) {
        if (body.tenantId() == null || body.planCode() == null) {
            throw TenantExceptions.badRequest("tenantId e planCode são obrigatórios");
        }
        var sub = stripeService.applyCheckoutCompleted(body.tenantId(), body.planCode(), body.sessionId(), body.customerId());
        // Cria o payment
        webhookService.handle(Map.of(
            "type", "checkout.session.completed",
            "data", Map.of("object", Map.of(
                "id", body.sessionId() != null ? body.sessionId() : "cs_simulated",
                "customer", body.customerId() != null ? body.customerId() : "cus_simulated",
                "subscription", sub.getStripeSubscriptionId(),
                "amount_total", 9900L,
                "metadata", Map.of(
                    "tenant_id", body.tenantId().toString(),
                    "plan_code", body.planCode()
                )
            ))
        ));
        return subscriptionService.getView(body.tenantId());
    }

    public record SimulateCheckoutRequest(
        UUID tenantId,
        String planCode,
        String sessionId,
        String customerId
    ) {}
}
