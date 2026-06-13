package br.com.rizermarketplaces.core.marketplace.billing;

import br.com.rizermarketplaces.core.marketplace.model.Plan;
import br.com.rizermarketplaces.core.marketplace.model.Subscription;
import br.com.rizermarketplaces.core.marketplace.model.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionStateMachineTest {

    private SubscriptionStateMachine sm;

    @BeforeEach
    void setUp() { sm = new SubscriptionStateMachine(); }

    @Test
    void canCreateStore_paraSubscriptionActive() {
        assertTrue(sm.canCreateStore(sub(SubscriptionStatus.active, null)));
    }

    @Test
    void canCreateStore_paraTrialing() {
        assertTrue(sm.canCreateStore(sub(SubscriptionStatus.trialing, null)));
    }

    @Test
    void canCreateStore_paraPastDue() {
        assertTrue(sm.canCreateStore(sub(SubscriptionStatus.past_due, null)));
    }

    @Test
    void canCreateStore_bloqueiaCanceledOuPausedOuUnpaid() {
        assertFalse(sm.canCreateStore(sub(SubscriptionStatus.canceled, null)));
        assertFalse(sm.canCreateStore(sub(SubscriptionStatus.paused, null)));
        assertFalse(sm.canCreateStore(sub(SubscriptionStatus.unpaid, null)));
    }

    @Test
    void canPublishAds_segueMesmaRegra() {
        assertTrue(sm.canPublishAds(sub(SubscriptionStatus.active, null)));
        assertFalse(sm.canPublishAds(sub(SubscriptionStatus.paused, null)));
    }

    @Test
    void isInGracePeriod_apenasPastDueRecente() {
        Subscription s = sub(SubscriptionStatus.past_due, OffsetDateTime.now().minusDays(3));
        s.setGracePeriodDays(7);
        assertTrue(sm.isInGracePeriod(s, OffsetDateTime.now()));
    }

    @Test
    void isInGracePeriod_false_seEstourou() {
        Subscription s = sub(SubscriptionStatus.past_due, OffsetDateTime.now().minusDays(10));
        s.setGracePeriodDays(7);
        assertFalse(sm.isInGracePeriod(s, OffsetDateTime.now()));
    }

    @Test
    void assertFeatureEnabled_exigeAssinaturaAtiva() {
        Plan plan = new Plan();
        plan.setCode("BASIC");
        plan.setHasInstagram(false);
        plan.setHasGoogleShopping(false);
        plan.setHasMetaDpa(false);
        assertThrows(IllegalStateException.class,
            () -> sm.assertFeatureEnabled(null, plan, SubscriptionStateMachine.Feature.INSTAGRAM));
    }

    @Test
    void assertFeatureEnabled_exigePlanoComFeature() {
        Plan plan = new Plan();
        plan.setCode("BASIC");
        plan.setHasInstagram(false);
        Subscription sub = sub(SubscriptionStatus.active, null);
        assertThrows(SubscriptionStateMachine.BillingException.class,
            () -> sm.assertFeatureEnabled(sub, plan, SubscriptionStateMachine.Feature.INSTAGRAM));
    }

    @Test
    void assertFeatureEnabled_passa_comPlatinum() {
        Plan plan = new Plan();
        plan.setCode("PLATINUM");
        plan.setHasInstagram(true);
        plan.setHasMetaDpa(true);
        plan.setHasGoogleShopping(true);
        Subscription sub = sub(SubscriptionStatus.active, null);
        sm.assertFeatureEnabled(sub, plan, SubscriptionStateMachine.Feature.META_DPA);
        sm.assertFeatureEnabled(sub, plan, SubscriptionStateMachine.Feature.GOOGLE_SHOPPING);
    }

    private Subscription sub(SubscriptionStatus status, OffsetDateTime currentPeriodEnd) {
        Subscription s = new Subscription();
        s.setTenantId(UUID.randomUUID());
        s.setStatus(status);
        s.setPlanCode("PRO");
        s.setCurrentPeriodStart(OffsetDateTime.now().minusDays(15));
        s.setCurrentPeriodEnd(currentPeriodEnd != null ? currentPeriodEnd : OffsetDateTime.now().plusDays(15));
        s.setGracePeriodDays(7);
        return s;
    }
}
