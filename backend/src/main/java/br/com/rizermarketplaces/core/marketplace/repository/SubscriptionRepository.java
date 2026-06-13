package br.com.rizermarketplaces.core.marketplace.repository;

import br.com.rizermarketplaces.core.marketplace.model.Subscription;
import br.com.rizermarketplaces.core.marketplace.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByTenantId(UUID tenantId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    List<Subscription> findAllByStatusAndCurrentPeriodEndBefore(SubscriptionStatus status, java.time.OffsetDateTime before);

    List<Subscription> findAllByStatusAndTrialEndBefore(SubscriptionStatus status, java.time.OffsetDateTime before);
}
