package br.com.rizermarketplaces.core.marketplace.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

// DTO retornado em cada item da busca por proximidade. Contém preço local e distância calculada.
@Schema(name = "ProductSearchResultResponse", description = "Item de resultado da busca por proximidade")
public record ProductSearchResultResponse(
    @Schema(description = "UUID do produto", example = "d6f95f20-c6a2-4f0e-aeb8-0360a0f9d4e2")
    UUID productUuid,
    @Schema(description = "Realm do produto", example = "VEHICLES")
    String realm,
    @Schema(description = "Pais da localizacao", example = "BR")
    String countryCode,
    @Schema(description = "Preco local", example = "85000.00")
    BigDecimal price,
    @Schema(description = "Moeda local", example = "BRL")
    String currency,
    @Schema(description = "Distancia em km do ponto de busca", example = "12.35")
    double distanceKm,
    @Schema(description = "Peso espacial calculado pela regra de proximidade", example = "2.0")
    double spatialWeight
) {
}
