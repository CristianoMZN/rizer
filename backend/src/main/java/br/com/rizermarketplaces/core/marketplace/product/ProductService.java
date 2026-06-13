package br.com.rizermarketplaces.core.marketplace.product;

import br.com.rizermarketplaces.core.marketplace.dto.CreateProductRequest;
import br.com.rizermarketplaces.core.marketplace.dto.ProductView;
import br.com.rizermarketplaces.core.marketplace.dto.UpdateProductRequest;
import br.com.rizermarketplaces.core.marketplace.model.AttributeSchema;
import br.com.rizermarketplaces.core.marketplace.model.Category;
import br.com.rizermarketplaces.core.marketplace.model.PhysicalStore;
import br.com.rizermarketplaces.core.marketplace.model.Product;
import br.com.rizermarketplaces.core.marketplace.model.ProductImage;
import br.com.rizermarketplaces.core.marketplace.model.ProductLocalization;
import br.com.rizermarketplaces.core.marketplace.model.ProductLocationSource;
import br.com.rizermarketplaces.core.marketplace.model.ProductStatus;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.model.VehicleBrand;
import br.com.rizermarketplaces.core.marketplace.model.VehicleModel;
import br.com.rizermarketplaces.core.marketplace.repository.CategoryRepository;
import br.com.rizermarketplaces.core.marketplace.repository.PhysicalStoreRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductImageRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductLocalizationRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.repository.VehicleBrandRepository;
import br.com.rizermarketplaces.core.marketplace.repository.VehicleModelRepository;
import br.com.rizermarketplaces.core.marketplace.rules.DynamicAttributeValidationService;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductLocalizationRepository localizationRepository;
    private final ProductImageRepository imageRepository;
    private final CategoryRepository categoryRepository;
    private final VehicleBrandRepository brandRepository;
    private final VehicleModelRepository modelRepository;
    private final PhysicalStoreRepository physicalStoreRepository;
    private final TenantRepository tenantRepository;
    private final DynamicAttributeValidationService validator;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public ProductService(
        ProductRepository productRepository,
        ProductLocalizationRepository localizationRepository,
        ProductImageRepository imageRepository,
        CategoryRepository categoryRepository,
        VehicleBrandRepository brandRepository,
        VehicleModelRepository modelRepository,
        PhysicalStoreRepository physicalStoreRepository,
        TenantRepository tenantRepository,
        DynamicAttributeValidationService validator
    ) {
        this.productRepository = productRepository;
        this.localizationRepository = localizationRepository;
        this.imageRepository = imageRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.modelRepository = modelRepository;
        this.physicalStoreRepository = physicalStoreRepository;
        this.tenantRepository = tenantRepository;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<ProductView> listByTenant(UUID tenantId) {
        return productRepository.findAllByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId)
            .stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public ProductView get(UUID tenantId, UUID productId) {
        Product p = mustFind(tenantId, productId);
        return toView(p);
    }

    @Transactional
    public ProductView create(UUID tenantId, CreateProductRequest req, UUID actorId) {
        // Validações de integridade
        PhysicalStore store = physicalStoreRepository
            .findByIdAndTenantIdAndDeletedAtIsNull(req.physicalStoreId(), tenantId)
            .orElseThrow(() -> TenantExceptions.badRequest("Loja inválida para este tenant"));

        Category category = categoryRepository.findById(req.categoryId())
            .orElseThrow(() -> TenantExceptions.badRequest("Categoria inválida"));
        if (req.brandId() != null) {
            VehicleBrand b = brandRepository.findById(req.brandId())
                .orElseThrow(() -> TenantExceptions.badRequest("Marca inválida"));
            if (!b.getVehicleType().equalsIgnoreCase(category.getRealm().name())) {
                throw TenantExceptions.badRequest("Marca incompatível com a categoria");
            }
        }
        if (req.modelId() != null && req.brandId() != null) {
            VehicleModel m = modelRepository.findById(req.modelId())
                .orElseThrow(() -> TenantExceptions.badRequest("Modelo inválido"));
            if (!m.getBrandId().equals(req.brandId())) {
                throw TenantExceptions.badRequest("Modelo não pertence à marca selecionada");
            }
        }

        Map<String, Object> attrs = req.attributes() != null ? new HashMap<>(req.attributes()) : new HashMap<>();
        if (req.yearModel() != null) attrs.putIfAbsent("year_model", req.yearModel());
        if (req.yearBuild() != null) attrs.putIfAbsent("year_build", req.yearBuild());
        if (req.mileageKm() != null) attrs.putIfAbsent("mileage_km", req.mileageKm());
        if (req.fuel() != null) attrs.putIfAbsent("fuel", req.fuel());
        if (req.transmission() != null) attrs.putIfAbsent("transmission", req.transmission());

        AttributeSchema schema = validator.findSchema(
            store.getLocation() != null ? "BR" : "BR",
            category.getRealm().name(),
            category.getPath()
        ).orElse(null);
        if (schema != null) {
            List<String> errors = validator.validate(schema, attrs);
            if (!errors.isEmpty()) {
                throw TenantExceptions.badRequest("Atributos inválidos: " + String.join("; ", errors));
            }
        }

        Product p = new Product();
        p.setTenantId(tenantId);
        p.setPhysicalStoreId(req.physicalStoreId());
        p.setCategoryId(req.categoryId());
        p.setBrandId(req.brandId());
        p.setModelId(req.modelId());
        p.setRealm(category.getRealm());
        if (req.yearModel() != null) p.setYearModel(req.yearModel().shortValue());
        if (req.yearBuild() != null) p.setYearBuild(req.yearBuild().shortValue());
        if (req.mileageKm() != null) p.setMileageKm(req.mileageKm());
        if (req.fuel() != null) p.setFuel(req.fuel());
        if (req.transmission() != null) p.setTransmission(req.transmission());
        p.setAttributes(attrs);
        p.setStatus(Boolean.TRUE.equals(req.publish()) ? ProductStatus.ACTIVE : ProductStatus.DRAFT);
        p.setCreatedByUserId(actorId);
        p = productRepository.save(p);

        // Localization (pt-BR)
        String cc = (req.countryCode() == null || req.countryCode().isBlank()) ? "BR" : req.countryCode().toUpperCase();
        ProductLocalization loc = new ProductLocalization();
        loc.setProductId(p.getId());
        loc.setCountryCode(cc);
        loc.setTitle(req.title());
        loc.setDescription(req.description());
        loc.setPriceCents(req.price().setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact());
        loc.setCurrency(req.currency().toUpperCase());
        loc.setLocationSource(ProductLocationSource.STORE);
        // Location null = herda da loja (compute on read)
        localizationRepository.save(loc);

        return toView(p);
    }

    @Transactional
    public ProductView update(UUID tenantId, UUID productId, UpdateProductRequest req) {
        Product p = mustFind(tenantId, productId);
        if (req.physicalStoreId() != null) {
            physicalStoreRepository.findByIdAndTenantIdAndDeletedAtIsNull(req.physicalStoreId(), tenantId)
                .orElseThrow(() -> TenantExceptions.badRequest("Loja inválida"));
            p.setPhysicalStoreId(req.physicalStoreId());
        }
        if (req.categoryId() != null) {
            Category c = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> TenantExceptions.badRequest("Categoria inválida"));
            p.setCategoryId(c.getId());
            p.setRealm(c.getRealm());
        }
        if (req.brandId() != null) p.setBrandId(req.brandId());
        if (req.modelId() != null) p.setModelId(req.modelId());
        if (req.yearModel() != null) p.setYearModel(req.yearModel().shortValue());
        if (req.yearBuild() != null) p.setYearBuild(req.yearBuild().shortValue());
        if (req.mileageKm() != null) p.setMileageKm(req.mileageKm());
        if (req.fuel() != null) p.setFuel(req.fuel());
        if (req.transmission() != null) p.setTransmission(req.transmission());
        if (req.attributes() != null) p.setAttributes(new HashMap<>(req.attributes()));
        if (req.status() != null) {
            try {
                p.setStatus(ProductStatus.valueOf(req.status()));
            } catch (IllegalArgumentException e) {
                throw TenantExceptions.badRequest("Status inválido: " + req.status());
            }
        }
        ProductLocalization loc = localizationRepository
            .findByProductIdAndCountryCode(p.getId(), "BR")
            .orElseGet(() -> {
                ProductLocalization l = new ProductLocalization();
                l.setProductId(p.getId());
                l.setCountryCode("BR");
                return l;
            });
        if (req.title() != null) loc.setTitle(req.title());
        if (req.description() != null) loc.setDescription(req.description());
        if (req.price() != null) {
            loc.setPriceCents(req.price().setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact());
        }
        if (req.currency() != null) loc.setCurrency(req.currency().toUpperCase());
        localizationRepository.save(loc);
        productRepository.save(p);
        return toView(p);
    }

    @Transactional
    public void softDelete(UUID tenantId, UUID productId) {
        Product p = mustFind(tenantId, productId);
        p.setDeletedAt(java.time.OffsetDateTime.now());
        p.setStatus(ProductStatus.ARCHIVED);
        productRepository.save(p);
    }

    private Product mustFind(UUID tenantId, UUID productId) {
        return productRepository.findByIdAndTenantIdAndDeletedAtIsNull(productId, tenantId)
            .orElseThrow(() -> TenantExceptions.notFound("Produto"));
    }

    public ProductView toView(Product p) {
        PhysicalStore store = physicalStoreRepository.findById(p.getPhysicalStoreId()).orElse(null);
        Category category = categoryRepository.findById(p.getCategoryId()).orElse(null);
        VehicleBrand brand = p.getBrandId() != null ? brandRepository.findById(p.getBrandId()).orElse(null) : null;
        VehicleModel model = p.getModelId() != null ? modelRepository.findById(p.getModelId()).orElse(null) : null;
        ProductLocalization loc = localizationRepository
            .findByProductIdAndCountryCode(p.getId(), "BR")
            .orElse(null);
        List<ProductImage> images = imageRepository.findAllByProductIdOrderBySortOrderAscCreatedAtAsc(p.getId());

        Double lat = null, lng = null;
        if (loc != null && loc.getLocation() != null) {
            lat = loc.getLocation().getY();
            lng = loc.getLocation().getX();
        } else if (store != null && store.getLocation() != null) {
            lat = store.getLocation().getY();
            lng = store.getLocation().getX();
        }

        BigDecimal price = loc != null
            ? BigDecimal.valueOf(loc.getPriceCents(), 2)
            : BigDecimal.ZERO;

        return new ProductView(
            p.getId(), p.getTenantId(), p.getPhysicalStoreId(),
            store != null ? store.getName() : null,
            p.getCategoryId(), category != null ? category.getName() : null,
            p.getBrandId(), brand != null ? brand.getName() : null,
            p.getModelId(), model != null ? model.getName() : null,
            p.getRealm().name(),
            p.getYearModel() != null ? p.getYearModel().intValue() : null,
            p.getYearBuild() != null ? p.getYearBuild().intValue() : null,
            p.getMileageKm(), p.getFuel(), p.getTransmission(),
            p.getAttributes(),
            p.getStatus().name(),
            loc != null ? loc.getTitle() : null,
            loc != null ? loc.getDescription() : null,
            price, loc != null ? loc.getCurrency() : "BRL",
            lat, lng,
            loc != null ? loc.getLocationSource().name() : ProductLocationSource.STORE.name(),
            images.stream().map(i -> new ProductView.ProductImageView(
                i.getId(), i.getPublicUrl(), i.getContentType(), i.getSortOrder(), i.isCover()
            )).toList(),
            p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
