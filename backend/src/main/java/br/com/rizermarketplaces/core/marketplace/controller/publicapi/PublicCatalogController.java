package br.com.rizermarketplaces.core.marketplace.controller.publicapi;

import br.com.rizermarketplaces.core.marketplace.catalog.CatalogService;
import br.com.rizermarketplaces.core.marketplace.dto.CategoryView;
import br.com.rizermarketplaces.core.marketplace.dto.PublicProductView;
import br.com.rizermarketplaces.core.marketplace.dto.VehicleBrandView;
import br.com.rizermarketplaces.core.marketplace.dto.VehicleModelView;
import br.com.rizermarketplaces.core.marketplace.model.VehicleRealm;
import br.com.rizermarketplaces.core.marketplace.partner.PublicPartnerService;
import br.com.rizermarketplaces.core.marketplace.repository.ProductRepository;
import br.com.rizermarketplaces.core.marketplace.tenant.TenantExceptions;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/{countryCode}/public")
@Tag(name = "Público · Catálogo", description = "Endpoints públicos para navegação de veículos")
public class PublicCatalogController {

    private final CatalogService catalogService;
    private final PublicPartnerService partnerService;
    private final ProductRepository productRepository;

    public PublicCatalogController(
        CatalogService catalogService,
        PublicPartnerService partnerService,
        ProductRepository productRepository
    ) {
        this.catalogService = catalogService;
        this.partnerService = partnerService;
        this.productRepository = productRepository;
    }

    @GetMapping("/categories")
    public List<CategoryView> listCategories(@PathVariable String countryCode) {
        return catalogService.listRoots(countryCode.toUpperCase());
    }

    @GetMapping("/categories/{realm}/subtypes")
    public List<CategoryView> listSubtypes(
        @PathVariable String countryCode,
        @PathVariable String realm
    ) {
        try { VehicleRealm.valueOf(realm.toUpperCase()); } catch (IllegalArgumentException e) {
            throw TenantExceptions.badRequest("Reino inválido: " + realm);
        }
        return catalogService.listChildren(countryCode.toUpperCase(), realm);
    }

    @GetMapping("/brands")
    public List<VehicleBrandView> listBrands(@RequestParam String realm) {
        return catalogService.listBrands(realm);
    }

    @GetMapping("/brands/{brandId}/models")
    public List<VehicleModelView> listModels(@PathVariable Integer brandId) {
        return catalogService.listModels(brandId);
    }

    @GetMapping("/products")
    public List<PublicProductView> searchProducts(
        @PathVariable String countryCode,
        @RequestParam(required = false) UUID tenantId,
        @RequestParam(required = false) String realm,
        @RequestParam(required = false) UUID categoryId,
        @RequestParam(required = false) Integer brandId,
        @RequestParam(required = false) Integer minYear,
        @RequestParam(required = false) Integer maxYear,
        @RequestParam(required = false) String fuel,
        @RequestParam(required = false) String transmission,
        @RequestParam(required = false) String transmissionDetail,
        @RequestParam(required = false) String color,
        @RequestParam(required = false) String bodyType,
        @RequestParam(required = false) String drivetrain,
        @RequestParam(required = false) String steering,
        @RequestParam(required = false) String condition,
        @RequestParam(required = false) String engine,
        @RequestParam(required = false) Integer cylinders,
        @RequestParam(required = false) Boolean armored,
        @RequestParam(required = false) Boolean abs,
        @RequestParam(defaultValue = "30") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        var products = productRepository.search(
            tenantId == null ? null : tenantId.toString(),
            realm,
            categoryId == null ? null : categoryId.toString(),
            brandId,
            minYear == null ? null : minYear.shortValue(),
            maxYear == null ? null : maxYear.shortValue(),
            fuel,
            transmission,
            transmissionDetail,
            color,
            bodyType,
            drivetrain,
            steering,
            condition,
            engine,
            cylinders,
            armored,
            abs,
            Math.min(limit, 100),
            Math.max(offset, 0)
        );
        return products.stream().map(partnerService::toPublicProduct).toList();
    }
}
