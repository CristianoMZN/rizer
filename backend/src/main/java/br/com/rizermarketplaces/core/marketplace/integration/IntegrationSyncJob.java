package br.com.rizermarketplaces.core.marketplace.integration;

import br.com.rizermarketplaces.core.marketplace.model.IntegrationProvider;
import br.com.rizermarketplaces.core.marketplace.repository.TenantIntegrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IntegrationSyncJob {

    private static final Logger log = LoggerFactory.getLogger(IntegrationSyncJob.class);

    private final TenantIntegrationRepository integrationRepository;
    private final InstagramService instagramService;
    private final MetaCatalogService metaCatalogService;

    public IntegrationSyncJob(
        TenantIntegrationRepository integrationRepository,
        InstagramService instagramService,
        MetaCatalogService metaCatalogService
    ) {
        this.integrationRepository = integrationRepository;
        this.instagramService = instagramService;
        this.metaCatalogService = metaCatalogService;
    }

    /**
     * A cada 5 min sincroniza Instagram e Meta Catalog.
     * Em prod, o limite de produtos por tick evita 429 da Meta.
     */
    @Scheduled(fixedDelayString = "${app.integration.sync-interval-ms:300000}", initialDelay = 120_000)
    public void run() {
        var connected = integrationRepository.findAllByProviderAndStatus(
            IntegrationProvider.INSTAGRAM,
            br.com.rizermarketplaces.core.marketplace.model.IntegrationStatus.CONNECTED
        );
        for (var i : connected) {
            try {
                int posted = instagramService.syncTenant(i.getTenantId(), 3);
                if (posted > 0) log.info("[integration-sync] instagram tenant={} posted={}", i.getTenantId(), posted);
            } catch (Exception e) {
                log.warn("[integration-sync] instagram tenant={} erro: {}", i.getTenantId(), e.getMessage());
            }
        }
        var metaConnected = integrationRepository.findAllByProviderAndStatus(
            IntegrationProvider.META_BUSINESS,
            br.com.rizermarketplaces.core.marketplace.model.IntegrationStatus.CONNECTED
        );
        for (var i : metaConnected) {
            try {
                int pushed = metaCatalogService.syncTenantProducts(i.getTenantId(), 20);
                if (pushed > 0) log.info("[integration-sync] meta-catalog tenant={} pushed={}", i.getTenantId(), pushed);
            } catch (Exception e) {
                log.warn("[integration-sync] meta-catalog tenant={} erro: {}", i.getTenantId(), e.getMessage());
            }
        }
    }
}
