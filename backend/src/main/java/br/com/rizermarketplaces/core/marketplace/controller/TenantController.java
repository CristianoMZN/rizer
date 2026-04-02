package br.com.rizermarketplaces.core.marketplace.controller;

import br.com.rizermarketplaces.core.marketplace.dto.tenant.TenantPublicResponse;
import br.com.rizermarketplaces.core.marketplace.repository.TenantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tenants")
@Tag(name = "Tenants", description = "Public storefront endpoints")
public class TenantController {

    private final TenantRepository tenantRepository;

    public TenantController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @GetMapping("/public")
    @Operation(summary = "List public storefronts", description = "Returns public tenant storefront metadata for visitors.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Public storefront list",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TenantPublicResponse.class)))
        )
    })
    public List<TenantPublicResponse> listPublicStorefronts() {
        return tenantRepository.findAllByIsPublicTrueAndStatusOrderByNameAsc("ACTIVE")
            .stream()
            .map(TenantPublicResponse::from)
            .toList();
    }
}
