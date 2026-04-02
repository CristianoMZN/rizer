package br.com.rizermarketplaces.core.marketplace.dto.product;

import br.com.rizermarketplaces.core.marketplace.model.ProductRealm;
import br.com.rizermarketplaces.core.marketplace.model.ProductStatus;
import br.com.rizermarketplaces.core.marketplace.model.UnitSystem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

// DTO de request para criação de produto. Inclui validações e descrição para OpenAPI.
@Schema(name = "CreateProductRequest", description = "Payload de cadastro de anuncio por contexto regional")
public record CreateProductRequest(
    @Schema(description = "Id legada do merchant. Quando omitido, usa o usuario autenticado", example = "b0c8bb63-8d2f-4f6d-9b46-2d13d6ab7c0d")
    UUID merchantId,
    @Schema(description = "Id do seller/tenant proprietario do anuncio", example = "1")
    @NotNull Long sellerId,
    @Schema(description = "Dominio de negocio do produto", example = "VEHICLES")
    @NotNull ProductRealm realm,
    @Schema(description = "Caminho hierarquico da categoria em formato ltree", example = "veiculos.passeio.suvs")
    @NotBlank String categoryPath,
    @Schema(description = "Slug da categoria final usada para validar metadados dinamicos", example = "suv")
    @NotBlank String subsubcategorySlug,
    @Schema(description = "Atributos dinamicos por realm e pais", example = "{\"quilometragem\":50000,\"combustivel\":\"Flex\"}")
    // Map<String, Object> usado para atributos dinâmicos (modelo flexível, sem esquema fixo)
    @NotEmpty Map<String, Object> attributes,
    @Schema(description = "Titulo exibido no catalogo regional", example = "SUV seminovo")
    @NotBlank @Size(max = 200) String title,
    @Schema(description = "Descricao detalhada do anuncio", example = "Unico dono, revisoes em dia")
    @Size(max = 2000) String description,
    @Schema(description = "Preco do anuncio na moeda local", example = "85000.00")
    @NotNull @DecimalMin("0.01") BigDecimal price,
    @Schema(description = "Codigo da moeda em ISO-4217", example = "BRL")
    @NotBlank @Size(min = 3, max = 3) String currency,
    @Schema(description = "Status inicial do anuncio", example = "ACTIVE")
    ProductStatus status,
    @Schema(description = "Sistema de medida do anuncio. Quando omitido, segue a regra do pais", example = "METRIC")
    UnitSystem unitSystem,
    @Schema(description = "Coordenadas geograficas WGS84")
    @NotNull @Valid Location location
) {

    @Schema(name = "CreateProductLocation", description = "Latitude e longitude em WGS84")
    public record Location(
        @Schema(description = "Latitude", example = "-28.448")
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
        @Schema(description = "Longitude", example = "-52.203")
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lon
    ) {
    }
}
