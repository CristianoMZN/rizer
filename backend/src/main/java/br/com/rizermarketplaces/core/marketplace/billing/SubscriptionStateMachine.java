package br.com.rizermarketplaces.core.marketplace.billing;

import br.com.rizermarketplaces.core.marketplace.model.Plan;
import br.com.rizermarketplaces.core.marketplace.model.Subscription;
import br.com.rizermarketplaces.core.marketplace.model.SubscriptionStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Centraliza as transições de status de Subscription.
 *
 * past_due → active (pagou) | unpaid (grace estourou) | canceled
 * active → past_due (falh) | canceled (cancel_at_period_end) | paused
 * trialing → active (checkout) | canceled (trial expirou)
 * paused → active
 * unpaid → active (pagou) | canceled
 * canceled → (terminal — reativar cria nova subscription)
 */
@Service
public class SubscriptionStateMachine {

    private static final Set<SubscriptionStatus> ACTIVE_LIKE = Set.of(
        SubscriptionStatus.active, SubscriptionStatus.trialing, SubscriptionStatus.past_due
    );

    private static final Set<SubscriptionStatus> ALLOWS_STORE_CREATION = Set.of(
        SubscriptionStatus.active, SubscriptionStatus.trialing, SubscriptionStatus.past_due
    );

    private static final Set<SubscriptionStatus> ALLOWS_PUBLISHING = Set.of(
        SubscriptionStatus.active, SubscriptionStatus.trialing, SubscriptionStatus.past_due
    );

    public boolean canCreateStore(Subscription sub) {
        return sub != null && ALLOWS_STORE_CREATION.contains(sub.getStatus());
    }

    public boolean canPublishAds(Subscription sub) {
        return sub != null && ALLOWS_PUBLISHING.contains(sub.getStatus());
    }

    public boolean isActiveLike(Subscription sub) {
        return sub != null && ACTIVE_LIKE.contains(sub.getStatus());
    }

    public boolean isInGracePeriod(Subscription sub, OffsetDateTime now) {
        if (sub == null || sub.getStatus() != SubscriptionStatus.past_due) return false;
        if (sub.getCurrentPeriodEnd() == null) return false;
        return sub.getCurrentPeriodEnd().plusDays(sub.getGracePeriodDays()).isAfter(now);
    }

    public void assertFeatureEnabled(Subscription sub, Plan plan, Feature feature) {
        if (sub == null) {
            throw new IllegalStateException("Tenant sem assinatura");
        }
        if (!isActiveLike(sub)) {
            throw new BillingException("Assinatura inativa. Regularize para continuar.");
        }
        if (!planSupports(plan, feature)) {
            throw new BillingException("Plano " + plan.getCode() + " não cobre o recurso: " + feature);
        }
    }

    private boolean planSupports(Plan p, Feature f) {
        return switch (f) {
            case PARTNER_PAGE -> p.isHasPartnerPage();
            case CUSTOM_DOMAIN -> p.isHasCustomDomain();
            case INSTAGRAM -> p.isHasInstagram();
            case META_DPA -> p.isHasMetaDpa();
            case GOOGLE_SHOPPING -> p.isHasGoogleShopping();
        };
    }

    public enum Feature {
        PARTNER_PAGE, CUSTOM_DOMAIN, INSTAGRAM, META_DPA, GOOGLE_SHOPPING
    }

    public static class BillingException extends RuntimeException {
        public BillingException(String msg) { super(msg); }
    }
}
