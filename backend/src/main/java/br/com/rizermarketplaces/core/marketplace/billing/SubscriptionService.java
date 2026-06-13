package br.com.rizermarketplaces.core.marketplace.billing;

import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.PlanView;
import br.com.rizermarketplaces.core.marketplace.dto.BillingDtos.SubscriptionView;
import br.com.rizermarketplaces.core.marketplace.model.Plan;
import br.com.rizermarketplaces.core.marketplace.model.Subscription;
import br.com.rizermarketplaces.core.marketplace.model.SubscriptionSource;
import br.com.rizermarketplaces.core.marketplace.model.SubscriptionStatus;
import br.com.rizermarketplaces.core.marketplace.repository.SubscriptionRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final PlanService planService;
    private final SubscriptionStateMachine stateMachine;

    @Autowired
    public SubscriptionService(
        SubscriptionRepository subscriptionRepository,
        TenantRepository tenantRepository,
        PlanService planService,
        SubscriptionStateMachine stateMachine
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.tenantRepository = tenantRepository;
        this.planService = planService;
        this.stateMachine = stateMachine;
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> getEntity(UUID tenantId) {
        return subscriptionRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> getEntityByStripeId(String stripeSubscriptionId) {
        if (stripeSubscriptionId == null) return Optional.empty();
        return subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
    }

    @Transactional
    public Subscription updateEntity(Subscription sub) {
        return subscriptionRepository.save(sub);
    }

    @Transactional(readOnly = true)
    public SubscriptionView getView(UUID tenantId) {
        Subscription sub = subscriptionRepository.findByTenantId(tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Assinatura"));
        return toView(sub);
    }

    @Transactional
    public Subscription ensureSubscription(UUID tenantId, String planCode, SubscriptionSource source,
                                           OffsetDateTime periodStart, OffsetDateTime periodEnd,
                                           SubscriptionStatus status) {
        if (tenantRepository.findById(tenantId).isEmpty()) {
            throw TenantExceptions.notFound("Tenant");
        }
        Plan plan = planService.get(planCode);
        Subscription sub = subscriptionRepository.findByTenantId(tenantId).orElseGet(Subscription::new);
        sub.setTenantId(tenantId);
        sub.setPlanCode(plan.getCode());
        sub.setStatus(status);
        sub.setSource(source);
        sub.setCurrency(plan.getCurrency());
        sub.setCurrentPeriodStart(periodStart);
        sub.setCurrentPeriodEnd(periodEnd);
        if (status == SubscriptionStatus.trialing) {
            sub.setTrialStart(periodStart);
            sub.setTrialEnd(periodEnd);
        }
        return subscriptionRepository.save(sub);
    }

    @Transactional
    public Subscription cancelAtPeriodEnd(UUID tenantId) {
        Subscription sub = subscriptionRepository.findByTenantId(tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Assinatura"));
        sub.setCancelAtPeriodEnd(true);
        sub.setCanceledAt(OffsetDateTime.now());
        return subscriptionRepository.save(sub);
    }

    @Transactional
    public Subscription resume(UUID tenantId) {
        Subscription sub = subscriptionRepository.findByTenantId(tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Assinatura"));
        sub.setCancelAtPeriodEnd(false);
        sub.setCanceledAt(null);
        if (sub.getStatus() == SubscriptionStatus.paused) {
            sub.setStatus(SubscriptionStatus.active);
        }
        return subscriptionRepository.save(sub);
    }

    @Transactional
    public Subscription changeStatus(UUID tenantId, SubscriptionStatus newStatus, String notes) {
        Subscription sub = subscriptionRepository.findByTenantId(tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Assinatura"));
        sub.setStatus(newStatus);
        if (notes != null && !notes.isBlank()) {
            String prev = sub.getNotes();
            sub.setNotes(prev == null ? notes : prev + " | " + notes);
        }
        return subscriptionRepository.save(sub);
    }

    @Transactional
    public Subscription renewPeriod(UUID tenantId, OffsetDateTime start, OffsetDateTime end) {
        Subscription sub = subscriptionRepository.findByTenantId(tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Assinatura"));
        sub.setCurrentPeriodStart(start);
        sub.setCurrentPeriodEnd(end);
        if (sub.getStatus() == SubscriptionStatus.trialing || sub.getStatus() == SubscriptionStatus.past_due) {
            sub.setStatus(SubscriptionStatus.active);
        }
        return subscriptionRepository.save(sub);
    }

    @Transactional
    public Subscription switchPlan(UUID tenantId, String newPlanCode, OffsetDateTime newPeriodStart, OffsetDateTime newPeriodEnd) {
        Plan plan = planService.get(newPlanCode);
        Subscription sub = subscriptionRepository.findByTenantId(tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Assinatura"));
        sub.setPlanCode(plan.getCode());
        sub.setCurrency(plan.getCurrency());
        sub.setCurrentPeriodStart(newPeriodStart);
        sub.setCurrentPeriodEnd(newPeriodEnd);
        if (sub.getStatus() == SubscriptionStatus.past_due) {
            sub.setStatus(SubscriptionStatus.active);
        }
        return subscriptionRepository.save(sub);
    }

    public SubscriptionView toView(Subscription sub) {
        Plan plan = planService.get(sub.getPlanCode());
        OffsetDateTime now = OffsetDateTime.now();
        Integer trialDaysRemaining = null;
        if (sub.getTrialEnd() != null && sub.getTrialEnd().isAfter(now)) {
            trialDaysRemaining = (int) java.time.Duration.between(now.toLocalDate(), sub.getTrialEnd().toLocalDate()).toDays();
        }
        Integer daysUntilPeriodEnd = (int) java.time.Duration.between(now.toLocalDate(), sub.getCurrentPeriodEnd().toLocalDate()).toDays();
        return new SubscriptionView(
            sub.getId(), sub.getTenantId(), plan.getCode(), plan.getName(),
            sub.getStatus(), sub.getSource(),
            BigDecimal.valueOf(plan.getPriceCents(), 2),
            sub.getCurrency(),
            sub.getCurrentPeriodStart(), sub.getCurrentPeriodEnd(),
            sub.getTrialStart(), sub.getTrialEnd(),
            sub.isCancelAtPeriodEnd(), sub.getCanceledAt(),
            trialDaysRemaining, daysUntilPeriodEnd,
            stateMachine.isInGracePeriod(sub, now),
            sub.getStripeCustomerId(), sub.getNotes()
        );
    }

    public PlanView toPlanView(Plan plan) {
        return new PlanView(
            plan.getCode(), plan.getName(), plan.getDescription(),
            plan.getMaxPhysicalStores(),
            plan.isHasPartnerPage(), plan.isHasCustomDomain(),
            plan.isHasInstagram(), plan.isHasMetaDpa(), plan.isHasGoogleShopping(),
            BigDecimal.valueOf(plan.getPriceCents(), 2),
            plan.getCurrency(),
            plan.getTrialDays(),
            plan.getSortOrder()
        );
    }
}
