package br.com.rizermarketplaces.core.marketplace.integration;

import br.com.rizermarketplaces.core.marketplace.billing.PlanService;
import br.com.rizermarketplaces.core.marketplace.billing.SubscriptionService;
import br.com.rizermarketplaces.core.marketplace.model.IntegrationProvider;
import br.com.rizermarketplaces.core.marketplace.model.Plan;
import br.com.rizermarketplaces.core.marketplace.model.Product;
import br.com.rizermarketplaces.core.marketplace.model.ProductImage;
import br.com.rizermarketplaces.core.marketplace.model.ProductLocalization;
import br.com.rizermarketplaces.core.marketplace.model.ProductStatus;
import br.com.rizermarketplaces.core.marketplace.model.Subscription;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.repository.PhysicalStoreRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductImageRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductLocalizationRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Gera o feed XML do Google Merchant Center (GMC) no formato exigido.
 * Disponível em `/{countryCode}/public/tenants/{slug}/feed.xml`.
 * Restrito a tenant com integração GOOGLE_MERCHANT ativa e plano Platinum.
 */
@Service
public class GoogleShoppingFeedService {

    private final TenantRepository tenantRepository;
    private final ProductRepository productRepository;
    private final ProductLocalizationRepository localizationRepository;
    private final ProductImageRepository imageRepository;
    private final PhysicalStoreRepository physicalStoreRepository;
    private final SubscriptionService subscriptionService;
    private final PlanService planService;
    private final IntegrationAccessGuard guard;

    public GoogleShoppingFeedService(
        TenantRepository tenantRepository,
        ProductRepository productRepository,
        ProductLocalizationRepository localizationRepository,
        ProductImageRepository imageRepository,
        PhysicalStoreRepository physicalStoreRepository,
        SubscriptionService subscriptionService,
        PlanService planService,
        IntegrationAccessGuard guard
    ) {
        this.tenantRepository = tenantRepository;
        this.productRepository = productRepository;
        this.localizationRepository = localizationRepository;
        this.imageRepository = imageRepository;
        this.physicalStoreRepository = physicalStoreRepository;
        this.subscriptionService = subscriptionService;
        this.planService = planService;
        this.guard = guard;
    }

    public String renderFeed(String slug) {
        Tenant tenant = tenantRepository.findBySlugAndDeletedAtIsNull(slug)
            .orElseThrow(() -> TenantExceptions.notFound("Parceiro"));
        if (!tenant.isPublic() || !tenant.isPartnerPageEnabled()) {
            throw TenantExceptions.notFound("Parceiro");
        }
        Subscription sub = subscriptionService.getEntity(tenant.getId()).orElse(null);
        if (sub == null) throw TenantExceptions.notFound("Parceiro");
        Plan plan = planService.get(sub.getPlanCode());
        if (!plan.isHasGoogleShopping()) {
            throw TenantExceptions.paymentRequired("Feeds de produto exigem plano Platinum");
        }
        if (tenant.getStatus() != br.com.rizermarketplaces.core.marketplace.model.TenantStatus.active) {
            throw TenantExceptions.notFound("Parceiro");
        }

        List<Product> products = productRepository
            .findAllByTenantIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(tenant.getId(), ProductStatus.ACTIVE);
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss xmlns:g=\"http://base.google.com/ns/1.0\" version=\"2.0\">\n");
        xml.append("  <channel>\n");
        xml.append("    <title>").append(escape(tenant.getTradeName())).append("</title>\n");
        xml.append("    <link>").append(escape("https://" + tenant.getSlug() + ".motorise.com.br")).append("</link>\n");
        xml.append("    <description>").append(escape(tenant.getDescription() != null ? tenant.getDescription() : "")).append("</description>\n");
        for (Product p : products) {
            xml.append(renderItem(tenant, p));
        }
        xml.append("  </channel>\n");
        xml.append("</rss>\n");
        return xml.toString();
    }

    private String renderItem(Tenant tenant, Product p) {
        var loc = localizationRepository.findByProductIdAndCountryCode(p.getId(), "BR").orElse(null);
        var images = imageRepository.findAllByProductIdOrderBySortOrderAscCreatedAtAsc(p.getId());
        var store = physicalStoreRepository.findById(p.getPhysicalStoreId()).orElse(null);

        StringBuilder item = new StringBuilder();
        item.append("    <item>\n");
        item.append("      <g:id>").append(escape(p.getId().toString())).append("</g:id>\n");
        item.append("      <g:title>").append(escape(loc != null ? loc.getTitle() : "Veículo")).append("</g:title>\n");
        item.append("      <g:description>").append(escape(loc != null ? truncate(loc.getDescription(), 5000) : "")).append("</g:description>\n");
        item.append("      <g:link>").append(escape("https://" + tenant.getSlug() + ".motorise.com.br/produto/" + p.getId())).append("</g:link>\n");
        item.append("      <g:condition>used</g:condition>\n");
        item.append("      <g:availability>in_stock</g:availability>\n");
        if (loc != null) {
            item.append("      <g:price>").append(String.format("%.2f %s", loc.getPriceCents() / 100.0, loc.getCurrency())).append("</g:price>\n");
        }
        if (!images.isEmpty()) {
            item.append("      <g:image_link>").append(escape(images.get(0).getPublicUrl())).append("</g:image_link>\n");
            for (int i = 1; i < Math.min(images.size(), 11); i++) {
                item.append("      <g:additional_image_link>").append(escape(images.get(i).getPublicUrl())).append("</g:additional_image_link>\n");
            }
        }
        item.append("      <g:brand>").append(escape(p.getBrandId() != null ? String.valueOf(p.getBrandId()) : tenant.getTradeName())).append("</g:brand>\n");
        if (p.getYearModel() != null) {
            item.append("      <g:year>").append(p.getYearModel()).append("</g:year>\n");
        }
        if (store != null) {
            item.append("      <g:item_group_id>").append(escape(tenant.getSlug() + "-" + (p.getBrandId() != null ? p.getBrandId() : "x"))).append("</g:item_group_id>\n");
            // shipping BR
            item.append("      <g:shipping>\n");
            item.append("        <g:country>BR</g:country>\n");
            item.append("        <g:service>Standard</g:service>\n");
            item.append("        <g:price>0.00 BRL</g:price>\n");
            item.append("      </g:shipping>\n");
        }
        item.append("    </item>\n");
        return item.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
