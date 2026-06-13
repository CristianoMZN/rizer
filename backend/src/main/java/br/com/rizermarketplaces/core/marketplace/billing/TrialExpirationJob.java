package br.com.rizermarketplaces.core.marketplace.billing;

import br.com.rizermarketplaces.core.marketplace.model.Subscription;
import br.com.rizermarketplaces.core.marketplace.model.SubscriptionStatus;
import br.com.rizermarketplaces.core.marketplace.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class TrialExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(TrialExpirationJob.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    public TrialExpirationJob(SubscriptionRepository subscriptionRepository, SubscriptionService subscriptionService) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(fixedDelayString = "${app.billing.trial-job-interval-ms:3600000}", initialDelay = 60_000)
    @Transactional
    public void run() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Subscription> expired = subscriptionRepository
            .findAllByStatusAndTrialEndBefore(SubscriptionStatus.trialing, now);
        for (Subscription s : expired) {
            log.info("[trial-expiration] cancelando trial do tenant {}", s.getTenantId());
            s.setStatus(SubscriptionStatus.canceled);
            s.setCanceledAt(now);
            subscriptionService.updateEntity(s);
        }
    }
}
