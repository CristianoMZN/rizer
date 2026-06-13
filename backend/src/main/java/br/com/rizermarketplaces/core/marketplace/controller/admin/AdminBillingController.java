package br.com.rizermarketplaces.core.marketplace.controller.admin;

import br.com.rizermarketplaces.core.marketplace.auth.CurrentUser;
import br.com.rizermarketplaces.core.marketplace.billing.ManualPaymentService;
import br.com.rizermarketplaces.core.marketplace.billing.PlanService;
import br.com.rizermarketplaces.core.marketplace.billing.SubscriptionService;
import br.com.rizermarketplaces.core.marketplace.billing.TrialService;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.AdminPaymentStats;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.ManualPaymentRequest;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.PaymentView;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.PlanView;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.SubscriptionView;
import br.com.rizermarketplaces.core.marketplace.model.PaymentMethod;
import br.com.rizermarketplaces.core.marketplace.model.PaymentStatus;
import br.com.rizermarketplaces.core.marketplace.model.SubscriptionStatus;
import br.com.rizermarketplaces.core.marketplace.repository.PaymentRepository;
import br.com.rizermarketplaces.core.marketplace.repository.SubscriptionRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/billing")
@PreAuthorize("hasAnyRole('sys_admin','sys_manager')")
@Tag(name = "Admin · Billing", description = "Livro-caixa, gestão de subscriptions e estatísticas globais")
public class AdminBillingController {

    private final PlanService planService;
    private final SubscriptionService subscriptionService;
    private final ManualPaymentService manualPaymentService;
    private final TrialService trialService;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;

    public AdminBillingController(
        PlanService planService,
        SubscriptionService subscriptionService,
        ManualPaymentService manualPaymentService,
        TrialService trialService,
        PaymentRepository paymentRepository,
        SubscriptionRepository subscriptionRepository
    ) {
        this.planService = planService;
        this.subscriptionService = subscriptionService;
        this.manualPaymentService = manualPaymentService;
        this.trialService = trialService;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping("/plans")
    public List<PlanView> listPlans() {
        return planService.listActive();
    }

    @PostMapping("/tenants/{tenantId}/trial/{planCode}")
    public SubscriptionView startTrial(@PathVariable UUID tenantId, @PathVariable String planCode) {
        trialService.startTrial(tenantId, planCode);
        return subscriptionService.getView(tenantId);
    }

    @PostMapping("/tenants/{tenantId}/subscription/{planCode}")
    public SubscriptionView changePlan(@PathVariable UUID tenantId, @PathVariable String planCode) {
        var plan = planService.get(planCode);
        var sub = subscriptionService.getEntity(tenantId).orElseThrow(() -> TenantExceptions.notFound("Assinatura"));
        var start = OffsetDateTime.now();
        var end = start.plusDays(30);
        return subscriptionService.toView(subscriptionService.switchPlan(tenantId, planCode, start, end));
    }

    @PatchMapping("/tenants/{tenantId}/subscription/status")
    public SubscriptionView changeStatus(
        @PathVariable UUID tenantId,
        @RequestParam SubscriptionStatus status,
        @RequestParam(required = false) String notes
    ) {
        var sub = subscriptionService.changeStatus(tenantId, status, notes);
        return subscriptionService.toView(sub);
    }

    @PostMapping("/tenants/{tenantId}/payments")
    public PaymentView recordPayment(
        @PathVariable UUID tenantId,
        @RequestBody ManualPaymentRequest req
    ) {
        var withTenant = new ManualPaymentRequest(
            tenantId, req.amount(), req.currency(), req.method(),
            req.paidAt(), req.periodStart(), req.periodEnd(), req.description(),
            req.externalReference(), req.receiptUrl(), req.notes(), req.newPlanCode()
        );
        UUID actor = CurrentUser.require().getId();
        return manualPaymentService.record(withTenant, actor);
    }

    @GetMapping("/payments")
    public Page<PaymentView> listAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return manualPaymentService.listAll(page, size);
    }

    @GetMapping("/stats")
    public AdminPaymentStats stats() {
        long active = subscriptionRepository.findAllByStatusAndCurrentPeriodEndBefore(
            SubscriptionStatus.active, OffsetDateTime.now().plusYears(10)).size();
        long trialing = subscriptionRepository.findAllByStatusAndTrialEndBefore(
            SubscriptionStatus.trialing, OffsetDateTime.now().plusYears(10)).size();
        long pastDue = subscriptionRepository.findAllByStatusAndCurrentPeriodEndBefore(
            SubscriptionStatus.past_due, OffsetDateTime.now().plusYears(10)).size();

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime d30 = now.minusDays(30);
        long mrr = paymentRepository.sumSucceededByMethodsBetween(
            List.of(PaymentMethod.stripe_card, PaymentMethod.stripe_pix, PaymentMethod.stripe_boleto,
                PaymentMethod.manual_cash, PaymentMethod.manual_bank_transfer, PaymentMethod.manual_pix_external,
                PaymentMethod.manual_courtesy),
            d30, now);

        long succeeded30d = paymentRepository.sumSucceededByMethodsBetween(
            List.of(PaymentMethod.stripe_card, PaymentMethod.stripe_pix, PaymentMethod.stripe_boleto,
                PaymentMethod.manual_cash, PaymentMethod.manual_bank_transfer, PaymentMethod.manual_pix_external,
                PaymentMethod.manual_bonus, PaymentMethod.manual_courtesy, PaymentMethod.manual_other),
            d30, now);

        return new AdminPaymentStats(
            active, trialing, pastDue,
            mrr,
            BigDecimal.valueOf(mrr, 2),
            "BRL",
            succeeded30d,
            BigDecimal.valueOf(succeeded30d, 2)
        );
    }
}
