package br.com.rizermarketplaces.core.marketplace.dto.tenant;

import br.com.rizermarketplaces.core.marketplace.model.Tenant;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "TenantPublicResponse", description = "Dados publicos de vitrine de seller")
public record TenantPublicResponse(
    @Schema(description = "Identificador do tenant", example = "2bc9ceb5-2a89-4d0f-a2bf-2f116a123456")
    UUID id,
    @Schema(description = "Slug publico da loja", example = "loja-prime-veiculos")
    String slug,
    @Schema(description = "Nome publico da loja", example = "Loja Prime Veiculos")
    String name,
    @Schema(description = "Indica se a vitrine e publica", example = "true")
    boolean isPublic
) {

    public static TenantPublicResponse from(Tenant tenant) {
        return new TenantPublicResponse(tenant.getId(), tenant.getSlug(), tenant.getName(), tenant.isPublic());
    }
}
