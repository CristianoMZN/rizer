package br.com.rizermarketplaces.core.marketplace.integration;

import br.com.rizermarketplaces.core.marketplace.model.IntegrationProvider;
import br.com.rizermarketplaces.core.marketplace.model.IntegrationStatus;
import br.com.rizermarketplaces.core.marketplace.model.Product;
import br.com.rizermarketplaces.core.marketplace.model.ProductImage;
import br.com.rizermarketplaces.core.marketplace.model.ProductLocalization;
import br.com.rizermarketplaces.core.marketplace.model.ProductStatus;
import br.com.rizermarketplaces.core.marketplace.model.TenantIntegration;
import br.com.rizermarketplaces.core.marketplace.repository.PhysicalStoreRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductImageRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductLocalizationRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantIntegrationRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sincroniza produtos ACTIVE com o Meta Commerce Manager via Graph API.
 * Requer META_BUSINESS integration conectada e plano Platinum.
 */
@Service
public class MetaCatalogService {

    private static final Logger log = LoggerFactory.getLogger(MetaCatalogService.class);

    private final MetaGraphClient metaClient;
    private final EncryptionService encryption;
    private final TenantIntegrationRepository integrationRepository;
    private final ProductRepository productRepository;
    private final ProductLocalizationRepository localizationRepository;
    private final ProductImageRepository imageRepository;
    private final PhysicalStoreRepository physicalStoreRepository;
    private final IntegrationAccessGuard guard;

    public MetaCatalogService(
        MetaGraphClient metaClient,
        EncryptionService encryption,
        TenantIntegrationRepository integrationRepository,
        ProductRepository productRepository,
        ProductLocalizationRepository localizationRepository,
        ProductImageRepository imageRepository,
        PhysicalStoreRepository physicalStoreRepository,
        IntegrationAccessGuard guard
    ) {
        this.metaClient = metaClient;
        this.encryption = encryption;
        this.integrationRepository = integrationRepository;
        this.productRepository = productRepository;
        this.localizationRepository = localizationRepository;
        this.imageRepository = imageRepository;
        this.physicalStoreRepository = physicalStoreRepository;
        this.guard = guard;
    }

    @Transactional
    public int syncTenantProducts(UUID tenantId, int maxProducts) {
        TenantIntegration integration = guard.requireConnected(tenantId, IntegrationProvider.META_BUSINESS);
        guard.requirePlanFeature(tenantId, IntegrationProvider.META_BUSINESS);
        String catalogId = integration.getRawMetadata() != null
            ? (String) integration.getRawMetadata().get("catalog_id")
            : null;
        if (catalogId == null) {
            throw TenantExceptions.badRequest("Integração META_BUSINESS sem catalog_id configurado. Use o fluxo de setup dedicado.");
        }
        String accessToken = encryption.decrypt(integration.getAccessTokenEncrypted());

        int pushed = 0;
        List<Product> products = productRepository
            .findAllByTenantIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId, ProductStatus.ACTIVE);
        for (Product p : products) {
            if (pushed >= maxProducts) break;
            try {
                Map<String, Object> payload = buildCatalogPayload(tenantId, p);
                String externalId = p.getId().toString();
                // productId no Meta = nosso UUID (idempotente)
                Map<String, Object> withRetargeting = new HashMap<>();
                withRetargeting.put("id", externalId);
                withRetargeting.putAll(payload);
                String respId = metaClient.createCatalogProduct(catalogId, accessToken, withRetargeting);
                log.info("[meta-catalog] tenant={} product={} catalog_id={} external_id={}",
                    tenantId, p.getId(), catalogId, respId);
                pushed++;
            } catch (Exception e) {
                log.warn("[meta-catalog] tenant={} product={} falhou: {}", tenantId, p.getId(), e.getMessage());
                integration.setStatus(IntegrationStatus.ERROR);
                integration.setLastErrorAt(java.time.OffsetDateTime.now());
                integration.setLastErrorMessage(e.getMessage());
                integrationRepository.save(integration);
                break;
            }
        }
        integration.setLastSyncAt(java.time.OffsetDateTime.now());
        integrationRepository.save(integration);
        return pushed;
    }

    private Map<String, Object> buildCatalogPayload(UUID tenantId, Product p) {
        var loc = localizationRepository.findByProductIdAndCountryCode(p.getId(), "BR").orElse(null);
        var images = imageRepository.findAllByProductIdOrderBySortOrderAscCreatedAtAsc(p.getId());
        var store = physicalStoreRepository.findById(p.getPhysicalStoreId()).orElse(null);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", loc != null ? loc.getTitle() : "Veículo " + p.getId());
        payload.put("description", loc != null ? truncate(loc.getDescription(), 5000) : "");
        payload.put("brand", ""); // preenchido pelo frontend se houver
        if (loc != null) {
            payload.put("price", String.valueOf(loc.getPriceCents()));
            payload.put("currency", loc.getCurrency());
        }
        if (!images.isEmpty()) {
            payload.put("image_url", images.get(0).getPublicUrl());
            payload.put("additional_image_urls", images.stream().skip(1).map(ProductImage::getPublicUrl).toList());
        }
        if (p.getYearModel() != null) payload.put("year", p.getYearModel().toString());
        payload.put("availability", "in stock");
        payload.put("condition", "used");
        payload.put("status", "active");
        if (store != null && store.getLocation() != null) {
            payload.put("address", Map.of(
                "lat", store.getLocation().getY(),
                "lng", store.getLocation().getX()
            ));
        }
        return payload;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
