package br.com.rizermarketplaces.core.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CreateProductRequest(
    @NotNull UUID physicalStoreId,
    @NotNull UUID categoryId,
    Integer brandId,
    Integer modelId,
    @NotBlank @Size(max = 200) String title,
    @Size(max = 8000) String description,
    @NotNull @PositiveOrZero BigDecimal price,
    @NotBlank @Size(min = 3, max = 3) String currency,
    String countryCode,                       // default BR
    // Campos de veículo (opcional conforme schema)
    Integer yearModel,
    Integer yearBuild,
    Integer mileageKm,
    String fuel,
    String transmission,
    Map<String, Object> attributes,           // validados pelo schema
    Boolean publish,                          // true = ACTIVE; false/null = DRAFT
    UUID sellerUserId,                        // vendedor responsável (opcional)
    Double latitude,                          // geolocalização custom (opcional)
    Double longitude
) {}
