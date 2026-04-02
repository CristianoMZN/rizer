package br.com.rizermarketplaces.core.marketplace.dto.product;

import br.com.rizermarketplaces.core.marketplace.model.ProductRealm;
import br.com.rizermarketplaces.core.marketplace.model.UnitSystem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

// DTO retornado após criar um produto regionalmente.
@Schema(name = "ProductCreatedResponse", description = "Retorno do cadastro regional de anuncio")
public record ProductCreatedResponse(
    @Schema(description = "UUID publico do produto", example = "d6f95f20-c6a2-4f0e-aeb8-0360a0f9d4e2")
    UUID productUuid,
    @Schema(description = "Realm de negocio", example = "VEHICLES")
    ProductRealm realm,
    @Schema(description = "Pais do contexto regional", example = "BR")
    String countryCode,
    @Schema(description = "Preco registrado", example = "85000.00")
    BigDecimal price,
    @Schema(description = "Moeda do preco", example = "BRL")
    String currency,
    @Schema(description = "Sistema de medida aplicado", example = "METRIC")
    UnitSystem unitSystem
) {
}
