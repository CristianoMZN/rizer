package br.com.rizermarketplaces.core.marketplace.service;

import br.com.rizermarketplaces.core.marketplace.dto.product.CreateProductRequest;
import br.com.rizermarketplaces.core.marketplace.dto.product.ProductCreatedResponse;
import br.com.rizermarketplaces.core.marketplace.dto.product.ProductSearchResultResponse;
import br.com.rizermarketplaces.core.marketplace.model.Product;
import br.com.rizermarketplaces.core.marketplace.model.ProductAttributeValue;
import br.com.rizermarketplaces.core.marketplace.model.ProductStatus;
import br.com.rizermarketplaces.core.marketplace.model.ProductLocalization;
import br.com.rizermarketplaces.core.marketplace.model.ProductRealm;
import br.com.rizermarketplaces.core.marketplace.model.SellerUserRole;
import br.com.rizermarketplaces.core.marketplace.model.SystemRole;
import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import br.com.rizermarketplaces.core.marketplace.repository.NearbyProductProjection;
import br.com.rizermarketplaces.core.marketplace.repository.ProductAttributeValueRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductLocalizationRepository;
import br.com.rizermarketplaces.core.marketplace.repository.ProductRepository;
import br.com.rizermarketplaces.core.marketplace.repository.SellerUserRepository;
import br.com.rizermarketplaces.core.marketplace.repository.SubsubcategoryRepository;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import br.com.rizermarketplaces.core.marketplace.rules.MeasurementService;
import br.com.rizermarketplaces.core.marketplace.rules.MeasurementServiceFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

// Serviço responsável por operações de negócio relacionadas a produtos:
// - criação de produto e projeção regional
// - busca por proximidade (utilizando consulta PostGIS via repository)
@Service
public class ProductService {

    // GeometryFactory para criar pontos geográficos com SRID 4326 (WGS84)
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ProductRepository productRepository;
    private final ProductLocalizationRepository productLocalizationRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final TenantRepository tenantRepository;
    private final SellerUserRepository sellerUserRepository;
    private final SubsubcategoryRepository subsubcategoryRepository;
    private final CurrentUserContextService currentUserContextService;
    private final DynamicAttributeValidationService dynamicAttributeValidationService;
    private final MeasurementServiceFactory measurementServiceFactory;

    public ProductService(
        ProductRepository productRepository,
        ProductLocalizationRepository productLocalizationRepository,
        ProductAttributeValueRepository productAttributeValueRepository,
        TenantRepository tenantRepository,
        SellerUserRepository sellerUserRepository,
        SubsubcategoryRepository subsubcategoryRepository,
        CurrentUserContextService currentUserContextService,
        DynamicAttributeValidationService dynamicAttributeValidationService,
        MeasurementServiceFactory measurementServiceFactory
    ) {
        this.productRepository = productRepository;
        this.productLocalizationRepository = productLocalizationRepository;
        this.productAttributeValueRepository = productAttributeValueRepository;
        this.tenantRepository = tenantRepository;
        this.sellerUserRepository = sellerUserRepository;
        this.subsubcategoryRepository = subsubcategoryRepository;
        this.currentUserContextService = currentUserContextService;
        this.dynamicAttributeValidationService = dynamicAttributeValidationService;
        this.measurementServiceFactory = measurementServiceFactory;
    }

    // @Transactional: inicia uma transação para as operações de escrita
    @Transactional
    public ProductCreatedResponse create(String countryCode, CreateProductRequest request) {
        String normalizedCountryCode = countryCode.toUpperCase(Locale.ROOT);
        CurrentUserContextService.AuthenticatedUser authenticatedUser = currentUserContextService.requireAuthenticatedUser();
        authorizeCreation(authenticatedUser, request.sellerId());

        Tenant tenant = tenantRepository.findBySellerId(request.sellerId())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Tenant not found for seller"));

        Long subsubcategoryId = subsubcategoryRepository.findBySlug(request.subsubcategorySlug())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Subsubcategory not found"))
            .getId();

        List<DynamicAttributeValidationService.ResolvedAttributeValue> resolvedAttributes =
            dynamicAttributeValidationService.validate(subsubcategoryId, request.attributes());

        // Seleciona um serviço de regra de medidas baseado no país (ex: BR x US)
        MeasurementService measurementService = measurementServiceFactory.getForCountry(normalizedCountryCode);

        // Cria a entidade Product com atributos normalizados
        Product product = new Product();
        product.setMerchantId(request.merchantId() != null ? request.merchantId() : authenticatedUser.userId());
        product.setTenantId(tenant.getId());
        product.setSellerId(request.sellerId());
        product.setSubsubcategoryId(subsubcategoryId);
        product.setCreatedByUserId(authenticatedUser.userId());
        product.setStatus(request.status() != null ? request.status() : ProductStatus.ACTIVE);
        product.setRealm(request.realm());
        product.setCategoryPath(request.categoryPath());
        product.setAttributes(normalizeAttributes(measurementService, request.attributes()));
        product = productRepository.save(product);

        saveAttributeValues(product, resolvedAttributes);

        // Cria a projeção regional (ProductLocalization) com preço, moeda e localização
        ProductLocalization localization = new ProductLocalization();
        localization.setProduct(product);
        localization.setCountryCode(normalizedCountryCode);
        localization.setTitle(request.title());
        localization.setDescription(request.description());
        localization.setPrice(request.price());
        localization.setCurrency(request.currency().toUpperCase(Locale.ROOT));
        localization.setUnitSystem(request.unitSystem() != null ? request.unitSystem() : measurementService.unitSystem());
        localization.setLocation(toPoint(request.location().lat(), request.location().lon()));
        productLocalizationRepository.save(localization);

        return new ProductCreatedResponse(
            product.getUuid(),
            product.getRealm(),
            localization.getCountryCode(),
            localization.getPrice(),
            localization.getCurrency(),
            localization.getUnitSystem()
        );
    }

    // Busca por proximidade: somente leitura, por isso readOnly=true
    @Transactional(readOnly = true)
    public List<ProductSearchResultResponse> searchNearby(
        String countryCode,
        Double lat,
        Double lon,
        Double radiusKm,
        ProductRealm realm,
        Integer limit
    ) {
        double normalizedRadiusKm = radiusKm == null ? 50d : radiusKm;
        int normalizedLimit = limit == null ? 30 : Math.min(limit, 100);

        List<NearbyProductProjection> rows = productLocalizationRepository.findNearbyByCountryAndRealm(
            countryCode.toUpperCase(Locale.ROOT),
            realm != null ? realm.name() : null,
            lat,
            lon,
            normalizedRadiusKm * 1000d,
            normalizedLimit
        );

        return rows.stream().map(row -> {
            double distanceKm = row.getDistanceKm() == null ? 0d : row.getDistanceKm();
            return new ProductSearchResultResponse(
                row.getProductUuid(),
                row.getRealm(),
                row.getCountryCode(),
                row.getPrice(),
                row.getCurrency(),
                distanceKm,
                spatialWeight(distanceKm)
            );
        }).toList();
    }

    // Normaliza chaves de atributos usando o serviço de medidas (ex: transformar 'km' para 'quilometragem')
    private JsonNode normalizeAttributes(MeasurementService measurementService, Map<String, Object> attributes) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        attributes.forEach((key, value) -> normalized.put(measurementService.normalizeAttributeKey(key), value));
        return OBJECT_MAPPER.valueToTree(normalized);
    }

    private void saveAttributeValues(
        Product product,
        List<DynamicAttributeValidationService.ResolvedAttributeValue> resolvedAttributes
    ) {
        List<ProductAttributeValue> rows = resolvedAttributes.stream().map(value -> {
            ProductAttributeValue row = new ProductAttributeValue();
            row.setProduct(product);
            row.setAttributeDefinitionId(value.attributeDefinitionId());
            row.setValueText(value.textValue());
            row.setValueNumber(value.numberValue());
            row.setValueBoolean(value.booleanValue());
            row.setValueDate(value.dateValue());
            row.setValueJson(value.jsonValue());
            return row;
        }).toList();

        productAttributeValueRepository.saveAll(rows);
    }

    private void authorizeCreation(CurrentUserContextService.AuthenticatedUser authenticatedUser, Long sellerId) {
        SystemRole role = authenticatedUser.systemRole();
        if (role == SystemRole.ADMIN || role == SystemRole.MANAGER) {
            return;
        }

        if (role == SystemRole.USER) {
            throw new ResponseStatusException(FORBIDDEN, "User role does not allow seller operations");
        }

        boolean hasSellerAccess = sellerUserRepository.existsBySellerIdAndUserIdAndActiveTrueAndRoleIn(
            sellerId,
            authenticatedUser.userId(),
            Set.of(SellerUserRole.SELLER, SellerUserRole.SELLER_EMPLOYEE)
        );

        if (!hasSellerAccess) {
            throw new ResponseStatusException(FORBIDDEN, "Authenticated user has no access to this seller");
        }
    }

    // Converte lat/lon para Point JTS com SRID 4326 para armazenamento PostGIS
    private Point toPoint(Double lat, Double lon) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
        point.setSRID(4326);
        return point;
    }

    // Função de peso espacial usada para ranking de resultados
    private double spatialWeight(double distanceKm) {
        if (distanceKm <= 50d) {
            return 2.0;
        }
        if (distanceKm <= 200d) {
            return 1.5;
        }
        return 1.0;
    }
}
