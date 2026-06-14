package br.com.rizermarketplaces.core.marketplace.controller.tenant;

import br.com.rizermarketplaces.core.marketplace.auth.CurrentUser;
import br.com.rizermarketplaces.core.marketplace.billing.ManualPaymentService;
import br.com.rizermarketplaces.core.marketplace.billing.PlanService;
import br.com.rizermarketplaces.core.marketplace.billing.StripeService;
import br.com.rizermarketplaces.core.marketplace.billing.SubscriptionService;
import br.com.rizermarketplaces.core.marketplace.context.TenantContextHolder;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.CheckoutResponse;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.ManualPaymentRequest;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.PaymentView;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.PlanView;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.PortalResponse;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.SubscriptionView;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tenant/billing")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Tenant · Assinatura", description = "Plano, pagamentos e upgrade do tenant atual")
public class TenantBillingController {

    private final PlanService planService;
    private final SubscriptionService subscriptionService;
    private final StripeService stripeService;
    private final ManualPaymentService manualPaymentService;

    public TenantBillingController(
        PlanService planService,
        SubscriptionService subscriptionService,
        StripeService stripeService,
        ManualPaymentService manualPaymentService
    ) {
        this.planService = planService;
        this.subscriptionService = subscriptionService;
        this.stripeService = stripeService;
        this.manualPaymentService = manualPaymentService;
    }

    @GetMapping("/plans")
    public List<PlanView> listPlans() {
        return planService.listActive();
    }

    @GetMapping("/subscription")
    public SubscriptionView getSubscription() {
        return subscriptionService.getView(TenantContextHolder.requireId());
    }

    @PostMapping("/checkout/{planCode}")
    public CheckoutResponse checkout(@PathVariable String planCode) {
        return stripeService.createCheckoutSession(TenantContextHolder.requireId(), planCode);
    }

    @PostMapping("/portal")
    public PortalResponse portal() {
        return stripeService.createBillingPortal(TenantContextHolder.requireId());
    }

    @PostMapping("/cancel")
    public SubscriptionView cancel() {
        UUID tenantId = TenantContextHolder.requireId();
        subscriptionService.cancelAtPeriodEnd(tenantId);
        return subscriptionService.getView(tenantId);
    }

    @PostMapping("/resume")
    public SubscriptionView resume() {
        UUID tenantId = TenantContextHolder.requireId();
        subscriptionService.resume(tenantId);
        return subscriptionService.getView(tenantId);
    }

    @PostMapping("/payments")
    public PaymentView recordManualPayment(@RequestBody ManualPaymentRequest req) {
        UUID tenantId = TenantContextHolder.requireId();
        if (req.tenantId() != null && !req.tenantId().equals(tenantId)) {
            throw TenantExceptions.forbidden("Não é possível lançar pagamento em outro tenant");
        }
        var newReq = req.tenantId() == null
            ? new ManualPaymentRequest(tenantId, req.amount(), req.currency(), req.method(),
                req.paidAt(), req.periodStart(), req.periodEnd(), req.description(),
                req.externalReference(), req.receiptUrl(), req.notes(), req.newPlanCode())
            : req;
        UUID actor = CurrentUser.require().getId();
        return manualPaymentService.record(newReq, actor);
    }

    @GetMapping("/payments")
    public Page<PaymentView> listPayments(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return manualPaymentService.listByTenant(TenantContextHolder.requireId(), page, size);
    }

}
