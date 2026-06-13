package br.com.rizermarketplaces.core.marketplace.billing;

import br.com.rizermarketplaces.core.marketplace.model.Payment;
import br.com.rizermarketplaces.core.marketplace.model.PaymentMethod;
import br.com.rizermarketplaces.core.marketplace.model.PaymentStatus;
import br.com.rizermarketplaces.core.marketplace.model.Plan;
import br.com.rizermarketplaces.core.marketplace.model.StripeInvoice;
import br.com.rizermarketplaces.core.marketplace.model.Subscription;
import br.com.rizermarketplaces.core.marketplace.model.SubscriptionStatus;
import br.com.rizermarketplaces.core.marketplace.repository.PaymentRepository;
import br.com.rizermarketplaces.core.marketplace.repository.StripeInvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Processa webhooks do Stripe (ou simulados em DEV).
 * Em prod: valida a assinatura com stripe.webhook.constructEvent
 * Em dev: confia no payload recebido do BillingWebhookController.
 */
@Service
public class BillingWebhookService {

    private static final Logger log = LoggerFactory.getLogger(BillingWebhookService.class);

    private final SubscriptionService subscriptionService;
    private final PaymentRepository paymentRepository;
    private final StripeInvoiceRepository stripeInvoiceRepository;
    private final StripeService stripeService;

    public BillingWebhookService(
        SubscriptionService subscriptionService,
        PaymentRepository paymentRepository,
        StripeInvoiceRepository stripeInvoiceRepository,
        StripeService stripeService
    ) {
        this.subscriptionService = subscriptionService;
        this.paymentRepository = paymentRepository;
        this.stripeInvoiceRepository = stripeInvoiceRepository;
        this.stripeService = stripeService;
    }

    @Transactional
    public void handle(Map<String, Object> event) {
        String type = (String) event.get("type");
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        Map<String, Object> obj = data == null ? null : (Map<String, Object>) data.get("object");
        if (type == null || obj == null) {
            log.warn("[stripe-webhook] evento sem type ou data.object: {}", event);
            return;
        }
        switch (type) {
            case "checkout.session.completed" -> onCheckoutCompleted(obj);
            case "invoice.paid" -> onInvoicePaid(obj);
            case "invoice.payment_failed" -> onInvoiceFailed(obj);
            case "customer.subscription.updated" -> onSubscriptionUpdated(obj);
            case "customer.subscription.deleted" -> onSubscriptionDeleted(obj);
            case "customer.subscription.trial_will_end" -> log.info("[stripe-webhook] trial_will_end {}", obj);
            default -> log.debug("[stripe-webhook] evento não tratado: {}", type);
        }
    }

    private void onCheckoutCompleted(Map<String, Object> obj) {
        String sessionId = (String) obj.get("id");
        String customerId = (String) obj.get("customer");
        String subscriptionId = (String) obj.get("subscription");
        Map<String, Object> metadata = (Map<String, Object>) obj.get("metadata");
        String tenantIdStr = metadata == null ? null : (String) metadata.get("tenant_id");
        String planCode = metadata == null ? null : (String) metadata.get("plan_code");
        if (tenantIdStr == null || planCode == null) {
            log.warn("[stripe-webhook] checkout.session.completed sem metadata: {}", obj);
            return;
        }
        UUID tenantId = UUID.fromString(tenantIdStr);
        Subscription sub = stripeService.applyCheckoutCompleted(tenantId, planCode, sessionId, customerId);
        Long amount = numberOrNull(obj.get("amount_total"));
        createOrAttachPayment(sub, PaymentMethod.stripe_card, PaymentStatus.succeeded,
            amount, "BRL", subscriptionId, obj);
    }

    private void onInvoicePaid(Map<String, Object> obj) {
        String invoiceId = (String) obj.get("id");
        String customerId = (String) obj.get("customer");
        String subscriptionId = (String) obj.get("subscription");
        Long amountPaid = numberOrNull(obj.get("amount_paid"));
        String currency = ((String) obj.get("currency")).toUpperCase();
        Long periodStart = numberOrNull(obj.get("period_start"));
        Long periodEnd = numberOrNull(obj.get("period_end"));
        Subscription sub = subscriptionService.getEntityByStripeId(subscriptionId).orElse(null);
        if (sub == null) {
            log.warn("[stripe-webhook] invoice.paid sem subscription local: {}", obj);
            return;
        }
        Payment payment = new Payment();
        payment.setTenantId(sub.getTenantId());
        payment.setSubscriptionId(sub.getId());
        payment.setMethod(PaymentMethod.stripe_card);
        payment.setStatus(PaymentStatus.succeeded);
        payment.setAmountCents(amountPaid != null ? amountPaid : 0L);
        payment.setCurrency(currency);
        if (periodStart != null) payment.setPeriodStart(java.time.Instant.ofEpochSecond(periodStart != null ? periodStart : 0).atOffset(java.time.ZoneOffset.UTC));
        if (periodEnd != null) payment.setPeriodEnd(java.time.Instant.ofEpochSecond(periodEnd != null ? periodEnd : 0).atOffset(java.time.ZoneOffset.UTC));
        payment.setDescription("Fatura Stripe " + invoiceId);
        payment.setExternalReference(invoiceId);
        payment.setPaidAt(OffsetDateTime.now());
        payment = paymentRepository.save(payment);

        StripeInvoice si = new StripeInvoice();
        si.setPaymentId(payment.getId());
        si.setStripeInvoiceId(invoiceId);
        si.setStripeChargeId((String) obj.get("charge"));
        si.setHostedInvoiceUrl((String) obj.get("hosted_invoice_url"));
        si.setInvoicePdf((String) obj.get("invoice_pdf"));
        si.setAmountDueCents(numberOrNull(obj.get("amount_due")));
        si.setAmountPaidCents(amountPaid);
        si.setRawPayload(obj);
        stripeInvoiceRepository.save(si);

        // Renova período e volta a active se estava past_due
        if (periodStart != null && periodEnd != null) {
            subscriptionService.renewPeriod(sub.getTenantId(),
                java.time.Instant.ofEpochSecond(periodStart != null ? periodStart : 0).atOffset(java.time.ZoneOffset.UTC),
                java.time.Instant.ofEpochSecond(periodEnd != null ? periodEnd : 0).atOffset(java.time.ZoneOffset.UTC));
        }
    }

    private void onInvoiceFailed(Map<String, Object> obj) {
        String subscriptionId = (String) obj.get("subscription");
        Subscription sub = subscriptionService.getEntityByStripeId(subscriptionId).orElse(null);
        if (sub == null) return;
        sub.setStatus(SubscriptionStatus.past_due);
        subscriptionService.updateEntity(sub);
    }

    private void onSubscriptionUpdated(Map<String, Object> obj) {
        String id = (String) obj.get("id");
        String status = (String) obj.get("status");
        Boolean cancelAtPeriodEnd = (Boolean) obj.get("cancel_at_period_end");
        Subscription sub = subscriptionService.getEntityByStripeId(id).orElse(null);
        if (sub == null) return;
        if (status != null) {
            sub.setStatus(mapStripeStatus(status, sub.getStatus()));
        }
        if (cancelAtPeriodEnd != null) {
            sub.setCancelAtPeriodEnd(cancelAtPeriodEnd);
        }
        subscriptionService.updateEntity(sub);
    }

    private void onSubscriptionDeleted(Map<String, Object> obj) {
        String id = (String) obj.get("id");
        Subscription sub = subscriptionService.getEntityByStripeId(id).orElse(null);
        if (sub == null) return;
        sub.setStatus(SubscriptionStatus.canceled);
        sub.setCanceledAt(OffsetDateTime.now());
        subscriptionService.updateEntity(sub);
    }

    private void createOrAttachPayment(Subscription sub, PaymentMethod method, PaymentStatus status,
                                       Long amountCents, String currency, String externalRef,
                                       Map<String, Object> rawPayload) {
        Payment p = new Payment();
        p.setTenantId(sub.getTenantId());
        p.setSubscriptionId(sub.getId());
        p.setMethod(method);
        p.setStatus(status);
        p.setAmountCents(amountCents != null ? amountCents : 0L);
        p.setCurrency(currency != null ? currency : sub.getCurrency());
        p.setDescription("Checkout Stripe " + sub.getStripeSubscriptionId());
        p.setExternalReference(externalRef);
        p.setPaidAt(OffsetDateTime.now());
        paymentRepository.save(p);
    }

    private SubscriptionStatus mapStripeStatus(String stripeStatus, SubscriptionStatus current) {
        return switch (stripeStatus) {
            case "active" -> SubscriptionStatus.active;
            case "trialing" -> SubscriptionStatus.trialing;
            case "past_due" -> SubscriptionStatus.past_due;
            case "canceled" -> SubscriptionStatus.canceled;
            case "unpaid" -> SubscriptionStatus.unpaid;
            case "incomplete" -> SubscriptionStatus.incomplete;
            case "incomplete_expired" -> SubscriptionStatus.incomplete_expired;
            case "paused" -> SubscriptionStatus.paused;
            default -> current;
        };
    }

    private Long numberOrNull(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }
}
