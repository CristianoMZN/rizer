package br.com.rizermarketplaces.core.marketplace.partner;

import br.com.rizermarketplaces.core.marketplace.dto.PublicPartnerView;
import br.com.rizermarketplaces.core.marketplace.dto.PublicProductView;
import br.com.rizermarketplaces.core.marketplace.dto.PublicTenantView;
import br.com.rizermarketplaces.core.marketplace.model.Address;
import br.com.rizermarketplaces.core.marketplace.model.PhysicalStore;
import br.com.rizermarketplaces.core.marketplace.model.Product;
import br.com.rizermarketplaces.core.marketplace.model.ProductImage;
import br.com.rizermarketplaces.core.marketplace.model.ProductStatus;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.model.TenantStatus;
import br.com.rizermarketplaces.core.marketplace.repository.AddressRepository;
import br.com.rizermarketplaces.core.marketplace.repository.PhysicalStoreRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductImageRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductLocalizationRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.repository.VehicleBrandRepository;
import br.com.rizermarketplaces.core.marketplace.repository.VehicleModelRepository;
import br.com.rizermarketplaces.core.marketplace.repository.CategoryRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PublicPartnerService {

    private final TenantRepository tenantRepository;
    private final PhysicalStoreRepository physicalStoreRepository;
    private final ProductRepository productRepository;
    private final ProductLocalizationRepository productLocalizationRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final VehicleBrandRepository brandRepository;
    private final VehicleModelRepository modelRepository;
    private final AddressRepository addressRepository;

    public PublicPartnerService(
        TenantRepository tenantRepository,
        PhysicalStoreRepository physicalStoreRepository,
        ProductRepository productRepository,
        ProductLocalizationRepository productLocalizationRepository,
        ProductImageRepository productImageRepository,
        CategoryRepository categoryRepository,
        VehicleBrandRepository brandRepository,
        VehicleModelRepository modelRepository,
        AddressRepository addressRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.physicalStoreRepository = physicalStoreRepository;
        this.productRepository = productRepository;
        this.productLocalizationRepository = productLocalizationRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.modelRepository = modelRepository;
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<PublicPartnerView> listPartners(String countryCode) {
        return tenantRepository.findActivePartners()
            .stream()
            .filter(t -> countryCode == null || countryCode.equalsIgnoreCase(t.getCountryCode()))
            .map(this::toPartnerSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public PublicTenantView getTenant(String slug) {
        Tenant t = tenantRepository.findBySlugAndDeletedAtIsNull(slug)
            .orElseThrow(() -> TenantExceptions.notFound("Parceiro"));
        if (t.getStatus() != TenantStatus.active || !t.isPublic() || !t.isPartnerPageEnabled()) {
            throw TenantExceptions.notFound("Parceiro");
        }
        return toTenantView(t);
    }

    @Transactional(readOnly = true)
    public List<PublicProductView> listTenantProducts(String slug, int limit) {
        Tenant t = tenantRepository.findBySlugAndDeletedAtIsNull(slug)
            .orElseThrow(() -> TenantExceptions.notFound("Parceiro"));
        var all = productRepository.findAllByTenantIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            t.getId(), ProductStatus.ACTIVE
        );
        return all.stream()
            .limit(Math.min(limit, 200))
            .map(this::toPublicProduct)
            .toList();
    }

    private PublicPartnerView toPartnerSummary(Tenant t) {
        List<PhysicalStore> stores = physicalStoreRepository
            .findAllByTenantIdAndIsActiveTrueAndDeletedAtIsNullOrderByIsMainDescNameAsc(t.getId());
        long activeProducts = productRepository.countByTenantIdAndStatusAndDeletedAtIsNull(
            t.getId(), ProductStatus.ACTIVE
        );
        Set<String> realms = new HashSet<>();
        stores.forEach(s -> realms.add(s.getName()));
        return new PublicPartnerView(
            t.getId().toString(), t.getSlug(), t.getTradeName(), t.getDescription(),
            t.getLogoUrl(), t.getBannerUrl(), t.getWebsite(),
            stores.stream().map(this::toStoreSummary).toList(),
            (int) activeProducts,
            new ArrayList<>(realms)
        );
    }

    private PublicTenantView toTenantView(Tenant t) {
        List<PhysicalStore> stores = physicalStoreRepository
            .findAllByTenantIdAndIsActiveTrueAndDeletedAtIsNullOrderByIsMainDescNameAsc(t.getId());
        long activeProducts = productRepository.countByTenantIdAndStatusAndDeletedAtIsNull(
            t.getId(), ProductStatus.ACTIVE
        );
        Map<String, Object> theme = t.getTheme() != null ? t.getTheme() : new HashMap<>();
        return new PublicTenantView(
            t.getId().toString(), t.getSlug(), t.getTradeName(), t.getLegalName(),
            t.getDescription(), t.getLogoUrl(), t.getBannerUrl(),
            t.getPhone(), t.getWhatsapp(), t.getEmail(), t.getWebsite(),
            theme,
            stores.stream().map(this::toStoreView).toList(),
            (int) activeProducts,
            List.of()
        );
    }

    private PublicPartnerView.PublicStoreSummary toStoreSummary(PhysicalStore s) {
        Address a = s.getAddressId() != null ? addressRepository.findById(s.getAddressId()).orElse(null) : null;
        return new PublicPartnerView.PublicStoreSummary(
            s.getId().toString(), s.getName(), s.getSlug(),
            a != null ? a.getCity() : null, a != null ? a.getState() : null, s.isMain()
        );
    }

    private PublicTenantView.PublicStoreView toStoreView(PhysicalStore s) {
        Address a = s.getAddressId() != null ? addressRepository.findById(s.getAddressId()).orElse(null) : null;
        Double lat = null, lng = null;
        if (s.getLocation() != null) {
            lat = s.getLocation().getY();
            lng = s.getLocation().getX();
        }
        return new PublicTenantView.PublicStoreView(
            s.getId().toString(), s.getName(), s.getSlug(),
            s.getPhone(), s.getWhatsapp(), s.getEmail(),
            a != null ? a.getCity() : null, a != null ? a.getState() : null,
            lat, lng, s.isMain()
        );
    }

    private PublicProductView toPublicProduct(Product p) {
        var loc = productLocalizationRepository
            .findByProductIdAndCountryCode(p.getId(), "BR")
            .orElse(null);
        var store = physicalStoreRepository.findById(p.getPhysicalStoreId()).orElse(null);
        Address addr = store != null && store.getAddressId() != null
            ? addressRepository.findById(store.getAddressId()).orElse(null) : null;
        var category = categoryRepository.findById(p.getCategoryId()).orElse(null);
        var brand = p.getBrandId() != null ? brandRepository.findById(p.getBrandId()).orElse(null) : null;
        var model = p.getModelId() != null ? modelRepository.findById(p.getModelId()).orElse(null) : null;
        List<ProductImage> images = productImageRepository
            .findAllByProductIdOrderBySortOrderAscCreatedAtAsc(p.getId());

        BigDecimal price = loc != null
            ? BigDecimal.valueOf(loc.getPriceCents(), 2)
            : BigDecimal.ZERO;

        return new PublicProductView(
            p.getId(),
            loc != null ? loc.getTitle() : null,
            loc != null ? loc.getDescription() : null,
            price,
            loc != null ? loc.getCurrency() : "BRL",
            p.getRealm().name(),
            p.getYearModel() != null ? p.getYearModel().intValue() : null,
            p.getYearBuild() != null ? p.getYearBuild().intValue() : null,
            p.getMileageKm(), p.getFuel(), p.getTransmission(),
            brand != null ? brand.getName() : null,
            model != null ? model.getName() : null,
            category != null ? category.getName() : null,
            store != null ? store.getId() : null,
            store != null ? store.getName() : null,
            addr != null ? addr.getCity() : null,
            addr != null ? addr.getState() : null,
            p.getAttributes(),
            images.stream().map(i -> new PublicProductView.PublicProductImage(
                i.getId().toString(), i.getPublicUrl(), i.isCover()
            )).toList(),
            p.getCreatedAt() != null ? p.getCreatedAt().toString() : null
        );
    }
}
