package br.com.rizermarketplaces.core.marketplace.billing;

import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.ManualPaymentRequest;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.PaymentView;
import br.com.rizermarketplaces.core.marketplace.model.Plan;
import br.com.rizermarketplaces.core.marketplace.model.Subscription;
import br.com.rizermarketplaces.core.marketplace.model.SubscriptionSource;
import br.com.rizermarketplaces.core.marketplace.model.SubscriptionStatus;
import br.com.rizermarketplaces.core.marketplace.repository.PaymentRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.repository.UserRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class ManualPaymentService {

    private final PaymentRepository paymentRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final PlanService planService;

    public ManualPaymentService(
        PaymentRepository paymentRepository,
        TenantRepository tenantRepository,
        UserRepository userRepository,
        SubscriptionService subscriptionService,
        PlanService planService
    ) {
        this.paymentRepository = paymentRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
        this.planService = planService;
    }

    /**
     * Registra um pagamento manual (dinheiro, transferência, pix externo, bônus, cortesia)
     * e ajusta a assinatura (cria/renova/muda plano).
     */
    @Transactional
    public PaymentView record(ManualPaymentRequest req, UUID recordedByUserId) {
        if (req.tenantId() == null) throw TenantExceptions.badRequest("tenantId é obrigatório");
        if (req.amount() == null || req.amount().signum() < 0) {
            throw TenantExceptions.badRequest("Valor inválido");
        }
        if (req.method() == null) throw TenantExceptions.badRequest("Método obrigatório");
        if (req.paidAt() == null) throw TenantExceptions.badRequest("paidAt é obrigatório");
        if (req.paidAt().isAfter(OffsetDateTime.now().plusDays(1))) {
            throw TenantExceptions.badRequest("paidAt não pode ser no futuro");
        }

        var tenant = tenantRepository.findById(req.tenantId())
            .orElseThrow(() -> TenantExceptions.notFound("Tenant"));

        long amountCents = req.amount().setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        String currency = (req.currency() == null || req.currency().isBlank()) ? "BRL" : req.currency().toUpperCase();

        // Determina plano e período
        String planCode = req.newPlanCode();
        if (planCode == null) {
            var existing = subscriptionService.getEntity(tenant.getId()).orElse(null);
            if (existing != null) planCode = existing.getPlanCode();
        }
        if (planCode == null) planCode = "BASIC";

        Subscription sub = subscriptionService.ensureSubscription(
            tenant.getId(), planCode, deriveSource(req.method()),
            req.paidAt(), req.periodEnd() != null ? req.periodEnd() : req.paidAt().plus(30, ChronoUnit.DAYS),
            SubscriptionStatus.active
        );

        var payment = new br.com.rizermarketplaces.core.marketplace.model.Payment();
        payment.setTenantId(tenant.getId());
        payment.setSubscriptionId(sub.getId());
        payment.setMethod(req.method());
        payment.setStatus(req.amount().signum() == 0
            ? br.com.rizermarketplaces.core.marketplace.model.PaymentStatus.succeeded
            : br.com.rizermarketplaces.core.marketplace.model.PaymentStatus.succeeded);
        payment.setAmountCents(amountCents);
        payment.setCurrency(currency);
        payment.setPeriodStart(req.periodStart() != null ? req.periodStart() : req.paidAt());
        payment.setPeriodEnd(req.periodEnd());
        payment.setDescription(req.description() != null ? req.description()
            : "Pagamento manual " + req.method() + " - " + planCode);
        payment.setExternalReference(req.externalReference());
        payment.setReceiptUrl(req.receiptUrl());
        payment.setPaidAt(req.paidAt());
        payment.setRecordedByUserId(recordedByUserId);
        payment.setNotes(req.notes());
        payment = paymentRepository.save(payment);

        var recordedBy = recordedByUserId != null
            ? userRepository.findByIdAndDeletedAtIsNull(recordedByUserId).orElse(null) : null;

        return toView(payment, tenant.getTradeName(), recordedBy != null ? recordedBy.getEmail() : null);
    }

    @Transactional(readOnly = true)
    public Page<PaymentView> listByTenant(UUID tenantId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paidAt"));
        return paymentRepository.findAllByTenantIdOrderByPaidAtDesc(tenantId, pageable)
            .map(p -> toView(p, null, null));
    }

    @Transactional(readOnly = true)
    public Page<PaymentView> listAll(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paidAt"));
        var tenantCache = new java.util.HashMap<UUID, String>();
        var userCache = new java.util.HashMap<UUID, String>();
        return paymentRepository.findAll(pageable).map(p -> {
            String tname = tenantCache.computeIfAbsent(p.getTenantId(),
                id -> tenantRepository.findById(id).map(t -> t.getTradeName()).orElse("—"));
            String recordedBy = null;
            if (p.getRecordedByUserId() != null) {
                recordedBy = userCache.computeIfAbsent(p.getRecordedByUserId(),
                    uid -> userRepository.findByIdAndDeletedAtIsNull(uid).map(u -> u.getEmail()).orElse(null));
            }
            return toView(p, tname, recordedBy);
        });
    }

    private SubscriptionSource deriveSource(br.com.rizermarketplaces.core.marketplace.model.PaymentMethod m) {
        return switch (m) {
            case manual_courtesy, manual_bonus -> SubscriptionSource.courtesy;
            case manual_cash, manual_bank_transfer, manual_pix_external, manual_other -> SubscriptionSource.manual;
            default -> SubscriptionSource.manual;
        };
    }

    private PaymentView toView(
        br.com.rizermarketplaces.core.marketplace.model.Payment p,
        String tenantName, String recordedByEmail
    ) {
        return new PaymentView(
            p.getId(), p.getTenantId(), tenantName, p.getSubscriptionId(),
            p.getMethod(), p.getStatus(),
            BigDecimal.valueOf(p.getAmountCents(), 2),
            p.getCurrency(),
            p.getPeriodStart(), p.getPeriodEnd(),
            p.getDescription(), p.getExternalReference(), p.getReceiptUrl(),
            p.getPaidAt(), recordedByEmail, p.getNotes()
        );
    }
}
