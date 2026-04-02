package br.com.rizermarketplaces.core.marketplace.controller;

/*
 * Controlador REST para operações de produtos em contexto regional.
 * Explicações em português sobre anotações do Spring, validações e uso de PathVariable/RequestParam.
 */

import br.com.rizermarketplaces.core.marketplace.context.RegionContextHolder;
import br.com.rizermarketplaces.core.marketplace.dto.product.CreateProductRequest;
import br.com.rizermarketplaces.core.marketplace.dto.product.ProductCreatedResponse;
import br.com.rizermarketplaces.core.marketplace.dto.product.ProductSearchResultResponse;
import br.com.rizermarketplaces.core.marketplace.model.ProductRealm;
import br.com.rizermarketplaces.core.marketplace.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

// @RestController: classe que contém endpoints REST.
@RestController
// @RequestMapping com path variable {countryCode}: todos os endpoints têm prefixo com o código do país.
@RequestMapping("/{countryCode}/products")
// @Validated: habilita validação de parâmetros (ex.: @Valid em request bodies).
@Validated
@Tag(name = "Products", description = "Regional product catalog endpoints with geospatial filtering")
public class ProductController {

    private final ProductService productService;

    // Injeção via construtor do serviço de produto.
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // @PostMapping: mapeia requisições HTTP POST para criar recurso.
    @PostMapping
    @Operation(summary = "Create product in regional context", description = "Creates one seller product with dynamic metadata validation by category and one localized projection for the country in the URL.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Product created", content = @Content(schema = @Schema(implementation = ProductCreatedResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Role/scope does not allow this seller operation")
    })
    public ResponseEntity<ProductCreatedResponse> create(
        // @PathVariable: captura o valor do {countryCode} informado na URL
        @PathVariable String countryCode,
        // @Valid + @RequestBody: desserializa o JSON do corpo e executa validação JSR-380 (Jakarta Validation)
        @Valid @RequestBody CreateProductRequest request
    ) {
        validateCountryContext(countryCode);
        ProductCreatedResponse response = productService.create(countryCode, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Endpoint de busca: usa @GetMapping com query params para latitude/longitude
    @GetMapping("/search")
    @Operation(summary = "Search products by radius", description = "Searches products in the same regional context applying geospatial distance and returning a spatial score.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Products found",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductSearchResultResponse.class)))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid query parameters")
    })
    public List<ProductSearchResultResponse> search(
        @PathVariable String countryCode,
        // @RequestParam: parametros de query extraídos da URL (ex: ?lat=...&lon=...)
        @Parameter(description = "Latitude in WGS84", example = "-28.448") @RequestParam Double lat,
        @Parameter(description = "Longitude in WGS84", example = "-52.203") @RequestParam Double lon,
        @Parameter(description = "Search radius in km", example = "50") @RequestParam(required = false) Double radiusKm,
        @Parameter(description = "Optional realm filter", example = "VEHICLES") @RequestParam(required = false) ProductRealm realm,
        @Parameter(description = "Max number of items", example = "30") @RequestParam(required = false) Integer limit
    ) {
        validateCountryContext(countryCode);
        return productService.searchNearby(countryCode, lat, lon, radiusKm, realm, limit);
    }

    // Valida o formato do countryCode (ISO-2) e compara com o contexto armazenado no filtro de região.
    private void validateCountryContext(String countryCode) {
        String normalized = countryCode.toUpperCase(Locale.ROOT);
        if (!normalized.matches("^[A-Z]{2}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "countryCode must use ISO-2 format");
        }

        String fromFilter = RegionContextHolder.getCountryCode();
        if (fromFilter != null && !normalized.equals(fromFilter)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "countryCode mismatch with request context");
        }
    }
}
