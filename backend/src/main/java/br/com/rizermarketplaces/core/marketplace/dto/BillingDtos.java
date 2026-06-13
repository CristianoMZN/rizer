package br.com.rizermarketplaces.core.marketplace.dto;

import br.com.rizermarketplaces.core.marketplace.model.PaymentMethod;
import br.com.rizermarketplaces.core.marketplace.model.PaymentStatus;
import br.com.rizermarketplaces.core.marketplace.model.SubscriptionSource;
import br.com.rizermarketplaces.core.marketplace.model.SubscriptionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class BillingDtos {

    public record PlanView(
        String code,
        String name,
        String description,
        Integer maxPhysicalStores,
        boolean hasPartnerPage,
        boolean hasCustomDomain,
        boolean hasInstagram,
        boolean hasMetaDpa,
        boolean hasGoogleShopping,
        BigDecimal price,
        String currency,
        int trialDays,
        int sortOrder
    ) {}

    public record SubscriptionView(
        UUID id,
        UUID tenantId,
        String planCode,
        String planName,
        SubscriptionStatus status,
        SubscriptionSource source,
        BigDecimal price,
        String currency,
        OffsetDateTime currentPeriodStart,
        OffsetDateTime currentPeriodEnd,
        OffsetDateTime trialStart,
        OffsetDateTime trialEnd,
        boolean cancelAtPeriodEnd,
        OffsetDateTime canceledAt,
        Integer trialDaysRemaining,
        Integer daysUntilPeriodEnd,
        boolean isInGracePeriod,
        String stripeCustomerId,
        String notes
    ) {}

    public record CheckoutResponse(
        String checkoutUrl,
        String sessionId,
        boolean usingManual
    ) {}

    public record PortalResponse(String portalUrl) {}

    public record PaymentView(
        UUID id,
        UUID tenantId,
        String tenantName,
        UUID subscriptionId,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd,
        String description,
        String externalReference,
        String receiptUrl,
        OffsetDateTime paidAt,
        String recordedByEmail,
        String notes
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ManualPaymentRequest(
        UUID tenantId,
        java.math.BigDecimal amount,
        String currency,
        PaymentMethod method,
        OffsetDateTime paidAt,
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd,
        String description,
        String externalReference,
        String receiptUrl,
        String notes,
        String newPlanCode
    ) {}

    public record AdminPaymentStats(
        long activeTenants,
        long trialingTenants,
        long pastDueTenants,
        long mrrCents,
        BigDecimal mrr,
        String currency,
        long succeededLast30d,
        BigDecimal revenueLast30d
    ) {}
}
