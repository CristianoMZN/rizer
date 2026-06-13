package br.com.rizermarketplaces.core.marketplace.integration;

import br.com.rizermarketplaces.core.marketplace.dto.IntegrationDtos.IntegrationView;
import br.com.rizermarketplaces.core.marketplace.dto.IntegrationDtos.OAuthCallbackRequest;
import br.com.rizermarketplaces.core.marketplace.model.IntegrationProvider;
import br.com.rizermarketplaces.core.marketplace.model.IntegrationStatus;
import br.com.rizermarketplaces.core.marketplace.model.Product;
import br.com.rizermarketplaces.core.marketplace.model.TenantIntegration;
import br.com.rizermarketplaces.core.marketplace.repository.PhysicalStoreRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductImageRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductLocalizationRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantIntegrationRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InstagramService {

    private static final Logger log = LoggerFactory.getLogger(InstagramService.class);

    private final MetaGraphClient metaClient;
    private final EncryptionService encryption;
    private final TenantIntegrationRepository integrationRepository;
    private final ProductRepository productRepository;
    private final ProductLocalizationRepository localizationRepository;
    private final ProductImageRepository imageRepository;
    private final PhysicalStoreRepository physicalStoreRepository;
    private final IntegrationAccessGuard guard;

    @Value("${app.oauth.state-secret:dev-state-secret-change-me}")
    private String stateSecret;

    public InstagramService(
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

    public String buildAuthorizeUrl(UUID tenantId) {
        guard.requirePlanFeature(tenantId, IntegrationProvider.INSTAGRAM);
        // state = base64(tenantId|secret) — verificado no callback
        String state = base64Url(tenantId.toString() + "|" + stateSecret);
        return metaClient.buildAuthorizeUrl(state);
    }

    @Transactional
    public IntegrationView completeOAuth(UUID tenantId, OAuthCallbackRequest body) throws Exception {
        if (body == null || body.code() == null || body.state() == null) {
            throw TenantExceptions.badRequest("code e state são obrigatórios");
        }
        // Valida state
        String decoded = new String(java.util.Base64.getUrlDecoder().decode(body.state()));
        String expected = tenantId.toString() + "|" + stateSecret;
        if (!expected.equals(decoded)) {
            throw TenantExceptions.badRequest("state inválido");
        }

        Map<String, Object> token = metaClient.exchangeCodeForToken(body.code());
        String accessToken = (String) token.get("access_token");
        Long expiresIn = ((Number) token.getOrDefault("expires_in", 3600L)).longValue();

        // Descobre páginas e a conta IG business
        JsonNode me = metaClient.meAccounts(accessToken);
        String pageId = null, pageName = null, pageAccessToken = null, igUserId = null;
        for (JsonNode page : me.path("data")) {
            if (page.path("instagram_business_account").isObject()) {
                pageId = page.path("id").asText();
                pageName = page.path("name").asText();
                pageAccessToken = page.path("access_token").asText();
                igUserId = page.path("instagram_business_account").path("id").asText();
                break;
            }
        }
        if (igUserId == null) {
            throw TenantExceptions.badRequest("Nenhuma conta Instagram Business vinculada à página. Conecte-a no Meta Business Suite primeiro.");
        }

        TenantIntegration integration = integrationRepository
            .findByTenantIdAndProvider(tenantId, IntegrationProvider.INSTAGRAM)
            .orElseGet(TenantIntegration::new);
        integration.setTenantId(tenantId);
        integration.setProvider(IntegrationProvider.INSTAGRAM);
        integration.setAccessTokenEncrypted(encryption.encrypt(accessToken));
        integration.setRefreshTokenEncrypted(pageAccessToken != null ? encryption.encrypt(pageAccessToken) : null);
        integration.setTokenExpiresAt(OffsetDateTime.now().plusSeconds(expiresIn));
        integration.setScopes("instagram_basic,instagram_content_publish,pages_show_list,pages_read_engagement,business_management");
        integration.setStatus(IntegrationStatus.CONNECTED);
        integration.setExternalAccountId(igUserId);
        integration.setExternalAccountName(pageName);
        Map<String, Object> meta = new HashMap<>();
        meta.put("page_id", pageId);
        meta.put("ig_user_id", igUserId);
        integration.setRawMetadata(meta);
        integrationRepository.save(integration);

        return toView(integration);
    }

    @Transactional
    public void disconnect(UUID tenantId) {
        integrationRepository.findByTenantIdAndProvider(tenantId, IntegrationProvider.INSTAGRAM)
            .ifPresent(i -> {
                i.setStatus(IntegrationStatus.REVOKED);
                integrationRepository.save(i);
            });
    }

    @Transactional
    public String publishProduct(UUID tenantId, UUID productId) {
        TenantIntegration integration = guard.requireConnected(tenantId, IntegrationProvider.INSTAGRAM);
        guard.requirePlanFeature(tenantId, IntegrationProvider.INSTAGRAM);

        Product p = productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Produto"));
        var loc = localizationRepository.findByProductIdAndCountryCode(productId, "BR").orElse(null);
        var images = imageRepository.findAllByProductIdOrderBySortOrderAscCreatedAtAsc(productId);
        if (images.isEmpty()) {
            throw TenantExceptions.badRequest("Anúncio precisa ter ao menos 1 foto");
        }
        String coverUrl = images.stream().filter(i -> i.isCover()).findFirst()
            .orElse(images.get(0)).getPublicUrl();
        String caption = buildCaption(p, loc, tenantId);

        String accessToken = encryption.decrypt(integration.getAccessTokenEncrypted());
        String igUserId = integration.getExternalAccountId();
        try {
            String creationId = metaClient.createIgMediaContainer(igUserId, accessToken, coverUrl, caption);
            String mediaId = metaClient.publishIgMedia(igUserId, accessToken, creationId);
            p.setPostedToInstagramAt(OffsetDateTime.now());
            p.setInstagramMediaId(mediaId);
            productRepository.save(p);
            integration.setLastSyncAt(OffsetDateTime.now());
            integrationRepository.save(integration);
            return mediaId;
        } catch (Exception e) {
            integration.setStatus(IntegrationStatus.ERROR);
            integration.setLastErrorAt(OffsetDateTime.now());
            integration.setLastErrorMessage(e.getMessage());
            integrationRepository.save(integration);
            throw new IllegalStateException("Falha ao postar no Instagram: " + e.getMessage(), e);
        }
    }

    /**
     * Varre produtos ACTIVE do tenant que ainda não postaram e posta os
     * primeiros N (limite para evitar rate-limit). Chamado pelo job.
     */
    @Transactional
    public int syncTenant(UUID tenantId, int maxProducts) {
        var integration = integrationRepository
            .findByTenantIdAndProvider(tenantId, IntegrationProvider.INSTAGRAM)
            .orElse(null);
        if (integration == null || integration.getStatus() != IntegrationStatus.CONNECTED) {
            return 0;
        }
        if (integration.getTokenExpiresAt() != null
            && integration.getTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            integration.setStatus(IntegrationStatus.EXPIRED);
            integrationRepository.save(integration);
            return 0;
        }
        int posted = 0;
        var products = productRepository
            .findAllByTenantIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId,
                br.com.rizermarketplaces.core.marketplace.model.ProductStatus.ACTIVE);
        for (Product p : products) {
            if (p.getPostedToInstagramAt() != null) continue;
            if (posted >= maxProducts) break;
            try {
                publishProduct(tenantId, p.getId());
                posted++;
            } catch (Exception e) {
                log.warn("[instagram-sync] tenant={} product={} falhou: {}", tenantId, p.getId(), e.getMessage());
                break;
            }
        }
        return posted;
    }

    private String buildCaption(Product p, br.com.rizermarketplaces.core.marketplace.model.ProductLocalization loc, UUID tenantId) {
        var store = physicalStoreRepository.findById(p.getPhysicalStoreId()).orElse(null);
        StringBuilder sb = new StringBuilder();
        if (loc != null && loc.getTitle() != null) sb.append(loc.getTitle());
        sb.append('\n');
        if (loc != null && loc.getDescription() != null) sb.append(truncate(loc.getDescription(), 400)).append("\n\n");
        if (loc != null) sb.append("💰 ").append(formatPrice(loc.getPriceCents())).append('\n');
        if (p.getYearModel() != null) sb.append("📅 ").append(p.getYearModel()).append('\n');
        if (p.getMileageKm() != null) sb.append("🛣️  ").append(p.getMileageKm().toString()).append(" km\n");
        if (store != null) sb.append("📍 ").append(store.getName());
        sb.append("\n\n#motorise #anuncio #veiculo");
        return sb.toString();
    }

    private String formatPrice(long cents) {
        return String.format("R$ %,.2f", cents / 100.0);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String base64Url(String s) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public IntegrationView toView(TenantIntegration i) {
        List<String> scopes = i.getScopes() == null ? List.of()
            : new ArrayList<>(List.of(i.getScopes().split(",")));
        return new IntegrationView(
            i.getId(), i.getProvider(), i.getStatus().name(),
            i.getExternalAccountId(), i.getExternalAccountName(),
            i.getTokenExpiresAt() != null && i.getTokenExpiresAt().isAfter(OffsetDateTime.now()),
            i.getTokenExpiresAt(),
            i.getLastSyncAt(),
            i.getLastErrorMessage(),
            scopes
        );
    }
}
